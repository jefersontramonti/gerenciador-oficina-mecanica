package com.pitstop.notificacao.integration.evolution;

/**
 * Resultado do envio via Evolution API.
 *
 * @author PitStop Team
 */
public record EvolutionSendResult(
    boolean sucesso,
    String messageId,
    String erroCodigo,
    String erroMensagem,
    String respostaJson
) {
    /**
     * Cria resultado de sucesso.
     */
    public static EvolutionSendResult sucesso(String messageId, String respostaJson) {
        return new EvolutionSendResult(true, messageId, null, null, respostaJson);
    }

    /**
     * Cria resultado de falha.
     */
    public static EvolutionSendResult falha(String codigo, String mensagem, String respostaJson) {
        return new EvolutionSendResult(false, null, codigo, mensagem, respostaJson);
    }
}
