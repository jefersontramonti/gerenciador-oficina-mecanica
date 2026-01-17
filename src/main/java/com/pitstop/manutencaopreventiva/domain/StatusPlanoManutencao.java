package com.pitstop.manutencaopreventiva.domain;

/**
 * Status do plano de manutenção preventiva.
 */
public enum StatusPlanoManutencao {
    /** Plano ativo e monitorando */
    ATIVO,
    /** Plano pausado temporariamente */
    PAUSADO,
    /** Plano concluído (veículo vendido, etc) */
    CONCLUIDO,
    /** Plano vencido (passou da data/km sem execução) */
    VENCIDO
}
