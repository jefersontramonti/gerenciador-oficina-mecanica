package com.pitstop.notificacao.service;

import com.pitstop.notificacao.controller.ConfiguracaoNotificacaoController.CriarInstanciaResponse;
import com.pitstop.notificacao.domain.ConfiguracaoNotificacao;
import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import com.pitstop.notificacao.dto.ConfiguracaoNotificacaoDTO;
import com.pitstop.notificacao.dto.ConfiguracaoNotificacaoRequest;
import com.pitstop.notificacao.integration.evolution.EvolutionApiClient;
import com.pitstop.notificacao.integration.evolution.EvolutionConfig;
import com.pitstop.notificacao.integration.evolution.EvolutionInstanceStatus;
import com.pitstop.notificacao.repository.ConfiguracaoNotificacaoRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Servico para gerenciamento de configuracoes de notificacao.
 *
 * @author PitStop Team
 */
@Service
@Slf4j
public class ConfiguracaoNotificacaoService {

    private final ConfiguracaoNotificacaoRepository repository;
    private final WhatsAppService whatsAppService;
    private final TelegramService telegramService;
    private final EvolutionApiClient evolutionApiClient;

    @Value("${pitstop.evolution.api-url:https://whatsapp.pitstopai.com.br}")
    private String evolutionApiUrl;

    @Value("${pitstop.evolution.global-api-key:}")
    private String evolutionGlobalApiKey;

    public ConfiguracaoNotificacaoService(
        ConfiguracaoNotificacaoRepository repository,
        WhatsAppService whatsAppService,
        TelegramService telegramService,
        EvolutionApiClient evolutionApiClient
    ) {
        this.repository = repository;
        this.whatsAppService = whatsAppService;
        this.telegramService = telegramService;
        this.evolutionApiClient = evolutionApiClient;
    }

    /**
     * Obtem a configuracao da oficina atual.
     * Cria uma configuracao padrao se nao existir.
     *
     * @return Configuracao da oficina
     */
    @Transactional
    public ConfiguracaoNotificacaoDTO getConfiguracao() {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoNotificacao config = getOrCreateConfig(oficinaId);
        return ConfiguracaoNotificacaoDTO.fromEntity(config);
    }

    /**
     * Obtem a configuracao de uma oficina especifica (para admin).
     *
     * @param oficinaId ID da oficina
     * @return Configuracao da oficina
     */
    @Transactional(readOnly = true)
    public Optional<ConfiguracaoNotificacaoDTO> getConfiguracaoPorOficina(UUID oficinaId) {
        return repository.findByOficinaId(oficinaId)
            .map(ConfiguracaoNotificacaoDTO::fromEntity);
    }

    /**
     * Atualiza a configuracao da oficina atual.
     *
     * @param request Dados para atualizar
     * @return Configuracao atualizada
     */
    @Transactional
    public ConfiguracaoNotificacaoDTO atualizar(ConfiguracaoNotificacaoRequest request) {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoNotificacao config = getOrCreateConfig(oficinaId);

        request.applyTo(config);

        config = repository.save(config);
        log.info("Configuracao de notificacao atualizada para oficina: {}", oficinaId);

        return ConfiguracaoNotificacaoDTO.fromEntity(config);
    }

    /**
     * Habilita ou desabilita um canal.
     *
     * @param canal Canal a modificar
     * @param habilitado Novo estado
     * @return Configuracao atualizada
     */
    @Transactional
    public ConfiguracaoNotificacaoDTO setCanal(TipoNotificacao canal, boolean habilitado) {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoNotificacao config = getOrCreateConfig(oficinaId);

        switch (canal) {
            case EMAIL -> config.setEmailHabilitado(habilitado);
            case WHATSAPP -> config.setWhatsappHabilitado(habilitado);
            case SMS -> config.setSmsHabilitado(habilitado);
            case TELEGRAM -> config.setTelegramHabilitado(habilitado);
        }

        config = repository.save(config);
        log.info("Canal {} {} para oficina: {}", canal, habilitado ? "habilitado" : "desabilitado", oficinaId);

        return ConfiguracaoNotificacaoDTO.fromEntity(config);
    }

