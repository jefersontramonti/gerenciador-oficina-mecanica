package com.pitstop.saas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for MRR evolution over time.
 *
 * Contains monthly MRR data for chart visualization,
 * showing revenue trends and growth patterns.
 *
 * @author PitStop Team
 */
public record MRREvolutionDTO(
    List<MonthlyMRRData> data,
    BigDecimal totalGrowth,        // Overall growth percentage
    BigDecimal averageMRR          // Average MRR over the period
) {
    /**
     * Monthly MRR data point.
     */
    public record MonthlyMRRData(
        String month,              // Format: "2024-01" or "Jan/24"
        String monthLabel,         // Display label: "Janeiro 2024"
        BigDecimal mrr,
        BigDecimal growth,         // % growth from previous month
        Integer oficinasAtivas     // Number of active workshops
    ) {}
}
