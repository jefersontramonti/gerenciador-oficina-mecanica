package com.pitstop.dashboard.controller;

import com.pitstop.dashboard.dto.*;
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
 * Fornece estatísticas agregadas, alertas dinâmicos e dados para widgets.
 *
 * @author PitStop Team
 * @version 2.0
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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
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
     * Retorna estatísticas com variação percentual vs mês anterior.
     * GET /api/dashboard/stats-trend
     *
     * @return estatísticas com trends
     */
    @GetMapping("/stats-trend")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(
            summary = "Estatísticas com variação",
            description = "Retorna estatísticas com variação percentual comparado ao mês anterior"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estatísticas retornadas com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    })
    public ResponseEntity<DashboardStatsComTrendDTO> getStatsComTrend() {
        log.info("GET /api/dashboard/stats-trend - Buscando estatísticas com trend");

        DashboardStatsComTrendDTO stats = dashboardService.getDashboardStatsComTrend();

        return ResponseEntity.ok(stats);
    }

    /**
     * Retorna alertas dinâmicos que requerem atenção.
     * GET /api/dashboard/alertas
     *
     * @return alertas do dashboard
     */
    @GetMapping("/alertas")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(
            summary = "Alertas do dashboard",
            description = "Retorna alertas dinâmicos: pagamentos vencidos, manutenções pendentes, peças críticas"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alertas retornados com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    })
    public ResponseEntity<DashboardAlertasDTO> getAlertas() {
        log.info("GET /api/dashboard/alertas - Buscando alertas");

        DashboardAlertasDTO alertas = dashboardService.getAlertas();

        return ResponseEntity.ok(alertas);
    }

    /**
     * Retorna resumo de pagamentos para widget expansível.
     * GET /api/dashboard/pagamentos-resumo
     *
     * @return resumo de pagamentos
     */
    @GetMapping("/pagamentos-resumo")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(
            summary = "Resumo de pagamentos",
            description = "Retorna resumo completo de pagamentos: recebido, pendentes, vencidos, por tipo"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    })
    public ResponseEntity<PagamentosResumoDTO> getPagamentosResumo() {
        log.info("GET /api/dashboard/pagamentos-resumo - Buscando resumo de pagamentos");

        PagamentosResumoDTO resumo = dashboardService.getPagamentosResumo();

        return ResponseEntity.ok(resumo);
    }

    /**
     * Retorna pagamentos agrupados por tipo.
     * GET /api/dashboard/pagamentos-por-tipo
     *
     * @return lista de pagamentos por tipo
     */
    @GetMapping("/pagamentos-por-tipo")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(
            summary = "Pagamentos por tipo",
            description = "Retorna pagamentos do mês agrupados por tipo para gráfico"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    })
    public ResponseEntity<List<PagamentoPorTipoDTO>> getPagamentosPorTipo() {
        log.info("GET /api/dashboard/pagamentos-por-tipo - Buscando pagamentos por tipo");

        List<PagamentoPorTipoDTO> porTipo = dashboardService.getPagamentosPorTipo();

        return ResponseEntity.ok(porTipo);
    }

    /**
     * Retorna resumo de manutenção preventiva para widget expansível.
     * GET /api/dashboard/manutencao-resumo
     *
     * @return resumo de manutenção
     */
    @GetMapping("/manutencao-resumo")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(
            summary = "Resumo de manutenção",
            description = "Retorna resumo de manutenção preventiva: planos ativos, alertas, próximas manutenções"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    })
    public ResponseEntity<ManutencaoResumoDTO> getManutencaoResumo() {
        log.info("GET /api/dashboard/manutencao-resumo - Buscando resumo de manutenção");

        ManutencaoResumoDTO resumo = dashboardService.getManutencaoResumo();

        return ResponseEntity.ok(resumo);
    }

    /**
     * Retorna lista das próximas manutenções.
     * GET /api/dashboard/proximas-manutencoes?dias=7&limite=5
     *
     * @param dias quantidade de dias à frente (padrão 7)
     * @param limite quantidade máxima de resultados (padrão 5)
     * @return lista de próximas manutenções
     */
    @GetMapping("/proximas-manutencoes")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(
            summary = "Próximas manutenções",
            description = "Retorna lista das próximas manutenções preventivas agendadas"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    })
    public ResponseEntity<List<ProximaManutencaoDTO>> getProximasManutencoes(
            @Parameter(description = "Dias à frente para buscar", example = "7")
            @RequestParam(defaultValue = "7")
            @Min(value = 1, message = "Dias mínimo é 1")
            @Max(value = 30, message = "Dias máximo é 30")
            int dias,
            @Parameter(description = "Quantidade máxima de resultados", example = "5")
            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "Limite mínimo é 1")
            @Max(value = 20, message = "Limite máximo é 20")
            int limite
    ) {
        log.info("GET /api/dashboard/proximas-manutencoes?dias={}&limite={}", dias, limite);

        List<ProximaManutencaoDTO> proximas = dashboardService.getProximasManutencoes(dias, limite);

        return ResponseEntity.ok(proximas);
    }

    /**
     * Retorna resumo de notas fiscais para widget expansível.
     * GET /api/dashboard/notas-fiscais-resumo
     *
     * @return resumo de notas fiscais
     */
    @GetMapping("/notas-fiscais-resumo")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(
            summary = "Resumo de notas fiscais",
            description = "Retorna resumo de notas fiscais: emitidas, rascunhos, canceladas no mês"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    })
    public ResponseEntity<NotasFiscaisResumoDTO> getNotasFiscaisResumo() {
        log.info("GET /api/dashboard/notas-fiscais-resumo - Buscando resumo de notas fiscais");

        NotasFiscaisResumoDTO resumo = dashboardService.getNotasFiscaisResumo();

        return ResponseEntity.ok(resumo);
    }

    /**
     * Retorna ordens de serviço recentes.
     * GET /api/dashboard/os-recentes?limit=10
     *
     * @param limit quantidade máxima de resultados (padrão 10, min 1, max 50)
     * @return lista de OS recentes
     */
    @GetMapping("/os-recentes")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
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
