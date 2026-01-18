package com.pitstop.dashboard.dto;

import java.math.BigDecimal;

/**
 * DTO para estatísticas do dashboard com variação percentual vs mês anterior.
 *
 * @param totalClientes total de clientes ativos
 * @param totalVeiculos total de veículos cadastrados
 * @param osAtivas quantidade de OS ativas (não entregues/canceladas)
 * @param faturamentoMes faturamento do mês atual
 * @param faturamentoMesAnterior faturamento do mês anterior
 * @param variacaoFaturamento variação percentual do faturamento (+15.5 ou -10.2)
 * @param ticketMedio ticket médio do mês atual
 * @param ticketMedioAnterior ticket médio do mês anterior
 * @param variacaoTicketMedio variação percentual do ticket médio
 *
 * @author PitStop Team
 * @since 2026-01-18
 */
public record DashboardStatsComTrendDTO(
        Long totalClientes,
        Long totalVeiculos,
        Long osAtivas,
        BigDecimal faturamentoMes,
        BigDecimal faturamentoMesAnterior,
        Double variacaoFaturamento,
        BigDecimal ticketMedio,
        BigDecimal ticketMedioAnterior,
        Double variacaoTicketMedio
) {}
