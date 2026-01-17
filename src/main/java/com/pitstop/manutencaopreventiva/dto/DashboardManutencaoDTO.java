package com.pitstop.manutencaopreventiva.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO para dashboard de manutenção preventiva.
 */
public record DashboardManutencaoDTO(
    EstatisticasDTO estatisticas,
    List<PlanoManutencaoResponseDTO> proximasManutencoes,
    List<AgendamentoManutencaoResponseDTO> agendamentosHoje,
    Long alertasPendentes
) {
    public record EstatisticasDTO(
        Long totalPlanosAtivos,
        Long planosVencidos,
        Long planosProximos30Dias,
        Long manutencoesRealizadasMes,
        Long agendamentosHoje,
        Long agendamentosSemana,
        Double taxaExecucao,
        Map<String, Long> planosPorStatus,
        Map<String, Long> manutencoesPorTipo
    ) {}
}
