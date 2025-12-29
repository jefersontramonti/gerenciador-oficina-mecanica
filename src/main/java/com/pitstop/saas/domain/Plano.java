package com.pitstop.saas.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a subscription plan in the PitStop SaaS platform.
 *
 * <p>Plans define the features, limits, and pricing for workshop subscriptions.
 * SUPER_ADMINs can create and manage plans dynamically through the admin panel.</p>
 *
 * <p><b>Plan Features:</b></p>
 * <ul>
 *   <li>emiteNotaFiscal - Can emit fiscal invoices (NF-e, NFS-e, NFC-e)</li>
 *   <li>whatsappAutomatizado - Automated WhatsApp messaging</li>
 *   <li>manutencaoPreventiva - Preventive maintenance tracking</li>
 *   <li>anexoImagensDocumentos - Image and document attachments</li>
 *   <li>relatoriosAvancados - Advanced reports</li>
 *   <li>integracaoMercadoPago - Mercado Pago integration</li>
 *   <li>suportePrioritario - Priority support</li>
 *   <li>backupAutomatico - Automatic backup</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "planos",
       indexes = {
           @Index(name = "idx_planos_codigo", columnList = "codigo"),
           @Index(name = "idx_planos_ativo", columnList = "ativo"),
           @Index(name = "idx_planos_visivel", columnList = "visivel"),
           @Index(name = "idx_planos_ordem", columnList = "ordem_exibicao")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // =====================================
    // BASIC INFO
    // =====================================

    @Column(name = "codigo", unique = true, nullable = false, length = 30)
    @NotBlank(message = "Código é obrigatório")
    @Size(max = 30, message = "Código deve ter no máximo 30 caracteres")
    private String codigo;

    @Column(name = "nome", nullable = false, length = 100)
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    // =====================================
    // PRICING
    // =====================================

    @Column(name = "valor_mensal", precision = 10, scale = 2, nullable = false)
    @NotNull(message = "Valor mensal é obrigatório")
    @Builder.Default
    private BigDecimal valorMensal = BigDecimal.ZERO;

    @Column(name = "valor_anual", precision = 10, scale = 2)
    private BigDecimal valorAnual;

    @Column(name = "trial_dias")
    @Builder.Default
    private Integer trialDias = 14;

    // =====================================
    // LIMITS (-1 = unlimited)
    // =====================================

    @Column(name = "limite_usuarios")
    @Builder.Default
    private Integer limiteUsuarios = 1;

    @Column(name = "limite_os_mes")
    @Builder.Default
    private Integer limiteOsMes = -1;

    @Column(name = "limite_clientes")
    @Builder.Default
    private Integer limiteClientes = -1;

    @Column(name = "limite_espaco_mb")
    @Builder.Default
    private Long limiteEspacoMb = 5120L;  // 5GB default

    @Column(name = "limite_api_calls")
    @Builder.Default
    private Integer limiteApiCalls = -1;

    @Column(name = "limite_whatsapp_mensagens")
    @Builder.Default
    private Integer limiteWhatsappMensagens = 0;

    @Column(name = "limite_emails_mes")
    @Builder.Default
    private Integer limiteEmailsMes = 100;

    // =====================================
    // FEATURES (JSONB)
    // =====================================

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Boolean> features = Map.of(
        "emiteNotaFiscal", false,
        "whatsappAutomatizado", false,
        "manutencaoPreventiva", false,
        "anexoImagensDocumentos", false,
        "relatoriosAvancados", false,
        "integracaoMercadoPago", false,
        "suportePrioritario", false,
        "backupAutomatico", true
    );

    // =====================================
    // DISPLAY & MARKETING
    // =====================================

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "visivel", nullable = false)
    @Builder.Default
    private Boolean visivel = true;

    @Column(name = "recomendado", nullable = false)
    @Builder.Default
    private Boolean recomendado = false;

    @Column(name = "cor_destaque", length = 20)
    private String corDestaque;

    @Column(name = "tag_promocao", length = 50)
    private String tagPromocao;

    @Column(name = "ordem_exibicao")
    @Builder.Default
    private Integer ordemExibicao = 0;

    // =====================================
    // TIMESTAMPS
    // =====================================

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =====================================
    // HELPER METHODS
    // =====================================

    /**
     * Checks if a specific feature is enabled for this plan.
     *
     * @param featureKey the feature key to check
     * @return true if the feature is enabled, false otherwise
     */
    public boolean hasFeature(String featureKey) {
        return features != null && Boolean.TRUE.equals(features.get(featureKey));
    }

    /**
     * Checks if users limit is unlimited.
     *
     * @return true if limiteUsuarios is -1
     */
    public boolean isUsuariosIlimitados() {
        return limiteUsuarios != null && limiteUsuarios == -1;
    }

    /**
     * Checks if storage limit is unlimited.
     *
     * @return true if limiteEspacoMb is -1
     */
    public boolean isEspacoIlimitado() {
        return limiteEspacoMb != null && limiteEspacoMb == -1;
    }

    /**
     * Gets the annual discount percentage compared to monthly pricing.
     *
     * @return discount percentage, or 0 if no annual pricing
     */
    public BigDecimal getDescontoAnual() {
        if (valorAnual == null || valorMensal == null || valorMensal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal valorAnualSemDesconto = valorMensal.multiply(new BigDecimal("12"));
        if (valorAnualSemDesconto.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ONE.subtract(valorAnual.divide(valorAnualSemDesconto, 2, java.math.RoundingMode.HALF_UP))
                             .multiply(new BigDecimal("100"));
    }

    /**
     * Checks if the plan is custom pricing (valor_mensal = 0).
     *
     * @return true if custom pricing
     */
    public boolean isPrecoSobConsulta() {
        return valorMensal == null || valorMensal.compareTo(BigDecimal.ZERO) == 0;
    }
}
