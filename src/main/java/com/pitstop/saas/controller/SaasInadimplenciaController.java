package com.pitstop.saas.controller;

import com.pitstop.saas.domain.StatusAcordo;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.service.InadimplenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for default management.
 * Only accessible by SUPER_ADMIN users.
 */
@RestController
@RequestMapping("/api/saas/inadimplencia")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class SaasInadimplenciaController {

    private final InadimplenciaService inadimplenciaService;

    // =====================================
    // DASHBOARD
    // =====================================

    /**
     * Get default management dashboard overview.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<InadimplenciaDashboardDTO> getDashboard() {
        log.debug("GET /api/saas/inadimplencia/dashboard");
        InadimplenciaDashboardDTO dashboard = inadimplenciaService.getDashboard();
        return ResponseEntity.ok(dashboard);
    }

    // =====================================
    // DEFAULTING WORKSHOPS
    // =====================================

    /**
     * List all defaulting workshops.
     */
    @GetMapping("/oficinas")
    public ResponseEntity<Page<OficinaInadimplenteDTO>> listarInadimplentes(
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("GET /api/saas/inadimplencia/oficinas");
        Page<OficinaInadimplenteDTO> inadimplentes = inadimplenciaService.listarInadimplentes(pageable);
        return ResponseEntity.ok(inadimplentes);
    }

    // =====================================
    // MASS ACTIONS
    // =====================================

    /**
     * Execute mass action on defaulting workshops.
     */
    @PostMapping("/acao-massa")
    public ResponseEntity<AcaoMassaResultDTO> executarAcaoMassa(
            @Valid @RequestBody AcaoMassaInadimplenciaRequest request) {
        log.info("POST /api/saas/inadimplencia/acao-massa - Ação: {} para {} oficinas",
                request.getAcao(), request.getOficinaIds().size());
        AcaoMassaResultDTO result = inadimplenciaService.executarAcaoMassa(request);
        return ResponseEntity.ok(result);
    }

    // =====================================
    // AGREEMENTS
    // =====================================

    /**
     * Create payment agreement for a workshop.
     */
    @PostMapping("/oficinas/{oficinaId}/acordo")
    public ResponseEntity<AcordoDTO> criarAcordo(
            @PathVariable UUID oficinaId,
            @Valid @RequestBody CriarAcordoRequest request) {
        log.info("POST /api/saas/inadimplencia/oficinas/{}/acordo", oficinaId);
        AcordoDTO acordo = inadimplenciaService.criarAcordo(oficinaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(acordo);
    }

    /**
     * Get agreement by ID.
     */
    @GetMapping("/acordos/{id}")
    public ResponseEntity<AcordoDTO> getAcordo(@PathVariable UUID id) {
        log.debug("GET /api/saas/inadimplencia/acordos/{}", id);
        AcordoDTO acordo = inadimplenciaService.getAcordo(id);
        return ResponseEntity.ok(acordo);
    }

    /**
     * List all agreements.
     */
    @GetMapping("/acordos")
    public ResponseEntity<Page<AcordoDTO>> listarAcordos(
            @RequestParam(required = false) UUID oficinaId,
            @RequestParam(required = false) StatusAcordo status,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("GET /api/saas/inadimplencia/acordos - oficinaId: {}, status: {}",
                oficinaId, status);
        Page<AcordoDTO> acordos = inadimplenciaService.listarAcordos(oficinaId, status, pageable);
        return ResponseEntity.ok(acordos);
    }

    /**
     * Cancel an agreement.
     */
    @PostMapping("/acordos/{id}/cancelar")
    public ResponseEntity<AcordoDTO> cancelarAcordo(
            @PathVariable UUID id,
            @RequestParam String motivo) {
        log.info("POST /api/saas/inadimplencia/acordos/{}/cancelar - Motivo: {}", id, motivo);
        AcordoDTO acordo = inadimplenciaService.cancelarAcordo(id, motivo);
        return ResponseEntity.ok(acordo);
    }

    /**
     * Register payment for an agreement installment.
     */
    @PostMapping("/acordos/{acordoId}/parcelas/{parcelaId}/pagar")
    public ResponseEntity<AcordoDTO> registrarPagamentoParcela(
            @PathVariable UUID acordoId,
            @PathVariable UUID parcelaId,
            @RequestParam String metodoPagamento,
            @RequestParam(required = false) String transacaoId) {
        log.info("POST /api/saas/inadimplencia/acordos/{}/parcelas/{}/pagar", acordoId, parcelaId);
        AcordoDTO acordo = inadimplenciaService.registrarPagamentoParcela(
                acordoId, parcelaId, metodoPagamento, transacaoId);
        return ResponseEntity.ok(acordo);
    }
}
