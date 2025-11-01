package com.pitstop.ordemservico.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO resumido de Veículo para inclusão em respostas de OS.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Schema(description = "Dados resumidos de veículo")
public record VeiculoResumoDTO(

    @Schema(description = "ID do veículo", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,

    @Schema(description = "Placa do veículo", example = "ABC1234")
    String placa,

    @Schema(description = "Marca do veículo", example = "Volkswagen")
    String marca,

    @Schema(description = "Modelo do veículo", example = "Gol")
    String modelo,

    @Schema(description = "Ano do veículo", example = "2020")
    Integer ano,

    @Schema(description = "Cor do veículo", example = "Prata")
    String cor
) {
}
