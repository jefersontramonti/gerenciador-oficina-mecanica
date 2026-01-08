package com.pitstop.ia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO para solicitar diagnóstico via IA.
 */
public record DiagnosticoIARequest(
        @NotBlank(message = "Problemas relatados são obrigatórios")
        @Size(min = 20, max = 5000, message = "Problemas relatados devem ter entre 20 e 5000 caracteres")
        String problemasRelatados,

        @NotNull(message = "ID do veículo é obrigatório")
        UUID veiculoId
) {}
