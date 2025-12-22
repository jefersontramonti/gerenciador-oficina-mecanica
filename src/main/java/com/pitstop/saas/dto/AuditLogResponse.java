package com.pitstop.saas.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for audit log entries.
 *
 * Represents an immutable record of administrative actions performed
 * by SUPER_ADMIN users. Used for compliance and security monitoring.
 *
 * @author PitStop Team
 */
public record AuditLogResponse(
    UUID id,
    String acao,
    String entidade,
    UUID entidadeId,
    String usuarioEmail,
    String detalhes,
    String ipAddress,
    String userAgent,
    LocalDateTime timestamp
) {
    /**
     * Creates an audit log response.
     *
     * @param id Log entry unique identifier
     * @param acao Action performed (CREATE_OFICINA, UPDATE_OFICINA, etc.)
     * @param entidade Entity type affected (Oficina, Pagamento, etc.)
     * @param entidadeId Affected entity's ID
     * @param usuarioEmail Email of user who performed the action
     * @param detalhes Additional context in JSON format
     * @param ipAddress IP address of the request
     * @param userAgent Browser/client user agent
     * @param timestamp When the action occurred
     */
    public AuditLogResponse {
        // Compact constructor
    }
}
