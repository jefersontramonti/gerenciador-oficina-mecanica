package com.pitstop.saas.dto;

public record ComunicadoMetricasDTO(
    // Contadores por status
    long totalRascunhos,
    long totalAgendados,
    long totalEnviados,
    long totalCancelados,
    // Estatísticas do mês
    long enviadosNoMes,
    long destinatariosNoMes,
    long visualizacoesNoMes,
    double taxaVisualizacaoMedia
) {}
