package com.pitstop.ia.service;

import com.pitstop.ia.domain.ConfiguracaoIA;
import com.pitstop.ia.domain.ProvedorIA;
import com.pitstop.ia.dto.AtualizarApiKeyRequest;
import com.pitstop.ia.dto.ConfiguracaoIARequest;
import com.pitstop.ia.dto.ConfiguracaoIAResponse;
import com.pitstop.ia.mapper.ConfiguracaoIAMapper;
import com.pitstop.ia.repository.ConfiguracaoIARepository;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service para gerenciar configurações de IA por oficina.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoIAService {

    private final ConfiguracaoIARepository repository;
    private final ConfiguracaoIAMapper mapper;
    private final CriptografiaService criptografiaService;

    /**
     * Busca configuração da oficina atual.
     * Cria uma configuração padrão se não existir.
     */
    @Transactional
    public ConfiguracaoIAResponse buscarConfiguracao() {
        UUID oficinaId = TenantContext.getTenantId();
        log.debug("Buscando configuração de IA para oficina: {}", oficinaId);

        ConfiguracaoIA config = repository.findByOficinaId(oficinaId)
                .orElseGet(() -> criarConfiguracaoPadrao(oficinaId));

        return mapper.toResponse(config);
    }

    /**
     * Atualiza configurações de IA.
     */
    @Transactional
    public ConfiguracaoIAResponse atualizarConfiguracao(ConfiguracaoIARequest request) {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("Atualizando configuração de IA para oficina: {}", oficinaId);

        ConfiguracaoIA config = repository.findByOficinaId(oficinaId)
                .orElseGet(() -> criarConfiguracaoPadrao(oficinaId));

        // Atualiza campos se fornecidos
        if (request.provedor() != null) {
            config.setProvedor(request.provedor());
        }
        if (request.modeloPadrao() != null) {
            config.setModeloPadrao(request.modeloPadrao());
        }
        if (request.modeloAvancado() != null) {
            config.setModeloAvancado(request.modeloAvancado());
        }
        if (request.iaHabilitada() != null) {
            config.setIaHabilitada(request.iaHabilitada());
        }
        if (request.usarCache() != null) {
            config.setUsarCache(request.usarCache());
        }
        if (request.usarPreValidacao() != null) {
            config.setUsarPreValidacao(request.usarPreValidacao());
        }
        if (request.usarRoteamentoInteligente() != null) {
            config.setUsarRoteamentoInteligente(request.usarRoteamentoInteligente());
        }
        if (request.maxTokensResposta() != null) {
            config.setMaxTokensResposta(request.maxTokensResposta());
        }
        if (request.maxRequisicoesDia() != null) {
            config.setMaxRequisicoesDia(request.maxRequisicoesDia());
        }

        ConfiguracaoIA saved = repository.save(config);
        log.info("Configuração de IA atualizada para oficina: {}", oficinaId);

        return mapper.toResponse(saved);
    }

    /**
     * Atualiza a API key (criptografada).
     */
    @Transactional
    public ConfiguracaoIAResponse atualizarApiKey(AtualizarApiKeyRequest request) {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("Atualizando API key de IA para oficina: {}", oficinaId);

        ConfiguracaoIA config = repository.findByOficinaId(oficinaId)
                .orElseGet(() -> criarConfiguracaoPadrao(oficinaId));

        // Criptografa a API key antes de salvar
        String apiKeyEncrypted = criptografiaService.encrypt(request.apiKey());
        config.setApiKeyEncrypted(apiKeyEncrypted);

        ConfiguracaoIA saved = repository.save(config);
        log.info("API key de IA atualizada para oficina: {}", oficinaId);

        return mapper.toResponse(saved);
    }

    /**
     * Remove a API key configurada.
     */
    @Transactional
    public ConfiguracaoIAResponse removerApiKey() {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("Removendo API key de IA para oficina: {}", oficinaId);

        ConfiguracaoIA config = repository.findByOficinaId(oficinaId)
                .orElseThrow(() -> new IllegalStateException("Configuração não encontrada"));

        config.setApiKeyEncrypted(null);
        config.setIaHabilitada(false);

        ConfiguracaoIA saved = repository.save(config);
        log.info("API key de IA removida para oficina: {}", oficinaId);

        return mapper.toResponse(saved);
    }

    /**
     * Busca a API key descriptografada (uso interno).
     */
    @Transactional(readOnly = true)
    public String buscarApiKeyDescriptografada(UUID oficinaId) {
        ConfiguracaoIA config = repository.findByOficinaId(oficinaId)
                .orElseThrow(() -> new IllegalStateException("Configuração de IA não encontrada"));

        if (config.getApiKeyEncrypted() == null) {
            throw new IllegalStateException("API key não configurada para esta oficina");
        }

        return criptografiaService.decrypt(config.getApiKeyEncrypted());
    }

    /**
     * Busca configuração para uso interno (com API key).
     */
    @Transactional(readOnly = true)
    public ConfiguracaoIA buscarConfiguracaoInterna(UUID oficinaId) {
        return repository.findByOficinaId(oficinaId)
                .orElseThrow(() -> new IllegalStateException("Configuração de IA não encontrada"));
    }

    /**
     * Testa a conexão com a API.
     */
    @Transactional(readOnly = true)
    public boolean testarConexao() {
        UUID oficinaId = TenantContext.getTenantId();

        ConfiguracaoIA config = repository.findByOficinaId(oficinaId).orElse(null);
        if (config == null || config.getApiKeyEncrypted() == null) {
            return false;
        }

        // TODO: Implementar teste real de conexão com Anthropic
        // Por enquanto, apenas verifica se a API key está configurada
        try {
            String apiKey = criptografiaService.decrypt(config.getApiKeyEncrypted());
            return apiKey != null && apiKey.startsWith("sk-ant-");
        } catch (Exception e) {
            log.error("Erro ao testar conexão com IA", e);
            return false;
        }
    }

    /**
     * Cria configuração padrão para uma oficina.
     */
    private ConfiguracaoIA criarConfiguracaoPadrao(UUID oficinaId) {
        log.info("Criando configuração de IA padrão para oficina: {}", oficinaId);

        Oficina oficina = new Oficina();
        oficina.setId(oficinaId);

        ConfiguracaoIA config = ConfiguracaoIA.builder()
                .oficina(oficina)
                .provedor(ProvedorIA.ANTHROPIC)
                .modeloPadrao("claude-haiku-4-5-20251001")
                .modeloAvancado("claude-sonnet-4-20250514")
                .iaHabilitada(false)
                .usarCache(true)
                .usarPreValidacao(true)
                .usarRoteamentoInteligente(true)
                .maxTokensResposta(1000)
                .maxRequisicoesDia(100)
                .build();

        return repository.save(config);
    }
}
