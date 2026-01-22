package com.pitstop.webhook.dto;

import lombok.Builder;

/**
 * DTO para estatísticas de webhooks de uma oficina.
 *
 * @author PitStop Team
 */
@Builder
public record WebhookStatsDTO(
    long totalWebhooks,
    long webhooksAtivos,
    long webhooksInativos,
    long sucessos24h,
    long falhas24h,
    Double tempoMedioResposta24h,
    long pendentesRetry
) {}
