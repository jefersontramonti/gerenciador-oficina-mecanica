package com.pitstop.notificacao.integration.evolution;

/**
 * Opcao de lista interativa para mensagem WhatsApp.
 *
 * @author PitStop Team
 */
public record EvolutionListOption(
    String id,
    String titulo,
    String descricao
) {
    public static EvolutionListOption of(String id, String titulo, String descricao) {
        return new EvolutionListOption(id, titulo, descricao);
    }

    public static EvolutionListOption of(String id, String titulo) {
        return new EvolutionListOption(id, titulo, "");
    }
}
