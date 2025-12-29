package com.pitstop.saas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateFeatureFlagRequest(
    @NotBlank(message = "Código é obrigatório")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Código deve ser em maiúsculas, começar com letra e conter apenas letras, números e underscores")
    String codigo,

    @NotBlank(message = "Nome é obrigatório")
    String nome,

    String descricao,

    Boolean habilitadoGlobal,

    Map<String, Boolean> habilitadoPorPlano,

    List<UUID> habilitadoPorOficina,

    @Min(value = 0, message = "Percentual deve ser entre 0 e 100")
    @Max(value = 100, message = "Percentual deve ser entre 0 e 100")
    Integer percentualRollout,

    OffsetDateTime dataInicio,

    OffsetDateTime dataFim,

    String categoria,

    Boolean requerAutorizacao
) {}
