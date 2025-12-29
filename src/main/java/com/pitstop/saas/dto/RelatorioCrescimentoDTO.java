package com.pitstop.saas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para relatório de crescimento do SaaS.
 */
public record RelatorioCrescimentoDTO(
    // Período do relatório
    LocalDate dataInicio,
    LocalDate dataFim,

    // Métricas de Crescimento
    Integer novasOficinas,
    Integer cancelamentos,
    Integer crescimentoLiquido,
    BigDecimal taxaCrescimento,

    // Churn
    BigDecimal churnRate,
    BigDecimal churnMRR,
    Integer oficinasChurned,

    // Aquisição
    BigDecimal cac, // Custo de Aquisição de Cliente
    BigDecimal ltv, // Lifetime Value
    BigDecimal ltvCacRatio,
    Integer diasMediaConversao,

    // Trial
    Integer trialsIniciados,
    Integer trialsConvertidos,
    BigDecimal taxaConversaoTrial,
    Double mediaDiasTrial,

    // Retenção
    BigDecimal taxaRetencao30d,
    BigDecimal taxaRetencao90d,
    BigDecimal taxaRetencao12m,

    // MRR Evolution
    BigDecimal mrrInicio,
    BigDecimal mrrFim,
    BigDecimal mrrNovo, // De novas oficinas
    BigDecimal mrrExpansao, // Upgrades
    BigDecimal mrrContracao, // Downgrades
    BigDecimal mrrChurn, // Cancelamentos
    BigDecimal mrrReativacao, // Reativações

    // Evolução Mensal
    List<EvolucaoCrescimento> evolucaoMensal,

    // Cohort Analysis (simplificado)
    List<CohortData> cohortAnalysis,

    // Top Motivos de Cancelamento
    List<MotivoCancelamento> motivosCancelamento,

    // Fontes de Aquisição
    List<FonteAquisicao> fontesAquisicao
) {
    public record EvolucaoCrescimento(
        String mesAno,
        Integer novasOficinas,
        Integer cancelamentos,
        Integer crescimentoLiquido,
        BigDecimal mrr,
        BigDecimal churnRate
    ) {}

    public record CohortData(
        String cohortMes, // Mês de aquisição
        Integer oficinasTotais,
        Integer oficinasAtivas,
        BigDecimal taxaRetencao
    ) {}

    public record MotivoCancelamento(
        String motivo,
        Integer quantidade,
        Double percentual
    ) {}

    public record FonteAquisicao(
        String fonte,
        Integer quantidade,
        Double percentual,
        BigDecimal receitaGerada
    ) {}
}
