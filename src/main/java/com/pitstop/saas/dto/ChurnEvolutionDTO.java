package com.pitstop.saas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for churn rate evolution over time.
 *
 * Contains monthly churn data for chart visualization,
 * helping identify retention trends and issues.
 *
 * @author PitStop Team
 */
public record ChurnEvolutionDTO(
    List<MonthlyChurnData> data,
    BigDecimal averageChurn,       // Average churn rate over the period
    BigDecimal currentChurn,       // Current month churn rate
    Integer totalCancelled         // Total cancelled in the period
) {
    /**
     * Monthly churn data point.
     */
    public record MonthlyChurnData(
        String month,              // Format: "2024-01"
        String monthLabel,         // Display label: "Janeiro 2024"
        BigDecimal churnRate,      // Percentage
        Integer cancelled,         // Number of cancellations
        Integer activeAtStart      // Active workshops at month start
    ) {}
}
