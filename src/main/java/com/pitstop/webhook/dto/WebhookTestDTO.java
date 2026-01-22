package com.pitstop.webhook.dto;

import com.pitstop.webhook.domain.TipoEventoWebhook;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO para testar um webhook com payload de exemplo.
 *
 * @author PitStop Team
 */
public record WebhookTestDTO(
    @NotNull(message = "ID do webhook é obrigatório")
    UUID webhookId,

    @NotNull(message = "Evento de teste é obrigatório")
    TipoEventoWebhook evento
) {}
