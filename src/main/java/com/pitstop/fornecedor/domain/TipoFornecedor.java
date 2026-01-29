package com.pitstop.fornecedor.domain;

/**
 * Enum que representa os tipos de fornecedor.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public enum TipoFornecedor {

    FABRICANTE("Fabricante"),
    DISTRIBUIDOR("Distribuidor"),
    ATACADISTA("Atacadista"),
    VAREJISTA("Varejista"),
    IMPORTADOR("Importador"),
    OUTRO("Outro");

    private final String descricao;

    TipoFornecedor(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
