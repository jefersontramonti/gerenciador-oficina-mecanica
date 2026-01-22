package com.pitstop.saas.dto;

import com.pitstop.saas.domain.TipoGateway;

/**
 * DTO for creating/updating gateway configuration.
 * Note: tipo is optional because specific endpoints (like /mercadopago) set it automatically.
 */
public record ConfiguracaoGatewayRequestDTO(
    TipoGateway tipo,

    Boolean ativo,

    Boolean sandbox,

    // Mercado Pago credentials
    String accessToken,
    String publicKey,
    String webhookSecret
) {
    /**
     * Check if access token was provided (not empty).
     */
    public boolean hasAccessToken() {
        return accessToken != null && !accessToken.isBlank();
    }

    /**
     * Check if public key was provided.
     */
    public boolean hasPublicKey() {
        return publicKey != null && !publicKey.isBlank();
    }

    /**
     * Check if webhook secret was provided.
     */
    public boolean hasWebhookSecret() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }
}
