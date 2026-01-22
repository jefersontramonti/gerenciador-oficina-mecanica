package com.pitstop.financeiro.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipo de lançamento financeiro.
 */
@Getter
@RequiredArgsConstructor
public enum TipoLancamento {

    RECEITA("Receita", "Entrada de valores"),
    DESPESA("Despesa", "Saída de valores");

    private final String nome;
    private final String descricao;
}
