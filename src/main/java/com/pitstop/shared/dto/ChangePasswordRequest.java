package com.pitstop.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for changing user password.
 *
 * <p>Requires the current password for security verification.
 * The new password must meet minimum security requirements.
 */
@Schema(description = "Request body for changing password")
public record ChangePasswordRequest(

        @Schema(description = "Current password", example = "senha123")
        @NotBlank(message = "Senha atual é obrigatória")
        String senhaAtual,

        @Schema(description = "New password (min 6 characters)", example = "novaSenha123")
        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 6, message = "Nova senha deve ter no mínimo 6 caracteres")
        String novaSenha
) {}
