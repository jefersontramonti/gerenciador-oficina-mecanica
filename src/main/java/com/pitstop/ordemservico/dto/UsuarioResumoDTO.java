package com.pitstop.ordemservico.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO resumido de Usuário para inclusão em respostas de OS.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Schema(description = "Dados resumidos de usuário/mecânico")
public record UsuarioResumoDTO(

    @Schema(description = "ID do usuário", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,

    @Schema(description = "Nome do usuário", example = "João Pedro Almeida")
    String nome,

    @Schema(description = "Email", example = "mecanico@pitstop.com")
    String email,

    @Schema(description = "Perfil", example = "MECANICO")
    String perfil
) {
}
