package com.pitstop.saas.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO for the default management dashboard.
 * Provides an overview of all overdue invoices and defaulting workshops.
 */
public record InadimplenciaDashboardDTO(
    // Total values
    BigDecimal valorTotalInadimplente,
    Integer oficinasInadimplentes,
    Integer faturasVencidas,

    // Breakdown by days overdue: 1-30, 31-60, 61-90, 90+
    Map<String, InadimplenciaFaixaDTO> porFaixaAtraso,

    // Top defaulting workshops
    List<OficinaInadimplenteDTO> top10Inadimplentes,

    // Trend metrics
    BigDecimal valorRecuperadoMes,
    Integer acordosAtivos,
    BigDecimal valorEmAcordos
) {
    /**
     * DTO for each overdue range.
     */
    public record InadimplenciaFaixaDTO(
        String faixa,
        Integer quantidadeFaturas,
        Integer quantidadeOficinas,
        BigDecimal valorTotal
    ) {}
}
