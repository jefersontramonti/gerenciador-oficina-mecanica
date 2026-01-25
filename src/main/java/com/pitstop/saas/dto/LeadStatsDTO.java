package com.pitstop.saas.dto;

/**
 * Lead statistics DTO.
 *
 * Contains counts of leads in each status for dashboard metrics.
 *
 * @author PitStop Team
 */
public record LeadStatsDTO(
    long totalNovos,
    long totalContatados,
    long totalQualificados,
    long totalConvertidos,
    long totalPerdidos,
    long totalGeral
) {
}
