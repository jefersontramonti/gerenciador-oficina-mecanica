package com.pitstop.ordemservico.dto;

import com.pitstop.ordemservico.domain.TipoItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para criação de item de Ordem de Serviço.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Schema(description = "Dados para criação de item de OS (peça ou serviço)")
public record CreateItemOSDTO(

    @Schema(description = "Tipo do item", example = "PECA", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Tipo do item é obrigatório")
    TipoItem tipo,

    @Schema(description = "ID da peça (obrigatório se tipo = PECA)", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID pecaId,

    @Schema(description = "Descrição do item", example = "Óleo de motor 5W30", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Descrição é obrigatória")
    @Size(min = 3, max = 500, message = "Descrição deve ter entre 3 e 500 caracteres")
    String descricao,

    @Schema(description = "Quantidade", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    Integer quantidade,

    @Schema(description = "Valor unitário", example = "45.90", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Valor unitário é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor unitário não pode ser negativo")
    BigDecimal valorUnitario,

    @Schema(description = "Desconto em valor absoluto", example = "5.00")
    @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
    BigDecimal desconto
) {
}
