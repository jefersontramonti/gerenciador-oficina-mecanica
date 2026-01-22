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
 * Configurações gerais de parcelamento por oficina.
 * Define limites, bandeiras aceitas e preferências de exibição.
 */
@Entity
@Table(
    name = "configuracoes_parcelamento",
    indexes = {
        @Index(name = "idx_config_parcelamento_oficina", columnList = "oficina_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ConfiguracaoParcelamento implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false, unique = true)
    private Oficina oficina;

    /**
     * Número máximo de parcelas permitidas.
     */
    @Column(name = "parcelas_maximas", nullable = false)
    @NotNull
    @Min(value = 1, message = "Mínimo de 1 parcela")
    @Max(value = 24, message = "Máximo de 24 parcelas")
    @Builder.Default
    private Integer parcelasMaximas = 12;

    /**
     * Valor mínimo de cada parcela.
     */
    @Column(name = "valor_minimo_parcela", precision = 10, scale = 2, nullable = false)
    @NotNull
    @Builder.Default
    private BigDecimal valorMinimoParcela = new BigDecimal("50.00");

    /**
     * Valor mínimo total para permitir parcelamento.
     */
    @Column(name = "valor_minimo_parcelamento", precision = 10, scale = 2, nullable = false)
    @NotNull
    @Builder.Default
    private BigDecimal valorMinimoParcelamento = new BigDecimal("100.00");

    // Bandeiras aceitas para parcelamento
    @Column(name = "aceita_visa", nullable = false)
    @Builder.Default
    private Boolean aceitaVisa = true;

    @Column(name = "aceita_mastercard", nullable = false)
    @Builder.Default
    private Boolean aceitaMastercard = true;

    @Column(name = "aceita_elo", nullable = false)
    @Builder.Default
    private Boolean aceitaElo = true;

    @Column(name = "aceita_amex", nullable = false)
    @Builder.Default
    private Boolean aceitaAmex = true;

    @Column(name = "aceita_hipercard", nullable = false)
    @Builder.Default
    private Boolean aceitaHipercard = true;

    /**
     * Exibir valor total com juros no resumo.
     */
    @Column(name = "exibir_valor_total", nullable = false)
    @Builder.Default
    private Boolean exibirValorTotal = true;

    /**
     * Exibir informações de juros ao cliente.
     */
    @Column(name = "exibir_juros", nullable = false)
    @Builder.Default
    private Boolean exibirJuros = true;

    /**
     * Indica se o parcelamento está ativo.
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
     * Calcula o número máximo de parcelas possíveis para um valor.
     */
    public int calcularParcelasMaximas(BigDecimal valor) {
        if (valor.compareTo(valorMinimoParcelamento) < 0) {
            return 1; // Não permite parcelamento
        }

        int maxPorValor = valor.divide(valorMinimoParcela, 0, java.math.RoundingMode.DOWN).intValue();
        return Math.min(maxPorValor, parcelasMaximas);
    }
}
