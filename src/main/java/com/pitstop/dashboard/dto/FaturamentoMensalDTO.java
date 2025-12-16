package com.pitstop.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * DTO para faturamento mensal.
 * Usado para exibição de gráficos de evolução no dashboard.
 *
 * @param mes Mês/Ano no formato "MMM/yyyy" (ex: "Nov/2025")
 * @param valor Valor faturado no mês
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Schema(description = "Faturamento mensal para gráficos")
public record FaturamentoMensalDTO(

    @Schema(description = "Mês/Ano no formato MMM/yyyy", example = "Nov/2025")
    String mes,

    @Schema(description = "Valor faturado no mês", example = "28750.50")
    BigDecimal valor
) {}
