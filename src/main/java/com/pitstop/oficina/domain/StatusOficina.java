package com.pitstop.oficina.domain;

/**
 * Status of a workshop in the PitStop system.
 *
 * <p>Represents the current operational state and subscription status.</p>
 *
 * @since 1.0.0
 */
public enum StatusOficina {

    /**
     * Trial period - Free 30-day trial for new workshops.
     */
    TRIAL("Trial", "Período de teste gratuito (30 dias)"),

    /**
     * Active workshop - In normal operation with valid subscription.
     */
    ATIVA("Ativa", "Em operação normal"),

    /**
     * Inactive workshop - Temporarily disabled by user.
     */
    INATIVA("Inativa", "Temporariamente desativada"),

    /**
     * Suspended workshop - Blocked due to payment issues or policy violation.
     */
    SUSPENSA("Suspensa", "Bloqueada por inadimplência ou violação de políticas"),

    /**
     * Canceled workshop - Account permanently closed.
     */
    CANCELADA("Cancelada", "Conta encerrada");

    private final String nome;
    private final String descricao;

    StatusOficina(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Checks if workshop is operational (can use the system).
     *
     * @return true if status is TRIAL or ATIVA
     */
    public boolean isOperacional() {
        return this == TRIAL || this == ATIVA;
    }

    /**
     * Checks if workshop is in trial period.
     *
     * @return true if status is TRIAL
     */
    public boolean isTrial() {
        return this == TRIAL;
    }

    /**
     * Checks if workshop can be reactivated.
     *
     * @return true if status is INATIVA or SUSPENSA
     */
    public boolean podeReativar() {
        return this == INATIVA || this == SUSPENSA;
    }
}
