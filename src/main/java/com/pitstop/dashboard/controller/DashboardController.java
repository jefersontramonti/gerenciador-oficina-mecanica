package com.pitstop.dashboard.controller;

import com.pitstop.dashboard.dto.DashboardStatsDTO;
import com.pitstop.dashboard.dto.FaturamentoMensalDTO;
import com.pitstop.dashboard.dto.OSStatusCountDTO;
import com.pitstop.dashboard.dto.RecentOSDTO;
import com.pitstop.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para endpoints do dashboard principal.
 * Fornece estatísticas agregadas e dados recentes do sistema.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Dashboard", description = "Endpoints para dados consolidados do dashboard principal")
@SecurityRequirement(name = "bearer-jwt")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Retorna estatísticas gerais do sistema.
     * GET /api/dashboard/stats
     *
     * @return estatísticas consolidadas
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(
            summary = "Estatísticas do dashboard",
            description = "Retorna estatísticas consolidadas: total de clientes, veículos, OS ativas e faturamento do mês"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estatísticas retornadas com sucesso"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado - usuário sem permissão",
                    content = @Content
            )
    })
    public ResponseEntity<DashboardStatsDTO> getStats() {
        log.info("GET /api/dashboard/stats - Buscando estatísticas do dashboard");

        DashboardStatsDTO stats = dashboardService.getDashboardStats();

        return ResponseEntity.ok(stats);
    }

    /**
     * Retorna ordens de serviço recentes.
     * GET /api/dashboard/os-recentes?limit=10
     *
     * @param limit quantidade máxima de resultados (padrão 10, min 1, max 50)
     * @return lista de OS recentes
     */
    @GetMapping("/os-recentes")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(
            summary = "Ordens de serviço recentes",
            description = "Retorna lista das OS mais recentes com informações de cliente e veículo"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de OS recentes retornada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetro limit inválido (deve estar entre 1 e 50)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado - usuário sem permissão",
                    content = @Content
            )
    })
    public ResponseEntity<List<RecentOSDTO>> getRecentOS(
            @Parameter(description = "Quantidade máxima de OS a retornar", example = "10")
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Limit mínimo é 1")
            @Max(value = 50, message = "Limit máximo é 50")
            int limit
    ) {
        log.info("GET /api/dashboard/os-recentes?limit={} - Buscando OS recentes", limit);

        List<RecentOSDTO> recentOS = dashboardService.getRecentOS(limit);

        log.debug("Retornando {} OS recentes", recentOS.size());

        return ResponseEntity.ok(recentOS);
    }

    /**
     * Retorna contagem de OS agrupadas por status.
     * GET /api/dashboard/os-por-status
     *
     * @return lista de contagens por status
     */
    @GetMapping("/os-por-status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(
            summary = "Contagem de OS por status",
            description = "Retorna quantidade de ordens de serviço agrupadas por status para gráficos"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de contagens retornada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado - usuário sem permissão",
                    content = @Content
            )
    })
    public ResponseEntity<List<OSStatusCountDTO>> getOSByStatus() {
        log.info("GET /api/dashboard/os-por-status - Buscando contagem de OS por status");

        List<OSStatusCountDTO> osByStatus = dashboardService.getOSByStatus();

        log.debug("Retornando contagem de {} status", osByStatus.size());

        return ResponseEntity.ok(osByStatus);
    }

    /**
     * Retorna faturamento mensal dos últimos N meses.
     * GET /api/dashboard/faturamento-mensal?meses=6
     *
     * @param meses quantidade de meses (padrão 6, min 1, max 24)
     * @return lista de faturamento mensal
     */
    @GetMapping("/faturamento-mensal")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(
            summary = "Faturamento mensal",
            description = "Retorna faturamento dos últimos N meses para gráficos de evolução"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de faturamento retornada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetro meses inválido (deve estar entre 1 e 24)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acesso negado - usuário sem permissão",
                    content = @Content
            )
    })
    public ResponseEntity<List<FaturamentoMensalDTO>> getFaturamentoMensal(
            @Parameter(description = "Quantidade de meses a buscar", example = "6")
            @RequestParam(defaultValue = "6")
            @Min(value = 1, message = "Meses mínimo é 1")
            @Max(value = 24, message = "Meses máximo é 24")
            int meses
    ) {
        log.info("GET /api/dashboard/faturamento-mensal?meses={} - Buscando faturamento mensal", meses);

        List<FaturamentoMensalDTO> faturamento = dashboardService.getFaturamentoMensal(meses);

        log.debug("Retornando faturamento de {} meses", faturamento.size());

        return ResponseEntity.ok(faturamento);
    }
}
