package com.pitstop.saas.domain;

import com.pitstop.oficina.domain.Oficina;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Entity representing a monthly subscription payment.
 *
 * Tracks all payments made by workshops for their subscription plans.
 * Immutable after creation - no updates allowed for audit compliance.
 *
 * @author PitStop Team
 */
@Entity
@Table(
    name = "saas_pagamentos",
    indexes = {
        @Index(name = "idx_saas_pagamentos_oficina", columnList = "oficina_id"),
        @Index(name = "idx_saas_pagamentos_referencia", columnList = "referencia_mes"),
        @Index(name = "idx_saas_pagamentos_vencimento", columnList = "data_vencimento")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"oficina_id", "referencia_mes"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaasPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    /**
     * Month being paid for (stored as DATE, typically first day of the month).
     * Example: 2025-01-01 represents January 2025.
     */
    @Column(name = "referencia_mes", nullable = false)
    private LocalDate referenciaMes;

    @Column(name = "valor_pago", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "forma_pagamento", length = 50)
    private String formaPagamento;

    @Column(name = "comprovante_url", length = 500)
    private String comprovanteUrl;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "registrado_por_usuario_id")
    private UUID registradoPorUsuarioId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Checks if this payment was made late (after due date).
     *
     * @return true if payment date is after due date
     */
    public boolean isAtrasado() {
        return dataPagamento != null && dataVencimento != null
            && dataPagamento.isAfter(dataVencimento);
    }

    /**
     * Calculates how many days late the payment was.
     *
     * @return number of days overdue, or 0 if paid on time
     */
    public int getDiasAtraso() {
        if (!isAtrasado()) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(dataVencimento, dataPagamento);
    }

    /**
     * Gets the reference month as a YearMonth object.
     *
     * @return YearMonth extracted from referenciaMes
     */
    public YearMonth getReferenciaMesAsYearMonth() {
        return referenciaMes != null ? YearMonth.from(referenciaMes) : null;
    }

    /**
     * Sets the reference month from a YearMonth object.
     * Stores as the first day of the month.
     *
     * @param yearMonth the month to set
     */
    public void setReferenciaMesFromYearMonth(YearMonth yearMonth) {
        this.referenciaMes = yearMonth.atDay(1);
    }

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
