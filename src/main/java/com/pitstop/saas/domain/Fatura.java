package com.pitstop.saas.domain;

import com.pitstop.oficina.domain.Oficina;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Invoice entity representing a monthly billing document for a workshop.
 *
 * An invoice is generated automatically each month and can be paid,
 * cancelled, or marked as overdue.
 */
@Entity
@Table(name = "faturas", indexes = {
    @Index(name = "idx_faturas_oficina_id", columnList = "oficina_id"),
    @Index(name = "idx_faturas_status", columnList = "status"),
    @Index(name = "idx_faturas_data_vencimento", columnList = "data_vencimento"),
    @Index(name = "idx_faturas_numero", columnList = "numero")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fatura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;  // FAT-2025-00001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @Column(name = "plano_codigo", length = 30)
    private String planoCodigo;  // Snapshot of plan at invoice time

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusFatura status = StatusFatura.PENDENTE;

    // Financial values
    @Column(name = "valor_base", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorBase = BigDecimal.ZERO;

    @Column(name = "valor_desconto", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorDesconto = BigDecimal.ZERO;

    @Column(name = "valor_acrescimos", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorAcrescimos = BigDecimal.ZERO;

    @Column(name = "valor_total", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    // Dates
    @Column(name = "mes_referencia", nullable = false)
    private LocalDate mesReferencia;

    @Column(name = "data_emissao", nullable = false)
    @Builder.Default
    private LocalDate dataEmissao = LocalDate.now();

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;

    // Payment details (when paid)
    @Column(name = "metodo_pagamento", length = 50)
    private String metodoPagamento;

    @Column(name = "transacao_id", length = 100)
    private String transacaoId;

    // Mercado Pago integration
    @Column(name = "qr_code_pix", columnDefinition = "TEXT")
    private String qrCodePix;

    @Column(name = "link_pagamento", length = 500)
    private String linkPagamento;

    // Notes and tracking
    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "tentativas_cobranca")
    @Builder.Default
    private Integer tentativasCobranca = 0;

    @Column(name = "proxima_tentativa")
    private LocalDateTime proximaTentativa;

    // Invoice items
    @OneToMany(mappedBy = "fatura", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemFatura> itens = new ArrayList<>();

    // Audit
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =====================================
    // BUSINESS METHODS
    // =====================================

    /**
     * Check if invoice is overdue.
     */
    public boolean isVencida() {
        return status == StatusFatura.PENDENTE
            && dataVencimento != null
            && LocalDate.now().isAfter(dataVencimento);
    }

    /**
     * Get days until or since due date.
     * Positive = days until due, Negative = days overdue
     */
    public long getDiasAteVencimento() {
        if (dataVencimento == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), dataVencimento);
    }

    /**
     * Check if invoice is paid.
     */
    public boolean isPaga() {
        return status == StatusFatura.PAGO;
    }

    /**
     * Check if invoice can be paid.
     */
    public boolean isPagavel() {
        return status.isPagavel();
    }

    /**
     * Check if invoice can be cancelled.
     */
    public boolean isCancelavel() {
        return status.isCancelavel();
    }

    /**
     * Add an item to the invoice.
     */
    public void addItem(ItemFatura item) {
        itens.add(item);
        item.setFatura(this);
        recalcularTotal();
    }

    /**
     * Remove an item from the invoice.
     */
    public void removeItem(ItemFatura item) {
        itens.remove(item);
        item.setFatura(null);
        recalcularTotal();
    }

    /**
     * Recalculate the total based on items and adjustments.
     */
    public void recalcularTotal() {
        this.valorBase = itens.stream()
            .map(ItemFatura::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.valorTotal = valorBase
            .subtract(valorDesconto)
            .add(valorAcrescimos);

        if (this.valorTotal.compareTo(BigDecimal.ZERO) < 0) {
            this.valorTotal = BigDecimal.ZERO;
        }
    }

    /**
     * Apply late fee (percentage).
     */
    public void aplicarMulta(BigDecimal percentual) {
        BigDecimal multa = valorBase.multiply(percentual).divide(new BigDecimal("100"));
        this.valorAcrescimos = this.valorAcrescimos.add(multa);
        recalcularTotal();
    }

    /**
     * Apply discount (fixed value).
     */
    public void aplicarDesconto(BigDecimal valor) {
        this.valorDesconto = this.valorDesconto.add(valor);
        recalcularTotal();
    }

    /**
     * Mark invoice as paid.
     */
    public void marcarComoPago(String metodoPagamento, String transacaoId) {
        this.status = StatusFatura.PAGO;
        this.dataPagamento = LocalDateTime.now();
        this.metodoPagamento = metodoPagamento;
        this.transacaoId = transacaoId;
    }

    /**
     * Mark invoice as overdue.
     */
    public void marcarComoVencido() {
        if (status == StatusFatura.PENDENTE) {
            this.status = StatusFatura.VENCIDO;
        }
    }

    /**
     * Cancel the invoice.
     */
    public void cancelar(String motivo) {
        if (!isCancelavel()) {
            throw new IllegalStateException("Fatura não pode ser cancelada no status atual: " + status);
        }
        this.status = StatusFatura.CANCELADO;
        this.observacao = (this.observacao != null ? this.observacao + "\n" : "") + "Cancelada: " + motivo;
    }

    /**
     * Generate invoice number in format FAT-YYYY-NNNNN.
     */
    public static String gerarNumero(int sequencia) {
        int ano = LocalDate.now().getYear();
        return String.format("FAT-%d-%05d", ano, sequencia);
    }

    /**
     * Get reference month formatted as "Janeiro 2025".
     */
    public String getMesReferenciaFormatado() {
        if (mesReferencia == null) return null;
        String[] meses = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        return meses[mesReferencia.getMonthValue() - 1] + " " + mesReferencia.getYear();
    }
}