    /**
     * Configura um evento especifico.
     *
     * @param evento Evento a configurar
     * @param eventConfig Configuracao do evento
     * @return Configuracao atualizada
     */
    @Transactional
    public ConfiguracaoNotificacaoDTO configurarEvento(
        EventoNotificacao evento,
        ConfiguracaoNotificacao.EventoConfig eventConfig
    ) {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoNotificacao config = getOrCreateConfig(oficinaId);

        var eventos = config.getEventosHabilitados();
        eventos.put(evento, eventConfig);
        config.setEventosHabilitados(eventos);

        config = repository.save(config);
        log.info("Evento {} configurado para oficina: {}", evento, oficinaId);

        return ConfiguracaoNotificacaoDTO.fromEntity(config);
    }

    /**
     * Configura o SMTP proprio da oficina.
     *
     * @param host Host SMTP
     * @param port Porta
     * @param username Usuario
     * @param password Senha
     * @param usarTls Usar TLS
     * @param emailRemetente Email remetente
     * @param nomeRemetente Nome remetente
     * @return Configuracao atualizada
     */
    @Transactional
    public ConfiguracaoNotificacaoDTO configurarSmtp(
        String host,
        Integer port,
        String username,
        String password,
        Boolean usarTls,
        String emailRemetente,
        String nomeRemetente
    ) {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoNotificacao config = getOrCreateConfig(oficinaId);

        config.setSmtpHost(host);
        config.setSmtpPort(port);
        config.setSmtpUsername(username);
        if (password != null && !password.isBlank()) {
            config.setSmtpPassword(password); // TODO: Criptografar
        }
        config.setSmtpUsarTls(usarTls != null ? usarTls : true);
        config.setEmailRemetente(emailRemetente);
        config.setEmailRemetenteNome(nomeRemetente);

        config = repository.save(config);
        log.info("SMTP configurado para oficina: {}", oficinaId);

        return ConfiguracaoNotificacaoDTO.fromEntity(config);
    }

    /**
     * Configura a Evolution API para WhatsApp.
     *
     * @param apiUrl URL da API
     * @param apiToken Token de autenticacao
     * @param instanceName Nome da instancia
     * @param whatsappNumero Numero do WhatsApp
     * @return Configuracao atualizada
     */
    @Transactional
    public ConfiguracaoNotificacaoDTO configurarEvolutionApi(
        String apiUrl,
        String apiToken,
        String instanceName,
        String whatsappNumero
    ) {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoNotificacao config = getOrCreateConfig(oficinaId);

        config.setEvolutionApiUrl(apiUrl);
        if (apiToken != null && !apiToken.isBlank()) {
            config.setEvolutionApiToken(apiToken); // TODO: Criptografar
        }
        config.setEvolutionInstanceName(instanceName);
        config.setWhatsappNumero(whatsappNumero);

        config = repository.save(config);
        log.info("Evolution API configurada para oficina: {}", oficinaId);

        return ConfiguracaoNotificacaoDTO.fromEntity(config);
    }

    /**
     * Verifica status da conexao WhatsApp.
     *
     * @return Status da instancia
     */
    public EvolutionInstanceStatus verificarConexaoWhatsApp() {
        UUID oficinaId = TenantContext.getTenantId();
        return whatsAppService.verificarConexao(oficinaId);
    }

    /**
     * Gera QR Code para conectar WhatsApp.
     *
     * @return QR Code em base64
     */
    public String gerarQrCodeWhatsApp() {
        UUID oficinaId = TenantContext.getTenantId();
        return whatsAppService.gerarQrCode(oficinaId);
    }

    /**
     * Configura o Telegram Bot.
     *
     * @param botToken Token do bot
     * @param chatId ID do chat
     * @return Configuracao atualizada
     */
    @Transactional
    public ConfiguracaoNotificacaoDTO configurarTelegram(String botToken, String chatId) {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoNotificacao config = getOrCreateConfig(oficinaId);

        if (botToken != null && !botToken.isBlank()) {
            config.setTelegramBotToken(botToken);
        }
        config.setTelegramChatId(chatId);
        config.setTelegramHabilitado(true);

        config = repository.save(config);
        log.info("Telegram configurado para oficina: {}", oficinaId);

        return ConfiguracaoNotificacaoDTO.fromEntity(config);
    }

