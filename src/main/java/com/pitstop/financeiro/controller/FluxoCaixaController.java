package com.pitstop.financeiro.controller;

import com.pitstop.financeiro.dto.DRESimplificadoDTO;
import com.pitstop.financeiro.dto.FluxoCaixaDTO;
import com.pitstop.financeiro.dto.ProjecaoFinanceiraDTO;
import com.pitstop.financeiro.service.FluxoCaixaService;
import com.pitstop.shared.security.feature.RequiresFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Controller para fluxo de caixa, DRE e projeções financeiras.
 */
@Slf4j
@RestController
@RequestMapping("/api/financeiro/fluxo-caixa")
@RequiredArgsConstructor
@Tag(name = "Fluxo de Caixa", description = "Fluxo de caixa, DRE e projeções financeiras")
@RequiresFeature("FLUXO_CAIXA_AVANCADO")
public class FluxoCaixaController {

    private final FluxoCaixaService fluxoCaixaService;

    // ========== Fluxo de Caixa ==========

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar fluxo de caixa do período")
    public ResponseEntity<FluxoCaixaDTO> getFluxoCaixa(
            @Parameter(description = "Data inicial (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,

            @Parameter(description = "Data final (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        log.debug("Buscando fluxo de caixa de {} a {}", inicio, fim);

        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Data inicial não pode ser maior que data final");
        }

        FluxoCaixaDTO fluxo = fluxoCaixaService.getFluxoCaixa(inicio, fim);
        return ResponseEntity.ok(fluxo);
    }

    @GetMapping("/mes-atual")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar fluxo de caixa do mês atual")
    public ResponseEntity<FluxoCaixaDTO> getFluxoCaixaMesAtual() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = hoje.withDayOfMonth(1);
        LocalDate fim = hoje.withDayOfMonth(hoje.lengthOfMonth());

        log.debug("Buscando fluxo de caixa do mês atual: {} a {}", inicio, fim);

        FluxoCaixaDTO fluxo = fluxoCaixaService.getFluxoCaixa(inicio, fim);
        return ResponseEntity.ok(fluxo);
    }

    @GetMapping("/ultimos-dias")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar fluxo de caixa dos últimos N dias")
    public ResponseEntity<FluxoCaixaDTO> getFluxoCaixaUltimosDias(
            @Parameter(description = "Quantidade de dias (default: 30)")
            @RequestParam(defaultValue = "30") int dias) {

        if (dias < 1 || dias > 365) {
            throw new IllegalArgumentException("Dias deve estar entre 1 e 365");
        }

        LocalDate fim = LocalDate.now();
        LocalDate inicio = fim.minusDays(dias - 1);

        log.debug("Buscando fluxo de caixa dos últimos {} dias: {} a {}", dias, inicio, fim);

        FluxoCaixaDTO fluxo = fluxoCaixaService.getFluxoCaixa(inicio, fim);
        return ResponseEntity.ok(fluxo);
    }

    // ========== DRE Simplificado ==========

    @GetMapping("/dre")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar DRE de um mês específico")
    public ResponseEntity<DRESimplificadoDTO> getDRE(
            @Parameter(description = "Mês (1-12)")
            @RequestParam int mes,

            @Parameter(description = "Ano (YYYY)")
            @RequestParam int ano) {

        log.debug("Buscando DRE para {}/{}", mes, ano);

        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mês deve estar entre 1 e 12");
        }

        if (ano < 2000 || ano > 2100) {
            throw new IllegalArgumentException("Ano inválido");
        }

        DRESimplificadoDTO dre = fluxoCaixaService.getDREMensal(mes, ano);
        return ResponseEntity.ok(dre);
    }

    @GetMapping("/dre/mes-atual")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar DRE do mês atual")
    public ResponseEntity<DRESimplificadoDTO> getDREMesAtual() {
        YearMonth atual = YearMonth.now();

        log.debug("Buscando DRE do mês atual: {}/{}", atual.getMonthValue(), atual.getYear());

        DRESimplificadoDTO dre = fluxoCaixaService.getDREMensal(
            atual.getMonthValue(),
            atual.getYear()
        );
        return ResponseEntity.ok(dre);
    }

    @GetMapping("/dre/mes-anterior")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar DRE do mês anterior")
    public ResponseEntity<DRESimplificadoDTO> getDREMesAnterior() {
        YearMonth anterior = YearMonth.now().minusMonths(1);

        log.debug("Buscando DRE do mês anterior: {}/{}", anterior.getMonthValue(), anterior.getYear());

        DRESimplificadoDTO dre = fluxoCaixaService.getDREMensal(
            anterior.getMonthValue(),
            anterior.getYear()
        );
        return ResponseEntity.ok(dre);
    }

    // ========== Projeção Financeira ==========

    @GetMapping("/projecao")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar projeção financeira para os próximos N dias")
    public ResponseEntity<ProjecaoFinanceiraDTO> getProjecao(
            @Parameter(description = "Quantidade de dias para projeção (default: 30)")
            @RequestParam(defaultValue = "30") int dias) {

        log.debug("Buscando projeção para {} dias", dias);

        if (dias < 1 || dias > 365) {
            throw new IllegalArgumentException("Dias deve estar entre 1 e 365");
        }

        ProjecaoFinanceiraDTO projecao = fluxoCaixaService.getProjecao(dias);
        return ResponseEntity.ok(projecao);
    }

    @GetMapping("/projecao/semanal")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar projeção financeira para próximos 7 dias")
    public ResponseEntity<ProjecaoFinanceiraDTO> getProjecaoSemanal() {
        log.debug("Buscando projeção semanal (7 dias)");
        ProjecaoFinanceiraDTO projecao = fluxoCaixaService.getProjecao(7);
        return ResponseEntity.ok(projecao);
    }

    @GetMapping("/projecao/mensal")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar projeção financeira para próximos 30 dias")
    public ResponseEntity<ProjecaoFinanceiraDTO> getProjecaoMensal() {
        log.debug("Buscando projeção mensal (30 dias)");
        ProjecaoFinanceiraDTO projecao = fluxoCaixaService.getProjecao(30);
        return ResponseEntity.ok(projecao);
    }

    @GetMapping("/projecao/trimestral")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar projeção financeira para próximos 90 dias")
    public ResponseEntity<ProjecaoFinanceiraDTO> getProjecaoTrimestral() {
        log.debug("Buscando projeção trimestral (90 dias)");
        ProjecaoFinanceiraDTO projecao = fluxoCaixaService.getProjecao(90);
        return ResponseEntity.ok(projecao);
    }
}
