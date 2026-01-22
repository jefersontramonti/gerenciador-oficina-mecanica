package com.pitstop.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para projeção financeira.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjecaoFinanceiraDTO {

    /**
     * Período de projeção.
     */
    private LocalDate dataBase;
    private Integer diasProjecao;
    private LocalDate dataFimProjecao;

    /**
     * Saldo atual e projetado.
     */
    private BigDecimal saldoAtual;
    private BigDecimal saldoProjetado;
    private BigDecimal variacaoProjetada;

    /**
     * Receitas esperadas.
     */
    private BigDecimal receitasEsperadas;
    private List<ReceitaEsperadaDTO> detalhamentoReceitas;

    /**
     * Despesas previstas.
     */
    private BigDecimal despesasPrevistas;
    private List<DespesaPrevistaDTO> detalhamentoDespesas;

    /**
     * Alertas de fluxo.
     */
    private List<AlertaFluxoDTO> alertas;

    /**
     * Projeção por dia.
     */
    private List<ProjecaoDiariaDTO> projecaoDiaria;

    /**
     * Indicadores.
     */
    private IndicadoresProjecaoDTO indicadores;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceitaEsperadaDTO {
        private String origem; // OS_PENDENTE, OS_APROVADA, PARCELAMENTO
        private String descricao;
        private BigDecimal valor;
        private LocalDate dataEsperada;
        private String probabilidade; // ALTA, MEDIA, BAIXA
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DespesaPrevistaDTO {
        private String categoria;
        private String descricao;
        private BigDecimal valor;
        private LocalDate dataVencimento;
        private String status; // PENDENTE, VENCIDO
        private Boolean recorrente;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertaFluxoDTO {
        private String tipo; // SALDO_NEGATIVO, VENCIMENTO_PROXIMO, RECEITA_BAIXA
        private String nivel; // INFO, WARNING, CRITICAL
        private String mensagem;
        private LocalDate data;
        private BigDecimal valor;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjecaoDiariaDTO {
        private LocalDate data;
        private BigDecimal receitasPrevistas;
        private BigDecimal despesasPrevistas;
        private BigDecimal saldoDia;
        private BigDecimal saldoAcumulado;
        private Boolean alertaSaldoNegativo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicadoresProjecaoDTO {
        private BigDecimal ticketMedioMes;
        private BigDecimal mediaReceitaDiaria;
        private BigDecimal mediaDespesaDiaria;
        private Integer diasAteSaldoNegativo;
        private BigDecimal necessidadeCapitalGiro;
        private String tendencia; // POSITIVA, ESTAVEL, NEGATIVA
    }
}
