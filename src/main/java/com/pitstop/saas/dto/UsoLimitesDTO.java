package com.pitstop.saas.dto;

import lombok.Builder;

/**
 * DTO with current usage vs plan limits information.
 *
 * @author PitStop Team
 */
@Builder
public record UsoLimitesDTO(
    String planoNome,
    String planoCodigo,

    // Usuários
    int limiteUsuarios,
    long usuariosAtivos,
    double percentualUsuarios,
    boolean usuariosIlimitados,

    // OS por mês
    int limiteOsMes,
    long osNoMes,
    double percentualOsMes,
    boolean osIlimitadas,

    // Mês de referência
    int mesReferencia,
    int anoReferencia
) {

    /**
     * Returns true if user limit is at 80% or more.
     */
    public boolean isUsuariosEmAlerta() {
        return !usuariosIlimitados && percentualUsuarios >= 80.0;
    }

    /**
     * Returns true if OS limit is at 80% or more.
     */
    public boolean isOsEmAlerta() {
        return !osIlimitadas && percentualOsMes >= 80.0;
    }

    /**
     * Returns true if user limit is reached.
     */
    public boolean isLimiteUsuariosAtingido() {
        return !usuariosIlimitados && usuariosAtivos >= limiteUsuarios;
    }

    /**
     * Returns true if OS limit is reached.
     */
    public boolean isLimiteOsAtingido() {
        return !osIlimitadas && osNoMes >= limiteOsMes;
    }
}
