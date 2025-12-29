package com.pitstop.saas.controller;

import com.pitstop.saas.dto.*;
import com.pitstop.saas.service.ImpersonationService;
import com.pitstop.saas.service.SaasOficinaService;
import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.domain.StatusOficina;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for managing workshops in the SaaS platform.
 *
 * Handles workshop lifecycle operations including creation, updates,
 * and status transitions. All endpoints require SUPER_ADMIN role.
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/saas/oficinas")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class SaasOficinaController {

    private final SaasOficinaService oficinaService;
    private final ImpersonationService impersonationService;

    /**
     * GET /api/saas/oficinas
     *
     * Lists all workshops with pagination and optional filters.
     *
     * @param status filter by status (optional)
     * @param plano filter by plan (optional)
     * @param nome filter by name (optional)
     * @param pageable pagination configuration
     * @return page of workshop summaries (200 OK)
     */
    @GetMapping
    public ResponseEntity<Page<OficinaResumoDTO>> findAll(
        @RequestParam(required = false) StatusOficina status,
        @RequestParam(required = false) PlanoAssinatura plano,
        @RequestParam(required = false) String nome,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.debug("SUPER_ADMIN listing workshops - status: {}, plano: {}, nome: {}", status, plano, nome);
        Page<OficinaResumoDTO> response = oficinaService.findAll(status, plano, nome, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/saas/oficinas
     *
     * Creates a new workshop with automatic 30-day trial period.
     * Also creates an initial admin user for the workshop.
     *
     * @param request creation request with workshop and admin details
     * @return created workshop details (201 CREATED)
     */
    @PostMapping
    public ResponseEntity<OficinaDetailResponse> createOficina(
        @Valid @RequestBody CreateOficinaRequest request
    ) {
        log.info("SUPER_ADMIN creating new workshop: {}", request.nomeFantasia());
        OficinaDetailResponse response = oficinaService.createOficina(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/saas/oficinas/{id}
     *
     * Gets complete details for a specific workshop including usage
     * statistics, payment history, and subscription information.
     *
     * @param id workshop identifier
     * @return workshop details (200 OK)
     */
    @GetMapping("/{id}")
    public ResponseEntity<OficinaDetailResponse> getOficinaDetail(
        @PathVariable UUID id
    ) {
        log.info("SUPER_ADMIN requested workshop details: {}", id);
        OficinaDetailResponse response = oficinaService.getOficinaDetail(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/saas/oficinas/{id}
     *
     * Updates workshop information (excluding status and CNPJ).
     * Status changes must be done through specific action endpoints.
     *
     * @param id workshop identifier
     * @param request update request
     * @return updated workshop details (200 OK)
     */
    @PutMapping("/{id}")
    public ResponseEntity<OficinaDetailResponse> updateOficina(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateOficinaRequest request
    ) {
        log.info("SUPER_ADMIN updating workshop: {}", id);
        OficinaDetailResponse response = oficinaService.updateOficina(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/saas/oficinas/{id}/activate
     *
     * Activates a workshop (changes status to ATIVA).
     * Typically used after trial period or payment confirmation.
     *
     * @param id workshop identifier
     * @return updated workshop details (200 OK)
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<OficinaDetailResponse> activateOficina(
        @PathVariable UUID id
    ) {
        log.info("SUPER_ADMIN activating workshop: {}", id);
        OficinaDetailResponse response = oficinaService.activateOficina(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/saas/oficinas/{id}/suspend
     *
     * Suspends a workshop (changes status to SUSPENSA).
     * Blocks user access until reactivated or payment received.
     *
     * @param id workshop identifier
     * @return updated workshop details (200 OK)
     */
    @PostMapping("/{id}/suspend")
    public ResponseEntity<OficinaDetailResponse> suspendOficina(
        @PathVariable UUID id
    ) {
        log.warn("SUPER_ADMIN suspending workshop: {}", id);
        OficinaDetailResponse response = oficinaService.suspendOficina(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/saas/oficinas/{id}/cancel
     *
     * Cancels a workshop subscription (changes status to CANCELADA).
     * Permanent action - workshop cannot be reactivated.
     * Data is preserved (soft delete) for audit purposes.
     *
     * @param id workshop identifier
     * @return updated workshop details (200 OK)
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OficinaDetailResponse> cancelOficina(
        @PathVariable UUID id
    ) {
        log.warn("SUPER_ADMIN cancelling workshop: {}", id);
        OficinaDetailResponse response = oficinaService.cancelOficina(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/saas/oficinas/{id}/metricas
     *
     * Gets detailed metrics for a specific workshop including
     * usage statistics, resource consumption, and activity data.
     *
     * @param id workshop identifier
     * @return detailed metrics (200 OK)
     */
    @GetMapping("/{id}/metricas")
    public ResponseEntity<OficinaMetricasDTO> getOficinaMetricas(
        @PathVariable UUID id
    ) {
        log.info("SUPER_ADMIN requested metrics for workshop: {}", id);
        OficinaMetricasDTO response = oficinaService.getOficinaMetricas(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/saas/oficinas/{id}/impersonate
     *
     * Generates a temporary access token to access the system
     * as the workshop's admin user. Used for support purposes.
     *
     * Security: All impersonation actions are logged for audit.
     * Token validity: 1 hour.
     *
     * @param id workshop identifier
     * @return impersonation token and redirect URL (200 OK)
     */
    @PostMapping("/{id}/impersonate")
    public ResponseEntity<ImpersonateResponse> impersonateOficina(
        @PathVariable UUID id
    ) {
        log.warn("SUPER_ADMIN initiating impersonation for workshop: {}", id);
        ImpersonateResponse response = impersonationService.impersonate(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/saas/oficinas/{id}/limites
     *
     * Updates resource limits for a workshop.
     * Allows customizing limits beyond plan defaults.
     *
     * @param id workshop identifier
     * @param request limits update request
     * @return updated workshop details (200 OK)
     */
    @PutMapping("/{id}/limites")
    public ResponseEntity<OficinaDetailResponse> updateLimites(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateLimitesRequest request
    ) {
        log.info("SUPER_ADMIN updating limits for workshop: {}", id);
        OficinaDetailResponse response = oficinaService.updateLimites(id, request);
        return ResponseEntity.ok(response);
    }
}
