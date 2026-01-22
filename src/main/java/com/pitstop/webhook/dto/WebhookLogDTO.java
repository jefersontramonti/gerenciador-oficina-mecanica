package com.pitstop.webhook.dto;

import com.pitstop.webhook.domain.StatusWebhookLog;
import com.pitstop.webhook.domain.TipoEventoWebhook;
import com.pitstop.webhook.domain.WebhookLog;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para visualização de log de webhook.
 *
 * @author PitStop Team
 */
@Builder
public record WebhookLogDTO(
    UUID id,
    UUID webhookConfigId,
    String webhookNome,
    UUID oficinaId,
    TipoEventoWebhook evento,
    String eventoNome,
    UUID entidadeId,
    String entidadeTipo,
    String url,
    String payload,
    Integer httpStatus,
    String responseBody,
    String erroMensagem,
    Long tempoRespostaMs,
    Integer tentativa,
    StatusWebhookLog status,
    String statusDescricao,
    LocalDateTime proximaTentativa,
    LocalDateTime createdAt
) {
    /**
     * Converte entidade para DTO.
     */
    public static WebhookLogDTO fromEntity(WebhookLog entity) {
        return WebhookLogDTO.builder()
            .id(entity.getId())
            .webhookConfigId(entity.getWebhookConfig().getId())
            .webhookNome(entity.getWebhookConfig().getNome())
            .oficinaId(entity.getOficinaId())
            .evento(entity.getEvento())
            .eventoNome(entity.getEvento().getNome())
            .entidadeId(entity.getEntidadeId())
            .entidadeTipo(entity.getEntidadeTipo())
            .url(entity.getUrl())
            .payload(entity.getPayload())
            .httpStatus(entity.getHttpStatus())
            .responseBody(entity.getResponseBody())
            .erroMensagem(entity.getErroMensagem())
            .tempoRespostaMs(entity.getTempoRespostaMs())
            .tentativa(entity.getTentativa())
            .status(entity.getStatus())
            .statusDescricao(entity.getStatus().getDescricao())
            .proximaTentativa(entity.getProximaTentativa())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
