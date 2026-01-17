package com.pitstop.manutencaopreventiva.service;

import com.pitstop.manutencaopreventiva.dto.*;
import com.pitstop.manutencaopreventiva.repository.*;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardManutencaoService {

    private final PlanoManutencaoRepository planoRepository;
    private final HistoricoManutencaoRepository historicoRepository;
    private final AgendamentoManutencaoRepository agendamentoRepository;
    private final AlertaManutencaoRepository alertaRepository;
    private final PlanoManutencaoService planoService;
    private final AgendamentoManutencaoService agendamentoService;

    /**
     * Retorna dados completos do dashboard.
     */
    public DashboardManutencaoDTO getDashboard() {
        UUID oficinaId = TenantContext.getTenantId();
        LocalDate hoje = LocalDate.now();

        DashboardManutencaoDTO.EstatisticasDTO estatisticas = getEstatisticas();
        List<PlanoManutencaoResponseDTO> proximasManutencoes = planoService.listarProximosAVencer(30)
            .stream()
            .limit(10)
            .toList();
        List<AgendamentoManutencaoResponseDTO> agendamentosHoje = agendamentoService.listarAgendamentosDoDia();
        Long alertasPendentes = alertaRepository.countPendentes(oficinaId);

        return new DashboardManutencaoDTO(
            estatisticas,
            proximasManutencoes,
            agendamentosHoje,
            alertasPendentes
        );
    }

    /**
     * Retorna estatísticas do módulo.
     */
    public DashboardManutencaoDTO.EstatisticasDTO getEstatisticas() {
        UUID oficinaId = TenantContext.getTenantId();
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate inicioSemana = hoje.minusDays(hoje.getDayOfWeek().getValue() - 1);
        LocalDate fimSemana = inicioSemana.plusDays(6);

        // Contadores
        Long totalPlanosAtivos = planoRepository.countAtivos(oficinaId);
        Long planosVencidos = planoRepository.countVencidos(oficinaId, hoje);
        Long planosProximos30Dias = (long) planoRepository.findPlanosProximosAVencerPorData(
            oficinaId, hoje, hoje.plusDays(30)).size();
        Long manutencoesRealizadasMes = historicoRepository.countByPeriodo(
            oficinaId, inicioMes, hoje);
        Long agendamentosHoje = agendamentoRepository.countAgendamentosDoDia(oficinaId, hoje);

        // Contar agendamentos da semana
        Long agendamentosSemana = 0L;
        List<Object[]> statusSemana = agendamentoRepository.countByStatusNoPeriodo(
            oficinaId, inicioSemana, fimSemana);
        for (Object[] row : statusSemana) {
            agendamentosSemana += (Long) row[1];
        }

        // Taxa de execução (manutenções realizadas / planos ativos)
        Double taxaExecucao = totalPlanosAtivos > 0 ?
            (double) manutencoesRealizadasMes / totalPlanosAtivos * 100 : 0.0;

        // Planos por status
        Map<String, Long> planosPorStatus = new HashMap<>();
        for (Object[] row : planoRepository.countByStatus(oficinaId)) {
            planosPorStatus.put(row[0].toString(), (Long) row[1]);
        }

        // Manutenções por tipo
        Map<String, Long> manutencoesPorTipo = new HashMap<>();
        for (Object[] row : historicoRepository.countByTipoManutencao(oficinaId, inicioMes, hoje)) {
            manutencoesPorTipo.put((String) row[0], (Long) row[1]);
        }

        return new DashboardManutencaoDTO.EstatisticasDTO(
            totalPlanosAtivos,
            planosVencidos,
            planosProximos30Dias,
            manutencoesRealizadasMes,
            agendamentosHoje,
            agendamentosSemana,
            taxaExecucao,
            planosPorStatus,
            manutencoesPorTipo
        );
    }
}
