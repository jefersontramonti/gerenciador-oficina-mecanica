package com.pitstop.saas.controller;

import com.pitstop.saas.domain.StatusLead;
import com.pitstop.saas.dto.LeadDTO;
import com.pitstop.saas.dto.LeadResumoDTO;
import com.pitstop.saas.dto.LeadStatsDTO;
import com.pitstop.saas.dto.UpdateLeadRequest;
import com.pitstop.saas.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for lead management (SUPER_ADMIN only).
 *
 * Provides endpoints for listing, viewing, and updating leads
 * captured from landing pages and other sources.
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/saas/leads")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Leads (Admin)", description = "Gerenciamento de leads do SaaS")
public class AdminLeadController {

    private final LeadService leadService;

    /**
     * GET /api/saas/leads
     *
     * Lists all leads with pagination and optional filters.
     *
     * @param status filter by status (optional)
     * @param origem filter by origem (optional)
     * @param nome filter by name (partial match, optional)
     * @param email filter by email (partial match, optional)
     * @param pageable pagination configuration
     * @return page of leads (200 OK)
     */
    @GetMapping
    @Operation(
        summary = "Lista leads paginados",
        description = "Lista todos os leads com filtros opcionais por status, origem, nome e email"
    )
    public ResponseEntity<Page<LeadResumoDTO>> listarLeads(
        @RequestParam(required = false) StatusLead status,
        @RequestParam(required = false) String origem,
        @RequestParam(required = false) String nome,
        @RequestParam(required = false) String email,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.debug("SUPER_ADMIN listando leads - status: {}, origem: {}, nome: {}, email: {}",
            status, origem, nome, email);
        Page<LeadResumoDTO> response = leadService.listarLeads(status, origem, nome, email, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/saas/leads/{id}
     *
     * Gets complete details for a specific lead.
     *
     * @param id lead identifier
     * @return lead details (200 OK)
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Busca lead por ID",
        description = "Retorna detalhes completos de um lead incluindo observações"
    )
    public ResponseEntity<LeadDTO> buscarLead(
        @PathVariable UUID id
    ) {
        log.debug("SUPER_ADMIN buscando lead: {}", id);
        LeadDTO lead = leadService.buscarPorId(id);
        return ResponseEntity.ok(lead);
    }

    /**
     * PATCH /api/saas/leads/{id}
     *
     * Updates a lead's status and observations.
     *
     * @param id lead identifier
     * @param request update data
     * @return updated lead (200 OK)
     */
    @PatchMapping("/{id}")
    @Operation(
        summary = "Atualiza lead",
        description = "Atualiza status e adiciona observações a um lead"
    )
    public ResponseEntity<LeadDTO> atualizarLead(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateLeadRequest request
    ) {
        log.info("SUPER_ADMIN atualizando lead: {} - novo status: {}", id, request.status());
        LeadDTO lead = leadService.atualizarLead(id, request);
        return ResponseEntity.ok(lead);
    }

    /**
     * GET /api/saas/leads/stats
     *
     * Gets lead statistics grouped by status.
     *
     * @return lead statistics (200 OK)
     */
    @GetMapping("/stats")
    @Operation(
        summary = "Estatísticas de leads",
        description = "Retorna contagem de leads agrupados por status"
    )
    public ResponseEntity<LeadStatsDTO> getEstatisticas() {
        log.debug("SUPER_ADMIN buscando estatísticas de leads");
        LeadStatsDTO stats = leadService.getEstatisticas();
        return ResponseEntity.ok(stats);
    }
}
