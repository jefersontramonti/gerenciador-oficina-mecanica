package com.pitstop.financeiro.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.security.tenant.TenantContext;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que armazena as credenciais de configuração de gateways de pagamento.
 * Cada oficina pode ter múltiplos gateways configurados (Mercado Pago, PagSeguro, etc.).
 *
 * <p>As credenciais são armazenadas de forma criptografada para segurança.</p>
 */
@Entity
@Table(
    name = "configuracoes_gateway",
    indexes = {
        @Index(name = "idx_config_gateway_oficina", columnList = "oficina_id"),
        @Index(name = "idx_config_gateway_tipo", columnList = "tipo_gateway"),
        @Index(name = "idx_config_gateway_ativo", columnList = "ativo")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_config_gateway_oficina_tipo",
            columnNames = {"oficina_id", "tipo_gateway"}
        )
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ConfiguracaoGateway implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id")
    private Oficina oficina;

    /**
     * Tipo do gateway de pagamento.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_gateway", nullable = false, length = 30)
    @NotNull(message = "Tipo do gateway é obrigatório")
    private TipoGateway tipoGateway;

    /**
     * Ambiente do gateway (sandbox ou produção).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ambiente", nullable = false, length = 20)
    @NotNull(message = "Ambiente é obrigatório")
    @Builder.Default
    private AmbienteGateway ambiente = AmbienteGateway.SANDBOX;

    /**
     * Access Token (chave privada) do gateway.
     * Armazenado de forma criptografada.
     */
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    /**
     * Public Key (chave pública) do gateway.
     * Usada no frontend para integração com SDK do gateway.
     */
    @Column(name = "public_key", length = 500)
    @Size(max = 500, message = "Public Key deve ter no máximo 500 caracteres")
    private String publicKey;

    /**
     * Client ID (identificador da aplicação no gateway).
     */
    @Column(name = "client_id", length = 255)
    @Size(max = 255, message = "Client ID deve ter no máximo 255 caracteres")
    private String clientId;

    /**
     * Client Secret (segredo da aplicação).
     */
    @Column(name = "client_secret", columnDefinition = "TEXT")
    private String clientSecret;

    /**
     * URL para notificações webhook do gateway.
     * Preenchida automaticamente pelo sistema.
     */
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    /**
     * Secret para validar assinatura dos webhooks.
     */
    @Column(name = "webhook_secret", length = 255)
    private String webhookSecret;

    /**
     * Indica se este gateway está ativo e pode processar pagamentos.
     */
    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = false;

    /**
     * Indica se este é o gateway padrão para novos pagamentos.
     */
    @Column(name = "padrao", nullable = false)
    @Builder.Default
    private Boolean padrao = false;

    /**
     * Taxa percentual cobrada pelo gateway (para cálculos).
     * Exemplo: 4.99 para 4.99%
     */
    @Column(name = "taxa_percentual", precision = 5, scale = 2)
    private BigDecimal taxaPercentual;

    /**
     * Taxa fixa por transação em reais.
     */
    @Column(name = "taxa_fixa", precision = 10, scale = 2)
    private BigDecimal taxaFixa;

    /**
     * Observações ou notas sobre a configuração.
     */
    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    /**
     * Data da última validação bem-sucedida das credenciais.
     */
    @Column(name = "data_ultima_validacao")
    private LocalDateTime dataUltimaValidacao;

    /**
     * Status da última validação.
     */
    @Column(name = "status_validacao", length = 20)
    private String statusValidacao;

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
     * Verifica se a configuração está completa e válida.
     */
    public boolean isConfiguracaoCompleta() {
        return accessToken != null && !accessToken.isBlank();
    }

    /**
     * Verifica se o gateway está pronto para uso.
     */
    public boolean isProntoParaUso() {
        return ativo && isConfiguracaoCompleta() &&
               "VALIDO".equals(statusValidacao);
    }

    /**
     * Retorna a URL base do gateway de acordo com o ambiente.
     */
    public String getBaseUrl() {
        if (tipoGateway == TipoGateway.MERCADO_PAGO) {
            return ambiente == AmbienteGateway.PRODUCAO
                ? "https://api.mercadopago.com"
                : "https://api.mercadopago.com"; // MP usa mesmo endpoint, diferencia pelo token
        }
        return null;
    }
}
