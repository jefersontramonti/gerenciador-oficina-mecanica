package com.pitstop.saas.domain;

/**
 * Status of a payment agreement.
 */
public enum StatusAcordo {
    ATIVO("Ativo", "green"),
    QUITADO("Quitado", "blue"),
    QUEBRADO("Quebrado", "red"),
    CANCELADO("Cancelado", "gray");

    private final String label;
    private final String cor;

    StatusAcordo(String label, String cor) {
        this.label = label;
        this.cor = cor;
    }

    public String getLabel() {
        return label;
    }

    public String getCor() {
        return cor;
    }

    /**
     * Check if agreement can have payments registered.
     */
    public boolean isAtivo() {
        return this == ATIVO;
    }

    /**
     * Check if agreement can be cancelled.
     */
    public boolean isCancelavel() {
        return this == ATIVO;
    }
}
