package com.pitstop.financeiro.domain;

/**
 * Tipo de transação bancária.
 */
public enum TipoTransacaoBancaria {
    CREDITO("Crédito", "Entrada de valores"),
    DEBITO("Débito", "Saída de valores");

    private final String nome;
    private final String descricao;

    TipoTransacaoBancaria(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }
}
