package com.pitstop.webhook.domain;

/**
 * Status de uma tentativa de envio de webhook.
 *
 * @author PitStop Team
 */
public enum StatusWebhookLog {

    /** Aguardando envio */
    PENDENTE("Pendente"),

    /** Enviado com sucesso (HTTP 2xx) */
    SUCESSO("Sucesso"),

    /** Falha no envio (HTTP 4xx/5xx ou erro de conexão) */
    FALHA("Falha"),

    /** Aguardando retry após falha */
    AGUARDANDO_RETRY("Aguardando Retry"),

    /** Todas as tentativas esgotadas */
    ESGOTADO("Tentativas Esgotadas");

    private final String descricao;

    StatusWebhookLog(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
