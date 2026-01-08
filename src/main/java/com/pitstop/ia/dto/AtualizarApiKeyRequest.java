package com.pitstop.ia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para atualizar a API key da Anthropic.
 */
public record AtualizarApiKeyRequest(
        @NotBlank(message = "API key é obrigatória")
        @Pattern(regexp = "^sk-ant-.*$", message = "API key deve começar com 'sk-ant-'")
        String apiKey
) {}
