package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.TipoJuros;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para tabela de juros.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TabelaJurosDTO {

    private UUID id;
    private Integer parcelasMinimo;
    private Integer parcelasMaximo;
    private BigDecimal percentualJuros;
    private TipoJuros tipoJuros;
    private String tipoJurosDescricao;
    private Boolean repassarCliente;
    private Boolean ativo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Descrição amigável da faixa.
     * Ex: "2x a 6x sem juros" ou "7x a 12x com 1.99% a.m."
     */
    private String descricaoFaixa;
}
