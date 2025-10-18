package com.pitstop.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for successful token refresh.
 *
 * <p>Contains new access and refresh tokens (token rotation).
 *
 * <p><b>Token rotation:</b>
 * For security reasons, when a refresh token is used, both the access token AND
 * the refresh token are rotated (new tokens generated, old refresh token invalidated).
 */
@Schema(description = "Response body for successful token refresh")
public record RefreshResponse(

        @Schema(description = "New JWT access token (15 minutes validity)", example = "eyJhbGciOiJIUzUxMiJ9...")
        String accessToken,

        @Schema(description = "New JWT refresh token (7 days validity)", example = "eyJhbGciOiJIUzUxMiJ9...")
        String refreshToken
) {}
