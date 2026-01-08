package com.pitstop.ia.dto;

import com.pitstop.ia.domain.ProvedorIA;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * DTO para criar/atualizar configuração de IA.
 */
public record ConfiguracaoIARequest(
        ProvedorIA provedor,
        String modeloPadrao,
        String modeloAvancado,
        Boolean iaHabilitada,
        Boolean usarCache,
        Boolean usarPreValidacao,
        Boolean usarRoteamentoInteligente,

        @Min(value = 100, message = "Mínimo de 100 tokens")
        @Max(value = 4000, message = "Máximo de 4000 tokens")
        Integer maxTokensResposta,

        @Min(value = 1, message = "Mínimo de 1 requisição/dia")
        @Max(value = 1000, message = "Máximo de 1000 requisições/dia")
        Integer maxRequisicoesDia
) {}
