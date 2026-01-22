package com.pitstop.webhook.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Log de tentativas de envio de webhook.
 * Registra cada tentativa, incluindo payload, resposta e status.
 *
 * @author PitStop Team
 */
@Entity
@Table(name = "webhook_logs", indexes = {
    @Index(name = "idx_webhook_log_config", columnList = "webhook_config_id"),
    @Index(name = "idx_webhook_log_oficina", columnList = "oficina_id"),
    @Index(name = "idx_webhook_log_evento", columnList = "evento"),
    @Index(name = "idx_webhook_log_status", columnList = "status"),
    @Index(name = "idx_webhook_log_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Referência à configuração do webhook.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_config_id", nullable = false)
    private WebhookConfig webhookConfig;

    /**
     * ID da oficina (desnormalizado para queries rápidas).
     */
    @Column(name = "oficina_id", nullable = false)
    private UUID oficinaId;

    /**
     * Tipo de evento que disparou o webhook.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoEventoWebhook evento;

    /**
     * ID da entidade relacionada ao evento (ex: ID da OS, do Cliente, etc).
     */
    @Column(name = "entidade_id")
    private UUID entidadeId;

    /**
     * Tipo da entidade (ex: "OrdemServico", "Cliente").
     */
    @Column(name = "entidade_tipo", length = 50)
    private String entidadeTipo;

    /**
     * URL para a qual o webhook foi enviado.
     */
    @Column(nullable = false, length = 500)
    private String url;

    /**
     * Payload JSON enviado.
     */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /**
     * Status HTTP da resposta (ex: 200, 404, 500).
     * Null se houve erro de conexão.
     */
    @Column(name = "http_status")
    private Integer httpStatus;

    /**
     * Corpo da resposta (truncado em 2000 chars).
     */
    @Column(name = "response_body", length = 2000)
    private String responseBody;

    /**
     * Mensagem de erro em caso de falha de conexão.
     */
    @Column(name = "erro_mensagem", length = 1000)
    private String erroMensagem;

    /**
     * Tempo de resposta em milissegundos.
     */
    @Column(name = "tempo_resposta_ms")
    private Long tempoRespostaMs;

    /**
     * Número da tentativa (1, 2, 3...).
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer tentativa = 1;

    /**
     * Status do envio.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusWebhookLog status = StatusWebhookLog.PENDENTE;

    /**
     * Próxima tentativa agendada (para retries).
     */
    @Column(name = "proxima_tentativa")
    private LocalDateTime proximaTentativa;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== Métodos de negócio =====

    /**
     * Verifica se o envio foi bem-sucedido (HTTP 2xx).
     */
    public boolean foiSucesso() {
        return httpStatus != null && httpStatus >= 200 && httpStatus < 300;
    }

    /**
     * Marca como sucesso.
     */
    public void marcarSucesso(int httpStatus, String responseBody, long tempoMs) {
        this.status = StatusWebhookLog.SUCESSO;
        this.httpStatus = httpStatus;
        this.responseBody = truncate(responseBody, 2000);
        this.tempoRespostaMs = tempoMs;
    }

    /**
     * Marca como falha.
     */
    public void marcarFalha(Integer httpStatus, String responseBody, String erro, long tempoMs) {
        this.status = StatusWebhookLog.FALHA;
        this.httpStatus = httpStatus;
        this.responseBody = truncate(responseBody, 2000);
        this.erroMensagem = truncate(erro, 1000);
        this.tempoRespostaMs = tempoMs;
    }

    /**
     * Agenda retry com backoff exponencial.
     * Delay: 1min, 5min, 15min, 30min...
     */
    public void agendarRetry(int maxTentativas) {
        if (this.tentativa >= maxTentativas) {
            this.status = StatusWebhookLog.ESGOTADO;
            return;
        }

        this.status = StatusWebhookLog.AGUARDANDO_RETRY;

        // Backoff exponencial: 1, 5, 15, 30 minutos
        int[] delaysMinutos = {1, 5, 15, 30, 60};
        int index = Math.min(this.tentativa - 1, delaysMinutos.length - 1);

        this.proximaTentativa = LocalDateTime.now().plusMinutes(delaysMinutos[index]);
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) : str;
    }
}
