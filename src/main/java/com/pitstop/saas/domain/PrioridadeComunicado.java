package com.pitstop.saas.domain;

public enum PrioridadeComunicado {
    BAIXA("Baixa"),
    NORMAL("Normal"),
    ALTA("Alta"),
    URGENTE("Urgente");

    private final String descricao;

    PrioridadeComunicado(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
