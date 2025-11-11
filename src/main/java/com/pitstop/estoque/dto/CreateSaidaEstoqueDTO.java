package com.pitstop.estoque.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para registrar saída manual de estoque.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public record CreateSaidaEstoqueDTO(
        @NotNull(message = "ID da peça é obrigatório")
        UUID pecaId,

        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser maior que zero")
        Integer quantidade,

        @NotNull(message = "Valor unitário é obrigatório")
        @DecimalMin(value = "0.00", message = "Valor unitário não pode ser negativo")
        BigDecimal valorUnitario,

        @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
        String motivo,

        @Size(max = 1000, message = "Observação deve ter no máximo 1000 caracteres")
        String observacao
) {
}
