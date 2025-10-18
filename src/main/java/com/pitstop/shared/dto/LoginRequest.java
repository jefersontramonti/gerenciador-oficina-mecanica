package com.pitstop.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 *
 * <p>Contains the user's email and password for authentication.
 */
@Schema(description = "Request body for user login")
public record LoginRequest(

        @Schema(description = "User email address", example = "admin@pitstop.com")
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @Schema(description = "User password", example = "admin123")
        @NotBlank(message = "Senha é obrigatória")
        String senha
) {}
