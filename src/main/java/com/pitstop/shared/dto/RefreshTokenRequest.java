package com.pitstop.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for refreshing an access token.
 *
 * <p>Contains the refresh token obtained from a previous login or refresh operation.
 *
 * <p><b>Note:</b> In production, the refresh token should be sent as an HttpOnly cookie
 * instead of in the request body for better XSS protection.
 */
@Schema(description = "Request body for refreshing access token")
public record RefreshTokenRequest(

        @Schema(description = "Refresh token from login response", example = "eyJhbGciOiJIUzUxMiJ9...")
        @NotBlank(message = "Refresh token é obrigatório")
        String refreshToken
) {}
