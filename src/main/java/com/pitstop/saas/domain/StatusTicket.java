package com.pitstop.saas.domain;

public enum StatusTicket {
    ABERTO("Aberto"),
    EM_ANDAMENTO("Em Andamento"),
    AGUARDANDO_CLIENTE("Aguardando Cliente"),
    AGUARDANDO_INTERNO("Aguardando Interno"),
    RESOLVIDO("Resolvido"),
    FECHADO("Fechado");

    private final String descricao;

    StatusTicket(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isAberto() {
        return this == ABERTO || this == EM_ANDAMENTO ||
               this == AGUARDANDO_CLIENTE || this == AGUARDANDO_INTERNO;
    }

    public boolean isFinalizado() {
        return this == RESOLVIDO || this == FECHADO;
    }
}
