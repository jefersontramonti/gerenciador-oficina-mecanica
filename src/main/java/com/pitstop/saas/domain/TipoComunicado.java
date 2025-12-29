package com.pitstop.saas.domain;

public enum TipoComunicado {
    NOVIDADE("Novidade"),
    MANUTENCAO("Manutenção"),
    FINANCEIRO("Financeiro"),
    ATUALIZACAO("Atualização"),
    ALERTA("Alerta"),
    PROMOCAO("Promoção"),
    OUTRO("Outro");

    private final String descricao;

    TipoComunicado(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
