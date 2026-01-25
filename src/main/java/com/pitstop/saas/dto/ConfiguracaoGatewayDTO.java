package com.pitstop.saas.dto;

import com.pitstop.saas.domain.ConfiguracaoGateway;
import com.pitstop.saas.domain.TipoGateway;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for gateway configuration (for viewing).
 * Sensitive data is masked.
 */
public record ConfiguracaoGatewayDTO(
    UUID id,
    TipoGateway tipo,
    String tipoNome,
    Boolean ativo,
    Boolean sandbox,
    String accessTokenMasked,
    String publicKeyMasked,
    boolean temWebhookSecret,
    String webhookUrl,
    LocalDateTime ultimaValidacao,
    Boolean validacaoSucesso,
    String mensagemValidacao,
    boolean configurado,
    LocalDateTime updatedAt,
    // Diagnostic info for debugging webhook issues
    boolean notificationUrlAtiva,
    String baseUrlConfigurada
) {
    public static ConfiguracaoGatewayDTO fromEntity(ConfiguracaoGateway config) {
        return fromEntity(config, null);
    }

    public static ConfiguracaoGatewayDTO fromEntity(ConfiguracaoGateway config, String baseUrl) {
        boolean notificationAtiva = baseUrl != null &&
            !baseUrl.contains("localhost") &&
            !baseUrl.contains("127.0.0.1");

        return new ConfiguracaoGatewayDTO(
            config.getId(),
            config.getTipo(),
            config.getTipo().getNome(),
            config.getAtivo(),
            config.getSandbox(),
            config.getAccessTokenMasked(),
            config.getPublicKeyMasked(),
            config.getWebhookSecret() != null && !config.getWebhookSecret().isBlank(),
            config.getWebhookUrl(),
            config.getUltimaValidacao(),
            config.getValidacaoSucesso(),
            config.getMensagemValidacao(),
            config.isConfigurado(),
            config.getUpdatedAt(),
            notificationAtiva,
            baseUrl
        );
    }
}
