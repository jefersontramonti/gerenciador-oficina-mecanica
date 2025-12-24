package com.pitstop.notificacao.dto;

import com.pitstop.notificacao.domain.ConfiguracaoNotificacao;
import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.time.LocalTime;
import java.util.Map;

/**
 * Request para criar/atualizar configuracao de notificacao.
 *
 * @author PitStop Team
 */
@Builder
public record ConfiguracaoNotificacaoRequest(
    // ===== CANAIS HABILITADOS =====
    Boolean emailHabilitado,
    Boolean whatsappHabilitado,
    Boolean smsHabilitado,
    Boolean telegramHabilitado,

    // ===== CONFIGURACOES DE EMAIL (SMTP Proprio) =====
    String smtpHost,
    @Min(value = 1, message = "Porta SMTP deve ser maior que 0")
    @Max(value = 65535, message = "Porta SMTP deve ser menor que 65536")
    Integer smtpPort,
    String smtpUsername,
    String smtpPassword,
    Boolean smtpUsarTls,
    String emailRemetente,
    String emailRemetenteNome,

    // ===== CONFIGURACOES DE WHATSAPP =====
    String evolutionApiUrl,
    String evolutionApiToken,
    String evolutionInstanceName,
    @Pattern(regexp = "^\\d{10,15}$", message = "Numero de WhatsApp deve conter apenas digitos (10-15)")
    String whatsappNumero,

    // ===== CONFIGURACOES DE TELEGRAM =====
    String telegramBotToken,
    String telegramChatId,

    // ===== HORARIO COMERCIAL =====
    Boolean respeitarHorarioComercial,
    LocalTime horarioInicio,
    LocalTime horarioFim,
    Boolean enviarSabados,
    Boolean enviarDomingos,

    // ===== EVENTOS HABILITADOS =====
    Map<EventoNotificacao, ConfiguracaoNotificacao.EventoConfig> eventosHabilitados,

    // ===== CONFIGURACOES AVANCADAS =====
    @Min(value = 100, message = "Delay minimo e 100ms")
    @Max(value = 60000, message = "Delay maximo e 60000ms")
    Integer delayEntreEnviosMs,

    @Min(value = 1, message = "Minimo de 1 tentativa")
    @Max(value = 10, message = "Maximo de 10 tentativas")
    Integer maxTentativasReenvio,

    Boolean modoSimulacao,
    TipoNotificacao canalFallback,

    Boolean ativo
) {
    /**
     * Aplica as configuracoes a uma entidade existente.
     * Campos nulos sao ignorados (partial update).
     */
    public void applyTo(ConfiguracaoNotificacao entity) {
        // Canais
        if (emailHabilitado != null) entity.setEmailHabilitado(emailHabilitado);
        if (whatsappHabilitado != null) entity.setWhatsappHabilitado(whatsappHabilitado);
        if (smsHabilitado != null) entity.setSmsHabilitado(smsHabilitado);
        if (telegramHabilitado != null) entity.setTelegramHabilitado(telegramHabilitado);

        // SMTP
        if (smtpHost != null) entity.setSmtpHost(smtpHost);
        if (smtpPort != null) entity.setSmtpPort(smtpPort);
        if (smtpUsername != null) entity.setSmtpUsername(smtpUsername);
        if (smtpPassword != null) entity.setSmtpPassword(smtpPassword);
        if (smtpUsarTls != null) entity.setSmtpUsarTls(smtpUsarTls);
        if (emailRemetente != null) entity.setEmailRemetente(emailRemetente);
        if (emailRemetenteNome != null) entity.setEmailRemetenteNome(emailRemetenteNome);

        // WhatsApp
        if (evolutionApiUrl != null) entity.setEvolutionApiUrl(evolutionApiUrl);
        if (evolutionApiToken != null) entity.setEvolutionApiToken(evolutionApiToken);
        if (evolutionInstanceName != null) entity.setEvolutionInstanceName(evolutionInstanceName);
        if (whatsappNumero != null) entity.setWhatsappNumero(whatsappNumero);

        // Telegram
        if (telegramBotToken != null) entity.setTelegramBotToken(telegramBotToken);
        if (telegramChatId != null) entity.setTelegramChatId(telegramChatId);

        // Horario comercial
        if (respeitarHorarioComercial != null) entity.setRespeitarHorarioComercial(respeitarHorarioComercial);
        if (horarioInicio != null) entity.setHorarioInicio(horarioInicio);
        if (horarioFim != null) entity.setHorarioFim(horarioFim);
        if (enviarSabados != null) entity.setEnviarSabados(enviarSabados);
        if (enviarDomingos != null) entity.setEnviarDomingos(enviarDomingos);

        // Eventos
        if (eventosHabilitados != null) entity.setEventosHabilitados(eventosHabilitados);

        // Avancadas
        if (delayEntreEnviosMs != null) entity.setDelayEntreEnviosMs(delayEntreEnviosMs);
        if (maxTentativasReenvio != null) entity.setMaxTentativasReenvio(maxTentativasReenvio);
        if (modoSimulacao != null) entity.setModoSimulacao(modoSimulacao);
        if (canalFallback != null) entity.setCanalFallback(canalFallback);

        // Status
        if (ativo != null) entity.setAtivo(ativo);
    }

    /**
     * Cria uma nova entidade a partir do request.
     */
    public ConfiguracaoNotificacao toEntity(java.util.UUID oficinaId) {
        ConfiguracaoNotificacao entity = ConfiguracaoNotificacao.builder()
            .oficinaId(oficinaId)
            .emailHabilitado(emailHabilitado != null ? emailHabilitado : true)
            .whatsappHabilitado(whatsappHabilitado != null ? whatsappHabilitado : false)
            .smsHabilitado(smsHabilitado != null ? smsHabilitado : false)
            .telegramHabilitado(telegramHabilitado != null ? telegramHabilitado : false)
            .smtpHost(smtpHost)
            .smtpPort(smtpPort)
            .smtpUsername(smtpUsername)
            .smtpPassword(smtpPassword)
            .smtpUsarTls(smtpUsarTls != null ? smtpUsarTls : true)
            .emailRemetente(emailRemetente)
            .emailRemetenteNome(emailRemetenteNome)
            .evolutionApiUrl(evolutionApiUrl)
            .evolutionApiToken(evolutionApiToken)
            .evolutionInstanceName(evolutionInstanceName)
            .whatsappNumero(whatsappNumero)
            .telegramBotToken(telegramBotToken)
            .telegramChatId(telegramChatId)
            .respeitarHorarioComercial(respeitarHorarioComercial != null ? respeitarHorarioComercial : true)
            .horarioInicio(horarioInicio != null ? horarioInicio : LocalTime.of(8, 0))
            .horarioFim(horarioFim != null ? horarioFim : LocalTime.of(18, 0))
            .enviarSabados(enviarSabados != null ? enviarSabados : true)
            .enviarDomingos(enviarDomingos != null ? enviarDomingos : false)
            .delayEntreEnviosMs(delayEntreEnviosMs != null ? delayEntreEnviosMs : 1000)
            .maxTentativasReenvio(maxTentativasReenvio != null ? maxTentativasReenvio : 3)
            .modoSimulacao(modoSimulacao != null ? modoSimulacao : false)
            .canalFallback(canalFallback)
            .ativo(ativo != null ? ativo : true)
            .build();

        if (eventosHabilitados != null) {
            entity.setEventosHabilitados(eventosHabilitados);
        }

        return entity;
    }
}
