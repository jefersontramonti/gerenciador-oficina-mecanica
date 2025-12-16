package com.pitstop.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * DTO para estatísticas gerais do dashboard.
 *
 * @param totalClientes Total de clientes ativos
 * @param totalVeiculos Total de veículos cadastrados
 * @param osAtivas Total de OS ativas (excluindo CANCELADO e ENTREGUE)
 * @param faturamentoMes Faturamento do mês atual (OS entregues)
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Schema(description = "Estatísticas gerais do dashboard")
public record DashboardStatsDTO(

    @Schema(description = "Total de clientes ativos", example = "45")
    Long totalClientes,

    @Schema(description = "Total de veículos cadastrados", example = "67")
    Long totalVeiculos,

    @Schema(description = "Total de OS ativas (em andamento)", example = "12")
    Long osAtivas,

    @Schema(description = "Faturamento do mês atual", example = "28750.50")
    BigDecimal faturamentoMes
) {}
