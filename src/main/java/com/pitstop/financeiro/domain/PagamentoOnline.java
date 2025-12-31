package com.pitstop.financeiro.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.security.tenant.TenantContext;
import jakarta.persistence.*;
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
 * Entidade que rastreia pagamentos processados via gateway online.
 * Armazena informações do gateway, IDs externos, e status de processamento.
 */
@Entity
@Table(
    name = "pagamentos_online",
    indexes = {
        @Index(name = "idx_pag_online_oficina", columnList = "oficina_id"),
        @Index(name = "idx_pag_online_os", columnList = "ordem_servico_id"),
        @Index(name = "idx_pag_online_pagamento", columnList = "pagamento_id"),
        @Index(name = "idx_pag_online_id_externo", columnList = "id_externo"),
        @Index(name = "idx_pag_online_status", columnList = "status"),
        @Index(name = "idx_pag_online_preference", columnList = "preference_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PagamentoOnline implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id")
    private Oficina oficina;

    /**
     * ID da Ordem de Serviço relacionada.
     */
    @Column(name = "ordem_servico_id", nullable = false)
    @NotNull(message = "Ordem de serviço é obrigatória")
    private UUID ordemServicoId;

    /**
     * ID do Pagamento local relacionado (se existir).
     * Preenchido quando o pagamento é confirmado.
     */
    @Column(name = "pagamento_id")
    private UUID pagamentoId;

    /**
     * Gateway utilizado para este pagamento.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gateway", nullable = false, length = 30)
    @NotNull(message = "Gateway é obrigatório")
    private TipoGateway gateway;

    /**
     * ID da preferência de pagamento no gateway (checkout).
     * No Mercado Pago, é o preference_id.
     */
    @Column(name = "preference_id", length = 255)
    private String preferenceId;

    /**
     * ID externo do pagamento no gateway.
     * No Mercado Pago, é o payment_id.
     */
    @Column(name = "id_externo", length = 255)
    private String idExterno;

    /**
     * ID externo da cobrança (se aplicável).
     * No Mercado Pago, pode ser o merchant_order_id.
     */
    @Column(name = "id_cobranca", length = 255)
    private String idCobranca;

    /**
     * Status do pagamento no gateway.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private StatusPagamentoOnline status = StatusPagamentoOnline.PENDENTE;

    /**
     * Status detalhado retornado pelo gateway.
     */
    @Column(name = "status_detalhe", length = 100)
    private String statusDetalhe;

    /**
     * Valor do pagamento.
     */
    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Valor é obrigatório")
    private BigDecimal valor;

    /**
     * Valor líquido (após taxas).
     */
    @Column(name = "valor_liquido", precision = 10, scale = 2)
    private BigDecimal valorLiquido;

    /**
     * Valor das taxas cobradas.
     */
    @Column(name = "valor_taxa", precision = 10, scale = 2)
    private BigDecimal valorTaxa;

    /**
     * Tipo de pagamento usado (pix, credit_card, etc).
     */
    @Column(name = "metodo_pagamento", length = 50)
    private String metodoPagamento;

    /**
     * Bandeira do cartão (se aplicável).
     */
    @Column(name = "bandeira_cartao", length = 50)
    private String bandeiraCartao;

    /**
     * Últimos 4 dígitos do cartão (se aplicável).
     */
    @Column(name = "ultimos_digitos", length = 4)
    private String ultimosDigitos;

    /**
     * Número de parcelas.
     */
    @Column(name = "parcelas")
    private Integer parcelas;

    /**
     * URL do checkout (para redirecionar o cliente).
     */
    @Column(name = "url_checkout", columnDefinition = "TEXT")
    private String urlCheckout;

    /**
     * URL do QR Code PIX (se aplicável).
     */
    @Column(name = "url_qr_code", columnDefinition = "TEXT")
    private String urlQrCode;

    /**
     * Código copia-cola do PIX.
     */
    @Column(name = "codigo_pix", columnDefinition = "TEXT")
    private String codigoPix;

    /**
     * Data de expiração do checkout/pagamento.
     */
    @Column(name = "data_expiracao")
    private LocalDateTime dataExpiracao;

    /**
     * Data em que o pagamento foi aprovado.
     */
    @Column(name = "data_aprovacao")
    private LocalDateTime dataAprovacao;

    /**
     * Dados brutos da resposta do gateway (JSON).
     */
    @Column(name = "resposta_gateway", columnDefinition = "TEXT")
    private String respostaGateway;

    /**
     * Dados brutos do webhook (JSON).
     */
    @Column(name = "dados_webhook", columnDefinition = "TEXT")
    private String dadosWebhook;

    /**
     * Mensagem de erro (se houver).
     */
    @Column(name = "erro_mensagem", columnDefinition = "TEXT")
    private String erroMensagem;

    /**
     * Código de erro do gateway.
     */
    @Column(name = "erro_codigo", length = 50)
    private String erroCodigo;

    /**
     * Número de tentativas de processamento.
     */
    @Column(name = "tentativas")
    @Builder.Default
    private Integer tentativas = 0;

    /**
     * E-mail do pagador.
     */
    @Column(name = "email_pagador", length = 255)
    private String emailPagador;

    /**
     * Nome do pagador.
     */
    @Column(name = "nome_pagador", length = 255)
    private String nomePagador;

    /**
     * CPF/CNPJ do pagador.
     */
    @Column(name = "documento_pagador", length = 20)
    private String documentoPagador;

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
     * Verifica se o pagamento foi aprovado.
     */
    public boolean isAprovado() {
        return status != null && status.isAprovado();
    }

    /**
     * Verifica se o pagamento está em estado final.
     */
    public boolean isFinalizado() {
        return status != null && status.isFinal();
    }

    /**
     * Verifica se o checkout expirou.
     */
    public boolean isExpirado() {
        return dataExpiracao != null && LocalDateTime.now().isAfter(dataExpiracao);
    }

    /**
     * Incrementa o contador de tentativas.
     */
    public void incrementarTentativas() {
        this.tentativas = (this.tentativas == null ? 0 : this.tentativas) + 1;
    }
}
