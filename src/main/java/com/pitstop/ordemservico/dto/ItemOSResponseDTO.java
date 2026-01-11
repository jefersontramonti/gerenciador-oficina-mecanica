package com.pitstop.ordemservico.dto;

import com.pitstop.ordemservico.domain.OrigemPeca;
import com.pitstop.ordemservico.domain.TipoItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para item de Ordem de Serviço.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Schema(description = "Dados de resposta de item de OS")
public record ItemOSResponseDTO(

    @Schema(description = "ID do item", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,

    @Schema(description = "Tipo do item", example = "PECA")
    TipoItem tipo,

    @Schema(description = "Origem da peça (ESTOQUE, AVULSA, CLIENTE)", example = "ESTOQUE")
    OrigemPeca origemPeca,

    @Schema(description = "ID da peça (se origemPeca = ESTOQUE)", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID pecaId,

    @Schema(description = "Descrição do item", example = "Óleo de motor 5W30")
    String descricao,

    @Schema(description = "Quantidade", example = "2")
    Integer quantidade,

    @Schema(description = "Valor unitário", example = "45.90")
    BigDecimal valorUnitario,

    @Schema(description = "Desconto", example = "5.00")
    BigDecimal desconto,

    @Schema(description = "Valor total (calculado)", example = "86.80")
    BigDecimal valorTotal,

    @Schema(description = "Data de criação", example = "2025-11-01T10:30:00")
    LocalDateTime createdAt
) {
}
