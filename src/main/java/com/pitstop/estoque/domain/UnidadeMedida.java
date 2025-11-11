package com.pitstop.estoque.domain;

/**
 * Enum que representa as unidades de medida disponíveis para peças de estoque.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public enum UnidadeMedida {

    /**
     * Unidade - para peças individuais (ex: filtro, vela, pneu)
     */
    UNIDADE("Unidade", "UN"),

    /**
     * Litro - para fluidos (ex: óleo, aditivo, líquido de freio)
     */
    LITRO("Litro", "L"),

    /**
     * Metro - para materiais lineares (ex: mangueira, cabo elétrico)
     */
    METRO("Metro", "M"),

    /**
     * Quilograma - para materiais a granel (ex: graxa, massa de polir)
     */
    QUILO("Quilograma", "KG");

    private final String descricao;
    private final String sigla;

    UnidadeMedida(String descricao, String sigla) {
        this.descricao = descricao;
        this.sigla = sigla;
    }

    /**
     * Retorna a descrição completa da unidade de medida.
     *
     * @return descrição da unidade (ex: "Quilograma")
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna a sigla da unidade de medida.
     *
     * @return sigla da unidade (ex: "KG")
     */
    public String getSigla() {
        return sigla;
    }

    /**
     * Retorna representação formatada da unidade.
     *
     * @return string no formato "Descrição (Sigla)"
     */
    @Override
    public String toString() {
        return String.format("%s (%s)", descricao, sigla);
    }
}
