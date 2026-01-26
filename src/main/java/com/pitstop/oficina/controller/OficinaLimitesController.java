package com.pitstop.oficina.controller;

import com.pitstop.saas.dto.UsoLimitesDTO;
import com.pitstop.saas.service.PlanoLimiteService;
import com.pitstop.shared.security.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller for workshop plan limits information.
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/oficinas")
@RequiredArgsConstructor
@Tag(name = "Oficina Limites", description = "Informações de uso vs limites do plano")
public class OficinaLimitesController {

    private final PlanoLimiteService planoLimiteService;

    /**
     * Returns current usage vs plan limits for the authenticated workshop.
     *
     * @return usage statistics
     */
    @GetMapping("/limites")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Obter uso atual vs limites do plano",
               description = "Retorna estatísticas de uso de usuários e OS vs limites do plano")
    public ResponseEntity<UsoLimitesDTO> obterUsoLimites() {
        UUID oficinaId = TenantContext.getTenantId();
        UsoLimitesDTO uso = planoLimiteService.obterUsoAtual(oficinaId);
        return ResponseEntity.ok(uso);
    }
}
