package com.pitstop.notificacao.integration.telegram;

/**
 * Resultado do envio de mensagem via Telegram Bot API.
 *
 * @param sucesso Se o envio foi bem-sucedido
 * @param messageId ID da mensagem no Telegram
 * @param erroCodigo Codigo de erro (se falhou)
 * @param erroMensagem Mensagem de erro (se falhou)
 * @param respostaJson Resposta JSON completa da API
 *
 * @author PitStop Team
 */
public record TelegramSendResult(
    boolean sucesso,
    String messageId,
    String erroCodigo,
    String erroMensagem,
    String respostaJson
) {

    /**
     * Cria resultado de sucesso.
     */
    public static TelegramSendResult sucesso(String messageId, String respostaJson) {
        return new TelegramSendResult(true, messageId, null, null, respostaJson);
    }

    /**
     * Cria resultado de falha.
     */
    public static TelegramSendResult falha(String erroCodigo, String erroMensagem, String respostaJson) {
        return new TelegramSendResult(false, null, erroCodigo, erroMensagem, respostaJson);
    }
}
