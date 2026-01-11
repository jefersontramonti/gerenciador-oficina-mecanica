package com.pitstop.ordemservico.dto;

import com.pitstop.ordemservico.domain.OrigemPeca;
import com.pitstop.ordemservico.domain.TipoItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para criação de item de Ordem de Serviço.
 *
 * <p>Suporta três origens de peças:</p>
 * <ul>
 *   <li><strong>ESTOQUE:</strong> Peça do inventário (requer pecaId)</li>
 *   <li><strong>AVULSA:</strong> Peça comprada externamente (requer descrição detalhada)</li>
 *   <li><strong>CLIENTE:</strong> Peça fornecida pelo cliente (requer descrição detalhada)</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Schema(description = "Dados para criação de item de OS (peça ou serviço)")
public record CreateItemOSDTO(

    @Schema(description = "Tipo do item", example = "PECA", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Tipo do item é obrigatório")
    TipoItem tipo,

    @Schema(description = "Origem da peça (obrigatório se tipo = PECA)", example = "ESTOQUE")
    OrigemPeca origemPeca,

    @Schema(description = "ID da peça (obrigatório se origemPeca = ESTOQUE)", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID pecaId,

    @Schema(description = "Descrição do item (mín 10 chars se peça avulsa/cliente)", example = "Óleo de motor 5W30", requiredMode = Schema.RequiredMode.REQUIRED)
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
    /**
     * Valida que itens do tipo PECA devem ter origemPeca definida.
     *
     * @return true se validação OK
     */
    @AssertTrue(message = "Peça requer origemPeca definida (ESTOQUE, AVULSA ou CLIENTE)")
    public boolean isOrigemPecaValida() {
        if (tipo != TipoItem.PECA) {
            return true; // Não se aplica a SERVICO
        }
        return origemPeca != null;
    }

    /**
     * Valida que peças do ESTOQUE devem ter pecaId.
     *
     * @return true se validação OK
     */
    @AssertTrue(message = "Peça do ESTOQUE requer pecaId")
    public boolean isPecaIdValida() {
        if (tipo != TipoItem.PECA) {
            return true; // Não se aplica a SERVICO
        }
        if (origemPeca != OrigemPeca.ESTOQUE) {
            return true; // Não se aplica a AVULSA/CLIENTE
        }
        return pecaId != null;
    }

    /**
     * Valida que peças AVULSA/CLIENTE devem ter descrição detalhada (mín 10 chars).
     *
     * @return true se validação OK
     */
    @AssertTrue(message = "Peça avulsa/cliente requer descrição detalhada (mínimo 10 caracteres)")
    public boolean isDescricaoAvulsaValida() {
        if (tipo != TipoItem.PECA) {
            return true; // Não se aplica a SERVICO
        }
        if (origemPeca == null || origemPeca == OrigemPeca.ESTOQUE) {
            return true; // Não se aplica ao ESTOQUE
        }
        // Para AVULSA/CLIENTE, descrição deve ter mínimo 10 caracteres
        return descricao != null && descricao.trim().length() >= 10;
    }
}
