package com.pitstop.saas.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Line item for an invoice.
 *
 * Represents individual charges like monthly subscription,
 * extra services, or usage overages.
 */
@Entity
@Table(name = "itens_fatura", indexes = {
    @Index(name = "idx_itens_fatura_fatura_id", columnList = "fatura_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemFatura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fatura_id", nullable = false)
    private Fatura fatura;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantidade = 1;

    @Column(name = "valor_unitario", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorUnitario;

    @Column(name = "valor_total", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorTotal;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // =====================================
    // BUSINESS METHODS
    // =====================================

    /**
     * Calculate total from quantity and unit price.
     */
    public void calcularTotal() {
        if (valorUnitario != null && quantidade != null) {
            this.valorTotal = valorUnitario.multiply(BigDecimal.valueOf(quantidade));
        }
    }

    /**
     * Set quantity and recalculate total.
     */
    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
        calcularTotal();
    }

    /**
     * Set unit price and recalculate total.
     */
    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
        calcularTotal();
    }

    /**
     * Create a subscription item.
     */
    public static ItemFatura criarItemMensalidade(String planoCodigo, BigDecimal valor) {
        ItemFatura item = ItemFatura.builder()
            .descricao("Mensalidade Plano " + planoCodigo)
            .quantidade(1)
            .valorUnitario(valor)
            .valorTotal(valor)
            .build();
        return item;
    }

    /**
     * Create an extra service item.
     */
    public static ItemFatura criarItemExtra(String descricao, int quantidade, BigDecimal valorUnitario) {
        BigDecimal total = valorUnitario.multiply(BigDecimal.valueOf(quantidade));
        return ItemFatura.builder()
            .descricao(descricao)
            .quantidade(quantidade)
            .valorUnitario(valorUnitario)
            .valorTotal(total)
            .build();
    }
}
