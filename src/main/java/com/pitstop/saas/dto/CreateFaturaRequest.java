package com.pitstop.saas.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for manually creating an invoice.
 */
public record CreateFaturaRequest(

    @NotNull(message = "Oficina é obrigatória")
    UUID oficinaId,

    @NotNull(message = "Mês de referência é obrigatório")
    LocalDate mesReferencia,

    @NotNull(message = "Data de vencimento é obrigatória")
    @Future(message = "Data de vencimento deve ser futura")
    LocalDate dataVencimento,

    @NotEmpty(message = "Pelo menos um item é obrigatório")
    List<ItemRequest> itens,

    @PositiveOrZero(message = "Desconto não pode ser negativo")
    BigDecimal desconto,

    String observacao

) {
    /**
     * Item request for invoice creation.
     */
    public record ItemRequest(

        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 200, message = "Descrição deve ter no máximo 200 caracteres")
        String descricao,

        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser positiva")
        Integer quantidade,

        @NotNull(message = "Valor unitário é obrigatório")
        @Positive(message = "Valor unitário deve ser positivo")
        BigDecimal valorUnitario
    ) {}
}
