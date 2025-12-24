package com.pitstop.notificacao.dto;

import com.pitstop.notificacao.domain.ConfiguracaoNotificacao;
import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO para visualizacao de configuracao de notificacao.
 *
 * @author PitStop Team
 */
@Builder
public record ConfiguracaoNotificacaoDTO(
    UUID id,
    UUID oficinaId,

    // Canais habilitados
    Boolean emailHabilitado,
    Boolean whatsappHabilitado,
    Boolean smsHabilitado,
    Boolean telegramHabilitado,

    // Status de configuracao
    Boolean temSmtpProprio,
    Boolean temEvolutionApiConfigurada,
    Boolean temTelegramConfigurado,

    // Dados do SMTP (sem senha)
    String smtpHost,
    Integer smtpPort,
    String smtpUsername,
    Boolean smtpUsarTls,
    String emailRemetente,
    String emailRemetenteNome,

    // Dados do WhatsApp (sem token)
    String evolutionApiUrl,
    String evolutionInstanceName,
    String whatsappNumero,
    Boolean evolutionApiConfigurada,

    // Telegram (sem token)
    String telegramChatId,
    Boolean telegramConfigurado,

    // Horario comercial
    Boolean respeitarHorarioComercial,
    LocalTime horarioInicio,
    LocalTime horarioFim,
    Boolean enviarSabados,
    Boolean enviarDomingos,

    // Eventos habilitados
    Map<EventoNotificacao, ConfiguracaoNotificacao.EventoConfig> eventosHabilitados,

    // Configuracoes avancadas
    Integer delayEntreEnviosMs,
    Integer maxTentativasReenvio,
    Boolean modoSimulacao,
    TipoNotificacao canalFallback,

    // Status
    Boolean ativo,
    Boolean podeEnviarAgora,

    // Auditoria
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Converte entidade para DTO.
     * Remove dados sensiveis (senhas e tokens).
     */
    public static ConfiguracaoNotificacaoDTO fromEntity(ConfiguracaoNotificacao entity) {
        return ConfiguracaoNotificacaoDTO.builder()
            .id(entity.getId())
            .oficinaId(entity.getOficinaId())
            // Canais
            .emailHabilitado(entity.getEmailHabilitado())
            .whatsappHabilitado(entity.getWhatsappHabilitado())
            .smsHabilitado(entity.getSmsHabilitado())
            .telegramHabilitado(entity.getTelegramHabilitado())
            // Status de configuracao
            .temSmtpProprio(entity.temSmtpProprio())
            .temEvolutionApiConfigurada(entity.temEvolutionApiConfigurada())
            .temTelegramConfigurado(entity.temTelegramConfigurado())
            // SMTP (sem senha)
            .smtpHost(entity.getSmtpHost())
            .smtpPort(entity.getSmtpPort())
            .smtpUsername(entity.getSmtpUsername())
            .smtpUsarTls(entity.getSmtpUsarTls())
            .emailRemetente(entity.getEmailRemetente())
            .emailRemetenteNome(entity.getEmailRemetenteNome())
            // WhatsApp (sem token)
            .evolutionApiUrl(entity.getEvolutionApiUrl())
            .evolutionInstanceName(entity.getEvolutionInstanceName())
            .whatsappNumero(entity.getWhatsappNumero())
            .evolutionApiConfigurada(entity.temEvolutionApiConfigurada())
            // Telegram (sem token)
            .telegramChatId(entity.getTelegramChatId())
            .telegramConfigurado(entity.temTelegramConfigurado())
            // Horario comercial
            .respeitarHorarioComercial(entity.getRespeitarHorarioComercial())
            .horarioInicio(entity.getHorarioInicio())
            .horarioFim(entity.getHorarioFim())
            .enviarSabados(entity.getEnviarSabados())
            .enviarDomingos(entity.getEnviarDomingos())
            // Eventos
            .eventosHabilitados(entity.getEventosHabilitados())
            // Avancadas
            .delayEntreEnviosMs(entity.getDelayEntreEnviosMs())
            .maxTentativasReenvio(entity.getMaxTentativasReenvio())
            .modoSimulacao(entity.getModoSimulacao())
            .canalFallback(entity.getCanalFallback())
            // Status
            .ativo(entity.getAtivo())
            .podeEnviarAgora(entity.podeEnviarAgora())
            // Auditoria
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
