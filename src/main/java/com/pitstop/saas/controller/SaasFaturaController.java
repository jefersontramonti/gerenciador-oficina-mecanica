package com.pitstop.saas.controller;

import com.pitstop.saas.domain.StatusFatura;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.service.FaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for managing invoices in the SaaS platform.
 *
 * Handles invoice lifecycle operations including creation, payment,
 * and cancellation. All endpoints require SUPER_ADMIN role.
 */
@RestController
@RequestMapping("/api/saas/faturas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Faturas", description = "Gerenciamento de faturas SaaS")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class SaasFaturaController {

    private final FaturaService faturaService;

    /**
     * GET /api/saas/faturas
     *
     * Lists invoices with optional filters.
     */
    @GetMapping
    @Operation(summary = "Listar faturas", description = "Lista faturas com filtros opcionais")
    public ResponseEntity<Page<FaturaResumoDTO>> findAll(
        @RequestParam(required = false) UUID oficinaId,
        @RequestParam(required = false) StatusFatura status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
        @PageableDefault(size = 20, sort = "dataEmissao", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.debug("Listing invoices - oficinaId: {}, status: {}", oficinaId, status);
        Page<FaturaResumoDTO> response = faturaService.findWithFilters(
            oficinaId, status, dataInicio, dataFim, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/saas/faturas/{id}
     *
     * Gets complete invoice details.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar fatura por ID", description = "Retorna detalhes completos da fatura")
    public ResponseEntity<FaturaDTO> findById(@PathVariable UUID id) {
        log.debug("Fetching invoice: {}", id);
        FaturaDTO response = faturaService.findById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/saas/faturas/numero/{numero}
     *
     * Gets invoice by number.
     */
    @GetMapping("/numero/{numero}")
    @Operation(summary = "Buscar fatura por número", description = "Retorna fatura pelo número (ex: FAT-2025-00001)")
    public ResponseEntity<FaturaDTO> findByNumero(@PathVariable String numero) {
        log.debug("Fetching invoice by number: {}", numero);
        FaturaDTO response = faturaService.findByNumero(numero);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/saas/faturas/oficina/{oficinaId}
     *
     * Gets invoices for a specific workshop.
     */
    @GetMapping("/oficina/{oficinaId}")
    @Operation(summary = "Listar faturas de uma oficina", description = "Retorna faturas de uma oficina específica")
    public ResponseEntity<Page<FaturaResumoDTO>> findByOficina(
        @PathVariable UUID oficinaId,
        @PageableDefault(size = 20, sort = "dataEmissao", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.debug("Fetching invoices for workshop: {}", oficinaId);
        Page<FaturaResumoDTO> response = faturaService.findByOficina(oficinaId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/saas/faturas
     *
     * Creates an invoice manually with custom items.
     */
    @PostMapping
    @Operation(summary = "Criar fatura manual", description = "Cria uma fatura com itens personalizados")
    public ResponseEntity<FaturaDTO> create(@Valid @RequestBody CreateFaturaRequest request) {
        log.info("Creating manual invoice for workshop: {}", request.oficinaId());
        FaturaDTO response = faturaService.criarFaturaManual(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/saas/faturas/gerar/{oficinaId}
     *
     * Generates an invoice for a specific workshop and month.
     */
    @PostMapping("/gerar/{oficinaId}")
    @Operation(summary = "Gerar fatura para oficina", description = "Gera fatura mensal para uma oficina específica")
    public ResponseEntity<FaturaDTO> gerarParaOficina(
        @PathVariable UUID oficinaId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mesReferencia
    ) {
        LocalDate mes = mesReferencia != null ? mesReferencia : LocalDate.now().withDayOfMonth(1);
        log.info("Generating invoice for workshop {} - month: {}", oficinaId, mes);
        FaturaDTO response = faturaService.gerarFatura(oficinaId, mes);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/saas/faturas/gerar-mensais
     *
     * Triggers monthly invoice generation for all workshops.
     * Usually called by scheduled job.
     */
    @PostMapping("/gerar-mensais")
    @Operation(summary = "Gerar faturas mensais", description = "Gera faturas para todas as oficinas ativas (job manual)")
    public ResponseEntity<Map<String, Object>> gerarMensais() {
        log.info("Manual trigger: Generating monthly invoices");
        int count = faturaService.gerarFaturasMensais();
        return ResponseEntity.ok(Map.of(
            "message", "Faturas geradas com sucesso",
            "count", count
        ));
    }

    /**
     * POST /api/saas/faturas/{id}/registrar-pagamento
     *
     * Registers payment for an invoice.
     */
    @PostMapping("/{id}/registrar-pagamento")
    @Operation(summary = "Registrar pagamento", description = "Registra pagamento manual para uma fatura")
    public ResponseEntity<FaturaDTO> registrarPagamento(
        @PathVariable UUID id,
        @Valid @RequestBody RegistrarPagamentoFaturaRequest request
    ) {
        log.info("Registering payment for invoice: {}", id);
        FaturaDTO response = faturaService.registrarPagamento(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/saas/faturas/{id}/cancelar
     *
     * Cancels an invoice.
     */
    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar fatura", description = "Cancela uma fatura pendente")
    public ResponseEntity<FaturaDTO> cancelar(
        @PathVariable UUID id,
        @RequestBody Map<String, String> body
    ) {
        String motivo = body.getOrDefault("motivo", "Cancelado pelo administrador");
        log.warn("Cancelling invoice: {} - Reason: {}", id, motivo);
        FaturaDTO response = faturaService.cancelar(id, motivo);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/saas/faturas/processar-vencidas
     *
     * Processes overdue invoices and marks them as VENCIDO.
     * Usually called by scheduled job.
     */
    @PostMapping("/processar-vencidas")
    @Operation(summary = "Processar faturas vencidas", description = "Marca faturas vencidas como VENCIDO (job manual)")
    public ResponseEntity<Map<String, Object>> processarVencidas() {
        log.info("Manual trigger: Processing overdue invoices");
        int count = faturaService.processarFaturasVencidas();
        return ResponseEntity.ok(Map.of(
            "message", "Faturas processadas com sucesso",
            "count", count
        ));
    }

    /**
     * GET /api/saas/faturas/summary
     *
     * Gets invoice statistics summary.
     */
    @GetMapping("/summary")
    @Operation(summary = "Resumo de faturas", description = "Retorna estatísticas de faturas")
    public ResponseEntity<FaturasResumoSummaryDTO> getSummary() {
        log.debug("Fetching invoice summary");
        FaturasResumoSummaryDTO response = faturaService.getSummary();
        return ResponseEntity.ok(response);
    }
}
