package com.pitstop.ia.dto;

/**
 * Resultado da classificação de complexidade do problema.
 */
public record ClassificacaoProblema(
        Complexidade complexidade,
        String categoria,
        String justificativa
) {
    /**
     * Nível de complexidade do problema.
     */
    public enum Complexidade {
        SIMPLES,   // Pode ser resolvido pelo modelo Haiku
        COMPLEXO   // Requer modelo Sonnet para análise detalhada
    }
}
