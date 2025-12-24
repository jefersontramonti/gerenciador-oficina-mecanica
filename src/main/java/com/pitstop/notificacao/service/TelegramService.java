package com.pitstop.notificacao.service;

import com.pitstop.notificacao.domain.ConfiguracaoNotificacao;
import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.HistoricoNotificacao;
import com.pitstop.notificacao.domain.StatusNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import com.pitstop.notificacao.integration.telegram.TelegramApiClient;
import com.pitstop.notificacao.integration.telegram.TelegramConfig;
import com.pitstop.notificacao.integration.telegram.TelegramSendResult;
import com.pitstop.notificacao.repository.ConfiguracaoNotificacaoRepository;
import com.pitstop.notificacao.repository.HistoricoNotificacaoRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Servico para envio de notificacoes via Telegram.
 *
 * @author PitStop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramService {

    private final TelegramApiClient telegramApiClient;
    private final ConfiguracaoNotificacaoRepository configuracaoRepository;
    private final HistoricoNotificacaoRepository historicoRepository;
    private final TemplateService templateService;

    /**
     * Envia mensagem de Telegram de forma sincrona.
     *
     * @param chatId Chat ID do destinatario (pode ser diferente do configurado)
     * @param nomeDestinatario Nome do destinatario
     * @param mensagem Mensagem a enviar
     * @param evento Evento que disparou
     * @param variaveis Variaveis do template
     * @param ordemServicoId ID da OS relacionada
     * @param clienteId ID do cliente
     * @param usuarioId ID do usuario que disparou (null = automatico)
     * @return Historico da notificacao
     */
    @Transactional
    public HistoricoNotificacao enviar(
        String chatId,
        String nomeDestinatario,
        String mensagem,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId,
        UUID usuarioId
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        // Busca configuracao da oficina
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        // Cria registro de historico
        HistoricoNotificacao historico = HistoricoNotificacao.criar(
            oficinaId,
            evento,
            TipoNotificacao.TELEGRAM,
            chatId != null ? chatId : (config != null ? config.getTelegramChatId() : null),
            nomeDestinatario,
            null, // Telegram nao tem assunto
            mensagem,
            variaveis,
            null, // templateId
            ordemServicoId,
            clienteId,
            usuarioId
        );

        // Valida configuracao
        if (config == null) {
            historico.marcarComoFalha("Configuracao de notificacao nao encontrada", "CONFIG_NOT_FOUND");
            return historicoRepository.save(historico);
        }

        if (!config.getTelegramHabilitado()) {
            historico.marcarComoFalha("Telegram nao esta habilitado para esta oficina", "TELEGRAM_DISABLED");
            return historicoRepository.save(historico);
        }

        if (!config.temTelegramConfigurado()) {
            historico.marcarComoFalha("Telegram nao esta configurado (bot token ou chat id ausente)", "TELEGRAM_NOT_CONFIGURED");
            return historicoRepository.save(historico);
        }

        // Verifica horario comercial
        if (!config.podeEnviarAgora()) {
            historico.agendar(calcularProximoHorario(config));
            return historicoRepository.save(historico);
        }

        // Modo simulacao
        if (config.getModoSimulacao()) {
            log.info("[SIMULACAO] Telegram para {}: {}", chatId, mensagem);
            historico.marcarComoEnviado("SIMULADO-" + UUID.randomUUID());
            return historicoRepository.save(historico);
        }

        // Determina o chat ID a usar
        String targetChatId = chatId != null ? chatId : config.getTelegramChatId();

        // Envia via Telegram API
        TelegramConfig telegramConfig = TelegramConfig.from(config);
        TelegramSendResult result = telegramApiClient.enviarTexto(telegramConfig, targetChatId, mensagem);

        if (result.sucesso()) {
            historico.marcarComoEnviado(result.messageId());
            historico.setRespostaApi(Map.of("response", result.respostaJson()));
        } else {
            historico.marcarComoFalha(result.erroMensagem(), result.erroCodigo());
            if (result.respostaJson() != null) {
                historico.setRespostaApi(Map.of("error", result.respostaJson()));
            }
        }

        return historicoRepository.save(historico);
    }

    /**
     * Envia mensagem de Telegram para o chat padrao configurado.
     */
    @Transactional
    public HistoricoNotificacao enviar(
        String nomeDestinatario,
        String mensagem,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId,
        UUID usuarioId
    ) {
        return enviar(null, nomeDestinatario, mensagem, evento, variaveis, ordemServicoId, clienteId, usuarioId);
    }

    /**
     * Envia mensagem de Telegram de forma assincrona.
     */
    @Async
    @Transactional
    public void enviarAsync(
        String chatId,
        String nomeDestinatario,
        String mensagem,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId,
        UUID usuarioId
    ) {
        enviar(chatId, nomeDestinatario, mensagem, evento, variaveis, ordemServicoId, clienteId, usuarioId);
    }

    /**
     * Envia mensagem usando template.
     *
     * @param chatId Chat ID do destinatario (null = usa configuracao)
     * @param nomeDestinatario Nome do destinatario
     * @param evento Evento que disparou
     * @param variaveis Variaveis do template
     * @param ordemServicoId ID da OS
     * @param clienteId ID do cliente
     * @param usuarioId ID do usuario
     * @return Historico da notificacao
     */
    @Transactional
    public HistoricoNotificacao enviarComTemplate(
        String chatId,
        String nomeDestinatario,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId,
        UUID usuarioId
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        // Renderiza o template (usa o template de WhatsApp que tem formato similar)
        var template = templateService.obterTemplate(
            oficinaId,
            evento.getTemplatePadrao(),
            TipoNotificacao.WHATSAPP // Telegram usa formato similar ao WhatsApp
        );
        String mensagem = templateService.processarCorpo(template, variaveis);

        return enviar(
            chatId,
            nomeDestinatario,
            mensagem,
            evento,
            variaveis,
            ordemServicoId,
            clienteId,
            usuarioId
        );
    }

    /**
     * Verifica se o bot esta funcionando.
     *
     * @param oficinaId ID da oficina
     * @return true se o bot responde corretamente
     */
    public boolean verificarConexao(UUID oficinaId) {
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        if (config == null || !config.temTelegramConfigurado()) {
            return false;
        }

        TelegramConfig telegramConfig = TelegramConfig.from(config);
        return telegramApiClient.verificarConexao(telegramConfig);
    }

    /**
     * Obtem informacoes do bot configurado.
     *
     * @param oficinaId ID da oficina
     * @return Informacoes do bot ou null
     */
    public TelegramApiClient.TelegramBotInfo getBotInfo(UUID oficinaId) {
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        if (config == null || !config.temTelegramConfigurado()) {
            return null;
        }

        TelegramConfig telegramConfig = TelegramConfig.from(config);
        return telegramApiClient.getMe(telegramConfig);
    }

    /**
     * Reenvia uma notificacao que falhou.
     *
     * @param historicoId ID do historico
     * @return Historico atualizado
     */
    @Transactional
    public HistoricoNotificacao reenviar(UUID historicoId) {
        HistoricoNotificacao historico = historicoRepository.findById(historicoId)
            .orElseThrow(() -> new IllegalArgumentException("Historico nao encontrado"));

        if (!historico.getStatus().permiteReenvio()) {
            throw new IllegalStateException("Esta notificacao nao pode ser reenviada. Status: " + historico.getStatus());
        }

        UUID oficinaId = historico.getOficinaId();
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElseThrow(() -> new IllegalStateException("Configuracao nao encontrada"));

        if (!historico.podeRetentar(config.getMaxTentativasReenvio())) {
            throw new IllegalStateException("Numero maximo de tentativas atingido");
        }

        // Modo simulacao
        if (config.getModoSimulacao()) {
            log.info("[SIMULACAO] Reenvio Telegram para {}", historico.getDestinatario());
            historico.marcarComoEnviado("SIMULADO-REENVIO-" + UUID.randomUUID());
            return historicoRepository.save(historico);
        }

        // Reenvia
        TelegramConfig telegramConfig = TelegramConfig.from(config);
        TelegramSendResult result = telegramApiClient.enviarTexto(
            telegramConfig,
            historico.getDestinatario(),
            historico.getMensagem()
        );

        if (result.sucesso()) {
            historico.marcarComoEnviado(result.messageId());
        } else {
            historico.marcarComoFalha(result.erroMensagem(), result.erroCodigo());
        }

        return historicoRepository.save(historico);
    }

    // ===== METODOS AUXILIARES =====

    private java.time.LocalDateTime calcularProximoHorario(ConfiguracaoNotificacao config) {
        java.time.LocalDateTime agora = java.time.LocalDateTime.now();
        java.time.LocalTime horarioInicio = config.getHorarioInicio();

        // Se ainda hoje, no horario de inicio
        if (agora.toLocalTime().isBefore(horarioInicio)) {
            return agora.toLocalDate().atTime(horarioInicio);
        }

        // Proximo dia util
        java.time.LocalDate proximoDia = agora.toLocalDate().plusDays(1);
        while (!isDiaPermitido(proximoDia, config)) {
            proximoDia = proximoDia.plusDays(1);
        }

        return proximoDia.atTime(horarioInicio);
    }

    private boolean isDiaPermitido(java.time.LocalDate data, ConfiguracaoNotificacao config) {
        java.time.DayOfWeek dia = data.getDayOfWeek();
        if (dia == java.time.DayOfWeek.SATURDAY && !config.getEnviarSabados()) {
            return false;
        }
        if (dia == java.time.DayOfWeek.SUNDAY && !config.getEnviarDomingos()) {
            return false;
        }
        return true;
    }
}
