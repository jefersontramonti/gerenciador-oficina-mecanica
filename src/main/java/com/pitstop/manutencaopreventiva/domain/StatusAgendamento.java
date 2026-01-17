package com.pitstop.manutencaopreventiva.domain;

/**
 * Status do agendamento de manutenção.
 */
public enum StatusAgendamento {
    /** Agendamento criado, aguardando confirmação */
    AGENDADO,
    /** Confirmado pelo cliente */
    CONFIRMADO,
    /** Remarcado para outra data */
    REMARCADO,
    /** Cancelado */
    CANCELADO,
    /** Manutenção realizada */
    REALIZADO
}
