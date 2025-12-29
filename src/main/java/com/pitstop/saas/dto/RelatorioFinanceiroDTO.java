package com.pitstop.saas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para relatório financeiro do SaaS.
 */
public record RelatorioFinanceiroDTO(
    // Período do relatório
    LocalDate dataInicio,
    LocalDate dataFim,

    // Resumo Financeiro
    BigDecimal receitaTotal,
    BigDecimal receitaMensal,
    BigDecimal mrrAtual,
    BigDecimal arrAtual,
    BigDecimal ticketMedio,

    // Comparativo com período anterior
    BigDecimal receitaPeriodoAnterior,
    BigDecimal variacaoPercentual,

    // Faturas
    Integer totalFaturas,
    Integer faturasPagas,
    Integer faturasPendentes,
    Integer faturasVencidas,
    Integer faturasCanceladas,
    BigDecimal valorFaturasPagas,
    BigDecimal valorFaturasPendentes,
    BigDecimal valorFaturasVencidas,

    // Inadimplência
    Integer oficinasInadimplentes,
    BigDecimal valorInadimplente,
    BigDecimal taxaInadimplencia,

    // Receita por Plano
    List<ReceitaPorPlano> receitaPorPlano,

    // Evolução Mensal
    List<EvolucaoMensal> evolucaoMensal,

    // Top 10 Oficinas por Receita
    List<OficinaReceita> topOficinas
) {
    public record ReceitaPorPlano(
        String planoNome,
        String planoCodigo,
        Integer quantidadeOficinas,
        BigDecimal receitaTotal,
        BigDecimal percentualReceita
    ) {}

    public record EvolucaoMensal(
        String mesAno,
        BigDecimal receita,
        BigDecimal mrr,
        Integer novasOficinas,
        Integer cancelamentos
    ) {}

    public record OficinaReceita(
        String oficinaId,
        String nomeFantasia,
        String cnpj,
        String plano,
        BigDecimal receitaTotal,
        Integer mesesAtivo
    ) {}
}
