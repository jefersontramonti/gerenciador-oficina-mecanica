package com.pitstop.usuario.dto;

import com.pitstop.usuario.domain.PerfilUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para criação de novos usuários.
 * Contém todas as informações necessárias para criar um usuário no sistema.
 */
@Schema(description = "Requisição para criação de novo usuário")
public record CreateUsuarioRequest(

        @Schema(description = "Nome completo do usuário", example = "João Silva")
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
        String nome,

        @Schema(description = "Email do usuário (usado para login)", example = "joao.silva@pitstop.com")
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String email,

        @Schema(description = "Senha do usuário (mínimo 6 caracteres)", example = "senha123")
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, max = 50, message = "Senha deve ter entre 6 e 50 caracteres")
        String senha,

        @Schema(description = "Perfil de acesso do usuário", example = "ATENDENTE")
        @NotNull(message = "Perfil é obrigatório")
        PerfilUsuario perfil
) {
}
