package com.pitstop.financeiro.domain;

/**
 * Status de um extrato bancário importado.
 */
public enum StatusExtrato {
    PENDENTE("Pendente", "Extrato importado, aguardando conciliação"),
    EM_ANDAMENTO("Em Andamento", "Conciliação em andamento"),
    CONCLUIDO("Concluído", "Todas as transações foram processadas");

    private final String nome;
    private final String descricao;

    StatusExtrato(String nome, String descricao) {
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
