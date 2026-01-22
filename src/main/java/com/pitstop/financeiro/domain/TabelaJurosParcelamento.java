package com.pitstop.financeiro.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.security.tenant.TenantContext;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que define as taxas de juros por faixa de parcelas.
 * Cada oficina pode ter múltiplas faixas configuradas.
 */
@Entity
@Table(
    name = "tabelas_juros_parcelamento",
    indexes = {
        @Index(name = "idx_tabela_juros_oficina", columnList = "oficina_id"),
        @Index(name = "idx_tabela_juros_ativo", columnList = "ativo"),
        @Index(name = "idx_tabela_juros_faixa", columnList = "parcelas_minimo, parcelas_maximo")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class TabelaJurosParcelamento implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    /**
     * Número mínimo de parcelas para esta faixa.
     */
    @Column(name = "parcelas_minimo", nullable = false)
    @NotNull(message = "Parcelas mínimas é obrigatório")
    @Min(value = 1, message = "Mínimo de 1 parcela")
    @Max(value = 24, message = "Máximo de 24 parcelas")
    private Integer parcelasMinimo;

    /**
     * Número máximo de parcelas para esta faixa.
     */
    @Column(name = "parcelas_maximo", nullable = false)
    @NotNull(message = "Parcelas máximas é obrigatório")
    @Min(value = 1, message = "Mínimo de 1 parcela")
    @Max(value = 24, message = "Máximo de 24 parcelas")
    private Integer parcelasMaximo;

    /**
     * Taxa de juros mensal em percentual.
     * Exemplo: 2.99 para 2.99% ao mês.
     */
    @Column(name = "percentual_juros", precision = 5, scale = 2, nullable = false)
    @NotNull(message = "Percentual de juros é obrigatório")
    @Min(value = 0, message = "Juros não pode ser negativo")
    @Builder.Default
    private BigDecimal percentualJuros = BigDecimal.ZERO;

    /**
     * Tipo de cálculo de juros.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_juros", nullable = false, length = 20)
    @NotNull(message = "Tipo de juros é obrigatório")
    @Builder.Default
    private TipoJuros tipoJuros = TipoJuros.SEM_JUROS;

    /**
     * Se true, o cliente paga os juros.
     * Se false, a oficina absorve (desconto promocional).
     */
    @Column(name = "repassar_cliente", nullable = false)
    @Builder.Default
    private Boolean repassarCliente = true;

    /**
     * Indica se esta faixa está ativa.
     */
    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void prePersist() {
        if (this.oficina == null && TenantContext.isSet()) {
            UUID tenantId = TenantContext.getTenantId();
            Oficina oficina = new Oficina();
            oficina.setId(tenantId);
            this.oficina = oficina;
        }
    }

    /**
     * Verifica se o número de parcelas está dentro desta faixa.
     */
    public boolean isParcelasDentroFaixa(int parcelas) {
        return parcelas >= parcelasMinimo && parcelas <= parcelasMaximo;
    }

    /**
     * Retorna true se esta faixa é sem juros.
     */
    public boolean isSemJuros() {
        return tipoJuros == TipoJuros.SEM_JUROS ||
               percentualJuros == null ||
               percentualJuros.compareTo(BigDecimal.ZERO) == 0;
    }
}
