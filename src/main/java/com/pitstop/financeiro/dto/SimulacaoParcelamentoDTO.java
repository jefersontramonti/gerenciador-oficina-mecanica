package com.pitstop.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de resposta para simulação de parcelamento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulacaoParcelamentoDTO {

    /**
     * Valor original a ser parcelado.
     */
    private BigDecimal valorOriginal;

    /**
     * Número máximo de parcelas disponíveis.
     */
    private Integer parcelasMaximas;

    /**
     * Lista de opções de parcelamento.
     */
    private List<OpcaoParcelamentoDTO> opcoes;

    /**
     * Representa uma opção de parcelamento.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpcaoParcelamentoDTO {

        /**
         * Número de parcelas.
         */
        private Integer parcelas;

        /**
         * Valor de cada parcela.
         */
        private BigDecimal valorParcela;

        /**
         * Valor total com juros.
         */
        private BigDecimal valorTotal;

        /**
         * Valor dos juros.
         */
        private BigDecimal valorJuros;

        /**
         * Percentual de juros mensal aplicado.
         */
        private BigDecimal percentualJurosMensal;

        /**
         * CET - Custo Efetivo Total (anual).
         */
        private BigDecimal cetAnual;

        /**
         * Se é sem juros.
         */
        private Boolean semJuros;

        /**
         * Texto de exibição. Ex: "3x de R$ 100,00 sem juros"
         */
        private String textoExibicao;

        /**
         * Se está disponível (valor parcela >= mínimo).
         */
        private Boolean disponivel;

        /**
         * Mensagem se não disponível.
         */
        private String mensagemIndisponivel;
    }
}
