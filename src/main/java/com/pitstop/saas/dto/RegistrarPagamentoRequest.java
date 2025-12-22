package com.pitstop.saas.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Request DTO for registering a monthly payment for a workshop.
 *
 * Records subscription payments and automatically updates workshop status
 * if payment confirms an active subscription (e.g., TRIAL → ATIVA).
 *
 * @author PitStop Team
 */
public record RegistrarPagamentoRequest(

    @NotNull(message = "ID da oficina é obrigatório")
    UUID oficinaId,

    @NotNull(message = "Mês de referência é obrigatório")
    YearMonth mesReferencia,

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    BigDecimal valor,

    @NotNull(message = "Data de pagamento é obrigatória")
    @PastOrPresent(message = "Data de pagamento não pode ser futura")
    LocalDate dataPagamento,

    @NotBlank(message = "Forma de pagamento é obrigatória")
    @Size(max = 50)
    String formaPagamento,

    @Size(max = 500)
    String observacao
) {
    /**
     * Creates a request for registering a subscription payment.
     *
     * @param oficinaId Workshop that made the payment
     * @param mesReferencia Month being paid for (e.g., 2025-10)
     * @param valor Payment amount
     * @param dataPagamento Date when payment was received
     * @param formaPagamento Payment method (PIX, boleto, credit card, etc.)
     * @param observacao Optional notes about the payment
     */
    public RegistrarPagamentoRequest {
        // Compact constructor
    }
}
