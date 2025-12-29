package com.pitstop.saas.domain;

/**
 * Status of an agreement installment.
 */
public enum StatusParcela {
    PENDENTE("Pendente", "yellow"),
    PAGO("Pago", "green"),
    VENCIDO("Vencido", "red"),
    CANCELADO("Cancelado", "gray");

    private final String label;
    private final String cor;

    StatusParcela(String label, String cor) {
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
     * Check if installment can be paid.
     */
    public boolean isPagavel() {
        return this == PENDENTE || this == VENCIDO;
    }
}
