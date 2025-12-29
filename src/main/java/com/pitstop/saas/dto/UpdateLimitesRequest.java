package com.pitstop.saas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for updating workshop resource limits.
 *
 * Allows SUPER_ADMIN to customize limits beyond the plan defaults,
 * such as extra users, storage, or specific feature access.
 *
 * @author PitStop Team
 */
public record UpdateLimitesRequest(

    /**
     * Maximum number of users allowed for this workshop.
     * Set to null to use plan default.
     */
    @Min(value = 1, message = "Limite de usuários deve ser pelo menos 1")
    Integer limiteUsuarios,

    /**
     * Maximum storage space in bytes.
     * Set to null to use plan default.
     */
    @Min(value = 0, message = "Limite de espaço não pode ser negativo")
    Long limiteEspaco,

    /**
     * Maximum number of service orders per month.
     * Set to null for unlimited (based on plan).
     */
    @Min(value = 0, message = "Limite de OS não pode ser negativo")
    Integer limiteOSMes,

    /**
     * Maximum number of API calls per month.
     * Set to null to use plan default.
     */
    @Min(value = 0, message = "Limite de API calls não pode ser negativo")
    Integer limiteApiCalls,

    /**
     * Feature flags override for this specific workshop.
     * Key: feature code (e.g., "whatsapp", "reports_advanced")
     * Value: true to enable, false to disable
     *
     * These override plan-based feature settings.
     */
    Map<String, Boolean> features,

    /**
     * Optional notes about why limits were changed.
     * Stored in audit log.
     */
    String motivo
) {
    /**
     * Creates a limits update request.
     * All limits are optional - only specified limits will be updated.
     */
    public UpdateLimitesRequest {
        // Compact constructor
    }
}
