package com.pitstop.financeiro.controller;

import com.pitstop.financeiro.dto.*;
import com.pitstop.financeiro.service.ParcelamentoService;
import com.pitstop.shared.security.feature.RequiresFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Controller para gerenciamento de parcelamento.
 */
@Slf4j
@RestController
@RequestMapping("/api/financeiro/parcelamento")
@RequiredArgsConstructor
@Tag(name = "Parcelamento", description = "Configuração e simulação de parcelamento")
@RequiresFeature("PARCELAMENTO_CARTAO")
public class ParcelamentoController {

    private final ParcelamentoService parcelamentoService;

    // ========== Simulação ==========

    @GetMapping("/simular")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Simular parcelamento para um valor")
    public ResponseEntity<SimulacaoParcelamentoDTO> simular(
            @RequestParam BigDecimal valor) {

        log.debug("Simulando parcelamento para valor: {}", valor);

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        }

        SimulacaoParcelamentoDTO simulacao = parcelamentoService.simularParcelamento(valor);
        return ResponseEntity.ok(simulacao);
    }

    // ========== Configuração ==========

    @GetMapping("/configuracao")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar configuração de parcelamento da oficina")
    public ResponseEntity<ConfiguracaoParcelamentoDTO> buscarConfiguracao() {
        ConfiguracaoParcelamentoDTO config = parcelamentoService.buscarConfiguracao();
        return ResponseEntity.ok(config);
    }

    @PutMapping("/configuracao")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Salvar configuração de parcelamento")
    public ResponseEntity<ConfiguracaoParcelamentoDTO> salvarConfiguracao(
            @Valid @RequestBody ConfiguracaoParcelamentoRequestDTO request) {

        log.info("Salvando configuração de parcelamento");
        ConfiguracaoParcelamentoDTO config = parcelamentoService.salvarConfiguracao(request);
        return ResponseEntity.ok(config);
    }

    // ========== Faixas de Juros ==========

    @GetMapping("/faixas-juros")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar faixas de juros da oficina")
    public ResponseEntity<List<TabelaJurosDTO>> listarFaixasJuros() {
        List<TabelaJurosDTO> faixas = parcelamentoService.listarFaixasJuros();
        return ResponseEntity.ok(faixas);
    }

    @PostMapping("/faixas-juros")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Criar nova faixa de juros")
    public ResponseEntity<TabelaJurosDTO> criarFaixaJuros(
            @Valid @RequestBody TabelaJurosRequestDTO request) {

        log.info("Criando faixa de juros: {}x a {}x",
            request.getParcelasMinimo(), request.getParcelasMaximo());

        TabelaJurosDTO faixa = parcelamentoService.criarFaixaJuros(request);
        return ResponseEntity.ok(faixa);
    }

    @PutMapping("/faixas-juros/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Atualizar faixa de juros")
    public ResponseEntity<TabelaJurosDTO> atualizarFaixaJuros(
            @PathVariable UUID id,
            @Valid @RequestBody TabelaJurosRequestDTO request) {

        log.info("Atualizando faixa de juros: {}", id);
        TabelaJurosDTO faixa = parcelamentoService.atualizarFaixaJuros(id, request);
        return ResponseEntity.ok(faixa);
    }

    @DeleteMapping("/faixas-juros/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Remover faixa de juros")
    public ResponseEntity<Void> removerFaixaJuros(@PathVariable UUID id) {
        log.info("Removendo faixa de juros: {}", id);
        parcelamentoService.removerFaixaJuros(id);
        return ResponseEntity.noContent().build();
    }
}
