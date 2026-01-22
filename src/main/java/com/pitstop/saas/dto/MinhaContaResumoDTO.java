package com.pitstop.saas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for workshop's financial summary in "Minha Conta" page.
 */
public record MinhaContaResumoDTO(
    // Plan info
    String planoCodigo,
    String planoNome,
    BigDecimal valorMensalidade,
    LocalDate dataVencimentoPlano,

    // Invoice summary
    int totalFaturas,
    int faturasPendentes,
    int faturasVencidas,
    int faturasPagas,

    // Financial summary
    BigDecimal valorPendente,
    BigDecimal valorVencido,
    BigDecimal valorPagoUltimos12Meses,

    // Next invoice
    FaturaResumoDTO proximaFatura,

    // Status
    String statusConta,
    boolean contaEmDia
) {
    public static MinhaContaResumoDTO criar(
        String planoCodigo,
        String planoNome,
        BigDecimal valorMensalidade,
        LocalDate dataVencimentoPlano,
        int totalFaturas,
        int faturasPendentes,
        int faturasVencidas,
        int faturasPagas,
        BigDecimal valorPendente,
        BigDecimal valorVencido,
        BigDecimal valorPagoUltimos12Meses,
        FaturaResumoDTO proximaFatura
    ) {
        boolean contaEmDia = faturasVencidas == 0;
        String statusConta = contaEmDia ? "EM_DIA" : "INADIMPLENTE";

        return new MinhaContaResumoDTO(
            planoCodigo,
            planoNome,
            valorMensalidade,
            dataVencimentoPlano,
            totalFaturas,
            faturasPendentes,
            faturasVencidas,
            faturasPagas,
            valorPendente,
            valorVencido,
            valorPagoUltimos12Meses,
            proximaFatura,
            statusConta,
            contaEmDia
        );
    }
}
