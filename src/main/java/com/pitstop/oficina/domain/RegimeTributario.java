package com.pitstop.oficina.domain;

/**
 * Tax regime for Brazilian companies.
 *
 * <p>Defines how the workshop is taxed by the government.</p>
 *
 * @since 1.0.0
 */
public enum RegimeTributario {

    /**
     * Microempreendedor Individual - Simplified tax regime for very small businesses.
     * Annual revenue up to R$ 81,000.
     */
    MEI("Microempreendedor Individual (MEI)", new java.math.BigDecimal("81000.00")),

    /**
     * Simples Nacional - Simplified tax regime for small and medium businesses.
     * Annual revenue up to R$ 4,800,000.
     */
    SIMPLES_NACIONAL("Simples Nacional", new java.math.BigDecimal("4800000.00")),

    /**
     * Lucro Presumido - Presumed profit tax regime.
     * For companies with annual revenue up to R$ 78,000,000.
     */
    LUCRO_PRESUMIDO("Lucro Presumido", new java.math.BigDecimal("78000000.00")),

    /**
     * Lucro Real - Actual profit tax regime.
     * Mandatory for companies with annual revenue above R$ 78,000,000 or specific activities.
     */
    LUCRO_REAL("Lucro Real", null); // No limit - mandatory for large companies

    private final String descricao;
    private final java.math.BigDecimal limiteReceita;

    RegimeTributario(String descricao, java.math.BigDecimal limiteReceita) {
        this.descricao = descricao;
        this.limiteReceita = limiteReceita;
    }

    public String getDescricao() {
        return descricao;
    }

    public java.math.BigDecimal getLimiteReceita() {
        return limiteReceita;
    }

    /**
     * Checks if this regime has a revenue limit.
     *
     * @return true if there's a maximum annual revenue limit
     */
    public boolean temLimite() {
        return limiteReceita != null;
    }
}
