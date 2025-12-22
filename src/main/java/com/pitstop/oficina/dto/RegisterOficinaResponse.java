package com.pitstop.oficina.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for oficina registration.
 * Contains the created oficina and admin user identifiers along with JWT authentication tokens.
 *
 * After successful registration, the client should:
 * 1. Store the accessToken for API requests (in memory, not localStorage)
 * 2. Store the refreshToken in HttpOnly cookie or secure storage
 * 3. Use oficinaId and adminUserId for context awareness
 * 4. Refresh the token before expiresAt using /api/auth/refresh endpoint
 *
 * @param oficinaId Unique identifier of the created oficina (workshop)
 * @param adminUserId Unique identifier of the created admin user
 * @param accessToken JWT access token with 15 minutes validity - use in Authorization header
 * @param refreshToken JWT refresh token with 7 days validity - use for token refresh
 * @param expiresAt Timestamp when the access token expires (UTC)
 *
 * @author PitStop Development Team
 * @version 1.0
 * @since 2025-12-21
 */
public record RegisterOficinaResponse(
    UUID oficinaId,
    UUID adminUserId,
    String accessToken,
    String refreshToken,
    LocalDateTime expiresAt
) {
}
