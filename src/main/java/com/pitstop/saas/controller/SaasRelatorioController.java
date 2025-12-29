package com.pitstop.saas.controller;

import com.pitstop.saas.dto.*;
import com.pitstop.saas.service.RelatorioService;
import com.pitstop.saas.service.RelatorioExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller para endpoints de relatórios do painel SUPER_ADMIN.
 */
@Slf4j
@RestController
@RequestMapping("/api/saas/relatorios")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Relatórios SaaS", description = "Endpoints para geração e exportação de relatórios")
public class SaasRelatorioController {

    private final RelatorioService relatorioService;
    private final RelatorioExportService relatorioExportService;

    @GetMapping
    @Operation(summary = "Obter resumo dos relatórios disponíveis")
    public ResponseEntity<RelatorioSummaryDTO> getRelatoriosSummary() {
        log.info("Buscando resumo dos relatórios disponíveis");
        return ResponseEntity.ok(relatorioService.getRelatoriosSummary());
    }

    @GetMapping("/financeiro")
    @Operation(summary = "Gerar relatório financeiro")
    public ResponseEntity<RelatorioFinanceiroDTO> getRelatorioFinanceiro(
            @Parameter(description = "Data de início do período")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data de fim do período")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        log.info("Gerando relatório financeiro de {} a {}", dataInicio, dataFim);
        return ResponseEntity.ok(relatorioService.gerarRelatorioFinanceiro(dataInicio, dataFim));
    }

    @GetMapping("/operacional")
    @Operation(summary = "Gerar relatório operacional")
    public ResponseEntity<RelatorioOperacionalDTO> getRelatorioOperacional(
            @Parameter(description = "Data de início do período")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data de fim do período")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        log.info("Gerando relatório operacional de {} a {}", dataInicio, dataFim);
        return ResponseEntity.ok(relatorioService.gerarRelatorioOperacional(dataInicio, dataFim));
    }

    @GetMapping("/crescimento")
    @Operation(summary = "Gerar relatório de crescimento")
    public ResponseEntity<RelatorioCrescimentoDTO> getRelatorioCrescimento(
            @Parameter(description = "Data de início do período")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @Parameter(description = "Data de fim do período")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        log.info("Gerando relatório de crescimento de {} a {}", dataInicio, dataFim);
        return ResponseEntity.ok(relatorioService.gerarRelatorioCrescimento(dataInicio, dataFim));
    }

    @GetMapping("/financeiro/export")
    @Operation(summary = "Exportar relatório financeiro")
    public ResponseEntity<byte[]> exportRelatorioFinanceiro(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "PDF") RelatorioRequest.FormatoExport formato) {
        log.info("Exportando relatório financeiro em formato {}", formato);

        RelatorioFinanceiroDTO relatorio = relatorioService.gerarRelatorioFinanceiro(dataInicio, dataFim);
        byte[] arquivo = relatorioExportService.exportarRelatorioFinanceiro(relatorio, formato);

        String filename = String.format("relatorio_financeiro_%s_%s.%s",
            dataInicio, dataFim, formato.name().toLowerCase());

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(getMediaType(formato))
            .body(arquivo);
    }

    @GetMapping("/operacional/export")
    @Operation(summary = "Exportar relatório operacional")
    public ResponseEntity<byte[]> exportRelatorioOperacional(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "PDF") RelatorioRequest.FormatoExport formato) {
        log.info("Exportando relatório operacional em formato {}", formato);

        RelatorioOperacionalDTO relatorio = relatorioService.gerarRelatorioOperacional(dataInicio, dataFim);
        byte[] arquivo = relatorioExportService.exportarRelatorioOperacional(relatorio, formato);

        String filename = String.format("relatorio_operacional_%s_%s.%s",
            dataInicio, dataFim, formato.name().toLowerCase());

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(getMediaType(formato))
            .body(arquivo);
    }

    @GetMapping("/crescimento/export")
    @Operation(summary = "Exportar relatório de crescimento")
    public ResponseEntity<byte[]> exportRelatorioCrescimento(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "PDF") RelatorioRequest.FormatoExport formato) {
        log.info("Exportando relatório de crescimento em formato {}", formato);

        RelatorioCrescimentoDTO relatorio = relatorioService.gerarRelatorioCrescimento(dataInicio, dataFim);
        byte[] arquivo = relatorioExportService.exportarRelatorioCrescimento(relatorio, formato);

        String filename = String.format("relatorio_crescimento_%s_%s.%s",
            dataInicio, dataFim, formato.name().toLowerCase());

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(getMediaType(formato))
            .body(arquivo);
    }

    private MediaType getMediaType(RelatorioRequest.FormatoExport formato) {
        return switch (formato) {
            case PDF -> MediaType.APPLICATION_PDF;
            case EXCEL -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case CSV -> MediaType.parseMediaType("text/csv");
            case JSON -> MediaType.APPLICATION_JSON;
        };
    }
}
