package com.pitstop.estoque.dto;

import com.pitstop.estoque.domain.UnidadeMedida;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta com dados completos da peça.
 * Inclui campos calculados (margem de lucro, estoque baixo, etc).
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public record PecaResponseDTO(
        UUID id,
        String codigo,
        String descricao,
        String marca,
        String aplicacao,
        LocalArmazenamentoSimplificadoDTO localArmazenamento,
        UnidadeMedida unidadeMedida,
        Integer quantidadeAtual,
        Integer quantidadeMinima,
        boolean estoqueBaixo,
        BigDecimal valorCusto,
        BigDecimal valorVenda,
        BigDecimal margemLucro,
        BigDecimal valorTotalEstoque,
        Boolean ativo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
