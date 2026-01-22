package com.pitstop.financeiro.domain;

/**
 * Status de uma assinatura de cliente.
 */
public enum StatusAssinatura {

    ATIVA("Ativa"),
    PAUSADA("Pausada"),
    CANCELADA("Cancelada"),
    INADIMPLENTE("Inadimplente");

    private final String descricao;

    StatusAssinatura(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se a assinatura está em estado ativo (pode ser cobrada).
     */
    public boolean isAtiva() {
        return this == ATIVA || this == INADIMPLENTE;
    }

    /**
     * Verifica se a assinatura está encerrada.
     */
    public boolean isEncerrada() {
        return this == CANCELADA;
    }
}
