package com.pitstop.saas.controller;

import com.pitstop.saas.dto.CreateOficinaRequest;
import com.pitstop.saas.dto.OficinaDetailResponse;
import com.pitstop.saas.dto.UpdateOficinaRequest;
import com.pitstop.saas.service.SaasOficinaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
