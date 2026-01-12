package com.pitstop.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating user profile.
 *
 * <p>Allows the authenticated user to update their name.
 * Email is optional (only updated if provided).
 * Password changes must be done through the dedicated endpoint.
 */
@Schema(description = "Request body for updating user profile")
public record UpdateProfileRequest(

        @Schema(description = "User full name", example = "João Silva")
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
        String nome,

        @Schema(description = "User email address (optional)", example = "joao.silva@example.com")
        @Email(message = "Email inválido")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String email
) {}
