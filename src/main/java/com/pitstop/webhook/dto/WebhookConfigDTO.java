package com.pitstop.webhook.dto;

import com.pitstop.webhook.domain.TipoEventoWebhook;
import com.pitstop.webhook.domain.WebhookConfig;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * DTO para visualização de configuração de webhook.
 * Omite dados sensíveis como secret.
 *
 * @author PitStop Team
 */
@Builder
public record WebhookConfigDTO(
    UUID id,
    UUID oficinaId,
    String nome,
    String descricao,
    String url,
    boolean temSecret,
    Map<String, String> headers,
    Set<TipoEventoWebhook> eventos,
    Integer maxTentativas,
    Integer timeoutSegundos,
    Boolean ativo,
    Integer falhasConsecutivas,
    LocalDateTime ultimaExecucaoSucesso,
    LocalDateTime ultimaFalha,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Converte entidade para DTO.
     * Remove dados sensíveis (secret).
     */
    public static WebhookConfigDTO fromEntity(WebhookConfig entity) {
        return WebhookConfigDTO.builder()
            .id(entity.getId())
            .oficinaId(entity.getOficinaId())
            .nome(entity.getNome())
            .descricao(entity.getDescricao())
            .url(entity.getUrl())
            .temSecret(entity.getSecret() != null && !entity.getSecret().isBlank())
            .headers(parseHeaders(entity.getHeadersJson()))
            .eventos(entity.getEventos())
            .maxTentativas(entity.getMaxTentativas())
            .timeoutSegundos(entity.getTimeoutSegundos())
            .ativo(entity.getAtivo())
            .falhasConsecutivas(entity.getFalhasConsecutivas())
            .ultimaExecucaoSucesso(entity.getUltimaExecucaoSucesso())
            .ultimaFalha(entity.getUltimaFalha())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private static Map<String, String> parseHeaders(String headersJson) {
        if (headersJson == null || headersJson.isBlank()) {
            return Map.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(headersJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
