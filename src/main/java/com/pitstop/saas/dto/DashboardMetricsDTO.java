package com.pitstop.saas.dto;

import java.math.BigDecimal;

/**
 * DTO for advanced SaaS dashboard metrics.
 *
 * Contains comprehensive financial and operational metrics including
 * MRR, ARR, churn rate, LTV, CAC, and growth indicators.
 *
 * @author PitStop Team
 */
public record DashboardMetricsDTO(
    // Financial metrics
    BigDecimal mrrTotal,
    BigDecimal mrrGrowth,         // % growth compared to last month
    BigDecimal arrTotal,           // Annual Recurring Revenue (MRR * 12)
    BigDecimal churnRate,          // % of workshops that cancelled
    BigDecimal ltv,                // Average Lifetime Value
    BigDecimal cac,                // Customer Acquisition Cost (placeholder)

    // Workshop metrics
    Integer oficinasAtivas,
    Integer oficinasTrial,
    Integer oficinasInativas,
    Integer oficinasInadimplentes,
    Integer novasOficinas30d,
    Integer cancelamentos30d,

    // User metrics
    Integer usuariosAtivos,
    Integer usuariosTotais,
    Integer loginsMes,

    // General data
    Long totalClientes,
    Long totalVeiculos,
    Long totalOS,
    Long totalOSMes,
    BigDecimal faturamentoMes      // Total billing from service orders this month
) {
    /**
     * Creates a dashboard metrics DTO with all advanced metrics.
     */
    public DashboardMetricsDTO {
        // Compact constructor for validation
        if (mrrTotal == null) mrrTotal = BigDecimal.ZERO;
        if (arrTotal == null) arrTotal = BigDecimal.ZERO;
        if (mrrGrowth == null) mrrGrowth = BigDecimal.ZERO;
        if (churnRate == null) churnRate = BigDecimal.ZERO;
        if (ltv == null) ltv = BigDecimal.ZERO;
        if (cac == null) cac = BigDecimal.ZERO;
        if (faturamentoMes == null) faturamentoMes = BigDecimal.ZERO;
    }
}
