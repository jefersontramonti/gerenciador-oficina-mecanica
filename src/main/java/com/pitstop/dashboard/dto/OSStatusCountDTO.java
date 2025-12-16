package com.pitstop.dashboard.dto;

import com.pitstop.ordemservico.domain.StatusOS;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para contagem de ordens de serviço por status.
 * Usado para exibição de gráficos no dashboard.
 *
 * @param status Status da OS
 * @param count Quantidade de OS neste status
 * @param label Label traduzida para exibição
 * @param color Cor hexadecimal para o gráfico
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Schema(description = "Contagem de OS por status para gráficos")
public record OSStatusCountDTO(

    @Schema(description = "Status da OS")
    StatusOS status,

    @Schema(description = "Quantidade de OS neste status", example = "12")
    Long count,

    @Schema(description = "Label traduzida", example = "Em Andamento")
    String label,

    @Schema(description = "Cor hexadecimal para gráfico", example = "#f59e0b")
    String color
) {}