    /**
     * Verifica status da conexao Telegram.
     *
     * @return true se conectado
     */
    public boolean verificarConexaoTelegram() {
        UUID oficinaId = TenantContext.getTenantId();
        return telegramService.verificarConexao(oficinaId);
    }

    /**
     * Retorna o ID da oficina atual.
     *
     * @return ID da oficina
     */
    public UUID getOficinaId() {
        return TenantContext.getTenantId();
    }

    /**
     * Ativa ou desativa o modo simulacao.
     *
     * @param ativo Se deve ativar
     * @return Configuracao atualizada
     */
    @Transactional
    public ConfiguracaoNotificacaoDTO setModoSimulacao(boolean ativo) {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoNotificacao config = getOrCreateConfig(oficinaId);

        config.setModoSimulacao(ativo);
        config = repository.save(config);

        log.info("Modo simulacao {} para oficina: {}", ativo ? "ativado" : "desativado", oficinaId);
        return ConfiguracaoNotificacaoDTO.fromEntity(config);
    }

    /**
     * Remove a configuracao SMTP propria.
     *
     * @return Configuracao atualizada
     */
    @Transactional
    public ConfiguracaoNotificacaoDTO removerSmtpProprio() {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoNotificacao config = getOrCreateConfig(oficinaId);

        config.setSmtpHost(null);
        config.setSmtpPort(null);
        config.setSmtpUsername(null);
        config.setSmtpPassword(null);
        config.setEmailRemetente(null);
        config.setEmailRemetenteNome(null);

        config = repository.save(config);
        log.info("SMTP proprio removido para oficina: {}", oficinaId);

        return ConfiguracaoNotificacaoDTO.fromEntity(config);
    }

    // ===== GERENCIAMENTO DE INSTANCIA WHATSAPP =====

    /**
     * Cria uma instancia automaticamente na Evolution API.
     * Gera o nome da instancia baseado no ID da oficina.
     *
     * @return Resultado da criacao com QR Code
     */
    @Transactional
    public CriarInstanciaResponse criarInstanciaAutomatica() {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoNotificacao config = getOrCreateConfig(oficinaId);

        // Verificar se ja tem instancia configurada
        if (config.getEvolutionInstanceName() != null && !config.getEvolutionInstanceName().isBlank()) {
            log.warn("Oficina {} ja possui instancia configurada: {}", oficinaId, config.getEvolutionInstanceName());
            return CriarInstanciaResponse.falha("Ja existe uma instancia configurada. Delete-a primeiro para criar uma nova.");
        }

        // Verificar se a API global esta configurada
        if (evolutionGlobalApiKey == null || evolutionGlobalApiKey.isBlank()) {
            log.error("API Key global da Evolution nao configurada");
            return CriarInstanciaResponse.falha("Evolution API nao configurada no servidor. Contate o administrador.");
        }

        // Gerar nome unico da instancia
        String instanceName = "pitstop_" + oficinaId.toString().substring(0, 8);

        log.info("Criando instancia Evolution para oficina {}: {}", oficinaId, instanceName);

        // Chamar Evolution API para criar instancia
        var resultado = evolutionApiClient.criarInstancia(evolutionApiUrl, evolutionGlobalApiKey, instanceName);

        if (!resultado.sucesso()) {
            log.error("Falha ao criar instancia para oficina {}: {}", oficinaId, resultado.erroMensagem());
            return CriarInstanciaResponse.falha(resultado.erroMensagem());
        }

        // Salvar configuracao
        config.setEvolutionApiUrl(evolutionApiUrl);
        config.setEvolutionApiToken(resultado.instanceToken());
        config.setEvolutionInstanceName(resultado.instanceName());
        config = repository.save(config);

        log.info("Instancia criada com sucesso para oficina {}: {}", oficinaId, instanceName);

        return CriarInstanciaResponse.sucesso(resultado.instanceName(), resultado.qrCode());
    }

