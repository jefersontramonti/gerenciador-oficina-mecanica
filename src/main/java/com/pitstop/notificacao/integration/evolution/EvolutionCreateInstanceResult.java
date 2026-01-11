package com.pitstop.notificacao.integration.evolution;

/**
 * Resultado da criacao de instancia na Evolution API.
 *
 * @author PitStop Team
 */
public record EvolutionCreateInstanceResult(
    boolean sucesso,
    String instanceName,
    String instanceToken,
    String qrCode,
    String erroMensagem
) {
    /**
     * Cria resultado de sucesso.
     */
    public static EvolutionCreateInstanceResult sucesso(String instanceName, String token, String qrCode) {
        return new EvolutionCreateInstanceResult(true, instanceName, token, qrCode, null);
    }

    /**
     * Cria resultado de falha.
     */
    public static EvolutionCreateInstanceResult falha(String mensagem) {
        return new EvolutionCreateInstanceResult(false, null, null, null, mensagem);
    }
}
