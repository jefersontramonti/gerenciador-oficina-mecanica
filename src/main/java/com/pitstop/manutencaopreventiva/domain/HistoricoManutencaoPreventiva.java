package com.pitstop.manutencaopreventiva.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.veiculo.domain.Veiculo;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Histórico de execuções de manutenções preventivas.
 */
@Entity
@Table(name = "historico_manutencao_preventiva",
    indexes = {
        @Index(name = "idx_historico_manutencao_oficina", columnList = "oficina_id"),
        @Index(name = "idx_historico_manutencao_plano", columnList = "plano_id"),
        @Index(name = "idx_historico_manutencao_veiculo", columnList = "veiculo_id"),
        @Index(name = "idx_historico_manutencao_data", columnList = "data_execucao")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "tipoManutencao", "dataExecucao"})
public class HistoricoManutencaoPreventiva implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false)
    @NotNull(message = "Plano é obrigatório")
    private PlanoManutencaoPreventiva plano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    @NotNull(message = "Veículo é obrigatório")
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id")
    private OrdemServico ordemServico;

    @Column(name = "data_execucao", nullable = false)
    @NotNull(message = "Data de execução é obrigatória")
    private LocalDate dataExecucao;

    @Column(name = "km_execucao")
    @Min(value = 0, message = "Km não pode ser negativo")
    private Integer kmExecucao;

    @Column(name = "tipo_manutencao", nullable = false, length = 50)
    @NotBlank(message = "Tipo de manutenção é obrigatório")
    private String tipoManutencao;

    /** Checklist executado com status de cada item */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist_executado", columnDefinition = "jsonb")
    private List<ChecklistExecutado> checklistExecutado;

    /** Peças utilizadas na manutenção */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pecas_utilizadas", columnDefinition = "jsonb")
    private List<PecaUtilizada> pecasUtilizadas;

    @Column(name = "valor_mao_obra", precision = 10, scale = 2)
    private BigDecimal valorMaoObra;

    @Column(name = "valor_pecas", precision = 10, scale = 2)
    private BigDecimal valorPecas;

    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "observacoes_mecanico", columnDefinition = "TEXT")
    private String observacoesMecanico;

    @Column(name = "proxima_previsao_data")
    private LocalDate proximaPrevisaoData;

    @Column(name = "proxima_previsao_km")
    private Integer proximaPrevisaoKm;

    @Column(name = "executado_por")
    private UUID executadoPor;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        if (this.oficina == null && TenantContext.isSet()) {
            UUID tenantId = TenantContext.getTenantId();
            Oficina oficina = new Oficina();
            oficina.setId(tenantId);
            this.oficina = oficina;
        }
        calcularValorTotal();
    }

    private void calcularValorTotal() {
        BigDecimal maoObra = valorMaoObra != null ? valorMaoObra : BigDecimal.ZERO;
        BigDecimal pecas = valorPecas != null ? valorPecas : BigDecimal.ZERO;
        this.valorTotal = maoObra.add(pecas);
    }

    /**
     * Item do checklist executado.
     */
    public record ChecklistExecutado(
        String item,
        Boolean executado,
        String observacao
    ) {}

    /**
     * Peça utilizada na manutenção.
     */
    public record PecaUtilizada(
        UUID pecaId,
        String descricao,
        Integer quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal
    ) {}
}
