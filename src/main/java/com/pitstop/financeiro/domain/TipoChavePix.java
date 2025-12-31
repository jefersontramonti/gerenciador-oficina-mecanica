package com.pitstop.financeiro.domain;

/**
 * Tipos de chave PIX suportados.
 */
public enum TipoChavePix {

    CPF("CPF"),
    CNPJ("CNPJ"),
    EMAIL("E-mail"),
    TELEFONE("Telefone"),
    ALEATORIA("Chave Aleatória");

    private final String descricao;

    TipoChavePix(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
