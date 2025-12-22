package com.pitstop.saas.dto;

import java.math.BigDecimal;

/**
 * Response DTO for SaaS dashboard overall statistics.
 *
 * Contains aggregated metrics about the entire platform including
 * workshop counts by status, financial metrics (MRR), and usage statistics.
 *
 * @author PitStop Team
 */
public record DashboardStatsResponse(
    Long totalOficinas,
    Long oficinasAtivas,
    Long oficinasTrial,
    Long oficinasSuspensas,
    Long oficinasCanceladas,
    BigDecimal mrrTotal,
    Long totalOrdensServico,
    Long totalClientes,
    Long totalVeiculos,
    Long pagamentosPendentes,
    Long pagamentosAtrasados
) {
    /**
     * Creates a dashboard statistics response with all metrics.
     *
     * @param totalOficinas Total number of workshops in the system
     * @param oficinasAtivas Number of active workshops
     * @param oficinasTrial Number of workshops in trial period
     * @param oficinasSuspensas Number of suspended workshops
     * @param oficinasCanceladas Number of cancelled workshops
     * @param mrrTotal Monthly Recurring Revenue across all active workshops
     * @param totalOrdensServico Total number of service orders system-wide
     * @param totalClientes Total number of customers system-wide
     * @param totalVeiculos Total number of vehicles system-wide
     * @param pagamentosPendentes Number of pending payments (not overdue)
     * @param pagamentosAtrasados Number of overdue payments
     */
    public DashboardStatsResponse {
        // Compact constructor for validation if needed in the future
    }
}
