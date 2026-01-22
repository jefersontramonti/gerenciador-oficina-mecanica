package com.pitstop.financeiro.domain;

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
 * Entidade que representa um extrato bancário importado.
 */
@Entity
@Table(name = "extratos_bancarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ExtratoBancario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_bancaria_id")
    private ContaBancaria contaBancaria;

    @Column(name = "arquivo_nome", nullable = false)
    private String arquivoNome;

    @Column(name = "arquivo_hash", nullable = false, length = 64)
    private String arquivoHash;

    @Column(name = "tipo_arquivo", nullable = false, length = 20)
    @Builder.Default
    private String tipoArquivo = "OFX";

    @Column(name = "data_importacao", nullable = false)
    @Builder.Default
    private LocalDateTime dataImportacao = LocalDateTime.now();

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "saldo_inicial", precision = 15, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name = "saldo_final", precision = 15, scale = 2)
    private BigDecimal saldoFinal;

    @Column(name = "total_transacoes")
    @Builder.Default
    private Integer totalTransacoes = 0;

    @Column(name = "total_conciliadas")
    @Builder.Default
    private Integer totalConciliadas = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StatusExtrato status = StatusExtrato.PENDENTE;

    @OneToMany(mappedBy = "extrato", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TransacaoExtrato> transacoes = new ArrayList<>();

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
     * Adiciona uma transação ao extrato.
     */
    public void addTransacao(TransacaoExtrato transacao) {
        transacoes.add(transacao);
        transacao.setExtrato(this);
        transacao.setOficina(this.oficina);
        this.totalTransacoes = transacoes.size();
    }

    /**
     * Calcula percentual de conciliação.
     */
    public double getPercentualConciliado() {
        if (totalTransacoes == null || totalTransacoes == 0) return 0;
        if (totalConciliadas == null) return 0;
        return (totalConciliadas.doubleValue() / totalTransacoes.doubleValue()) * 100;
    }

    /**
     * Atualiza contador de transações conciliadas.
     */
    public void atualizarContadores() {
        this.totalTransacoes = transacoes.size();
        this.totalConciliadas = (int) transacoes.stream()
            .filter(t -> t.getStatus() != StatusConciliacao.NAO_CONCILIADA)
            .count();

        if (this.totalConciliadas.equals(this.totalTransacoes) && this.totalTransacoes > 0) {
            this.status = StatusExtrato.CONCLUIDO;
        } else if (this.totalConciliadas > 0) {
            this.status = StatusExtrato.EM_ANDAMENTO;
        }
    }
}
