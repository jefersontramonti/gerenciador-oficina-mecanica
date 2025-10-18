package com.pitstop.shared.dto;

import com.pitstop.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for successful login.
 *
 * <p>Contains:
 * <ul>
 *   <li><b>accessToken</b>: Short-lived JWT (15 minutes) for API authentication</li>
 *   <li><b>refreshToken</b>: Long-lived JWT (7 days) for renewing access tokens</li>
 *   <li><b>usuario</b>: User information (without password)</li>
 * </ul>
 *
 * <p><b>Security note:</b>
 * The refresh token should also be sent as an HttpOnly cookie for XSS protection.
 */
@Schema(description = "Response body for successful login")
public record LoginResponse(

        @Schema(description = "JWT access token (15 minutes validity)", example = "eyJhbGciOiJIUzUxMiJ9...")
        String accessToken,

        @Schema(description = "JWT refresh token (7 days validity)", example = "eyJhbGciOiJIUzUxMiJ9...")
        String refreshToken,

        @Schema(description = "Authenticated user information")
        UsuarioResponse usuario
) {}
