package com.pitstop.notificacao.domain;

import lombok.Getter;

/**
 * Status possíveis de uma notificação no histórico.
 */
@Getter
public enum StatusNotificacao {

    /**
     * Notificação aguardando envio.
     */
    PENDENTE("Aguardando envio"),

    /**
     * Notificação enviada com sucesso para o gateway.
     */
    ENVIADO("Enviado com sucesso"),

    /**
     * Notificação confirmada como entregue ao destinatário.
     */
    ENTREGUE("Entregue ao destinatário"),

    /**
     * Notificação lida pelo destinatário (WhatsApp read receipt).
     */
    LIDO("Lido pelo destinatário"),

    /**
     * Falha no envio da notificação.
     */
    FALHA("Falha no envio"),

    /**
     * Notificação cancelada antes do envio.
     */
    CANCELADO("Cancelado antes do envio"),

    /**
     * Notificação agendada para envio posterior.
     */
    AGENDADO("Agendado para envio");

    private final String descricao;

    StatusNotificacao(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Verifica se o status permite reenvio.
     */
    public boolean permiteReenvio() {
        return this == PENDENTE || this == FALHA || this == AGENDADO;
    }

    /**
     * Verifica se o status é final (não pode mais mudar).
     */
    public boolean isFinal() {
        return this == ENTREGUE || this == LIDO || this == CANCELADO;
    }
}
