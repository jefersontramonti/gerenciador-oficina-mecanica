package com.pitstop.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for resetting password with token.
 *
 * <p>Used when a user clicks the reset link and provides a new password.
 */
@Schema(description = "Request body for resetting password")
public record ResetPasswordRequest(

        @Schema(description = "Password reset token from email", example = "abc123def456")
        @NotBlank(message = "Token é obrigatório")
        String token,

        @Schema(description = "New password (min 6 characters)", example = "newPassword123")
        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 6, message = "Nova senha deve ter no mínimo 6 caracteres")
        String novaSenha
) {}
