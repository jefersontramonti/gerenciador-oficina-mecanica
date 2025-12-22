package com.pitstop.saas.dto;

import com.pitstop.oficina.domain.PlanoAssinatura;

import java.math.BigDecimal;

/**
 * Response DTO for Monthly Recurring Revenue breakdown by plan.
 *
 * Provides granular MRR metrics segmented by subscription plan tier,
 * enabling financial analysis and trend monitoring.
 *
 * @author PitStop Team
 */
public record MRRBreakdownResponse(
    PlanoAssinatura plano,
    Long quantidadeOficinas,
    BigDecimal mrrPlano,
    BigDecimal percentualTotal
) {
    /**
     * Creates an MRR breakdown response for a specific plan.
     *
     * @param plano Subscription plan tier
     * @param quantidadeOficinas Number of active workshops on this plan
     * @param mrrPlano Total MRR from this plan
     * @param percentualTotal Percentage of total MRR (0-100)
     */
    public MRRBreakdownResponse {
        // Compact constructor
    }
}
