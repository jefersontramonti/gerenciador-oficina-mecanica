package com.pitstop.saas.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Installment of a payment agreement.
 */
@Entity
@Table(name = "parcelas_acordo", indexes = {
    @Index(name = "idx_parcelas_acordo_id", columnList = "acordo_id"),
    @Index(name = "idx_parcelas_status", columnList = "status"),
    @Index(name = "idx_parcelas_vencimento", columnList = "data_vencimento")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParcelaAcordo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acordo_id", nullable = false)
    private Acordo acordo;

    @Column(name = "numero_parcela", nullable = false)
    private Integer numeroParcela;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal valor;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusParcela status = StatusParcela.PENDENTE;

    // Payment details
    @Column(name = "metodo_pagamento", length = 50)
    private String metodoPagamento;

    @Column(name = "transacao_id", length = 100)
    private String transacaoId;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // =====================================
    // BUSINESS METHODS
    // =====================================

    /**
     * Check if installment is overdue.
     */
    public boolean isVencida() {
        return status == StatusParcela.PENDENTE
            && dataVencimento != null
            && LocalDate.now().isAfter(dataVencimento);
    }

    /**
     * Get days overdue.
     */
    public long getDiasAtraso() {
        if (dataVencimento == null || !isVencida()) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(dataVencimento, LocalDate.now());
    }

    /**
     * Mark as paid.
     */
    public void marcarComoPago(String metodoPagamento, String transacaoId) {
        this.status = StatusParcela.PAGO;
        this.dataPagamento = LocalDate.now();
        this.metodoPagamento = metodoPagamento;
        this.transacaoId = transacaoId;
    }

    /**
     * Get formatted label like "1/3".
     */
    public String getLabel() {
        if (acordo != null) {
            return numeroParcela + "/" + acordo.getNumeroParcelas();
        }
        return String.valueOf(numeroParcela);
    }
}
