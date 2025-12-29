package com.pitstop.saas.controller;

import com.pitstop.saas.dto.*;
import com.pitstop.saas.service.PlanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for subscription plan management.
 * Only accessible by SUPER_ADMIN users.
 */
@RestController
@RequestMapping("/api/saas/planos")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class SaasPlanoController {

    private final PlanoService planoService;

    /**
     * Get all plans.
     */
    @GetMapping
    public ResponseEntity<List<PlanoDTO>> findAll() {
        log.debug("GET /api/saas/planos");
        List<PlanoDTO> planos = planoService.findAll();
        return ResponseEntity.ok(planos);
    }

    /**
     * Get all active plans.
     */
    @GetMapping("/ativos")
    public ResponseEntity<List<PlanoDTO>> findAllActive() {
        log.debug("GET /api/saas/planos/ativos");
        List<PlanoDTO> planos = planoService.findAllActive();
        return ResponseEntity.ok(planos);
    }

    /**
     * Get visible plans for pricing page.
     */
    @GetMapping("/visiveis")
    @PreAuthorize("permitAll()")  // Public endpoint for pricing page
    public ResponseEntity<List<PlanoDTO>> findVisiblePlans() {
        log.debug("GET /api/saas/planos/visiveis");
        List<PlanoDTO> planos = planoService.findVisiblePlans();
        return ResponseEntity.ok(planos);
    }

    /**
     * Get plan by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlanoDTO> findById(@PathVariable UUID id) {
        log.debug("GET /api/saas/planos/{}", id);
        PlanoDTO plano = planoService.findById(id);
        return ResponseEntity.ok(plano);
    }

    /**
     * Get plan by code.
     */
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<PlanoDTO> findByCodigo(@PathVariable String codigo) {
        log.debug("GET /api/saas/planos/codigo/{}", codigo);
        PlanoDTO plano = planoService.findByCodigo(codigo);
        return ResponseEntity.ok(plano);
    }

    /**
     * Create a new plan.
     */
    @PostMapping
    public ResponseEntity<PlanoDTO> create(@Valid @RequestBody CreatePlanoRequest request) {
        log.info("POST /api/saas/planos - Criando plano: {}", request.getCodigo());
        PlanoDTO plano = planoService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(plano);
    }

    /**
     * Update an existing plan.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlanoDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlanoRequest request) {
        log.info("PUT /api/saas/planos/{}", id);
        PlanoDTO plano = planoService.update(id, request);
        return ResponseEntity.ok(plano);
    }

    /**
     * Delete a plan (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("DELETE /api/saas/planos/{}", id);
        planoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle plan visibility.
     */
    @PostMapping("/{id}/toggle-visibilidade")
    public ResponseEntity<PlanoDTO> toggleVisibility(@PathVariable UUID id) {
        log.info("POST /api/saas/planos/{}/toggle-visibilidade", id);
        PlanoDTO plano = planoService.toggleVisibility(id);
        return ResponseEntity.ok(plano);
    }

    /**
     * Get plan statistics.
     */
    @GetMapping("/estatisticas")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.debug("GET /api/saas/planos/estatisticas");
        Map<String, Object> stats = planoService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Change a workshop's plan.
     */
    @PostMapping("/oficinas/{oficinaId}/alterar-plano")
    public ResponseEntity<Void> alterarPlanoOficina(
            @PathVariable UUID oficinaId,
            @Valid @RequestBody AlterarPlanoOficinaRequest request) {
        log.info("POST /api/saas/planos/oficinas/{}/alterar-plano - Novo plano: {}",
                oficinaId, request.getNovoPlano());
        planoService.alterarPlanoOficina(oficinaId, request);
        return ResponseEntity.ok().build();
    }
}
