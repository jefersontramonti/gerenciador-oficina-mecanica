package com.pitstop.financeiro.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa um pagamento no sistema PitStop.
 *
 * <p>Um pagamento está sempre vinculado a uma Ordem de Serviço e pode ser
 * parcelado (ex: cartão de crédito, boleto). Cada parcela é um registro
 * separado na tabela.</p>
 *
 * <p><strong>Regras de Negócio:</strong></p>
 * <ul>
 *   <li>Valor deve ser maior que zero</li>
 *   <li>Data de pagamento só é preenchida quando status = PAGO</li>
 *   <li>Pagamentos podem ser estornados apenas se status = PAGO</li>
 *   <li>Total de pagamentos não pode exceder valorFinal da OS</li>
 *   <li>Uma OS só muda para ENTREGUE se totalmente paga</li>
 *   <li>Parcela atual deve ser menor ou igual ao total de parcelas</li>
 * </ul>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Entity
@Table(
    name = "pagamentos",
    indexes = {
        @Index(name = "idx_pagamentos_os", columnList = "ordem_servico_id"),
        @Index(name = "idx_pagamentos_status", columnList = "status"),
        @Index(name = "idx_pagamentos_tipo", columnList = "tipo"),
        @Index(name = "idx_pagamentos_vencimento", columnList = "data_vencimento"),
        @Index(name = "idx_pagamentos_data_pagamento", columnList = "data_pagamento")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "tipo", "valor", "status"})
public class Pagamento implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Identificador único do pagamento (UUID v4).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * ID da Ordem de Serviço relacionada.
     * Todo pagamento DEVE estar vinculado a uma OS.
     */
    @Column(name = "ordem_servico_id", nullable = false)
    @NotNull(message = "Ordem de serviço é obrigatória")
    private UUID ordemServicoId;

    /**
     * Tipo de pagamento (dinheiro, PIX, cartão, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    @NotNull(message = "Tipo de pagamento é obrigatório")
    private TipoPagamento tipo;

    /**
     * Status atual do pagamento.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Status do pagamento é obrigatório")
    @Builder.Default
    private StatusPagamento status = StatusPagamento.PENDENTE;

    /**
     * Valor do pagamento (ou da parcela, se parcelado).
     */
    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Valor do pagamento é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal valor;

    /**
     * Total de parcelas (1 para pagamento à vista).
     */
    @Column(name = "parcelas", nullable = false)
    @NotNull(message = "Número de parcelas é obrigatório")
    @Min(value = 1, message = "Número mínimo de parcelas é 1")
    @Max(value = 12, message = "Número máximo de parcelas é 12")
    @Builder.Default
    private Integer parcelas = 1;

    /**
     * Parcela atual (1 para primeira parcela, 2 para segunda, etc.).
     * Para pagamentos à vista, sempre 1.
     */
    @Column(name = "parcela_atual", nullable = false)
    @NotNull(message = "Parcela atual é obrigatória")
    @Min(value = 1, message = "Parcela atual mínima é 1")
    @Builder.Default
    private Integer parcelaAtual = 1;

    /**
     * Data de vencimento do pagamento/parcela.
     */
    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    /**
     * Data efetiva do pagamento (quando foi quitado).
     * Preenchido apenas quando status = PAGO.
     */
    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    /**
     * Observações sobre o pagamento.
     * Ex: "Pago com desconto de 5%", "Cliente solicitou boleto".
     */
    @Column(name = "observacao", columnDefinition = "TEXT")
    @Size(max = 1000, message = "Observação deve ter no máximo 1000 caracteres")
    private String observacao;

    /**
     * Comprovante de pagamento (pode ser path do arquivo ou base64).
     * Usado para armazenar comprovantes de transferência, PIX, etc.
     */
    @Column(name = "comprovante", columnDefinition = "TEXT")
    private String comprovante;

    /**
     * ID da Nota Fiscal relacionada (se houver).
     * Preenchido apenas se a OS gerar NF-e após pagamento.
     */
    @Column(name = "nota_fiscal_id")
    private UUID notaFiscalId;

    /**
     * Data e hora de criação do registro (auditoria).
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data e hora da última atualização do registro (auditoria).
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Valida regras de negócio antes de persistir.
     */
    @PrePersist
    @PreUpdate
    private void validarRegrasDeNegocio() {
        // Parcela atual não pode ser maior que total de parcelas
        if (parcelaAtual != null && parcelas != null && parcelaAtual > parcelas) {
            throw new IllegalStateException(
                String.format("Parcela atual (%d) não pode ser maior que total de parcelas (%d)",
                    parcelaAtual, parcelas)
            );
        }

        // Se pagamento for parcelado, tipo deve aceitar parcelas
        if (parcelas != null && parcelas > 1 && tipo != null && !tipo.aceitaParcelas()) {
            throw new IllegalStateException(
                String.format("Tipo de pagamento %s não aceita parcelamento", tipo.getDescricao())
            );
        }

        // Data de pagamento só pode estar preenchida se status = PAGO
        if (dataPagamento != null && status != StatusPagamento.PAGO) {
            throw new IllegalStateException("Data de pagamento só pode ser definida quando status = PAGO");
        }

        // Se status = PAGO, data de pagamento é obrigatória
        if (status == StatusPagamento.PAGO && dataPagamento == null) {
            throw new IllegalStateException("Data de pagamento é obrigatória quando status = PAGO");
        }
    }

    /**
     * Confirma o pagamento, alterando status para PAGO.
     *
     * @param dataPagamento data efetiva do pagamento
     * @throws IllegalStateException se pagamento já foi pago ou cancelado
     */
    public void confirmar(LocalDate dataPagamento) {
        if (this.status.isEstadoFinal()) {
            throw new IllegalStateException(
                String.format("Pagamento não pode ser confirmado. Status atual: %s", this.status.getDescricao())
            );
        }

        this.status = StatusPagamento.PAGO;
        this.dataPagamento = dataPagamento;
    }

    /**
     * Cancela o pagamento.
     *
     * @throws IllegalStateException se pagamento já foi pago
     */
    public void cancelar() {
        if (this.status == StatusPagamento.PAGO) {
            throw new IllegalStateException("Pagamento já foi pago. Use estornar() para reverter.");
        }

        if (this.status.isEstadoFinal()) {
            throw new IllegalStateException(
                String.format("Pagamento não pode ser cancelado. Status atual: %s", this.status.getDescricao())
            );
        }

        this.status = StatusPagamento.CANCELADO;
    }

    /**
     * Estorna um pagamento já realizado.
     *
     * @throws IllegalStateException se pagamento não foi pago
     */
    public void estornar() {
        if (this.status != StatusPagamento.PAGO) {
            throw new IllegalStateException("Apenas pagamentos já realizados podem ser estornados");
        }

        this.status = StatusPagamento.ESTORNADO;
        this.dataPagamento = null;
    }

    /**
     * Verifica se o pagamento está vencido.
     *
     * @return true se data de vencimento passou e status ainda é PENDENTE
     */
    public boolean isVencido() {
        return dataVencimento != null &&
               dataVencimento.isBefore(LocalDate.now()) &&
               status == StatusPagamento.PENDENTE;
    }

    /**
     * Marca o pagamento como vencido.
     */
    public void marcarComoVencido() {
        if (isVencido()) {
            this.status = StatusPagamento.VENCIDO;
        }
    }
}
