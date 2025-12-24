package com.pitstop.notificacao.integration.evolution;

/**
 * Status de uma instancia na Evolution API.
 *
 * @author PitStop Team
 */
public record EvolutionInstanceStatus(
    boolean disponivel,
    String estado,
    boolean conectado,
    String erro
) {
    /**
     * Verifica se a instancia esta pronta para enviar mensagens.
     */
    public boolean prontaParaEnviar() {
        return disponivel && conectado;
    }
}
