package com.pitstop.saas.dto;

import java.math.BigDecimal;

/**
 * Summary DTO for invoice dashboard statistics.
 */
public record FaturasResumoSummaryDTO(
    long totalPendentes,
    long totalVencidas,
    long totalPagas,
    long totalCanceladas,
    BigDecimal valorPendente,
    BigDecimal valorVencido,
    BigDecimal valorRecebidoMes,
    long oficinasInadimplentes
) {}
