package com.pitstop.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para fluxo de caixa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FluxoCaixaDTO {

    /**
     * Período de análise.
     */
    private LocalDate dataInicio;
    private LocalDate dataFim;

    /**
     * Totais do período.
     */
    private BigDecimal saldoInicial;
    private BigDecimal totalReceitas;
    private BigDecimal totalDespesas;
    private BigDecimal saldoFinal;

    /**
     * Variação em relação ao período anterior.
     */
    private BigDecimal variacaoReceitas;
    private BigDecimal variacaoDespesas;
    private BigDecimal variacaoSaldo;

    /**
     * Detalhamento diário.
     */
    private List<MovimentoDiarioDTO> movimentosDiarios;

    /**
     * Detalhamento por categoria.
     */
    private List<MovimentoCategoriaDTO> receitasPorCategoria;
    private List<MovimentoCategoriaDTO> despesasPorCategoria;

    /**
     * Alertas inteligentes do fluxo de caixa.
     */
    private List<AlertaFluxoCaixaDTO> alertas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovimentoDiarioDTO {
        private LocalDate data;
        private BigDecimal receitas;
        private BigDecimal despesas;
        private BigDecimal saldo;
        private BigDecimal saldoAcumulado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovimentoCategoriaDTO {
        private String categoria;
        private String cor;
        private BigDecimal valor;
        private BigDecimal percentual;
        private Integer quantidade;
    }
}
