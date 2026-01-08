package com.pitstop.ia.dto;

import com.pitstop.ia.domain.ProvedorIA;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para configuração de IA.
 */
public record ConfiguracaoIAResponse(
        UUID id,
        UUID oficinaId,
        ProvedorIA provedor,
        String modeloPadrao,
        String modeloAvancado,
        boolean iaHabilitada,
        boolean apiKeyConfigurada,
        boolean usarCache,
        boolean usarPreValidacao,
        boolean usarRoteamentoInteligente,
        Integer maxTokensResposta,
        Integer maxRequisicoesDia,
        Integer requisicoesHoje,
        Integer requisicoesRestantes,
        EstatisticasIA estatisticas,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Estatísticas de uso da IA.
     */
    public record EstatisticasIA(
            Long totalRequisicoes,
            Long totalTokensConsumidos,
            Long totalCacheHits,
            Long totalTemplateHits,
            BigDecimal custoEstimadoTotal,
            Double taxaCacheHit,
            Double taxaTemplateHit
    ) {}
}
