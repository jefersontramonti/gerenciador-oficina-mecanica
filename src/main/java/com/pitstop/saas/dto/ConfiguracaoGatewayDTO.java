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
    LocalDateTime updatedAt
) {
    public static ConfiguracaoGatewayDTO fromEntity(ConfiguracaoGateway config) {
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
            config.getUpdatedAt()
        );
    }
}
