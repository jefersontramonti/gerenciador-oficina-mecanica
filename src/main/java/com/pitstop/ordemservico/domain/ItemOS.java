package com.pitstop.ordemservico.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.security.tenant.TenantContext;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa um item de uma Ordem de Serviço.
 *
 * <p>Um item pode ser:</p>
 * <ul>
 *   <li><strong>PECA:</strong> Peça do estoque (requer peca_id)</li>
 *   <li><strong>SERVICO:</strong> Mão de obra ou serviço executado</li>
 * </ul>
 *
 * <p><strong>Regras de Negócio:</strong></p>
 * <ul>
 *   <li>Se tipo = PECA, o campo pecaId é obrigatório (FK para tabela pecas)</li>
 *   <li>Quantidade deve ser maior que zero</li>
 *   <li>Valor unitário não pode ser negativo</li>
 *   <li>Desconto não pode ser maior que o valor total do item</li>
 *   <li>Valor total é calculado automaticamente: (quantidade * valorUnitario) - desconto</li>
 *   <li>Itens de peças afetam o estoque ao finalizar a OS</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Entity
@Table(
    name = "item_os",
    indexes = {
        @Index(name = "idx_item_os_ordem_servico_id", columnList = "ordem_servico_id"),
        @Index(name = "idx_item_os_peca_id", columnList = "peca_id"),
        @Index(name = "idx_item_os_tipo", columnList = "tipo"),
        @Index(name = "idx_item_os_created_at", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "tipo", "descricao", "quantidade", "valorUnitario"})
public class ItemOS implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Identificador único do item (UUID v4).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Oficina à qual este item pertence (multi-tenant).
     * Preenchida automaticamente via TenantContext no @PrePersist.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id")
    private Oficina oficina;

    /**
     * Ordem de serviço à qual este item pertence (relacionamento Many-to-One).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_os_ordem_servico"))
    @JsonBackReference
    private OrdemServico ordemServico;

    /**
     * Tipo do item (PECA ou SERVICO).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    @NotNull(message = "Tipo do item é obrigatório")
    private TipoItem tipo;

    /**
     * Identificador da peça (FK para tabela pecas).
     * Obrigatório se tipo = PECA e origemPeca = ESTOQUE.
     * Nullable para SERVICO ou peças avulsas/cliente.
     */
    @Column(name = "peca_id")
    private UUID pecaId;

    /**
     * Origem da peça (ESTOQUE, AVULSA, CLIENTE).
     * Obrigatório para itens do tipo PECA.
     * - ESTOQUE: Peça do inventário, gera baixa automática
     * - AVULSA: Peça comprada externamente, sem controle de estoque
     * - CLIENTE: Peça fornecida pelo cliente, sem controle de estoque
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "origem_peca", length = 20)
    private OrigemPeca origemPeca;

    /**
     * Descrição do item (nome da peça ou serviço).
     * Mínimo 3 caracteres.
     */
    @Column(name = "descricao", nullable = false, length = 500)
    @NotBlank(message = "Descrição do item é obrigatória")
    @Size(min = 3, max = 500, message = "Descrição deve ter entre 3 e 500 caracteres")
    private String descricao;

    /**
     * Quantidade do item (peças ou horas de serviço).
     * Deve ser maior que zero.
     */
    @Column(name = "quantidade", nullable = false)
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    private Integer quantidade;

    /**
     * Valor unitário do item.
     * Não pode ser negativo.
     */
    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Valor unitário é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor unitário não pode ser negativo")
    private BigDecimal valorUnitario;

    /**
     * Desconto aplicado neste item (em valor absoluto).
     * Não pode ser negativo nem maior que o valor total do item.
     */
    @Column(name = "desconto", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Desconto não pode ser negativo")
    @Builder.Default
    private BigDecimal desconto = BigDecimal.ZERO;

    /**
     * Valor total do item (calculado automaticamente).
     * Fórmula: (quantidade * valorUnitario) - desconto
     */
    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor total não pode ser negativo")
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    /**
     * Data/hora de criação do registro (preenchido automaticamente).
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== MÉTODOS DE NEGÓCIO =====

    /**
     * Calcula o valor total do item.
     * Fórmula: (quantidade * valorUnitario) - desconto
     *
     * @return Valor total calculado (sempre >= 0)
     */
    public BigDecimal calcularValorTotal() {
        if (this.quantidade == null || this.valorUnitario == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal valorBruto = this.valorUnitario
            .multiply(BigDecimal.valueOf(this.quantidade))
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal descontoAplicado = this.desconto != null ? this.desconto : BigDecimal.ZERO;

        BigDecimal valorLiquido = valorBruto.subtract(descontoAplicado)
            .setScale(2, RoundingMode.HALF_UP);

        // Garante que não seja negativo
        return valorLiquido.max(BigDecimal.ZERO);
    }

    /**
     * Valida regras de negócio para itens de peça conforme a origem.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>PECA + ESTOQUE: pecaId obrigatório</li>
     *   <li>PECA + AVULSA/CLIENTE: descrição detalhada obrigatória (mín 10 chars)</li>
     * </ul>
     *
     * @throws IllegalStateException se regras não forem atendidas
     */
    public void validarPecaObrigatoria() {
        if (this.tipo != TipoItem.PECA) {
            return; // Não é peça, nada a validar
        }

        // Para peças, origemPeca deve ser definida
        if (this.origemPeca == null) {
            throw new IllegalStateException("Item do tipo PECA deve ter origemPeca informada (ESTOQUE, AVULSA ou CLIENTE)");
        }

        // Peça do estoque requer pecaId
        if (this.origemPeca == OrigemPeca.ESTOQUE && this.pecaId == null) {
            throw new IllegalStateException("Peça do ESTOQUE deve ter pecaId informado");
        }

        // Peça avulsa/cliente requer descrição detalhada
        if (this.origemPeca.isExterna()) {
            if (this.descricao == null || this.descricao.trim().length() < 10) {
                throw new IllegalStateException(
                    "Peça " + this.origemPeca.getDisplayName() + " requer descrição detalhada (mínimo 10 caracteres)"
                );
            }
        }
    }

    // ===== MÉTODOS HELPER PARA ORIGEM DA PEÇA =====

    /**
     * Verifica se este item é uma peça do estoque (gera baixa automática).
     *
     * @return true se tipo=PECA e origemPeca=ESTOQUE
     */
    public boolean isPecaDoEstoque() {
        return this.tipo == TipoItem.PECA && this.origemPeca == OrigemPeca.ESTOQUE;
    }

    /**
     * Verifica se este item é uma peça avulsa (comprada externamente).
     *
     * @return true se tipo=PECA e origemPeca=AVULSA
     */
    public boolean isPecaAvulsa() {
        return this.tipo == TipoItem.PECA && this.origemPeca == OrigemPeca.AVULSA;
    }

    /**
     * Verifica se este item é uma peça fornecida pelo cliente.
     *
     * @return true se tipo=PECA e origemPeca=CLIENTE
     */
    public boolean isPecaCliente() {
        return this.tipo == TipoItem.PECA && this.origemPeca == OrigemPeca.CLIENTE;
    }

    /**
     * Verifica se este item deve gerar movimentação de estoque.
     *
     * @return true se tipo=PECA, origemPeca=ESTOQUE e pecaId preenchido
     */
    public boolean deveGerarMovimentacaoEstoque() {
        return isPecaDoEstoque() && this.pecaId != null;
    }

    /**
     * Valida se o desconto não excede o valor total do item.
     *
     * @throws IllegalStateException se desconto > (quantidade * valorUnitario)
     */
    public void validarDesconto() {
        if (this.desconto == null || this.quantidade == null || this.valorUnitario == null) {
            return;
        }

        BigDecimal valorBruto = this.valorUnitario.multiply(BigDecimal.valueOf(this.quantidade));

        if (this.desconto.compareTo(valorBruto) > 0) {
            throw new IllegalStateException(
                String.format("Desconto (R$ %.2f) não pode ser maior que o valor total do item (R$ %.2f)",
                    this.desconto, valorBruto)
            );
        }
    }

    /**
     * Lifecycle callback executado antes de salvar no banco.
     * Define oficina via TenantContext, valida regras de negócio e normaliza dados.
     */
    @PrePersist
    @PreUpdate
    protected void prePersistOrUpdate() {
        // Multi-tenancy: set oficina from TenantContext
        if (this.oficina == null && TenantContext.isSet()) {
            UUID tenantId = TenantContext.getTenantId();
            Oficina oficina = new Oficina();
            oficina.setId(tenantId);
            this.oficina = oficina;
        }

        // Trim da descrição
        if (this.descricao != null) {
            this.descricao = this.descricao.trim();
        }

        // Validações
        validarPecaObrigatoria();
        validarDesconto();

        // Calcula valor total
        this.valorTotal = calcularValorTotal();
    }
}
