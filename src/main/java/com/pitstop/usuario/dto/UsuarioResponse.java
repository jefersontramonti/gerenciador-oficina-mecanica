package com.pitstop.usuario.dto;

import com.pitstop.usuario.domain.PerfilUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta contendo informações de um usuário.
 * Este DTO é retornado nas consultas e nunca expõe a senha.
 */
@Schema(description = "Resposta contendo dados de um usuário")
public record UsuarioResponse(

        @Schema(description = "ID único do usuário", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Nome completo do usuário", example = "João Silva")
        String nome,

        @Schema(description = "Email do usuário", example = "joao.silva@pitstop.com")
        String email,

        @Schema(description = "Perfil de acesso do usuário", example = "ATENDENTE")
        PerfilUsuario perfil,

        @Schema(description = "Nome descritivo do perfil", example = "Atendente")
        String perfilNome,

        @Schema(description = "Indica se o usuário está ativo", example = "true")
        Boolean ativo,

        @Schema(description = "Data/hora do último acesso do usuário", example = "2025-01-15T10:30:00")
        LocalDateTime ultimoAcesso,

        @Schema(description = "Data/hora de criação do usuário", example = "2025-01-10T08:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Data/hora da última atualização", example = "2025-01-15T14:20:00")
        LocalDateTime updatedAt
) {

    /**
     * Construtor auxiliar que popula automaticamente o nome do perfil.
     */
    public UsuarioResponse(UUID id, String nome, String email, PerfilUsuario perfil,
                          Boolean ativo, LocalDateTime ultimoAcesso,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, nome, email, perfil, perfil.getNome(), ativo, ultimoAcesso, createdAt, updatedAt);
    }
}
