package com.pitstop.notificacao.integration.evolution;

import java.util.List;

/**
 * Secao de lista interativa para mensagem WhatsApp.
 *
 * @author PitStop Team
 */
public record EvolutionListSection(
    String titulo,
    List<EvolutionListOption> opcoes
) {
    public static EvolutionListSection of(String titulo, List<EvolutionListOption> opcoes) {
        return new EvolutionListSection(titulo, opcoes);
    }
}
