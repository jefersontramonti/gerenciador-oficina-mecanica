package com.pitstop.notificacao.controller;

import com.pitstop.notificacao.domain.ConfiguracaoNotificacao;
import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import com.pitstop.notificacao.dto.ConfiguracaoNotificacaoDTO;
import com.pitstop.notificacao.dto.ConfiguracaoNotificacaoRequest;
import com.pitstop.notificacao.integration.evolution.EvolutionInstanceStatus;
import com.pitstop.notificacao.service.ConfiguracaoNotificacaoService;
import com.pitstop.notificacao.service.EmailService;
import com.pitstop.notificacao.service.TelegramService;
import com.pitstop.notificacao.service.WhatsAppService;
import com.pitstop.shared.security.feature.RequiresFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para configuracoes de notificacao.
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/notificacoes/configuracao")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Configuracao de Notificacoes", description = "Gerenciamento de configuracoes de notificacao da oficina")
@SecurityRequirement(name = "bearer-jwt")
public class ConfiguracaoNotificacaoController {

    private final ConfiguracaoNotificacaoService service;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final TelegramService telegramService;

    /**
     * Obtem a configuracao atual da oficina.
     * GET /api/notificacoes/configuracao
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Obter configuracao", description = "Retorna a configuracao de notificacoes da oficina")
    public ResponseEntity<ConfiguracaoNotificacaoDTO> getConfiguracao() {
        ConfiguracaoNotificacaoDTO config = service.getConfiguracao();
        return ResponseEntity.ok(config);
    }

    /**
     * Atualiza a configuracao da oficina.
     * PUT /api/notificacoes/configuracao
     */
    @PutMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Atualizar configuracao", description = "Atualiza as configuracoes de notificacao")
    public ResponseEntity<ConfiguracaoNotificacaoDTO> atualizar(
        @Valid @RequestBody ConfiguracaoNotificacaoRequest request
    ) {
        log.info("PUT /api/notificacoes/configuracao - Atualizando configuracao");
        ConfiguracaoNotificacaoDTO config = service.atualizar(request);
        return ResponseEntity.ok(config);
    }

    /**
     * Habilita/desabilita um canal.
     * PATCH /api/notificacoes/configuracao/canais/{canal}
     */
    @PatchMapping("/canais/{canal}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Configurar canal", description = "Habilita ou desabilita um canal de notificacao")
    public ResponseEntity<ConfiguracaoNotificacaoDTO> setCanal(
        @PathVariable TipoNotificacao canal,
        @RequestParam boolean habilitado
    ) {
        log.info("PATCH /api/notificacoes/configuracao/canais/{} - Habilitado: {}", canal, habilitado);
        ConfiguracaoNotificacaoDTO config = service.setCanal(canal, habilitado);
        return ResponseEntity.ok(config);
    }

    /**
     * Configura um evento especifico.
     * PUT /api/notificacoes/configuracao/eventos/{evento}
     */
    @PutMapping("/eventos/{evento}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Configurar evento", description = "Define configuracao para um evento especifico")
    public ResponseEntity<ConfiguracaoNotificacaoDTO> configurarEvento(
        @PathVariable EventoNotificacao evento,
        @RequestBody ConfiguracaoNotificacao.EventoConfig eventConfig
    ) {
        log.info("PUT /api/notificacoes/configuracao/eventos/{}", evento);
        ConfiguracaoNotificacaoDTO config = service.configurarEvento(evento, eventConfig);
        return ResponseEntity.ok(config);
    }

    // ===== SMTP =====

    /**
     * Configura SMTP proprio.
     * PUT /api/notificacoes/configuracao/smtp
     */
    @PutMapping("/smtp")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @RequiresFeature(value = "SMTP_CUSTOMIZADO", name = "SMTP Customizado", requiredPlan = "PROFISSIONAL")
    @Operation(summary = "Configurar SMTP", description = "Configura servidor SMTP proprio da oficina")
    public ResponseEntity<ConfiguracaoNotificacaoDTO> configurarSmtp(
        @RequestBody SmtpConfigRequest request
    ) {
        log.info("PUT /api/notificacoes/configuracao/smtp - Host: {}", request.host());
        ConfiguracaoNotificacaoDTO config = service.configurarSmtp(
            request.host(),
            request.port(),
            request.username(),
            request.password(),
            request.usarTls(),
            request.emailRemetente(),
            request.nomeRemetente()
        );
        return ResponseEntity.ok(config);
    }

