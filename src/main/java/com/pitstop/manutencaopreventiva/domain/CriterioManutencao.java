package com.pitstop.manutencaopreventiva.domain;

/**
 * Critério para determinar quando uma manutenção preventiva deve ser realizada.
 */
public enum CriterioManutencao {
    /** Baseado apenas em intervalo de tempo (dias) */
    TEMPO,
    /** Baseado apenas em quilometragem */
    KM,
    /** O que ocorrer primeiro: tempo ou km */
    AMBOS
}
