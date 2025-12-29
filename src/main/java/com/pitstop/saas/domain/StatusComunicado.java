package com.pitstop.saas.domain;

public enum StatusComunicado {
    RASCUNHO("Rascunho"),
    AGENDADO("Agendado"),
    ENVIADO("Enviado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusComunicado(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
