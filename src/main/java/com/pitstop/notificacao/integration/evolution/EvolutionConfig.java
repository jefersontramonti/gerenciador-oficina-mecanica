package com.pitstop.notificacao.integration.evolution;

import com.pitstop.notificacao.domain.ConfiguracaoNotificacao;

/**
 * Configuracao da Evolution API extraida de ConfiguracaoNotificacao.
 *
 * @author PitStop Team
 */
public record EvolutionConfig(
    String apiUrl,
    String apiToken,
    String instanceName,
    String whatsappNumero
) {
    /**
     * Cria config a partir da entidade.
     */
    public static EvolutionConfig from(ConfiguracaoNotificacao config) {
        return new EvolutionConfig(
            config.getEvolutionApiUrl(),
            config.getEvolutionApiToken(),
            config.getEvolutionInstanceName(),
            config.getWhatsappNumero()
        );
    }

    /**
     * Verifica se a config esta completa.
     */
    public boolean isValida() {
        return apiUrl != null && !apiUrl.isBlank() &&
               apiToken != null && !apiToken.isBlank() &&
               instanceName != null && !instanceName.isBlank();
    }
}
