package com.pitstop.saas.domain;

public enum TipoAutorMensagem {
    CLIENTE("Cliente"),
    SUPORTE("Suporte"),
    SISTEMA("Sistema");

    private final String descricao;

    TipoAutorMensagem(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
