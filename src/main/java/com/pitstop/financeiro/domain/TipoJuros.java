package com.pitstop.financeiro.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipos de cálculo de juros para parcelamento.
 */
@Getter
@RequiredArgsConstructor
public enum TipoJuros {

    SEM_JUROS("Sem juros", "Parcelamento sem acréscimo"),
    JUROS_SIMPLES("Juros simples", "Juros calculados sobre o valor original"),
    JUROS_COMPOSTO("Juros composto", "Juros calculados sobre juros anteriores");

    private final String nome;
    private final String descricao;
}
