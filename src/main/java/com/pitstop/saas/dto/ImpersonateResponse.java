package com.pitstop.saas.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for impersonation token generation.
 *
 * Contains a temporary access token that allows SUPER_ADMIN
 * to access the system as if they were a user from the workshop.
 * Used for support and troubleshooting purposes.
 *
 * Security: All impersonation actions are logged for audit.
 *
 * @author PitStop Team
 */
public record ImpersonateResponse(
    /**
     * Temporary JWT access token for the impersonated session.
     * Valid for a limited time (default: 1 hour).
     */
    String accessToken,

    /**
     * URL to redirect to after impersonation.
     * Typically the workshop's dashboard.
     */
    String redirectUrl,

    /**
     * When this impersonation token expires.
     * After this time, the token is invalid.
     */
    LocalDateTime expiresAt,

    /**
     * ID of the workshop being impersonated.
     */
    String oficinaId,

    /**
     * Name of the workshop for display purposes.
     */
    String oficinaNome,

    /**
     * Email of the user being impersonated (workshop admin).
     */
    String usuarioEmail
) {
    /**
     * Creates an impersonation response.
     * Token should be used immediately as it has limited validity.
     */
    public ImpersonateResponse {
        // Compact constructor
    }
}
