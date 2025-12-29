package com.pitstop.saas.domain;

public enum TipoTicket {
    TECNICO("Técnico"),
    FINANCEIRO("Financeiro"),
    COMERCIAL("Comercial"),
    SUGESTAO("Sugestão"),
    OUTRO("Outro");

    private final String descricao;

    TipoTicket(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
