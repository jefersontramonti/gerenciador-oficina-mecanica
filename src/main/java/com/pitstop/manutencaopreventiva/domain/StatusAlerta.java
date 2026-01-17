package com.pitstop.manutencaopreventiva.domain;

/**
 * Status do alerta de manutenção.
 */
public enum StatusAlerta {
    /** Aguardando envio */
    PENDENTE,
    /** Enviado com sucesso */
    ENVIADO,
    /** Falhou no envio (pode ter retry) */
    FALHOU,
    /** Cancelado manualmente ou por regra */
    CANCELADO
}
