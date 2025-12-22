package com.pitstop.saas.controller;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.saas.dto.PagamentoHistoricoResponse;
import com.pitstop.saas.dto.RegistrarPagamentoRequest;
import com.pitstop.saas.service.SaasPagamentoService;
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
import java.util.UUID;

/**
 * REST Controller for managing SaaS subscription payments.
 *
 * Handles payment registration, tracking, and financial reporting.
 * All endpoints require SUPER_ADMIN role.
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/saas/pagamentos")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class SaasPagamentoController {

    private final SaasPagamentoService pagamentoService;

    /**
     * POST /api/saas/pagamentos
     *
     * Registers a new subscription payment for a workshop.
     * Automatically updates workshop status if payment confirms active subscription.
     *
     * @param request payment details
     * @return registered payment (201 CREATED)
     */
    @PostMapping
    public ResponseEntity<PagamentoHistoricoResponse> registrarPagamento(
        @Valid @RequestBody RegistrarPagamentoRequest request
    ) {
        log.info("SUPER_ADMIN registering payment for workshop: {}", request.oficinaId());
        PagamentoHistoricoResponse response = pagamentoService.registrarPagamento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/saas/pagamentos
     *
     * Lists all payments with optional filters.
     *
     * Query parameters:
     * - oficinaId: filter by workshop
     * - dataInicio: start date for date range filter
     * - dataFim: end date for date range filter
     *
     * @param oficinaId optional workshop filter
     * @param dataInicio optional start date
     * @param dataFim optional end date
     * @param pageable pagination parameters
     * @return paginated payment history (200 OK)
     */
    @GetMapping
    public ResponseEntity<Page<PagamentoHistoricoResponse>> listarPagamentos(
        @RequestParam(required = false) UUID oficinaId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        log.info("SUPER_ADMIN listing payments (filters - oficina: {}, period: {} to {})",
            oficinaId, dataInicio, dataFim);

        Page<PagamentoHistoricoResponse> payments;

        if (oficinaId != null) {
            payments = pagamentoService.getHistoricoPagamentos(oficinaId, pageable);
        } else if (dataInicio != null && dataFim != null) {
            payments = pagamentoService.getPagamentosPorPeriodo(dataInicio, dataFim, pageable);
        } else {
            // If no filters, return overdue payments by default
            payments = pagamentoService.getPagamentosAtrasados(pageable);
        }

        return ResponseEntity.ok(payments);
    }

    /**
     * GET /api/saas/pagamentos/pendentes
     *
     * Lists workshops with pending payments (within grace period).
     * These are not yet overdue but should be monitored.
     *
     * @param pageable pagination parameters
     * @return paginated list of workshops (200 OK)
     */
    @GetMapping("/pendentes")
    public ResponseEntity<Page<Oficina>> listarPagamentosPendentes(
        @PageableDefault(size = 20, sort = "dataVencimentoPlano", direction = Sort.Direction.ASC)
        Pageable pageable
    ) {
        log.info("SUPER_ADMIN listing pending payments");
        Page<Oficina> workshops = pagamentoService.getOficinasPagamentosPendentes(pageable);
        return ResponseEntity.ok(workshops);
    }

    /**
     * GET /api/saas/pagamentos/inadimplentes
     *
     * Lists workshops with overdue payments (past grace period).
     * These workshops should be suspended or contacted immediately.
     *
     * @param pageable pagination parameters
     * @return paginated list of overdue workshops (200 OK)
     */
    @GetMapping("/inadimplentes")
    public ResponseEntity<Page<Oficina>> listarInadimplentes(
        @PageableDefault(size = 20, sort = "dataVencimentoPlano", direction = Sort.Direction.ASC)
        Pageable pageable
    ) {
        log.warn("SUPER_ADMIN listing overdue workshops");
        Page<Oficina> workshops = pagamentoService.getOficinasInadimplentes(pageable);
        return ResponseEntity.ok(workshops);
    }
}