    /**
     * Deleta a instancia na Evolution API e limpa as configuracoes.
     *
     * @return true se deletou com sucesso
     */
    @Transactional
    public boolean deletarInstancia() {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoNotificacao config = getOrCreateConfig(oficinaId);

        // Verificar se tem instancia configurada
        if (config.getEvolutionInstanceName() == null || config.getEvolutionInstanceName().isBlank()) {
            log.warn("Oficina {} nao possui instancia configurada", oficinaId);
            return false;
        }

        // Criar config para chamar API
        EvolutionConfig evolutionConfig = new EvolutionConfig(
            config.getEvolutionApiUrl(),
            config.getEvolutionApiToken(),
            config.getEvolutionInstanceName(),
            config.getWhatsappNumero()
        );

        // Tentar deletar na Evolution API
        boolean deletado = evolutionApiClient.deletarInstancia(evolutionConfig);

        // Limpar configuracao local (mesmo que falhe na API)
        config.setEvolutionApiToken(null);
        config.setEvolutionInstanceName(null);
        config.setWhatsappNumero(null);
        config.setWhatsappHabilitado(false);
        repository.save(config);

        log.info("Configuracao de instancia removida para oficina: {}", oficinaId);

        return deletado;
    }

    /**
     * Desconecta (logout) a instancia na Evolution API.
     *
     * @return true se desconectou com sucesso
     */
    public boolean desconectarInstancia() {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoNotificacao config = getOrCreateConfig(oficinaId);

        // Verificar se tem instancia configurada
        if (config.getEvolutionInstanceName() == null || config.getEvolutionInstanceName().isBlank()) {
            log.warn("Oficina {} nao possui instancia configurada", oficinaId);
            return false;
        }

        // Criar config para chamar API
        EvolutionConfig evolutionConfig = new EvolutionConfig(
            config.getEvolutionApiUrl(),
            config.getEvolutionApiToken(),
            config.getEvolutionInstanceName(),
            config.getWhatsappNumero()
        );

        boolean desconectado = evolutionApiClient.desconectarInstancia(evolutionConfig);

        if (desconectado) {
            log.info("Instancia desconectada para oficina: {}", oficinaId);
        }

        return desconectado;
    }

    /**
     * Reconecta (reinicia) a instancia na Evolution API.
     *
     * @return true se reconectou com sucesso
     */
    public boolean reconectarInstancia() {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoNotificacao config = getOrCreateConfig(oficinaId);

        // Verificar se tem instancia configurada
        if (config.getEvolutionInstanceName() == null || config.getEvolutionInstanceName().isBlank()) {
            log.warn("Oficina {} nao possui instancia configurada", oficinaId);
            return false;
        }

        // Criar config para chamar API
        EvolutionConfig evolutionConfig = new EvolutionConfig(
            config.getEvolutionApiUrl(),
            config.getEvolutionApiToken(),
            config.getEvolutionInstanceName(),
            config.getWhatsappNumero()
        );

        boolean reiniciado = evolutionApiClient.reiniciarInstancia(evolutionConfig);

        if (reiniciado) {
            log.info("Instancia reiniciada para oficina: {}", oficinaId);
        }

        return reiniciado;
    }

    // ===== METODOS AUXILIARES =====

    /**
     * Obtem ou cria configuracao padrao para uma oficina.
     */
    private ConfiguracaoNotificacao getOrCreateConfig(UUID oficinaId) {
        return repository.findByOficinaId(oficinaId)
            .orElseGet(() -> {
                log.info("Criando configuracao padrao para oficina: {}", oficinaId);
                ConfiguracaoNotificacao nova = ConfiguracaoNotificacao.builder()
                    .oficinaId(oficinaId)
                    .emailHabilitado(true)
                    .whatsappHabilitado(false)
                    .smsHabilitado(false)
                    .telegramHabilitado(false)
                    .build();
                return repository.save(nova);
            });
    }
}
