package com.pitstop.estoque.dto;

import com.pitstop.estoque.domain.CategoriaPeca;
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
        String nome,
        String descricao,
        String marca,
        String aplicacao,
        String codigoOriginal,
        String codigoFabricante,
        String codigoBarras,
        String ncm,
        CategoriaPeca categoria,
        UUID localArmazenamentoId,
        LocalArmazenamentoSimplificadoDTO localArmazenamento,
        UnidadeMedida unidadeMedida,
        Integer quantidadeAtual,
        Integer quantidadeMinima,
        Integer quantidadeMaxima,
        Integer pontoPedido,
        boolean estoqueBaixo,
        boolean atingiuPontoPedido,
        BigDecimal valorCusto,
        BigDecimal valorVenda,
        BigDecimal margemLucro,
        BigDecimal valorTotalEstoque,
        String fornecedorPrincipal,
        String observacoes,
        Boolean ativo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
