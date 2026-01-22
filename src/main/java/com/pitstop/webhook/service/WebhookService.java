package com.pitstop.webhook.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pitstop.saas.service.FeatureFlagService;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.webhook.domain.*;
import com.pitstop.webhook.dto.*;
import com.pitstop.webhook.repository.WebhookConfigRepository;
import com.pitstop.webhook.repository.WebhookLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Serviço para gerenciamento e disparo de webhooks.
 *
 * @author PitStop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private static final String FEATURE_CODE = "WEBHOOK_NOTIFICATIONS";

    private final WebhookConfigRepository configRepository;
    private final WebhookLogRepository logRepository;
    private final FeatureFlagService featureFlagService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    // ========== CRUD de Configuração ==========

    /**
     * Lista webhooks da oficina atual.
     */
    @Transactional(readOnly = true)
    public Page<WebhookConfigDTO> listar(Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return configRepository.findByOficinaId(oficinaId, pageable)
            .map(WebhookConfigDTO::fromEntity);
    }

    /**
     * Busca webhook por ID.
     */
    @Transactional(readOnly = true)
    public WebhookConfigDTO buscarPorId(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        WebhookConfig config = configRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new EntityNotFoundException("Webhook não encontrado: " + id));
        return WebhookConfigDTO.fromEntity(config);
    }

    /**
     * Cria novo webhook.
     */
    @Transactional
    public WebhookConfigDTO criar(WebhookConfigCreateDTO dto) {
        UUID oficinaId = TenantContext.getTenantId();

        // Validar URL única por oficina
        if (configRepository.existsByOficinaIdAndUrl(oficinaId, dto.url())) {
            throw new IllegalArgumentException("Já existe um webhook com esta URL");
        }

        WebhookConfig config = WebhookConfig.builder()
            .oficinaId(oficinaId)
            .nome(dto.nome())
            .descricao(dto.descricao())
            .url(dto.url())
            .secret(dto.secret())
            .headersJson(toJson(dto.headers()))
            .eventos(dto.eventos() != null ? new HashSet<>(dto.eventos()) : new HashSet<>())
            .maxTentativas(dto.maxTentativas())
            .timeoutSegundos(dto.timeoutSegundos())
            .ativo(true)
            .build();

        config = configRepository.save(config);
        log.info("Webhook criado: {} ({})", config.getNome(), config.getId());

        return WebhookConfigDTO.fromEntity(config);
    }

    /**
     * Atualiza webhook existente.
     */
    @Transactional
    public WebhookConfigDTO atualizar(UUID id, WebhookConfigUpdateDTO dto) {
        UUID oficinaId = TenantContext.getTenantId();
        WebhookConfig config = configRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new EntityNotFoundException("Webhook não encontrado: " + id));

        // Validar URL única se alterada
        if (dto.url() != null && !dto.url().equals(config.getUrl())) {
            if (configRepository.existsByOficinaIdAndUrl(oficinaId, dto.url())) {
                throw new IllegalArgumentException("Já existe um webhook com esta URL");
            }
            config.setUrl(dto.url());
        }

        if (dto.nome() != null) config.setNome(dto.nome());
        if (dto.descricao() != null) config.setDescricao(dto.descricao());
        if (dto.secret() != null) config.setSecret(dto.secret());
        if (Boolean.TRUE.equals(dto.removerSecret())) config.setSecret(null);
        if (dto.headers() != null) config.setHeadersJson(toJson(dto.headers()));
        if (dto.eventos() != null) config.setEventos(new HashSet<>(dto.eventos()));
        if (dto.maxTentativas() != null) config.setMaxTentativas(dto.maxTentativas());
        if (dto.timeoutSegundos() != null) config.setTimeoutSegundos(dto.timeoutSegundos());
        if (dto.ativo() != null) {
            config.setAtivo(dto.ativo());
            if (Boolean.TRUE.equals(dto.ativo())) {
                config.setFalhasConsecutivas(0); // Reset ao reativar
            }
        }

        config = configRepository.save(config);
        log.info("Webhook atualizado: {} ({})", config.getNome(), config.getId());

        return WebhookConfigDTO.fromEntity(config);
    }

    /**
     * Remove webhook.
     */
    @Transactional
    public void remover(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        WebhookConfig config = configRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new EntityNotFoundException("Webhook não encontrado: " + id));

        configRepository.delete(config);
        log.info("Webhook removido: {} ({})", config.getNome(), config.getId());
    }

    /**
     * Reativa webhook desativado por falhas.
     */
    @Transactional
    public WebhookConfigDTO reativar(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        WebhookConfig config = configRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new EntityNotFoundException("Webhook não encontrado: " + id));

        config.reativar();
        config = configRepository.save(config);
        log.info("Webhook reativado: {} ({})", config.getNome(), config.getId());

        return WebhookConfigDTO.fromEntity(config);
    }

    // ========== Disparo de Webhooks ==========

    /**
     * Dispara webhooks para um evento.
     * Método assíncrono - não bloqueia a thread chamadora.
     *
     * @param evento Tipo de evento
     * @param entidadeId ID da entidade relacionada
     * @param entidadeTipo Tipo da entidade (ex: "OrdemServico")
     * @param payload Dados do evento
     */
    @Async
    public void dispararEvento(TipoEventoWebhook evento, UUID entidadeId, String entidadeTipo, Object payload) {
        UUID oficinaId = TenantContext.getTenantId();

        // Verificar feature flag
        if (!featureFlagService.isEnabled(FEATURE_CODE, oficinaId)) {
            log.debug("Webhook desabilitado para oficina {}", oficinaId);
            return;
        }

        // Buscar webhooks ativos para este evento
        List<WebhookConfig> configs = configRepository.findActiveByOficinaIdAndEvento(oficinaId, evento);

        if (configs.isEmpty()) {
            log.debug("Nenhum webhook configurado para evento {} na oficina {}", evento, oficinaId);
            return;
        }

        log.info("Disparando {} webhook(s) para evento {} (entidade: {})", configs.size(), evento, entidadeId);

        for (WebhookConfig config : configs) {
            try {
                dispararWebhook(config, evento, entidadeId, entidadeTipo, payload);
            } catch (Exception e) {
                log.error("Erro ao disparar webhook {}: {}", config.getNome(), e.getMessage());
            }
        }
    }

    /**
     * Dispara um webhook específico.
     */
    private void dispararWebhook(WebhookConfig config, TipoEventoWebhook evento,
                                  UUID entidadeId, String entidadeTipo, Object payload) {
        long startTime = System.currentTimeMillis();

        // Montar payload completo
        Map<String, Object> webhookPayload = new LinkedHashMap<>();
        webhookPayload.put("evento", evento.name());
        webhookPayload.put("eventoNome", evento.getNome());
        webhookPayload.put("timestamp", LocalDateTime.now().toString());
        webhookPayload.put("entidadeId", entidadeId != null ? entidadeId.toString() : null);
        webhookPayload.put("entidadeTipo", entidadeTipo);
        webhookPayload.put("dados", payload);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(webhookPayload);
        } catch (Exception e) {
            log.error("Erro ao serializar payload do webhook: {}", e.getMessage());
            return;
        }

        // Criar log
        WebhookLog webhookLog = WebhookLog.builder()
            .webhookConfig(config)
            .oficinaId(config.getOficinaId())
            .evento(evento)
            .entidadeId(entidadeId)
            .entidadeTipo(entidadeTipo)
            .url(config.getUrl())
            .payload(payloadJson)
            .tentativa(1)
            .status(StatusWebhookLog.PENDENTE)
            .build();

        try {
            // Montar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Headers customizados
            Map<String, String> customHeaders = parseHeaders(config.getHeadersJson());
            customHeaders.forEach(headers::set);

            // Assinatura HMAC se configurada
            if (config.getSecret() != null && !config.getSecret().isBlank()) {
                String signature = generateHmacSignature(payloadJson, config.getSecret());
                headers.set("X-Webhook-Signature", signature);
                headers.set("X-Webhook-Timestamp", String.valueOf(System.currentTimeMillis()));
            }

            // Enviar requisição
            HttpEntity<String> entity = new HttpEntity<>(payloadJson, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                URI.create(config.getUrl()),
                HttpMethod.POST,
                entity,
                String.class
            );

            long tempoResposta = System.currentTimeMillis() - startTime;

            // Verificar sucesso
            if (response.getStatusCode().is2xxSuccessful()) {
                webhookLog.marcarSucesso(
                    response.getStatusCode().value(),
                    response.getBody(),
                    tempoResposta
                );
                config.registrarSucesso();
                log.info("Webhook {} enviado com sucesso ({} ms)", config.getNome(), tempoResposta);
            } else {
                webhookLog.marcarFalha(
                    response.getStatusCode().value(),
                    response.getBody(),
                    "HTTP " + response.getStatusCode().value(),
                    tempoResposta
                );
                webhookLog.agendarRetry(config.getMaxTentativas());
                boolean desativado = config.registrarFalha();
                if (desativado) {
                    log.warn("Webhook {} desativado após muitas falhas", config.getNome());
                }
            }

        } catch (Exception e) {
            long tempoResposta = System.currentTimeMillis() - startTime;
            webhookLog.marcarFalha(null, null, e.getMessage(), tempoResposta);
            webhookLog.agendarRetry(config.getMaxTentativas());
            boolean desativado = config.registrarFalha();
            if (desativado) {
                log.warn("Webhook {} desativado após muitas falhas", config.getNome());
            }
            log.error("Erro ao enviar webhook {}: {}", config.getNome(), e.getMessage());
        }

        // Salvar log e config
        logRepository.save(webhookLog);
        configRepository.save(config);
    }

    // ========== Retry de Webhooks ==========

    /**
     * Processa retries pendentes (executado a cada 1 minuto).
     */
    @Scheduled(fixedRate = 60000) // 1 minuto
    @Transactional
    public void processarRetries() {
        List<WebhookLog> pendentes = logRepository.findPendingRetries(LocalDateTime.now());

        if (pendentes.isEmpty()) {
            return;
        }

        log.info("Processando {} webhook(s) pendentes de retry", pendentes.size());

        for (WebhookLog webhookLog : pendentes) {
            try {
                reenviarWebhook(webhookLog);
            } catch (Exception e) {
                log.error("Erro ao processar retry do webhook: {}", e.getMessage());
            }
        }
    }

    /**
     * Reenvia um webhook que falhou.
     */
    private void reenviarWebhook(WebhookLog webhookLog) {
        WebhookConfig config = webhookLog.getWebhookConfig();

        if (!config.getAtivo()) {
            webhookLog.setStatus(StatusWebhookLog.ESGOTADO);
            logRepository.save(webhookLog);
            return;
        }

        long startTime = System.currentTimeMillis();
        webhookLog.setTentativa(webhookLog.getTentativa() + 1);

        try {
            // Montar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> customHeaders = parseHeaders(config.getHeadersJson());
            customHeaders.forEach(headers::set);

            if (config.getSecret() != null && !config.getSecret().isBlank()) {
                String signature = generateHmacSignature(webhookLog.getPayload(), config.getSecret());
                headers.set("X-Webhook-Signature", signature);
                headers.set("X-Webhook-Timestamp", String.valueOf(System.currentTimeMillis()));
            }

            HttpEntity<String> entity = new HttpEntity<>(webhookLog.getPayload(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                URI.create(webhookLog.getUrl()),
                HttpMethod.POST,
                entity,
                String.class
            );

            long tempoResposta = System.currentTimeMillis() - startTime;

            if (response.getStatusCode().is2xxSuccessful()) {
                webhookLog.marcarSucesso(
                    response.getStatusCode().value(),
                    response.getBody(),
                    tempoResposta
                );
                config.registrarSucesso();
                log.info("Retry do webhook {} bem-sucedido (tentativa {})", config.getNome(), webhookLog.getTentativa());
            } else {
                webhookLog.marcarFalha(
                    response.getStatusCode().value(),
                    response.getBody(),
                    "HTTP " + response.getStatusCode().value(),
                    tempoResposta
                );
                webhookLog.agendarRetry(config.getMaxTentativas());
                config.registrarFalha();
            }

        } catch (Exception e) {
            long tempoResposta = System.currentTimeMillis() - startTime;
            webhookLog.marcarFalha(null, null, e.getMessage(), tempoResposta);
            webhookLog.agendarRetry(config.getMaxTentativas());
            config.registrarFalha();
            log.error("Retry do webhook {} falhou: {}", config.getNome(), e.getMessage());
        }

        logRepository.save(webhookLog);
        configRepository.save(config);
    }

    // ========== Teste de Webhook ==========

    /**
     * Testa um webhook com payload de exemplo.
     */
    @Transactional
    public WebhookTestResultDTO testar(WebhookTestDTO dto) {
        UUID oficinaId = TenantContext.getTenantId();
        WebhookConfig config = configRepository.findByIdAndOficinaId(dto.webhookId(), oficinaId)
            .orElseThrow(() -> new EntityNotFoundException("Webhook não encontrado"));

        long startTime = System.currentTimeMillis();

        // Montar payload de teste
        Map<String, Object> webhookPayload = new LinkedHashMap<>();
        webhookPayload.put("evento", dto.evento().name());
        webhookPayload.put("eventoNome", dto.evento().getNome());
        webhookPayload.put("timestamp", LocalDateTime.now().toString());
        webhookPayload.put("teste", true);
        webhookPayload.put("entidadeId", UUID.randomUUID().toString());
        webhookPayload.put("entidadeTipo", "Teste");
        webhookPayload.put("dados", Map.of(
            "mensagem", "Este é um webhook de teste do PitStop",
            "oficinaId", oficinaId.toString()
        ));

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(webhookPayload);
        } catch (Exception e) {
            return WebhookTestResultDTO.builder()
                .sucesso(false)
                .erro("Erro ao serializar payload: " + e.getMessage())
                .payloadEnviado(null)
                .build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> customHeaders = parseHeaders(config.getHeadersJson());
            customHeaders.forEach(headers::set);

            if (config.getSecret() != null && !config.getSecret().isBlank()) {
                String signature = generateHmacSignature(payloadJson, config.getSecret());
                headers.set("X-Webhook-Signature", signature);
                headers.set("X-Webhook-Timestamp", String.valueOf(System.currentTimeMillis()));
            }

            HttpEntity<String> entity = new HttpEntity<>(payloadJson, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                URI.create(config.getUrl()),
                HttpMethod.POST,
                entity,
                String.class
            );

            long tempoResposta = System.currentTimeMillis() - startTime;

            return WebhookTestResultDTO.builder()
                .sucesso(response.getStatusCode().is2xxSuccessful())
                .httpStatus(response.getStatusCode().value())
                .responseBody(truncate(response.getBody(), 2000))
                .tempoRespostaMs(tempoResposta)
                .payloadEnviado(payloadJson)
                .build();

        } catch (Exception e) {
            return WebhookTestResultDTO.builder()
                .sucesso(false)
                .erro(e.getMessage())
                .tempoRespostaMs(System.currentTimeMillis() - startTime)
                .payloadEnviado(payloadJson)
                .build();
        }
    }

    // ========== Logs e Estatísticas ==========

    /**
     * Lista logs de webhooks da oficina.
     */
    @Transactional(readOnly = true)
    public Page<WebhookLogDTO> listarLogs(Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return logRepository.findByOficinaIdOrderByCreatedAtDesc(oficinaId, pageable)
            .map(WebhookLogDTO::fromEntity);
    }

    /**
     * Lista logs de um webhook específico.
     */
    @Transactional(readOnly = true)
    public Page<WebhookLogDTO> listarLogsPorWebhook(UUID webhookId, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();

        // Verificar se webhook pertence à oficina
        if (!configRepository.findByIdAndOficinaId(webhookId, oficinaId).isPresent()) {
            throw new EntityNotFoundException("Webhook não encontrado");
        }

        return logRepository.findByWebhookConfigIdOrderByCreatedAtDesc(webhookId, pageable)
            .map(WebhookLogDTO::fromEntity);
    }

    /**
     * Retorna estatísticas de webhooks da oficina.
     */
    @Transactional(readOnly = true)
    public WebhookStatsDTO getEstatisticas() {
        UUID oficinaId = TenantContext.getTenantId();
        LocalDateTime ontem = LocalDateTime.now().minusHours(24);

        long total = configRepository.countByOficinaId(oficinaId);
        long ativos = configRepository.countByOficinaIdAndAtivoTrue(oficinaId);
        long sucessos = logRepository.countSuccessSince(oficinaId, ontem);
        long falhas = logRepository.countFailuresSince(oficinaId, ontem);
        Double tempoMedio = logRepository.avgResponseTimeSince(oficinaId, ontem);
        long pendentes = logRepository.countByOficinaIdAndStatus(oficinaId, StatusWebhookLog.AGUARDANDO_RETRY);

        return WebhookStatsDTO.builder()
            .totalWebhooks(total)
            .webhooksAtivos(ativos)
            .webhooksInativos(total - ativos)
            .sucessos24h(sucessos)
            .falhas24h(falhas)
            .tempoMedioResposta24h(tempoMedio)
            .pendentesRetry(pendentes)
            .build();
    }

    /**
     * Lista todos os tipos de eventos disponíveis.
     */
    public List<Map<String, String>> listarEventos() {
        return Arrays.stream(TipoEventoWebhook.values())
            .map(e -> Map.of(
                "codigo", e.name(),
                "nome", e.getNome(),
                "descricao", e.getDescricao()
            ))
            .toList();
    }

    // ========== Utilitários ==========

    private String toJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> parseHeaders(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String generateHmacSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacBytes);
        } catch (Exception e) {
            log.error("Erro ao gerar assinatura HMAC: {}", e.getMessage());
            return "";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) : str;
    }
}
