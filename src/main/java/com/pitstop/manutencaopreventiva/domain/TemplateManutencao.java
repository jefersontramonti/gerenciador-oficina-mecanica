package com.pitstop.manutencaopreventiva.domain;

import com.pitstop.oficina.domain.Oficina;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Template de manutenção preventiva reutilizável.
 * Pode ser global (oficina_id = null) ou específico de uma oficina.
 */
@Entity
@Table(name = "templates_manutencao",
    indexes = {
        @Index(name = "idx_templates_manutencao_oficina", columnList = "oficina_id"),
        @Index(name = "idx_templates_manutencao_tipo", columnList = "tipo_manutencao")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "nome", "tipoManutencao"})
public class TemplateManutencao implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Oficina dona do template. Se null, é template global. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id")
    private Oficina oficina;

    @Column(name = "nome", nullable = false, length = 100)
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "tipo_manutencao", nullable = false, length = 50)
    @NotBlank(message = "Tipo de manutenção é obrigatório")
    @Size(max = 50, message = "Tipo de manutenção deve ter no máximo 50 caracteres")
    private String tipoManutencao;

    /** Intervalo em dias entre manutenções */
    @Column(name = "intervalo_dias")
    @Min(value = 1, message = "Intervalo de dias deve ser maior que zero")
    private Integer intervaloDias;

    /** Intervalo em km entre manutenções */
    @Column(name = "intervalo_km")
    @Min(value = 1, message = "Intervalo de km deve ser maior que zero")
    private Integer intervaloKm;

    @Column(name = "criterio", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CriterioManutencao criterio = CriterioManutencao.AMBOS;

    /** Dias de antecedência para alertar */
    @Column(name = "antecedencia_dias")
    @Builder.Default
    private Integer antecedenciaDias = 15;

    /** KM de antecedência para alertar */
    @Column(name = "antecedencia_km")
    @Builder.Default
    private Integer antecedenciaKm = 1000;

    /** Lista de itens do checklist em JSON */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist", columnDefinition = "jsonb")
    private List<ChecklistItem> checklist;

    /** Lista de peças sugeridas em JSON */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pecas_sugeridas", columnDefinition = "jsonb")
    private List<PecaSugerida> pecasSugeridas;

    @Column(name = "valor_estimado", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Valor estimado não pode ser negativo")
    private BigDecimal valorEstimado;

    @Column(name = "tempo_estimado_minutos")
    @Min(value = 1, message = "Tempo estimado deve ser maior que zero")
    private Integer tempoEstimadoMinutos;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Item do checklist de manutenção.
     */
    public record ChecklistItem(
        String item,
        Boolean obrigatorio
    ) {}

    /**
     * Peça sugerida para a manutenção.
     */
    public record PecaSugerida(
        UUID pecaId,
        Integer quantidade
    ) {}

    /**
     * Verifica se é um template global (sem oficina).
     */
    public boolean isGlobal() {
        return oficina == null;
    }
}
