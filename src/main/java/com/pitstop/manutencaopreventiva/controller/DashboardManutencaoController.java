package com.pitstop.manutencaopreventiva.controller;

import com.pitstop.manutencaopreventiva.dto.*;
import com.pitstop.manutencaopreventiva.service.AgendamentoManutencaoService;
import com.pitstop.manutencaopreventiva.service.DashboardManutencaoService;
import com.pitstop.manutencaopreventiva.service.PlanoManutencaoService;
import com.pitstop.shared.security.feature.RequiresFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manutencao-preventiva/dashboard")
@RequiredArgsConstructor
@Tag(name = "Manutenção Preventiva - Dashboard", description = "Dashboard de manutenção preventiva")
@PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
@RequiresFeature("MANUTENCAO_PREVENTIVA")
public class DashboardManutencaoController {

    private final DashboardManutencaoService dashboardService;
    private final PlanoManutencaoService planoService;
    private final AgendamentoManutencaoService agendamentoService;

    @GetMapping
    @Operation(summary = "Obter dashboard completo")
    public ResponseEntity<DashboardManutencaoDTO> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }

    @GetMapping("/estatisticas")
    @Operation(summary = "Obter estatísticas")
    public ResponseEntity<DashboardManutencaoDTO.EstatisticasDTO> getEstatisticas() {
        return ResponseEntity.ok(dashboardService.getEstatisticas());
    }

    @GetMapping("/proximas-manutencoes")
    @Operation(summary = "Listar próximas manutenções")
    public ResponseEntity<List<PlanoManutencaoResponseDTO>> getProximasManutencoes(
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(planoService.listarProximosAVencer(30)
            .stream()
            .limit(limite)
            .toList());
    }

    @GetMapping("/agendamentos-hoje")
    @Operation(summary = "Listar agendamentos de hoje")
    public ResponseEntity<List<AgendamentoManutencaoResponseDTO>> getAgendamentosHoje() {
        return ResponseEntity.ok(agendamentoService.listarAgendamentosDoDia());
    }
}
