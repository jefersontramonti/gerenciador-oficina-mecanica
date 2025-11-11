package com.pitstop.estoque.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa uma peça/item do estoque da oficina.
 * Gerencia catálogo de peças, quantidades, valores e localização física.
 *
 * <p>Características:</p>
 * <ul>
 *   <li>Soft delete via campo 'ativo'</li>
 *   <li>Optimistic locking via @Version para controle de concorrência</li>
 *   <li>Quantidade atualizada automaticamente por MovimentacaoEstoque</li>
 *   <li>Cálculo automático de margem de lucro</li>
 * </ul>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Entity
@Table(name = "pecas", indexes = {
        @Index(name = "idx_pecas_codigo", columnList = "codigo"),
        @Index(name = "idx_pecas_descricao", columnList = "descricao"),
        @Index(name = "idx_pecas_marca", columnList = "marca"),
        @Index(name = "idx_pecas_quantidade_atual", columnList = "quantidade_atual"),
        @Index(name = "idx_pecas_ativo", columnList = "ativo")
})
@EntityListeners(AuditingEntityListener.class)
@Where(clause = "ativo = true") // Soft delete filter
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"createdAt", "updatedAt"})
public class Peca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Código único da peça (SKU).
     * Usado para identificação rápida no sistema.
     */
    @Column(unique = true, nullable = false, length = 50)
    private String codigo;

    /**
     * Descrição completa da peça.
     */
    @Column(nullable = false, length = 500)
    private String descricao;

    /**
     * Fabricante ou marca da peça.
     */
    @Column(length = 100)
    private String marca;

    /**
     * Aplicação da peça (veículos ou sistemas compatíveis).
     * Ex: "Motores 1.0 e 1.4", "Todos os veículos Fiat"
     */
    @Column(length = 500)
    private String aplicacao;

    /**
     * Local de armazenamento físico (hierárquico).
     * Relacionamento com LocalArmazenamento para organização estruturada.
     * Pode ser NULL se a peça ainda não foi localizada.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_armazenamento_id")
    private LocalArmazenamento localArmazenamento;

    /**
     * Unidade de medida para contabilização do estoque.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida", nullable = false, length = 20)
    @Builder.Default
    private UnidadeMedida unidadeMedida = UnidadeMedida.UNIDADE;

    // ========== STOCK CONTROL ==========

    /**
     * Quantidade atualmente disponível em estoque.
     * <p><strong>IMPORTANTE:</strong> Este campo é atualizado automaticamente
     * pelas movimentações de estoque. NÃO deve ser modificado diretamente.</p>
     */
    @Column(name = "quantidade_atual", nullable = false)
    @Builder.Default
    private Integer quantidadeAtual = 0;

    /**
     * Quantidade mínima desejada em estoque.
     * Gera alerta quando quantidadeAtual <= quantidadeMinima.
     */
    @Column(name = "quantidade_minima", nullable = false)
    @Builder.Default
    private Integer quantidadeMinima = 1;

    // ========== FINANCIAL ==========

    /**
     * Valor de custo/compra da peça (preço pago ao fornecedor).
     */
    @Column(name = "valor_custo", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorCusto = BigDecimal.ZERO;

    /**
     * Valor de venda da peça ao cliente.
     */
    @Column(name = "valor_venda", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorVenda = BigDecimal.ZERO;

    // ========== SOFT DELETE & AUDIT ==========

    /**
     * Indica se a peça está ativa no catálogo.
     * Soft delete: false = peça desativada (não aparece em listagens).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Versão para controle de concorrência otimista.
     * Previne lost updates quando múltiplos usuários editam a mesma peça.
     */
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    // ========== CALCULATED PROPERTIES ==========

    /**
     * Calcula a margem de lucro percentual.
     * Fórmula: ((valorVenda - valorCusto) / valorCusto) * 100
     *
     * @return margem de lucro em porcentagem, ou 0 se valorCusto = 0
     */
    public BigDecimal getMargemLucro() {
        if (valorCusto == null || valorCusto.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return valorVenda.subtract(valorCusto)
                .divide(valorCusto, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Verifica se a peça está com estoque baixo (alerta).
     *
     * @return true se quantidadeAtual <= quantidadeMinima
     */
    public boolean isEstoqueBaixo() {
        return quantidadeAtual != null && quantidadeMinima != null
                && quantidadeAtual <= quantidadeMinima;
    }

    /**
     * Verifica se há estoque disponível suficiente para uma quantidade requerida.
     *
     * @param quantidadeRequerida quantidade desejada
     * @return true se há estoque suficiente
     */
    public boolean temEstoqueDisponivel(Integer quantidadeRequerida) {
        return quantidadeAtual != null && quantidadeAtual >= quantidadeRequerida;
    }

    /**
     * Calcula o valor total do estoque desta peça.
     *
     * @return quantidadeAtual * valorCusto
     */
    public BigDecimal getValorTotalEstoque() {
        if (quantidadeAtual == null || valorCusto == null) {
            return BigDecimal.ZERO;
        }
        return valorCusto.multiply(BigDecimal.valueOf(quantidadeAtual))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ========== LIFECYCLE CALLBACKS ==========

    /**
     * Validações executadas antes de persistir ou atualizar a entidade.
     */
    @PrePersist
    @PreUpdate
    protected void validate() {
        if (quantidadeAtual < 0) {
            throw new IllegalStateException("Quantidade atual não pode ser negativa");
        }
        if (quantidadeMinima < 0) {
            throw new IllegalStateException("Quantidade mínima não pode ser negativa");
        }
        if (valorVenda.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Valor de venda não pode ser negativo");
        }
        if (valorCusto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Valor de custo não pode ser negativo");
        }
        if (codigo == null || codigo.trim().length() < 3) {
            throw new IllegalStateException("Código deve ter no mínimo 3 caracteres");
        }
        if (descricao == null || descricao.trim().length() < 3) {
            throw new IllegalStateException("Descrição deve ter no mínimo 3 caracteres");
        }
    }

    // ========== BUSINESS METHODS ==========

    /**
     * Desativa a peça (soft delete).
     */
    public void desativar() {
        this.ativo = false;
    }

    /**
     * Reativa a peça.
     */
    public void reativar() {
        this.ativo = true;
    }
}
