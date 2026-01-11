package com.pitstop.ia.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de resposta com o diagnóstico gerado pela IA.
 */
public record DiagnosticoIAResponse(
        String resumo,
        List<CausaPossivel> causasPossiveis,
        List<String> acoesRecomendadas,
        List<PecaProvavel> pecasProvaveis,
        String estimativaTempo,
        FaixaCusto faixaCusto,
        MetadadosDiagnostico metadados
) {
    /**
     * Causa possível do problema com probabilidade.
     */
    public record CausaPossivel(
            String descricao,
            Integer probabilidade,
            Gravidade gravidade
    ) {}

    /**
     * Peça que provavelmente será necessária.
     */
    public record PecaProvavel(
            String nome,
            String codigoReferencia,
            Urgencia urgencia,
            BigDecimal custoEstimado
    ) {}

    /**
     * Faixa de custo estimado para o reparo.
     */
    public record FaixaCusto(
            BigDecimal minimo,
            BigDecimal maximo,
            String moeda
    ) {}

    /**
     * Metadados sobre a geração do diagnóstico.
     */
    public record MetadadosDiagnostico(
            Origem origem,
            String modeloUtilizado,
            Integer tokensConsumidos,
            BigDecimal custoEstimado,
            Long tempoProcessamentoMs
    ) {}

    /**
     * Gravidade do problema.
     */
    public enum Gravidade {
        BAIXA, MEDIA, ALTA, CRITICA
    }

    /**
     * Urgência da peça/ação.
     */
    public enum Urgencia {
        BAIXA, MEDIA, ALTA, IMEDIATA
    }

    /**
     * Origem do diagnóstico.
     */
    public enum Origem {
        TEMPLATE,   // Veio de template pré-definido
        CACHE,      // Veio do cache
        IA_HAIKU,   // Gerado pelo modelo Haiku (barato)
        IA_SONNET   // Gerado pelo modelo Sonnet (avançado)
    }
}
