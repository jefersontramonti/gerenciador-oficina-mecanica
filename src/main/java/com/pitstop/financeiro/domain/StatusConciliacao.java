package com.pitstop.financeiro.domain;

/**
 * Status de conciliação de uma transação bancária.
 */
public enum StatusConciliacao {
    NAO_CONCILIADA("Não Conciliada", "Transação ainda não foi conciliada"),
    CONCILIADA("Conciliada", "Transação foi conciliada com um pagamento"),
    IGNORADA("Ignorada", "Transação foi marcada para ser ignorada"),
    MANUAL("Manual", "Transação foi conciliada manualmente sem pagamento correspondente");

    private final String nome;
    private final String descricao;

    StatusConciliacao(String nome, String descricao) {
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
