package com.pitstop.saas.dto;

import java.util.List;

/**
 * DTO for signups vs cancellations comparison over time.
 *
 * Contains monthly data for visualizing new workshop registrations
 * compared to cancellations, showing net growth trends.
 *
 * @author PitStop Team
 */
public record SignupsVsCancellationsDTO(
    List<MonthlySignupData> data,
    Integer totalSignups,          // Total new signups in period
    Integer totalCancellations,    // Total cancellations in period
    Integer netGrowth              // Net growth (signups - cancellations)
) {
    /**
     * Monthly signup vs cancellation data point.
     */
    public record MonthlySignupData(
        String month,              // Format: "2024-01"
        String monthLabel,         // Display label: "Janeiro 2024"
        Integer signups,           // New workshops registered
        Integer cancellations,     // Workshops cancelled
        Integer netGrowth,         // signups - cancellations
        Integer trialConversions   // Trials converted to paid
    ) {}
}
