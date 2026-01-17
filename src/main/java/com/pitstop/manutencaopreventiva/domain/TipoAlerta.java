package com.pitstop.manutencaopreventiva.domain;

/**
 * Tipo de alerta de manutenção.
 */
public enum TipoAlerta {
    /** Alerta de manutenção próxima de vencer */
    PROXIMIDADE,
    /** Alerta de manutenção vencida */
    VENCIDO,
    /** Lembrete de agendamento */
    LEMBRETE_AGENDAMENTO,
    /** Confirmação de agendamento */
    CONFIRMACAO
}
