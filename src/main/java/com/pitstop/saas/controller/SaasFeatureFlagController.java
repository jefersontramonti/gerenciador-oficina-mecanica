package com.pitstop.saas.controller;

import com.pitstop.saas.dto.*;
import com.pitstop.saas.service.FeatureFlagService;
import com.pitstop.shared.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/saas/features")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class SaasFeatureFlagController {

    private static final Logger logger = LoggerFactory.getLogger(SaasFeatureFlagController.class);

    private final FeatureFlagService featureFlagService;

    public SaasFeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    // =====================================
    // CRUD Endpoints
    // =====================================

    @GetMapping
    public ResponseEntity<List<FeatureFlagDTO>> findAll() {
        logger.debug("GET /api/saas/features - Listando todas feature flags");
        List<FeatureFlagDTO> flags = featureFlagService.findAll();
        return ResponseEntity.ok(flags);
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<String>> findAllCategorias() {
        logger.debug("GET /api/saas/features/categorias - Listando categorias");
        List<String> categorias = featureFlagService.findAllCategorias();
        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/by-plano")
    public ResponseEntity<Map<String, List<FeatureFlagDTO>>> getFeaturesByPlano() {
        logger.debug("GET /api/saas/features/by-plano - Listando features por plano");
        Map<String, List<FeatureFlagDTO>> features = featureFlagService.getFeaturesByPlano();
        return ResponseEntity.ok(features);
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<FeatureFlagDTO>> findByCategoria(@PathVariable String categoria) {
        logger.debug("GET /api/saas/features/categoria/{} - Listando por categoria", categoria);
        List<FeatureFlagDTO> flags = featureFlagService.findByCategoria(categoria);
        return ResponseEntity.ok(flags);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeatureFlagDTO> findById(@PathVariable UUID id) {
        logger.debug("GET /api/saas/features/{} - Buscando feature flag", id);
        FeatureFlagDTO flag = featureFlagService.findById(id);
        return ResponseEntity.ok(flag);
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<FeatureFlagDTO> findByCodigo(@PathVariable String codigo) {
        logger.debug("GET /api/saas/features/codigo/{} - Buscando por código", codigo);
        FeatureFlagDTO flag = featureFlagService.findByCodigo(codigo);
        return ResponseEntity.ok(flag);
    }

    @PostMapping
    public ResponseEntity<FeatureFlagDTO> create(
            @Valid @RequestBody CreateFeatureFlagRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        logger.info("POST /api/saas/features - Criando feature flag: {}", request.codigo());
        FeatureFlagDTO created = featureFlagService.create(request, userDetails.getUsuario().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeatureFlagDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFeatureFlagRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        logger.info("PUT /api/saas/features/{} - Atualizando feature flag", id);
        FeatureFlagDTO updated = featureFlagService.update(id, request, userDetails.getUsuario().getId());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        logger.info("DELETE /api/saas/features/{} - Deletando feature flag", id);
        featureFlagService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // =====================================
    // Toggle Endpoints
    // =====================================

    @PostMapping("/{id}/toggle")
    public ResponseEntity<FeatureFlagDTO> toggle(
            @PathVariable UUID id,
            @RequestBody ToggleFeatureFlagRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        logger.info("POST /api/saas/features/{}/toggle - Toggle feature flag", id);
        FeatureFlagDTO toggled = featureFlagService.toggle(id, request, userDetails.getUsuario().getId());
        return ResponseEntity.ok(toggled);
    }

    @PostMapping("/{id}/toggle-global")
    public ResponseEntity<FeatureFlagDTO> toggleGlobal(
            @PathVariable UUID id,
            @RequestParam boolean habilitado,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        logger.info("POST /api/saas/features/{}/toggle-global - Toggle global: {}", id, habilitado);
        FeatureFlagDTO toggled = featureFlagService.toggleGlobal(id, habilitado, userDetails.getUsuario().getId());
        return ResponseEntity.ok(toggled);
    }

    // =====================================
    // Verification Endpoints
    // =====================================

    @GetMapping("/oficina/{oficinaId}")
    public ResponseEntity<OficinaFeatureFlagsDTO> getOficinaFeatures(@PathVariable UUID oficinaId) {
        logger.debug("GET /api/saas/features/oficina/{} - Buscando features da oficina", oficinaId);
        OficinaFeatureFlagsDTO features = featureFlagService.getOficinaFeatures(oficinaId);
        return ResponseEntity.ok(features);
    }

    @GetMapping("/check/{codigo}/oficina/{oficinaId}")
    public ResponseEntity<Map<String, Boolean>> checkFeature(
            @PathVariable String codigo,
            @PathVariable UUID oficinaId) {
        logger.debug("GET /api/saas/features/check/{}/oficina/{}", codigo, oficinaId);
        boolean enabled = featureFlagService.isEnabled(codigo, oficinaId);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable UUID id) {
        logger.debug("GET /api/saas/features/{}/stats - Buscando estatísticas", id);
        Map<String, Object> stats = featureFlagService.getFeatureStats(id);
        return ResponseEntity.ok(stats);
    }
}
