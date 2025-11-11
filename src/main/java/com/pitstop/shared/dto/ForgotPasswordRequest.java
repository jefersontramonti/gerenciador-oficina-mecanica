package com.pitstop.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for forgot password operation.
 *
 * <p>Used when a user requests a password reset link.
 */
@Schema(description = "Request body for forgot password")
public record ForgotPasswordRequest(

        @Schema(description = "User email address", example = "user@example.com")
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email
) {}
