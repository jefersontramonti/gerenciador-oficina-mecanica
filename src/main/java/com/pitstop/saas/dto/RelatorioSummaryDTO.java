package com.pitstop.saas.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO com resumo e metadados dos relatórios disponíveis.
 */
public record RelatorioSummaryDTO(
    List<RelatorioDisponivel> relatoriosDisponiveis,
    List<RelatorioRecente> relatoriosRecentes,
    PeriodoDisponivel periodoDisponivel
) {
    public record RelatorioDisponivel(
        String tipo,
        String nome,
        String descricao,
        String icone,
        List<String> formatosDisponiveis
    ) {}

    public record RelatorioRecente(
        String id,
        String tipo,
        String nome,
        LocalDateTime geradoEm,
        String formato,
        String url
    ) {}

    public record PeriodoDisponivel(
        String dataMinima,
        String dataMaxima
    ) {}
}
