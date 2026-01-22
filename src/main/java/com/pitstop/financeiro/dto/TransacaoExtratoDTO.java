package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.StatusConciliacao;
import com.pitstop.financeiro.domain.TipoTransacaoBancaria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para transação do extrato.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransacaoExtratoDTO {

    private UUID id;
    private UUID extratoId;
    private LocalDate dataTransacao;
    private LocalDate dataLancamento;
    private TipoTransacaoBancaria tipo;
    private BigDecimal valor;
    private String descricao;
    private String identificadorBanco;
    private String referencia;
    private String categoriaBanco;
    private StatusConciliacao status;
    private UUID pagamentoId;
    private String pagamentoDescricao;
    private LocalDateTime dataConciliacao;
    private String metodoConciliacao;
    private String observacao;

    /**
     * Sugestões de pagamentos para conciliação.
     */
    private List<SugestaoConciliacaoDTO> sugestoes;

    /**
     * DTO para sugestão de conciliação.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SugestaoConciliacaoDTO {
        private UUID pagamentoId;
        private LocalDate dataPagamento;
        private BigDecimal valor;
        private String tipoPagamento;
        private String osNumero;
        private String clienteNome;
        private Double score; // Score de similaridade (0-100)
        private String motivoSugestao;
    }
}
