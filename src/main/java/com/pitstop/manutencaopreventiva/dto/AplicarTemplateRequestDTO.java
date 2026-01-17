package com.pitstop.manutencaopreventiva.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO para aplicar template em um veículo.
 */
public record AplicarTemplateRequestDTO(
    @NotNull(message = "Veículo é obrigatório")
    UUID veiculoId,

    LocalDate ultimaExecucaoData,

    Integer ultimaExecucaoKm
) {}
