package com.pitstop.saas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for payment agreement information.
 */
public record AcordoDTO(
    UUID id,
    String numero,

    // Workshop info
    UUID oficinaId,
    String oficinaNome,

    // Agreement details
    StatusAcordo status,
    String statusLabel,
    BigDecimal valorOriginal,
    BigDecimal valorDesconto,
    BigDecimal valorAcordado,
    BigDecimal percentualDesconto,

    // Installments
    Integer totalParcelas,
    Integer parcelasPagas,
    Integer parcelasPendentes,
    BigDecimal valorPago,
    BigDecimal valorRestante,

    // Dates
    LocalDate dataAcordo,
    LocalDate primeiroVencimento,
    LocalDate proximoVencimento,

    // Metadata
    String observacoes,
    UUID criadoPor,
    String criadoPorNome,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    // Related invoices and installments
    List<FaturaAcordoDTO> faturas,
    List<ParcelaAcordoDTO> parcelas
) {
    /**
     * Agreement status enum.
     */
    public enum StatusAcordo {
        ATIVO("Ativo", "green"),
        QUITADO("Quitado", "blue"),
        QUEBRADO("Quebrado", "red"),
        CANCELADO("Cancelado", "gray");

        private final String label;
        private final String cor;

        StatusAcordo(String label, String cor) {
            this.label = label;
            this.cor = cor;
        }

        public String getLabel() {
            return label;
        }

        public String getCor() {
            return cor;
        }
    }

    /**
     * Invoice included in agreement.
     */
    public record FaturaAcordoDTO(
        UUID faturaId,
        String numero,
        BigDecimal valorOriginal,
        LocalDate dataVencimento,
        String mesReferencia
    ) {}

    /**
     * Installment of the agreement.
     */
    public record ParcelaAcordoDTO(
        UUID id,
        Integer numeroParcela,
        BigDecimal valor,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        StatusParcela status,
        String statusLabel
    ) {
        public enum StatusParcela {
            PENDENTE("Pendente", "yellow"),
            PAGO("Pago", "green"),
            VENCIDO("Vencido", "red"),
            CANCELADO("Cancelado", "gray");

            private final String label;
            private final String cor;

            StatusParcela(String label, String cor) {
                this.label = label;
                this.cor = cor;
            }

            public String getLabel() {
                return label;
            }

            public String getCor() {
                return cor;
            }
        }
    }
}
