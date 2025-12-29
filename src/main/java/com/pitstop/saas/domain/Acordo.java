package com.pitstop.saas.domain;

import com.pitstop.oficina.domain.Oficina;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Payment agreement entity for negotiating overdue invoices.
 *
 * An agreement allows a workshop to pay overdue invoices in installments
 * with optional discounts.
 */
@Entity
@Table(name = "acordos", indexes = {
    @Index(name = "idx_acordos_oficina_id", columnList = "oficina_id"),
    @Index(name = "idx_acordos_status", columnList = "status"),
    @Index(name = "idx_acordos_numero", columnList = "numero")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Acordo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;  // ACO-2025-00001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusAcordo status = StatusAcordo.ATIVO;

    // Original values from invoices
    @Column(name = "valor_original", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorOriginal;

    // Discount applied
    @Column(name = "percentual_desconto", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal percentualDesconto = BigDecimal.ZERO;

    @Column(name = "valor_desconto", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorDesconto = BigDecimal.ZERO;

    // Final agreed value
    @Column(name = "valor_acordado", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorAcordado;

    // Installment info
    @Column(name = "numero_parcelas", nullable = false)
    private Integer numeroParcelas;

    @Column(name = "valor_parcela", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorParcela;

    // Tracking
    @Column(name = "parcelas_pagas")
    @Builder.Default
    private Integer parcelasPagas = 0;

    @Column(name = "valor_pago", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorPago = BigDecimal.ZERO;

    // Dates
    @Column(name = "data_acordo", nullable = false)
    @Builder.Default
    private LocalDate dataAcordo = LocalDate.now();

    @Column(name = "primeiro_vencimento", nullable = false)
    private LocalDate primeiroVencimento;

    // Notes
    @Column(columnDefinition = "TEXT")
    private String observacoes;

    // Audit
    @Column(name = "criado_por")
    private UUID criadoPor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToMany
    @JoinTable(
        name = "acordo_faturas",
        joinColumns = @JoinColumn(name = "acordo_id"),
        inverseJoinColumns = @JoinColumn(name = "fatura_id")
    )
    @Builder.Default
    private List<Fatura> faturas = new ArrayList<>();

    @OneToMany(mappedBy = "acordo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroParcela ASC")
    @Builder.Default
    private List<ParcelaAcordo> parcelas = new ArrayList<>();

    // =====================================
    // BUSINESS METHODS
    // =====================================

    /**
     * Generate agreement number in format ACO-YYYY-NNNNN.
     */
    public static String gerarNumero(int sequencia) {
        int ano = LocalDate.now().getYear();
        return String.format("ACO-%d-%05d", ano, sequencia);
    }

    /**
     * Add invoice to agreement.
     */
    public void addFatura(Fatura fatura) {
        faturas.add(fatura);
    }

    /**
     * Add installment to agreement.
     */
    public void addParcela(ParcelaAcordo parcela) {
        parcelas.add(parcela);
        parcela.setAcordo(this);
    }

    /**
     * Get remaining value to pay.
     */
    public BigDecimal getValorRestante() {
        return valorAcordado.subtract(valorPago);
    }

    /**
     * Get number of pending installments.
     */
    public int getParcelasPendentes() {
        return numeroParcelas - parcelasPagas;
    }

    /**
     * Get next due date from pending installments.
     */
    public LocalDate getProximoVencimento() {
        return parcelas.stream()
            .filter(p -> p.getStatus().isPagavel())
            .map(ParcelaAcordo::getDataVencimento)
            .min(LocalDate::compareTo)
            .orElse(null);
    }

    /**
     * Register payment for an installment.
     */
    public void registrarPagamentoParcela(ParcelaAcordo parcela) {
        if (parcela.getStatus() != StatusParcela.PAGO) {
            parcela.setStatus(StatusParcela.PAGO);
            parcela.setDataPagamento(LocalDate.now());
            this.parcelasPagas++;
            this.valorPago = this.valorPago.add(parcela.getValor());

            // Check if agreement is fully paid
            if (parcelasPagas.equals(numeroParcelas)) {
                this.status = StatusAcordo.QUITADO;
            }
        }
    }

    /**
     * Mark agreement as broken (broken by late payment).
     */
    public void quebrar(String motivo) {
        this.status = StatusAcordo.QUEBRADO;
        this.observacoes = (this.observacoes != null ? this.observacoes + "\n" : "")
            + "Acordo quebrado: " + motivo + " em " + LocalDateTime.now();
    }

    /**
     * Cancel the agreement.
     */
    public void cancelar(String motivo) {
        if (!status.isCancelavel()) {
            throw new IllegalStateException("Acordo não pode ser cancelado no status atual: " + status);
        }
        this.status = StatusAcordo.CANCELADO;
        this.observacoes = (this.observacoes != null ? this.observacoes + "\n" : "")
            + "Cancelado: " + motivo;

        // Cancel all pending installments
        parcelas.stream()
            .filter(p -> p.getStatus().isPagavel())
            .forEach(p -> p.setStatus(StatusParcela.CANCELADO));
    }

    /**
     * Generate installments based on agreement terms.
     */
    public void gerarParcelas() {
        parcelas.clear();

        BigDecimal valorBase = valorAcordado.divide(
            BigDecimal.valueOf(numeroParcelas), 2, RoundingMode.FLOOR);
        BigDecimal diferenca = valorAcordado.subtract(
            valorBase.multiply(BigDecimal.valueOf(numeroParcelas)));

        for (int i = 1; i <= numeroParcelas; i++) {
            BigDecimal valorParcela = valorBase;
            // Add remainder to last installment
            if (i == numeroParcelas) {
                valorParcela = valorParcela.add(diferenca);
            }

            LocalDate vencimento = primeiroVencimento.plusMonths(i - 1);

            ParcelaAcordo parcela = ParcelaAcordo.builder()
                .numeroParcela(i)
                .valor(valorParcela)
                .dataVencimento(vencimento)
                .status(StatusParcela.PENDENTE)
                .build();

            addParcela(parcela);
        }

        this.valorParcela = valorBase;
    }

    /**
     * Check if agreement has any overdue installment.
     */
    public boolean possuiParcelaVencida() {
        return parcelas.stream()
            .anyMatch(p -> p.getStatus() == StatusParcela.VENCIDO);
    }

    /**
     * Update overdue installments status.
     */
    public void atualizarParcelasVencidas() {
        LocalDate hoje = LocalDate.now();
        parcelas.stream()
            .filter(p -> p.getStatus() == StatusParcela.PENDENTE)
            .filter(p -> p.getDataVencimento().isBefore(hoje))
            .forEach(p -> p.setStatus(StatusParcela.VENCIDO));
    }
}
