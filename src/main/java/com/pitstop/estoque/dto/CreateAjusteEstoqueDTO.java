package com.pitstop.estoque.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para registrar ajuste de inventário.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public record CreateAjusteEstoqueDTO(
        @NotNull(message = "ID da peça é obrigatório")
        UUID pecaId,

        @NotNull(message = "Nova quantidade é obrigatória")
        @Min(value = 0, message = "Nova quantidade não pode ser negativa")
        Integer quantidadeNova,

        @NotNull(message = "Valor unitário é obrigatório")
        @DecimalMin(value = "0.00", message = "Valor unitário não pode ser negativo")
        BigDecimal valorUnitario,

        @NotBlank(message = "Motivo é obrigatório")
        @Size(min = 3, max = 500, message = "Motivo deve ter entre 3 e 500 caracteres")
        String motivo,

        @Size(max = 1000, message = "Observação deve ter no máximo 1000 caracteres")
        String observacao
) {
}
