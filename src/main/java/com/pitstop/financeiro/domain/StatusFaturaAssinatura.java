package com.pitstop.financeiro.domain;

/**
 * Status de uma fatura de assinatura.
 */
public enum StatusFaturaAssinatura {

    PENDENTE("Pendente"),
    PAGA("Paga"),
    VENCIDA("Vencida"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusFaturaAssinatura(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se a fatura ainda pode ser paga.
     */
    public boolean podeSerPaga() {
        return this == PENDENTE || this == VENCIDA;
    }

    /**
     * Verifica se é um estado final.
     */
    public boolean isEstadoFinal() {
        return this == PAGA || this == CANCELADA;
    }
}
