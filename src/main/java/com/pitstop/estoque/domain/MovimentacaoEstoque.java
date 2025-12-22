package com.pitstop.estoque.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.security.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa uma movimentação de estoque (audit trail).
 * Registra todas as entradas, saídas, ajustes e devoluções de peças.
 *
 * <p>Características:</p>
 * <ul>
 *   <li>Entidade IMUTÁVEL - não permite UPDATE/DELETE após criação</li>
 *   <li>Audit trail completo com quantidade anterior e atual</li>
 *   <li>Vinculação opcional com Ordem de Serviço</li>
 *   <li>Registro de usuário responsável pela movimentação</li>
 * </ul>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Entity
@Table(name = "movimentacao_estoque", indexes = {
        @Index(name = "idx_movimentacao_peca_id", columnList = "peca_id"),
        @Index(name = "idx_movimentacao_os_id", columnList = "ordem_servico_id"),
        @Index(name = "idx_movimentacao_usuario_id", columnList = "usuario_id"),
        @Index(name = "idx_movimentacao_tipo", columnList = "tipo"),
        @Index(name = "idx_movimentacao_data", columnList = "data_movimentacao"),
        @Index(name = "idx_movimentacao_peca_data", columnList = "peca_id, data_movimentacao")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"createdAt"})
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Oficina à qual esta movimentação pertence (multi-tenant).
     * Preenchida automaticamente via TenantContext no @PrePersist.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id")
    private Oficina oficina;

    /**
     * ID da peça movimentada.
     */
    @Column(name = "peca_id", nullable = false)
    private UUID pecaId;

    /**
     * ID da Ordem de Serviço associada (opcional).
     * Preenchido quando a movimentação é resultado de finalização/cancelamento de OS.
     */
    @Column(name = "ordem_servico_id")
    private UUID ordemServicoId;

    /**
     * ID do usuário que realizou a movimentação.
     */
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    /**
     * Tipo da movimentação (ENTRADA, SAIDA, AJUSTE, DEVOLUCAO, BAIXA_OS).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMovimentacao tipo;

    /**
     * Quantidade movimentada (sempre positiva).
     * O tipo da movimentação define se é entrada ou saída.
     */
    @Column(nullable = false)
    private Integer quantidade;

    // ========== AUDIT TRAIL ==========

    /**
     * Quantidade da peça ANTES desta movimentação.
     * Permite rastrear histórico completo do estoque.
     */
    @Column(name = "quantidade_anterior", nullable = false)
    private Integer quantidadeAnterior;

    /**
     * Quantidade da peça DEPOIS desta movimentação.
     * Deve ser igual a: quantidadeAnterior +/- quantidade (conforme tipo).
     */
    @Column(name = "quantidade_atual", nullable = false)
    private Integer quantidadeAtual;

    // ========== FINANCIAL ==========

    /**
     * Valor unitário da peça no momento da movimentação.
     * Registra o valor histórico para cálculos financeiros.
     */
    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    /**
     * Valor total da movimentação (quantidade * valorUnitario).
     * Calculado automaticamente antes da persistência.
     */
    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    // ========== DETAILS ==========

    /**
     * Motivo da movimentação.
     * Obrigatório apenas para AJUSTE.
     * Exemplos: "Compra fornecedor", "OS #123 finalizada", "Ajuste inventário"
     */
    @Column(length = 500)
    private String motivo;

    /**
     * Observações adicionais opcionais.
     * Pode conter detalhes extras sobre a movimentação.
     */
    @Column(columnDefinition = "TEXT")
    private String observacao;

    /**
     * Data e hora em que a movimentação ocorreu.
     * Pode ser diferente de createdAt (registro histórico retroativo).
     */
    @Column(name = "data_movimentacao", nullable = false)
    @Builder.Default
    private LocalDateTime dataMovimentacao = LocalDateTime.now();

    // ========== AUDIT ==========

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========== LIFECYCLE CALLBACKS ==========

    /**
     * Callback executado antes de persistir a entidade.
     * Define oficina via TenantContext, calcula valor total e executa validações.
     */
    @PrePersist
    protected void onPrePersist() {
        // Multi-tenancy: set oficina from TenantContext
        if (this.oficina == null && TenantContext.isSet()) {
            UUID tenantId = TenantContext.getTenantId();
            Oficina oficina = new Oficina();
            oficina.setId(tenantId);
            this.oficina = oficina;
        }

        // 1. Calcula valor total
        if (this.quantidade != null && this.valorUnitario != null) {
            this.valorTotal = this.valorUnitario
                    .multiply(BigDecimal.valueOf(this.quantidade))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 2. Validações
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalStateException("Quantidade deve ser maior que zero");
        }
        if (quantidadeAnterior == null || quantidadeAnterior < 0) {
            throw new IllegalStateException("Quantidade anterior não pode ser negativa");
        }
        if (quantidadeAtual == null || quantidadeAtual < 0) {
            throw new IllegalStateException("Quantidade atual não pode ser negativa");
        }
        if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Valor unitário não pode ser negativo");
        }

        // Motivo é obrigatório apenas para AJUSTE
        if (tipo == TipoMovimentacao.AJUSTE && (motivo == null || motivo.trim().length() < 3)) {
            throw new IllegalStateException("Motivo é obrigatório para ajuste e deve ter no mínimo 3 caracteres");
        }

        // Para outros tipos, motivo é opcional mas se fornecido deve ter tamanho válido
        if (motivo != null && !motivo.trim().isEmpty() && motivo.trim().length() < 3) {
            throw new IllegalStateException("Motivo deve ter no mínimo 3 caracteres");
        }

        // Valida consistência da movimentação baseado no tipo
        validarConsistenciaTipo();
    }

    /**
     * Valida se a quantidade anterior/atual é consistente com o tipo de movimentação.
     */
    private void validarConsistenciaTipo() {
        if (tipo == null) {
            throw new IllegalStateException("Tipo da movimentação é obrigatório");
        }

        int diferencaEsperada;
        switch (tipo) {
            case ENTRADA, DEVOLUCAO -> diferencaEsperada = quantidade; // Aumenta estoque
            case SAIDA, BAIXA_OS -> diferencaEsperada = -quantidade;   // Diminui estoque
            case AJUSTE -> {
                // Ajuste pode aumentar ou diminuir - não valida diferença específica
                return;
            }
            default -> throw new IllegalStateException("Tipo de movimentação inválido: " + tipo);
        }

        int diferencaReal = quantidadeAtual - quantidadeAnterior;
        if (diferencaReal != diferencaEsperada) {
            throw new IllegalStateException(
                    String.format("Inconsistência na movimentação tipo %s: esperado diferença de %d, mas foi %d",
                            tipo, diferencaEsperada, diferencaReal)
            );
        }
    }

    // ========== BUSINESS METHODS ==========

    /**
     * Verifica se esta movimentação está vinculada a uma Ordem de Serviço.
     *
     * @return true se ordemServicoId não é nulo
     */
    public boolean isVinculadaOS() {
        return ordemServicoId != null;
    }

    /**
     * Calcula o impacto desta movimentação no estoque (positivo ou negativo).
     *
     * @return quantidade com sinal (+/-)
     */
    public Integer getImpactoEstoque() {
        return switch (tipo) {
            case ENTRADA, DEVOLUCAO -> quantidade;
            case SAIDA, BAIXA_OS -> -quantidade;
            case AJUSTE -> quantidadeAtual - quantidadeAnterior;
        };
    }
}
