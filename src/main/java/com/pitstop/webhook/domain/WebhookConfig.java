package com.pitstop.webhook.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Configuração de webhook para uma oficina.
 * Define URL de destino, eventos que disparam e autenticação.
 *
 * @author PitStop Team
 */
@Entity
@Table(name = "webhook_configs", indexes = {
    @Index(name = "idx_webhook_config_oficina", columnList = "oficina_id"),
    @Index(name = "idx_webhook_config_ativo", columnList = "ativo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * ID da oficina dona deste webhook.
     */
    @Column(name = "oficina_id", nullable = false)
    private UUID oficinaId;

    /**
     * Nome identificador do webhook (ex: "Integração ERP", "Sistema Contábil").
     */
    @Column(nullable = false, length = 100)
    private String nome;

    /**
     * Descrição opcional do propósito deste webhook.
     */
    @Column(length = 500)
    private String descricao;

    /**
     * URL de destino para envio dos webhooks.
     * Deve ser HTTPS em produção.
     */
    @Column(nullable = false, length = 500)
    private String url;

    /**
     * Secret para assinatura HMAC-SHA256 do payload.
     * Permite que o receptor valide a autenticidade.
     */
    @Column(length = 200)
    private String secret;

    /**
     * Headers customizados a serem enviados (JSON).
     * Ex: {"Authorization": "Bearer xxx", "X-Custom": "value"}
     */
    @Column(name = "headers_json", columnDefinition = "TEXT")
    private String headersJson;

    /**
     * Eventos que disparam este webhook.
     * Armazenado como lista separada por vírgula para simplificar.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "webhook_config_eventos",
        joinColumns = @JoinColumn(name = "webhook_config_id")
    )
    @Column(name = "evento")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<TipoEventoWebhook> eventos = new HashSet<>();

    /**
     * Máximo de tentativas em caso de falha.
     * Após atingir, o webhook é marcado como ESGOTADO.
     */
    @Column(name = "max_tentativas", nullable = false)
    @Builder.Default
    private Integer maxTentativas = 3;

    /**
     * Timeout em segundos para a requisição HTTP.
     */
    @Column(name = "timeout_segundos", nullable = false)
    @Builder.Default
    private Integer timeoutSegundos = 30;

    /**
     * Se true, o webhook está ativo e será disparado.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    /**
     * Contador de falhas consecutivas.
     * Se atingir um limite (ex: 10), o webhook é desativado automaticamente.
     */
    @Column(name = "falhas_consecutivas", nullable = false)
    @Builder.Default
    private Integer falhasConsecutivas = 0;

    /**
     * Timestamp da última execução bem-sucedida.
     */
    @Column(name = "ultima_execucao_sucesso")
    private LocalDateTime ultimaExecucaoSucesso;

    /**
     * Timestamp da última falha.
     */
    @Column(name = "ultima_falha")
    private LocalDateTime ultimaFalha;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== Métodos de negócio =====

    /**
     * Verifica se este webhook deve ser disparado para um evento.
     */
    @JsonIgnore
    public boolean deveDispararPara(TipoEventoWebhook evento) {
        return ativo && eventos != null && eventos.contains(evento);
    }

    /**
     * Registra uma execução bem-sucedida.
     */
    public void registrarSucesso() {
        this.falhasConsecutivas = 0;
        this.ultimaExecucaoSucesso = LocalDateTime.now();
    }

    /**
     * Registra uma falha e retorna se deve desativar o webhook.
     * Desativa automaticamente após 10 falhas consecutivas.
     */
    public boolean registrarFalha() {
        this.falhasConsecutivas++;
        this.ultimaFalha = LocalDateTime.now();

        if (this.falhasConsecutivas >= 10) {
            this.ativo = false;
            return true; // Foi desativado
        }
        return false;
    }

    /**
     * Reativa o webhook (útil após correção de URL, etc).
     */
    public void reativar() {
        this.ativo = true;
        this.falhasConsecutivas = 0;
    }
}