    /**
     * Remove SMTP proprio.
     * DELETE /api/notificacoes/configuracao/smtp
     */
    @DeleteMapping("/smtp")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @RequiresFeature(value = "SMTP_CUSTOMIZADO", name = "SMTP Customizado", requiredPlan = "PROFISSIONAL")
    @Operation(summary = "Remover SMTP", description = "Remove configuracao de SMTP proprio")
    public ResponseEntity<ConfiguracaoNotificacaoDTO> removerSmtp() {
        log.info("DELETE /api/notificacoes/configuracao/smtp");
        ConfiguracaoNotificacaoDTO config = service.removerSmtpProprio();
        return ResponseEntity.ok(config);
    }

    // ===== WHATSAPP =====

    /**
     * Configura Evolution API para WhatsApp.
     * PUT /api/notificacoes/configuracao/whatsapp
     */
    @PutMapping("/whatsapp")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @RequiresFeature(value = "WHATSAPP_NOTIFICATIONS", name = "Notificacoes WhatsApp", requiredPlan = "PROFISSIONAL")
    @Operation(summary = "Configurar WhatsApp", description = "Configura Evolution API para WhatsApp")
    public ResponseEntity<ConfiguracaoNotificacaoDTO> configurarWhatsApp(
        @RequestBody WhatsAppConfigRequest request
    ) {
        log.info("PUT /api/notificacoes/configuracao/whatsapp - URL: {}", request.apiUrl());
        ConfiguracaoNotificacaoDTO config = service.configurarEvolutionApi(
            request.apiUrl(),
            request.apiToken(),
            request.instanceName(),
            request.whatsappNumero()
        );
        return ResponseEntity.ok(config);
    }

