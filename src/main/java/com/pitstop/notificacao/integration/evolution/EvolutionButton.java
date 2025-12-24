package com.pitstop.notificacao.integration.evolution;

/**
 * Botao interativo para mensagem WhatsApp.
 *
 * @author PitStop Team
 */
public record EvolutionButton(
    String id,
    String texto
) {
    public static EvolutionButton of(String id, String texto) {
        return new EvolutionButton(id, texto);
    }
}
