package com.pitstop.financeiro.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Status de uma despesa.
 */
@Getter
@RequiredArgsConstructor
public enum StatusDespesa {

    PENDENTE("Pendente", "#F59E0B"),
    PAGA("Paga", "#22C55E"),
    VENCIDA("Vencida", "#EF4444"),
    CANCELADA("Cancelada", "#6B7280");

    private final String descricao;
    private final String cor;
}
