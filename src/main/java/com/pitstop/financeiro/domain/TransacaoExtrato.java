package com.pitstop.financeiro.domain;

import com.pitstop.oficina.domain.Oficina;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa uma transação individual do extrato bancário.
 */
@Entity
@Table(name = "transacoes_extrato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class TransacaoExtrato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extrato_id", nullable = false)
    private ExtratoBancario extrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    // ========== Dados da transação bancária ==========

    @Column(name = "data_transacao", nullable = false)
    private LocalDate dataTransacao;

    @Column(name = "data_lancamento")
    private LocalDate dataLancamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoTransacaoBancaria tipo;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "identificador_banco", length = 100)
    private String identificadorBanco;

    @Column(name = "referencia", length = 100)
    private String referencia;

    @Column(name = "categoria_banco", length = 100)
    private String categoriaBanco;

    // ========== Status da conciliação ==========

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StatusConciliacao status = StatusConciliacao.NAO_CONCILIADA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagamento_id")
    private Pagamento pagamento;

    @Column(name = "data_conciliacao")
    private LocalDateTime dataConciliacao;

    @Column(name = "metodo_conciliacao", length = 20)
    private String metodoConciliacao;

    @Column(name = "observacao", length = 500)
    private String observacao;

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
     * Verifica se é um crédito (entrada).
     */
    public boolean isCredito() {
        return TipoTransacaoBancaria.CREDITO.equals(this.tipo);
    }

    /**
     * Verifica se é um débito (saída).
     */
    public boolean isDebito() {
        return TipoTransacaoBancaria.DEBITO.equals(this.tipo);
    }

    /**
     * Verifica se pode ser conciliada.
     */
    public boolean podeSerConciliada() {
        return StatusConciliacao.NAO_CONCILIADA.equals(this.status);
    }

    /**
     * Concilia a transação com um pagamento.
     */
    public void conciliar(Pagamento pagamento, String metodo) {
        this.pagamento = pagamento;
        this.status = StatusConciliacao.CONCILIADA;
        this.metodoConciliacao = metodo;
        this.dataConciliacao = LocalDateTime.now();
    }

    /**
     * Marca a transação como ignorada.
     */
    public void ignorar(String observacao) {
        this.status = StatusConciliacao.IGNORADA;
        this.observacao = observacao;
        this.dataConciliacao = LocalDateTime.now();
    }

    /**
     * Desconcilia a transação.
     */
    public void desconciliar() {
        this.pagamento = null;
        this.status = StatusConciliacao.NAO_CONCILIADA;
        this.metodoConciliacao = null;
        this.dataConciliacao = null;
        this.observacao = null;
    }
}
