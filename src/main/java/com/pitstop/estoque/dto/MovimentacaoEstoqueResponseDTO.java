package com.pitstop.estoque.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pitstop.estoque.domain.TipoMovimentacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta com dados completos da movimentação de estoque.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public record MovimentacaoEstoqueResponseDTO(
        UUID id,
        PecaResumoDTO peca,
        UsuarioResumoDTO usuario,
        Long numeroOS,
        TipoMovimentacao tipo,
        Integer quantidade,
        Integer quantidadeAnterior,
        Integer quantidadeAtual,
        BigDecimal valorUnitario,
        BigDecimal valorTotal,
        String motivo,
        String observacao,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataMovimentacao,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    /**
     * DTO resumido de peça para movimentação.
     */
    public record PecaResumoDTO(
            UUID id,
            String codigo,
            String descricao
    ) {
    }

    /**
     * DTO resumido de usuário para movimentação.
     */
    public record UsuarioResumoDTO(
            UUID id,
            String nome,
            String email
    ) {
    }
}
