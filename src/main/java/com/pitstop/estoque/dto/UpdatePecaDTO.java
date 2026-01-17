package com.pitstop.estoque.dto;

import com.pitstop.estoque.domain.UnidadeMedida;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para atualização de peça existente.
 * Todos os campos são obrigatórios (substituição completa).
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public record UpdatePecaDTO(
        @NotBlank(message = "Código é obrigatório")
        @Size(min = 3, max = 50, message = "Código deve ter entre 3 e 50 caracteres")
        String codigo,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(min = 3, max = 500, message = "Descrição deve ter entre 3 e 500 caracteres")
        String descricao,

        @Size(max = 100, message = "Marca deve ter no máximo 100 caracteres")
        String marca,

        @Size(max = 500, message = "Aplicação deve ter no máximo 500 caracteres")
        String aplicacao,

        @Size(max = 100, message = "Localização deve ter no máximo 100 caracteres")
        String localizacao,

        UUID localArmazenamentoId,

        @NotNull(message = "Unidade de medida é obrigatória")
        UnidadeMedida unidadeMedida,

        @NotNull(message = "Quantidade mínima é obrigatória")
        @Min(value = 0, message = "Quantidade mínima não pode ser negativa")
        Integer quantidadeMinima,

        @NotNull(message = "Valor de custo é obrigatório")
        @DecimalMin(value = "0.00", message = "Valor de custo não pode ser negativo")
        BigDecimal valorCusto,

        @NotNull(message = "Valor de venda é obrigatório")
        @DecimalMin(value = "0.00", message = "Valor de venda não pode ser negativo")
        BigDecimal valorVenda
) {
}
