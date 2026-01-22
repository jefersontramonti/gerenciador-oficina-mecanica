package com.pitstop.financeiro.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.security.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa uma despesa operacional da oficina.
 * Usado para controle de fluxo de caixa e DRE.
 */
@Entity
@Table(name = "despesas", indexes = {
    @Index(name = "idx_despesas_oficina", columnList = "oficina_id"),
    @Index(name = "idx_despesas_categoria", columnList = "categoria"),
    @Index(name = "idx_despesas_data", columnList = "data_vencimento"),
    @Index(name = "idx_despesas_status", columnList = "status"),
    @Index(name = "idx_despesas_data_pagamento", columnList = "data_pagamento")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    /**
     * Categoria da despesa.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategoriaDespesa categoria;

    /**
     * Descrição detalhada da despesa.
     */
    @Column(nullable = false, length = 500)
    private String descricao;

    /**
     * Valor da despesa.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    /**
     * Data de vencimento.
     */
    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    /**
     * Data de pagamento (quando paga).
     */
    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    /**
     * Status da despesa.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusDespesa status = StatusDespesa.PENDENTE;

    /**
     * Número do documento/nota fiscal.
     */
    @Column(name = "numero_documento", length = 100)
    private String numeroDocumento;

    /**
     * Fornecedor/Beneficiário.
     */
    @Column(length = 200)
    private String fornecedor;

    /**
     * Observações adicionais.
     */
    @Column(columnDefinition = "TEXT")
    private String observacoes;

    /**
     * Indica se é despesa recorrente (mensal).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean recorrente = false;

    /**
     * Tipo de pagamento utilizado.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pagamento", length = 30)
    private TipoPagamento tipoPagamento;

    /**
     * ID da movimentação de estoque relacionada (se for compra de peças).
     * Preenchido automaticamente quando a despesa é gerada por entrada de estoque.
     */
    @Column(name = "movimentacao_estoque_id")
    private UUID movimentacaoEstoqueId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    @PrePersist
    @PreUpdate
    protected void prePersist() {
        if (this.oficina == null && TenantContext.isSet()) {
            UUID tenantId = TenantContext.getTenantId();
            Oficina oficina = new Oficina();
            oficina.setId(tenantId);
            this.oficina = oficina;
        }

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Valor deve ser maior que zero");
        }
    }

    /**
     * Marca a despesa como paga.
     */
    public void pagar(LocalDate dataPagamento, TipoPagamento tipoPagamento) {
        this.dataPagamento = dataPagamento;
        this.tipoPagamento = tipoPagamento;
        this.status = StatusDespesa.PAGA;
    }

    /**
     * Cancela a despesa.
     */
    public void cancelar() {
        this.status = StatusDespesa.CANCELADA;
    }

    /**
     * Verifica se está vencida.
     */
    public boolean isVencida() {
        return status == StatusDespesa.PENDENTE &&
               dataVencimento != null &&
               dataVencimento.isBefore(LocalDate.now());
    }
}
