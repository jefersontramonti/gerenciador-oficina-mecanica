package com.pitstop.notificacao.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * DTO para metricas de notificacoes.
 *
 * @author PitStop Team
 */
@Builder
public record NotificacaoMetricasDTO(
    UUID oficinaId,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dataInicio,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dataFim,

    // Totais
    long totalEnviadas,
    long totalEntregues,
    long totalLidas,
    long totalFalhas,
    long totalPendentes,

    // Por canal
    Map<String, Long> enviadasPorCanal,
    Map<String, Long> falhasPorCanal,

    // Por evento
    Map<String, Long> enviadasPorEvento,

    // Taxas
    double taxaEntrega,
    double taxaLeitura,
    double taxaFalha,

    // Tendencia (comparacao com periodo anterior)
    Double variacaoEnvios,
    Double variacaoFalhas
) {
    /**
     * Calcula a taxa de entrega.
     */
    public static double calcularTaxaEntrega(long entregues, long enviadas) {
        if (enviadas == 0) return 0.0;
        return (double) entregues / enviadas * 100;
    }

    /**
     * Calcula a taxa de leitura.
     */
    public static double calcularTaxaLeitura(long lidas, long entregues) {
        if (entregues == 0) return 0.0;
        return (double) lidas / entregues * 100;
    }

    /**
     * Calcula a taxa de falha.
     */
    public static double calcularTaxaFalha(long falhas, long total) {
        if (total == 0) return 0.0;
        return (double) falhas / total * 100;
    }
}
