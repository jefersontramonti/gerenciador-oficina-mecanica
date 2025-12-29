package com.pitstop.saas.dto;

import com.pitstop.saas.domain.ItemFatura;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for invoice line items.
 */
public record ItemFaturaDTO(
    UUID id,
    String descricao,
    Integer quantidade,
    BigDecimal valorUnitario,
    BigDecimal valorTotal
) {
    /**
     * Convert entity to DTO.
     */
    public static ItemFaturaDTO fromEntity(ItemFatura item) {
        return new ItemFaturaDTO(
            item.getId(),
            item.getDescricao(),
            item.getQuantidade(),
            item.getValorUnitario(),
            item.getValorTotal()
        );
    }
}
