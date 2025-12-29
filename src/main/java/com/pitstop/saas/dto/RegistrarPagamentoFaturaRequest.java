package com.pitstop.saas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * Request DTO for registering a payment for an invoice.
 */
public record RegistrarPagamentoFaturaRequest(

    @NotNull(message = "Data de pagamento é obrigatória")
    @PastOrPresent(message = "Data de pagamento não pode ser futura")
    LocalDate dataPagamento,

    @NotBlank(message = "Método de pagamento é obrigatório")
    String metodoPagamento,

    String transacaoId,

    String observacao

) {}
