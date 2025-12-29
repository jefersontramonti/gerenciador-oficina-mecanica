package com.pitstop.saas.controller;

import com.pitstop.saas.dto.OficinaFeatureFlagsDTO;
import com.pitstop.saas.service.FeatureFlagService;
import com.pitstop.shared.security.CustomUserDetails;
import com.pitstop.shared.security.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller para oficinas verificarem suas Feature Flags.
 * Diferente do SaasFeatureFlagController (SUPER_ADMIN only),
 * este controller é acessível por usuários comuns das oficinas.
 */
@RestController
@RequestMapping("/api/features")
public class OficinaFeatureFlagController {

    private static final Logger logger = LoggerFactory.getLogger(OficinaFeatureFlagController.class);

    private final FeatureFlagService featureFlagService;

    public OficinaFeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    /**
     * Retorna todas as features habilitadas para a oficina do usuário logado.
     *
     * GET /api/features/me
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OficinaFeatureFlagsDTO> getMyFeatures(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID oficinaId = TenantContext.getTenantIdOrNull();

        if (oficinaId == null) {
            // SUPER_ADMIN não tem oficina, retornar todas as features habilitadas
            if (userDetails.getUsuario().getPerfil().name().equals("SUPER_ADMIN")) {
                logger.debug("SUPER_ADMIN acessando features - retornando todas habilitadas");
                return ResponseEntity.ok(new OficinaFeatureFlagsDTO(null, Map.of()));
            }
            return ResponseEntity.badRequest().build();
        }

        logger.debug("GET /api/features/me - Oficina: {}", oficinaId);
        OficinaFeatureFlagsDTO features = featureFlagService.getOficinaFeatures(oficinaId);
        return ResponseEntity.ok(features);
    }

    /**
     * Verifica se uma feature específica está habilitada para a oficina do usuário.
     *
     * GET /api/features/check/{codigo}
     */
    @GetMapping("/check/{codigo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> checkFeature(
            @PathVariable String codigo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID oficinaId = TenantContext.getTenantIdOrNull();

        if (oficinaId == null) {
            // SUPER_ADMIN - todas features habilitadas
            if (userDetails.getUsuario().getPerfil().name().equals("SUPER_ADMIN")) {
                return ResponseEntity.ok(Map.of(
                    "codigo", codigo,
                    "enabled", true,
                    "reason", "SUPER_ADMIN"
                ));
            }
            return ResponseEntity.badRequest().build();
        }

        logger.debug("GET /api/features/check/{} - Oficina: {}", codigo, oficinaId);
        boolean enabled = featureFlagService.isEnabled(codigo, oficinaId);

        return ResponseEntity.ok(Map.of(
            "codigo", codigo,
            "enabled", enabled,
            "oficinaId", oficinaId.toString()
        ));
    }

    /**
     * Verifica múltiplas features de uma vez.
     *
     * POST /api/features/check-batch
     * Body: ["FEATURE_1", "FEATURE_2", "FEATURE_3"]
     */
    @PostMapping("/check-batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> checkFeaturesBatch(
            @RequestBody java.util.List<String> codigos,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID oficinaId = TenantContext.getTenantIdOrNull();

        if (oficinaId == null) {
            // SUPER_ADMIN - todas features habilitadas
            if (userDetails.getUsuario().getPerfil().name().equals("SUPER_ADMIN")) {
                Map<String, Boolean> result = new java.util.HashMap<>();
                for (String codigo : codigos) {
                    result.put(codigo, true);
                }
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.badRequest().build();
        }

        logger.debug("POST /api/features/check-batch - Oficina: {}, Features: {}", oficinaId, codigos.size());

        Map<String, Boolean> result = new java.util.HashMap<>();
        for (String codigo : codigos) {
            result.put(codigo, featureFlagService.isEnabled(codigo, oficinaId));
        }

        return ResponseEntity.ok(result);
    }
}
