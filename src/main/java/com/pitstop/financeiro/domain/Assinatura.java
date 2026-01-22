package com.pitstop.financeiro.domain;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.oficina.domain.Oficina;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa uma assinatura de cliente.
 */
@Entity
@Table(name = "assinaturas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false)
    private PlanoAssinatura plano;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StatusAssinatura status = StatusAssinatura.ATIVA;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "data_proximo_vencimento", nullable = false)
    private LocalDate dataProximoVencimento;

    @Column(name = "valor_atual", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorAtual;

    // Integração com gateway
    @Column(name = "gateway_subscription_id", length = 100)
    private String gatewaySubscriptionId;

    @Column(name = "gateway_payer_id", length = 100)
    private String gatewayPayerId;

    // Configurações
    @Column(name = "dia_vencimento")
    @Builder.Default
    private Integer diaVencimento = 10;

    @Column(name = "tolerancia_dias")
    @Builder.Default
    private Integer toleranciaDias = 5;

    // Controle de uso
    @Column(name = "os_utilizadas_mes")
    @Builder.Default
    private Integer osUtilizadasMes = 0;

    @Column(name = "mes_referencia")
    private LocalDate mesReferencia;

    // Histórico de cancelamento
    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "data_cancelamento")
    private LocalDateTime dataCancelamento;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "assinatura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FaturaAssinatura> faturas = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica se a assinatura está ativa.
     */
    public boolean isAtiva() {
        return status == StatusAssinatura.ATIVA;
    }

    /**
     * Verifica se a assinatura está inadimplente.
     */
    public boolean isInadimplente() {
        return status == StatusAssinatura.INADIMPLENTE;
    }

    /**
     * Verifica se atingiu o limite de OS do mês.
     */
    public boolean atingiuLimiteOs() {
        if (plano == null || !plano.temLimiteOs()) {
            return false;
        }
        return osUtilizadasMes >= plano.getLimiteOsMes();
    }

    /**
     * Incrementa o contador de OS utilizadas.
     */
    public void incrementarOsUtilizadas() {
        this.osUtilizadasMes++;
    }

    /**
     * Reseta o contador de OS para o novo mês.
     */
    public void resetarContadorOs(LocalDate novoMes) {
        this.osUtilizadasMes = 0;
        this.mesReferencia = novoMes;
    }

    /**
     * Pausa a assinatura.
     */
    public void pausar() {
        if (status == StatusAssinatura.ATIVA) {
            this.status = StatusAssinatura.PAUSADA;
        }
    }

    /**
     * Reativa a assinatura.
     */
    public void reativar() {
        if (status == StatusAssinatura.PAUSADA || status == StatusAssinatura.INADIMPLENTE) {
            this.status = StatusAssinatura.ATIVA;
        }
    }

    /**
     * Cancela a assinatura.
     */
    public void cancelar(String motivo) {
        this.status = StatusAssinatura.CANCELADA;
        this.motivoCancelamento = motivo;
        this.dataCancelamento = LocalDateTime.now();
        this.dataFim = LocalDate.now();
    }

    /**
     * Marca como inadimplente.
     */
    public void marcarInadimplente() {
        if (status == StatusAssinatura.ATIVA) {
            this.status = StatusAssinatura.INADIMPLENTE;
        }
    }

    /**
     * Calcula a próxima data de vencimento.
     */
    public LocalDate calcularProximoVencimento() {
        if (plano == null) return dataProximoVencimento;

        return dataProximoVencimento.plusDays(plano.getPeriodicidade().getDiasIntervalo());
    }

    /**
     * Avança para o próximo período.
     */
    public void avancarPeriodo() {
        this.dataProximoVencimento = calcularProximoVencimento();
    }
}
