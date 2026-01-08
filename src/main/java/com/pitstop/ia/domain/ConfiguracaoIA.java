package com.pitstop.ia.domain;

import com.pitstop.oficina.domain.Oficina;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que armazena as configurações de IA por oficina.
 * Cada oficina pode ter sua própria API key da Anthropic e preferências.
 */
@Entity
@Table(name = "configuracao_ia")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ConfiguracaoIA {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false, unique = true)
    private Oficina oficina;

    /**
     * API Key criptografada da Anthropic.
     */
    @Column(name = "api_key_encrypted", length = 500)
    private String apiKeyEncrypted;

    /**
     * Provedor de IA (ANTHROPIC por padrão).
     */
    @Column(name = "provedor", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProvedorIA provedor = ProvedorIA.ANTHROPIC;

    /**
     * Modelo para diagnósticos simples (mais barato).
     */
    @Column(name = "modelo_padrao", length = 100)
    @Builder.Default
    private String modeloPadrao = "claude-haiku-4-5-20251001";

    /**
     * Modelo para diagnósticos complexos (mais avançado).
     */
    @Column(name = "modelo_avancado", length = 100)
    @Builder.Default
    private String modeloAvancado = "claude-sonnet-4-20250514";

    // ===== CONFIGURAÇÕES DE COMPORTAMENTO =====

    /**
     * Se a IA está habilitada para esta oficina.
     */
    @Column(name = "ia_habilitada", nullable = false)
    @Builder.Default
    private Boolean iaHabilitada = false;

    /**
     * Se deve usar cache de diagnósticos similares.
     */
    @Column(name = "usar_cache", nullable = false)
    @Builder.Default
    private Boolean usarCache = true;

    /**
     * Se deve usar pré-validação com templates.
     */
    @Column(name = "usar_pre_validacao", nullable = false)
    @Builder.Default
    private Boolean usarPreValidacao = true;

    /**
     * Se deve usar roteamento inteligente (Haiku classifica, Sonnet analisa complexos).
     */
    @Column(name = "usar_roteamento_inteligente", nullable = false)
    @Builder.Default
    private Boolean usarRoteamentoInteligente = true;

    // ===== LIMITES =====

    /**
     * Máximo de tokens na resposta.
     */
    @Column(name = "max_tokens_resposta")
    @Min(100)
    @Max(4000)
    @Builder.Default
    private Integer maxTokensResposta = 1000;

    /**
     * Máximo de requisições por dia.
     */
    @Column(name = "max_requisicoes_dia")
    @Min(1)
    @Max(1000)
    @Builder.Default
    private Integer maxRequisicoesDia = 100;

    /**
     * Contador de requisições do dia atual.
     */
    @Column(name = "requisicoes_hoje")
    @Builder.Default
    private Integer requisicoesHoje = 0;

    /**
     * Data do último reset do contador.
     */
    @Column(name = "data_reset_contador")
    @Builder.Default
    private LocalDate dataResetContador = LocalDate.now();

    // ===== ESTATÍSTICAS =====

    @Column(name = "total_requisicoes")
    @Builder.Default
    private Long totalRequisicoes = 0L;

    @Column(name = "total_tokens_consumidos")
    @Builder.Default
    private Long totalTokensConsumidos = 0L;

    @Column(name = "total_cache_hits")
    @Builder.Default
    private Long totalCacheHits = 0L;

    @Column(name = "total_template_hits")
    @Builder.Default
    private Long totalTemplateHits = 0L;

    @Column(name = "custo_estimado_total", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal custoEstimadoTotal = BigDecimal.ZERO;

    // ===== AUDITORIA =====

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== MÉTODOS DE NEGÓCIO =====

    /**
     * Verifica se pode fazer mais requisições hoje.
     */
    public boolean podeRequisitar() {
        resetContadorSeNecessario();
        return requisicoesHoje < maxRequisicoesDia;
    }

    /**
     * Incrementa o contador de requisições.
     */
    public void incrementarRequisicoes(int tokens, BigDecimal custo) {
        resetContadorSeNecessario();
        this.requisicoesHoje++;
        this.totalRequisicoes++;
        this.totalTokensConsumidos += tokens;
        this.custoEstimadoTotal = this.custoEstimadoTotal.add(custo);
    }

    /**
     * Incrementa contador de cache hits.
     */
    public void incrementarCacheHit() {
        this.totalCacheHits++;
    }

    /**
     * Incrementa contador de template hits.
     */
    public void incrementarTemplateHit() {
        this.totalTemplateHits++;
    }

    /**
     * Reseta o contador diário se necessário.
     */
    private void resetContadorSeNecessario() {
        if (dataResetContador == null || !dataResetContador.equals(LocalDate.now())) {
            this.requisicoesHoje = 0;
            this.dataResetContador = LocalDate.now();
        }
    }

    /**
     * Verifica se a configuração está pronta para uso.
     */
    public boolean isConfigurada() {
        return iaHabilitada && apiKeyEncrypted != null && !apiKeyEncrypted.isBlank();
    }
}
