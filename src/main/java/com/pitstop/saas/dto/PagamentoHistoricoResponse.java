package com.pitstop.saas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Response DTO for payment history records.
 *
 * Represents a single monthly subscription payment with all relevant
 * details for financial tracking and auditing.
 *
 * @author PitStop Team
 */
public record PagamentoHistoricoResponse(
    UUID id,
    UUID oficinaId,
    String nomeOficina,
    YearMonth mesReferencia,
    BigDecimal valor,
    LocalDate dataPagamento,
    LocalDate dataVencimento,
    String formaPagamento,
    String observacao,
    Boolean atrasado,
    Integer diasAtraso,
    LocalDateTime createdAt
) {
    /**
     * Creates a payment history response.
     *
     * @param id Payment unique identifier
     * @param oficinaId Workshop that made the payment
     * @param nomeOficina Workshop trade name
     * @param mesReferencia Month being paid for
     * @param valor Payment amount
     * @param dataPagamento Date when payment was received
     * @param dataVencimento Original due date
     * @param formaPagamento Payment method
     * @param observacao Optional notes
     * @param atrasado Whether payment was late
     * @param diasAtraso Number of days overdue (null if paid on time)
     * @param createdAt Payment registration timestamp
     */
    public PagamentoHistoricoResponse {
        // Compact constructor
    }
}
