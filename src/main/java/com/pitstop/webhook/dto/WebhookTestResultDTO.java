package com.pitstop.webhook.dto;

import lombok.Builder;

/**
 * DTO para resultado de teste de webhook.
 *
 * @author PitStop Team
 */
@Builder
public record WebhookTestResultDTO(
    boolean sucesso,
    Integer httpStatus,
    String responseBody,
    String erro,
    Long tempoRespostaMs,
    String payloadEnviado
) {}
