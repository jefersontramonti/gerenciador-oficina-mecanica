package com.pitstop.usuario.dto;

import com.pitstop.usuario.domain.PerfilUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO para atualização de usuários existentes.
 * Todos os campos são opcionais - apenas os campos fornecidos serão atualizados.
 */
@Schema(description = "Requisição para atualização de usuário existente")
public record UpdateUsuarioRequest(

        @Schema(description = "Novo nome do usuário", example = "João Silva Santos")
        @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
        String nome,

        @Schema(description = "Novo email do usuário", example = "joao.santos@pitstop.com")
        @Email(message = "Email deve ser válido")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String email,

        @Schema(description = "Nova senha do usuário (mínimo 6 caracteres)", example = "novaSenha456")
        @Size(min = 6, max = 50, message = "Senha deve ter entre 6 e 50 caracteres")
        String senha,

        @Schema(description = "Novo perfil de acesso", example = "GERENTE")
        PerfilUsuario perfil,

        @Schema(description = "Status ativo/inativo do usuário", example = "true")
        Boolean ativo
) {
}
