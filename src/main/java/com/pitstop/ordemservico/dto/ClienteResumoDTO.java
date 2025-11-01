package com.pitstop.ordemservico.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO resumido de Cliente para inclusão em respostas de OS.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Schema(description = "Dados resumidos de cliente")
public record ClienteResumoDTO(

    @Schema(description = "ID do cliente", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,

    @Schema(description = "Nome do cliente", example = "José da Silva")
    String nome,

    @Schema(description = "CPF/CNPJ", example = "12345678901")
    String cpfCnpj,

    @Schema(description = "Telefone/celular", example = "11987654321")
    String telefone,

    @Schema(description = "Email", example = "jose@email.com")
    String email
) {
}
