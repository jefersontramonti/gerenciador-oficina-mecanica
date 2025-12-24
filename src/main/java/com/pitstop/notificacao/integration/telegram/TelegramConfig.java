package com.pitstop.notificacao.integration.telegram;

import com.pitstop.notificacao.domain.ConfiguracaoNotificacao;

/**
 * Configuracao para integracao com Telegram Bot API.
 *
 * @param botToken Token do bot (obtido via @BotFather)
 * @param chatId ID do chat ou grupo para envio
 *
 * @author PitStop Team
 */
public record TelegramConfig(
    String botToken,
    String chatId
) {

    /**
     * URL base da API do Telegram.
     */
    public static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";

    /**
     * Cria configuracao a partir da entidade ConfiguracaoNotificacao.
     *
     * @param config Configuracao da oficina
     * @return TelegramConfig ou null se nao configurado
     */
    public static TelegramConfig from(ConfiguracaoNotificacao config) {
        if (config == null || !config.temTelegramConfigurado()) {
            return null;
        }
        return new TelegramConfig(
            config.getTelegramBotToken(),
            config.getTelegramChatId()
        );
    }

    /**
     * Retorna a URL completa para um endpoint da API.
     *
     * @param endpoint Endpoint (ex: "sendMessage")
     * @return URL completa
     */
    public String getApiUrl(String endpoint) {
        return TELEGRAM_API_BASE + botToken + "/" + endpoint;
    }

    /**
     * Verifica se a configuracao esta valida.
     */
    public boolean isValid() {
        return botToken != null && !botToken.isBlank() &&
               chatId != null && !chatId.isBlank();
    }
}
