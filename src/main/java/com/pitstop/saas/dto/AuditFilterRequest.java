package com.pitstop.saas.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for filtering audit logs.
 *
 * All fields are optional. Multiple filters are combined with AND logic.
 * Used to support advanced search and reporting in the audit log interface.
 *
 * @author PitStop Team
 */
public record AuditFilterRequest(
    String acao,
    String entidade,
    UUID entidadeId,
    String usuarioEmail,
    LocalDateTime dataInicio,
    LocalDateTime dataFim,
    String ipAddress
) {
    /**
     * Creates a filter request for audit logs.
     *
     * @param acao Filter by action type (partial match)
     * @param entidade Filter by entity type (exact match)
     * @param entidadeId Filter by specific entity ID
     * @param usuarioEmail Filter by user email (partial match)
     * @param dataInicio Start of date range (inclusive)
     * @param dataFim End of date range (inclusive)
     * @param ipAddress Filter by IP address (exact match)
     */
    public AuditFilterRequest {
        // Compact constructor
    }
}
