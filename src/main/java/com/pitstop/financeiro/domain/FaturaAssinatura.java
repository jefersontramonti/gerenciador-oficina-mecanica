package com.pitstop.financeiro.domain;

import com.pitstop.oficina.domain.Oficina;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa uma fatura de assinatura.
 */
@Entity
@Table(name = "faturas_assinatura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class FaturaAssinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id", nullable = false)
    private Assinatura assinatura;

    @Column(name = "numero_fatura", nullable = false, length = 50)
    private String numeroFatura;

    @Column(name = "mes_referencia", nullable = false)
    private LocalDate mesReferencia;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StatusFaturaAssinatura status = StatusFaturaAssinatura.PENDENTE;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;

    // Gateway
    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    @Column(name = "gateway_payment_status", length = 50)
    private String gatewayPaymentStatus;

    @Column(name = "link_pagamento", length = 500)
    private String linkPagamento;

    // Detalhes
    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica se a fatura está vencida.
     */
    public boolean isVencida() {
        return status == StatusFaturaAssinatura.PENDENTE &&
               dataVencimento.isBefore(LocalDate.now());
    }

    /**
     * Verifica se ainda pode ser paga.
     */
    public boolean podeSerPaga() {
        return status.podeSerPaga();
    }

    /**
     * Marca a fatura como vencida.
     */
    public void marcarVencida() {
        if (isVencida()) {
            this.status = StatusFaturaAssinatura.VENCIDA;
        }
    }

    /**
     * Registra o pagamento.
     */
    public void registrarPagamento(String gatewayPaymentId, String gatewayStatus) {
        this.status = StatusFaturaAssinatura.PAGA;
        this.dataPagamento = LocalDateTime.now();
        this.gatewayPaymentId = gatewayPaymentId;
        this.gatewayPaymentStatus = gatewayStatus;
    }

    /**
     * Cancela a fatura.
     */
    public void cancelar(String observacao) {
        this.status = StatusFaturaAssinatura.CANCELADA;
        this.observacoes = observacao;
    }
}
