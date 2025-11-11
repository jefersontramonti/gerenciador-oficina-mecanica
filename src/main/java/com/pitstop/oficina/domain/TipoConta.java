package com.pitstop.oficina.domain;

/**
 * Type of bank account for receiving payments.
 *
 * @since 1.0.0
 */
public enum TipoConta {

    /**
     * Checking account (Conta Corrente).
     */
    CORRENTE("Conta Corrente"),

    /**
     * Savings account (Conta Poupança).
     */
    POUPANCA("Conta Poupança");

    private final String descricao;

    TipoConta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
