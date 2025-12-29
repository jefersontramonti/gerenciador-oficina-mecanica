package com.pitstop.saas.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO para retornar as feature flags habilitadas para uma oficina específica
 */
public record OficinaFeatureFlagsDTO(
    UUID oficinaId,
    Map<String, Boolean> features
) {
    public boolean isEnabled(String featureCode) {
        return Boolean.TRUE.equals(features.get(featureCode));
    }
}
