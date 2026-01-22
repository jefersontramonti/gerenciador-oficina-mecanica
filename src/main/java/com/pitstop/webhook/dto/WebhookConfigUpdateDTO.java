package com.pitstop.webhook.dto;

import com.pitstop.webhook.domain.TipoEventoWebhook;
import jakarta.validation.constraints.*;

import java.util.Map;
import java.util.Set;

/**
 * DTO para atualização de webhook.
 * Todos os campos são opcionais - apenas os fornecidos serão atualizados.
 *
 * @author PitStop Team
 */
public record WebhookConfigUpdateDTO(
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    String nome,

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    String descricao,

    @Size(max = 500, message = "URL deve ter no máximo 500 caracteres")
    @Pattern(regexp = "^https?://.*", message = "URL deve começar com http:// ou https://")
    String url,

    @Size(max = 200, message = "Secret deve ter no máximo 200 caracteres")
    String secret,

    /**
     * Se true, remove o secret existente.
     */
    Boolean removerSecret,

    Map<String, String> headers,

    Set<TipoEventoWebhook> eventos,

    @Min(value = 1, message = "Mínimo de tentativas é 1")
    @Max(value = 10, message = "Máximo de tentativas é 10")
    Integer maxTentativas,

    @Min(value = 5, message = "Timeout mínimo é 5 segundos")
    @Max(value = 120, message = "Timeout máximo é 120 segundos")
    Integer timeoutSegundos,

    Boolean ativo
) {}
