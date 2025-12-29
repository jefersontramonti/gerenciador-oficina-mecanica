package com.pitstop.saas.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Status of an invoice in the billing lifecycle.
 */
@Getter
@RequiredArgsConstructor
public enum StatusFatura {

    PENDENTE("Pendente", "Aguardando pagamento", "yellow"),
    PAGO("Pago", "Pagamento confirmado", "green"),
    VENCIDO("Vencido", "Pagamento em atraso", "red"),
    CANCELADO("Cancelado", "Fatura cancelada", "gray");

    private final String label;
    private final String descricao;
    private final String cor;

    /**
     * Check if the invoice can be paid.
     */
    public boolean isPagavel() {
        return this == PENDENTE || this == VENCIDO;
    }

    /**
     * Check if the invoice can be cancelled.
     */
    public boolean isCancelavel() {
        return this == PENDENTE;
    }

    /**
     * Check if this is a final status.
     */
    public boolean isFinal() {
        return this == PAGO || this == CANCELADO;
    }
}
