package com.pitstop.financeiro.domain;

import com.pitstop.oficina.domain.Oficina;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa um plano de assinatura oferecido pela oficina.
 */
@Entity
@Table(name = "planos_assinatura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PlanoAssinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodicidade", nullable = false, length = 20)
    @Builder.Default
    private PeriodicidadeAssinatura periodicidade = PeriodicidadeAssinatura.MENSAL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "servicos_incluidos", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> servicosIncluidos = new ArrayList<>();

    @Column(name = "limite_os_mes")
    private Integer limiteOsMes;

    @Column(name = "desconto_pecas", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descontoPecas = BigDecimal.ZERO;

    @Column(name = "desconto_mao_obra", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descontoMaoObra = BigDecimal.ZERO;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "plano", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Assinatura> assinaturas = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica se o plano tem limite de OS.
     */
    public boolean temLimiteOs() {
        return limiteOsMes != null && limiteOsMes > 0;
    }

    /**
     * Verifica se oferece desconto em peças.
     */
    public boolean temDescontoPecas() {
        return descontoPecas != null && descontoPecas.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Verifica se oferece desconto em mão de obra.
     */
    public boolean temDescontoMaoObra() {
        return descontoMaoObra != null && descontoMaoObra.compareTo(BigDecimal.ZERO) > 0;
    }
}