    /**
     * Verifica status da conexao WhatsApp.
     * GET /api/notificacoes/configuracao/whatsapp/status
     */
    @GetMapping("/whatsapp/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @RequiresFeature(value = "WHATSAPP_NOTIFICATIONS", name = "Notificacoes WhatsApp", requiredPlan = "PROFISSIONAL")
    @Operation(summary = "Status WhatsApp", description = "Verifica status da conexao com Evolution API")
    public ResponseEntity<EvolutionInstanceStatus> statusWhatsApp() {
        EvolutionInstanceStatus status = service.verificarConexaoWhatsApp();
        return ResponseEntity.ok(status);
    }

    /**
     * Gera QR Code para conectar WhatsApp.
     * GET /api/notificacoes/configuracao/whatsapp/qrcode
     */
    @GetMapping("/whatsapp/qrcode")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @RequiresFeature(value = "WHATSAPP_NOTIFICATIONS", name = "Notificacoes WhatsApp", requiredPlan = "PROFISSIONAL")
    @Operation(summary = "QR Code WhatsApp", description = "Gera QR Code para conectar WhatsApp")
    public ResponseEntity<Map<String, String>> qrCodeWhatsApp() {
        String qrCode = service.gerarQrCodeWhatsApp();
        if (qrCode == null) {
            return ResponseEntity.ok(Map.of("status", "connected", "message", "WhatsApp ja esta conectado"));
        }
        return ResponseEntity.ok(Map.of("qrcode", qrCode));
    }

    /**
     * Cria instancia automaticamente na Evolution API.
     * POST /api/notificacoes/configuracao/whatsapp/criar-instancia
     */
    @PostMapping("/whatsapp/criar-instancia")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @RequiresFeature(value = "WHATSAPP_NOTIFICATIONS", name = "Notificacoes WhatsApp", requiredPlan = "PROFISSIONAL")
    @Operation(summary = "Criar instancia WhatsApp", description = "Cria automaticamente uma instancia na Evolution API")
    public ResponseEntity<CriarInstanciaResponse> criarInstanciaWhatsApp() {
        log.info("POST /api/notificacoes/configuracao/whatsapp/criar-instancia");
        var resultado = service.criarInstanciaAutomatica();
        return ResponseEntity.ok(resultado);
    }

    /**
     * Deleta instancia na Evolution API e limpa configuracoes.
     * DELETE /api/notificacoes/configuracao/whatsapp/instancia
     */
    @DeleteMapping("/whatsapp/instancia")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @RequiresFeature(value = "WHATSAPP_NOTIFICATIONS", name = "Notificacoes WhatsApp", requiredPlan = "PROFISSIONAL")
    @Operation(summary = "Deletar instancia WhatsApp", description = "Remove instancia da Evolution API e limpa configuracoes")
    public ResponseEntity<Map<String, Object>> deletarInstanciaWhatsApp() {
        log.info("DELETE /api/notificacoes/configuracao/whatsapp/instancia");
        boolean sucesso = service.deletarInstancia();
        return ResponseEntity.ok(Map.of(
            "sucesso", sucesso,
            "mensagem", sucesso ? "Instancia removida com sucesso" : "Erro ao remover instancia"
        ));
    }

    /**
     * Desconecta instancia na Evolution API (logout).
     * POST /api/notificacoes/configuracao/whatsapp/desconectar
     */
    @PostMapping("/whatsapp/desconectar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @RequiresFeature(value = "WHATSAPP_NOTIFICATIONS", name = "Notificacoes WhatsApp", requiredPlan = "PROFISSIONAL")
    @Operation(summary = "Desconectar WhatsApp", description = "Faz logout do WhatsApp na instancia")
    public ResponseEntity<Map<String, Object>> desconectarWhatsApp() {
        log.info("POST /api/notificacoes/configuracao/whatsapp/desconectar");
        boolean sucesso = service.desconectarInstancia();
        return ResponseEntity.ok(Map.of(
            "sucesso", sucesso,
            "mensagem", sucesso ? "WhatsApp desconectado com sucesso" : "Erro ao desconectar"
        ));
    }

    /**
     * Reconecta/reinicia instancia na Evolution API.
     * POST /api/notificacoes/configuracao/whatsapp/reconectar
     */
    @PostMapping("/whatsapp/reconectar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @RequiresFeature(value = "WHATSAPP_NOTIFICATIONS", name = "Notificacoes WhatsApp", requiredPlan = "PROFISSIONAL")
    @Operation(summary = "Reconectar WhatsApp", description = "Reinicia a instancia para reconectar")
    public ResponseEntity<Map<String, Object>> reconectarWhatsApp() {
        log.info("POST /api/notificacoes/configuracao/whatsapp/reconectar");
        boolean sucesso = service.reconectarInstancia();
        return ResponseEntity.ok(Map.of(
            "sucesso", sucesso,
            "mensagem", sucesso ? "Reconexao iniciada. Escaneie o QR Code novamente." : "Erro ao reconectar"
        ));
    }

    // ===== TELEGRAM =====

    /**
     * Configura Telegram Bot.
     * PUT /api/notificacoes/configuracao/telegram
     */
    @PutMapping("/telegram")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @RequiresFeature(value = "TELEGRAM_NOTIFICATIONS", name = "Notificacoes Telegram", requiredPlan = "PROFISSIONAL")
    @Operation(summary = "Configurar Telegram", description = "Configura o bot do Telegram para notificacoes")
    public ResponseEntity<ConfiguracaoNotificacaoDTO> configurarTelegram(
        @RequestBody TelegramConfigRequest request
    ) {
        log.info("PUT /api/notificacoes/configuracao/telegram - ChatId: {}", request.chatId());
        ConfiguracaoNotificacaoDTO config = service.configurarTelegram(
            request.botToken(),
            request.chatId()
        );
        return ResponseEntity.ok(config);
    }

    /**
     * Verifica status da conexao Telegram.
     * GET /api/notificacoes/configuracao/telegram/status
     */
    @GetMapping("/telegram/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @RequiresFeature(value = "TELEGRAM_NOTIFICATIONS", name = "Notificacoes Telegram", requiredPlan = "PROFISSIONAL")
    @Operation(summary = "Status Telegram", description = "Verifica status da conexao com o bot do Telegram")
    public ResponseEntity<Map<String, Object>> statusTelegram() {
        boolean conectado = service.verificarConexaoTelegram();
        var botInfo = telegramService.getBotInfo(service.getOficinaId());

        if (botInfo != null) {
            return ResponseEntity.ok(Map.of(
                "conectado", conectado,
                "botUsername", "@" + botInfo.username(),
                "botNome", botInfo.firstName()
            ));
        }
        return ResponseEntity.ok(Map.of("conectado", conectado));
    }

    // ===== TESTE DE NOTIFICACAO =====

    /**
     * Testa o envio de uma notificacao.
     * POST /api/notificacoes/configuracao/testar
     */
    @PostMapping("/testar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Testar notificacao", description = "Envia uma notificacao de teste para validar a configuracao")
    public ResponseEntity<TesteNotificacaoResponse> testarNotificacao(
        @Valid @RequestBody TesteNotificacaoRequest request
    ) {
        log.info("POST /api/notificacoes/configuracao/testar - Tipo: {}, Destinatario: {}",
            request.tipo(), request.destinatario());

        try {
            switch (request.tipo()) {
                case EMAIL -> {
                    if (request.destinatario() == null || request.destinatario().isBlank()) {
                        return ResponseEntity.badRequest()
                            .body(new TesteNotificacaoResponse(false, "E-mail de destino e obrigatorio"));
                    }
                    emailService.enviarTeste(request.destinatario());
                    return ResponseEntity.ok(new TesteNotificacaoResponse(true, "E-mail de teste enviado com sucesso"));
                }
                case WHATSAPP -> {
                    if (request.destinatario() == null || request.destinatario().isBlank()) {
                        return ResponseEntity.badRequest()
                            .body(new TesteNotificacaoResponse(false, "Numero de destino e obrigatorio"));
                    }
                    String mensagem = request.mensagem() != null && !request.mensagem().isBlank()
                        ? request.mensagem()
                        : "Esta e uma mensagem de teste do sistema PitStop. Se voce recebeu, o WhatsApp esta configurado corretamente!";

                    var historico = whatsAppService.enviar(
                        request.destinatario(),
                        "Teste",
                        mensagem,
                        EventoNotificacao.TESTE,
                        null,
                        null,
                        null,
                        null
                    );

                    if (historico.getStatus() == com.pitstop.notificacao.domain.StatusNotificacao.ENVIADO) {
                        return ResponseEntity.ok(new TesteNotificacaoResponse(true, "Mensagem de teste enviada com sucesso"));
                    } else if (historico.getStatus() == com.pitstop.notificacao.domain.StatusNotificacao.AGENDADO) {
                        String motivo = historico.getMotivoAgendamento() != null
                            ? historico.getMotivoAgendamento()
                            : "Notificacao agendada para envio posterior conforme politicas de envio.";
                        return ResponseEntity.ok(new TesteNotificacaoResponse(false, motivo));
                    } else {
                        String erro = historico.getErroMensagem() != null
                            ? historico.getErroMensagem()
                            : "Falha ao enviar. Verifique as configuracoes.";
                        return ResponseEntity.ok(new TesteNotificacaoResponse(false, erro));
                    }
                }
                case SMS -> {
                    return ResponseEntity.ok(new TesteNotificacaoResponse(false, "SMS ainda nao implementado"));
                }
                case TELEGRAM -> {
                    String mensagem = request.mensagem() != null && !request.mensagem().isBlank()
                        ? request.mensagem()
                        : "Esta e uma mensagem de teste do sistema PitStop. Se voce recebeu, o Telegram esta configurado corretamente!";

                    var historico = telegramService.enviar(
                        request.destinatario(), // chatId (null = usa configurado)
                        "Teste",
                        mensagem,
                        EventoNotificacao.TESTE,
                        null,
                        null,
                        null,
                        null
                    );

                    if (historico.getStatus() == com.pitstop.notificacao.domain.StatusNotificacao.ENVIADO) {
                        return ResponseEntity.ok(new TesteNotificacaoResponse(true, "Mensagem de teste enviada com sucesso"));
                    } else if (historico.getStatus() == com.pitstop.notificacao.domain.StatusNotificacao.AGENDADO) {
                        String motivo = historico.getMotivoAgendamento() != null
                            ? historico.getMotivoAgendamento()
                            : "Notificacao agendada para envio posterior conforme politicas de envio.";
                        return ResponseEntity.ok(new TesteNotificacaoResponse(false, motivo));
                    } else {
                        String erro = historico.getErroMensagem() != null
                            ? historico.getErroMensagem()
                            : "Falha ao enviar. Verifique as configuracoes.";
                        return ResponseEntity.ok(new TesteNotificacaoResponse(false, erro));
                    }
                }
                default -> {
                    return ResponseEntity.badRequest()
                        .body(new TesteNotificacaoResponse(false, "Tipo de notificacao nao suportado"));
                }
            }
        } catch (Exception e) {
            log.error("Erro ao enviar notificacao de teste: {}", e.getMessage(), e);
            return ResponseEntity.ok(new TesteNotificacaoResponse(false, "Erro ao enviar: " + e.getMessage()));
        }
    }

    // ===== MODO SIMULACAO =====

    /**
     * Ativa/desativa modo simulacao.
     * PATCH /api/notificacoes/configuracao/simulacao
     */
    @PatchMapping("/simulacao")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Modo simulacao", description = "Ativa ou desativa o modo simulacao")
    public ResponseEntity<ConfiguracaoNotificacaoDTO> setModoSimulacao(
        @RequestParam boolean ativo
    ) {
        log.info("PATCH /api/notificacoes/configuracao/simulacao - Ativo: {}", ativo);
        ConfiguracaoNotificacaoDTO config = service.setModoSimulacao(ativo);
        return ResponseEntity.ok(config);
    }

    // ===== LISTAS AUXILIARES =====

    /**
     * Lista todos os eventos disponiveis.
     * GET /api/notificacoes/configuracao/eventos
     */
    @GetMapping("/eventos")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar eventos", description = "Lista todos os eventos de notificacao disponiveis")
    public ResponseEntity<List<EventoInfo>> listarEventos() {
        List<EventoInfo> eventos = Arrays.stream(EventoNotificacao.values())
            .map(e -> new EventoInfo(
                e.name(),
                e.getNome(),
                e.getDescricao(),
                e.getVariaveisDisponiveis()
            ))
            .toList();
        return ResponseEntity.ok(eventos);
    }

    /**
     * Lista todos os canais disponiveis.
     * GET /api/notificacoes/configuracao/canais
     */
    @GetMapping("/canais")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar canais", description = "Lista todos os canais de notificacao disponiveis")
    public ResponseEntity<List<TipoNotificacao>> listarCanais() {
        return ResponseEntity.ok(Arrays.asList(TipoNotificacao.values()));
    }

    // ===== RECORDS AUXILIARES =====

    public record SmtpConfigRequest(
        String host,
        Integer port,
        String username,
        String password,
        Boolean usarTls,
        String emailRemetente,
        String nomeRemetente
    ) {}

    public record WhatsAppConfigRequest(
        String apiUrl,
        String apiToken,
        String instanceName,
        String whatsappNumero
    ) {}

    public record TelegramConfigRequest(
        String botToken,
        String chatId
    ) {}

    public record EventoInfo(
        String codigo,
        String nome,
        String descricao,
        Map<String, String> variaveisDisponiveis
    ) {}

    public record TesteNotificacaoRequest(
        TipoNotificacao tipo,
        String destinatario,
        String mensagem
    ) {}

    public record TesteNotificacaoResponse(
        boolean sucesso,
        String mensagem
    ) {}

    public record CriarInstanciaResponse(
        boolean sucesso,
        String instanceName,
        String qrCode,
        String mensagem
    ) {
        public static CriarInstanciaResponse sucesso(String instanceName, String qrCode) {
            return new CriarInstanciaResponse(true, instanceName, qrCode, "Instancia criada com sucesso");
        }

        public static CriarInstanciaResponse falha(String mensagem) {
            return new CriarInstanciaResponse(false, null, null, mensagem);
        }
    }
}
