package com.pitstop.saas.controller;

import com.pitstop.saas.dto.CreateLeadRequest;
import com.pitstop.saas.dto.LeadDTO;
import com.pitstop.saas.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public REST Controller for lead capture.
 *
 * This endpoint is publicly accessible (no authentication required)
 * and is used by landing pages and contact forms to capture leads.
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/public/leads")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Leads Públicos", description = "Captura de leads sem autenticação")
public class PublicLeadController {

    private final LeadService leadService;

    /**
     * POST /api/public/leads
     *
     * Captures a new lead from landing page or contact form.
     * No authentication required.
     *
     * @param request lead information
     * @return created lead (201 CREATED)
     */
    @PostMapping
    @Operation(
        summary = "Captura novo lead",
        description = "Endpoint público para captura de leads via formulários de contato e landing pages"
    )
    public ResponseEntity<LeadDTO> criarLead(
        @Valid @RequestBody CreateLeadRequest request
    ) {
        log.info("Lead recebido da origem: {} - email: {}", request.origem(), request.email());
        LeadDTO lead = leadService.criarLead(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(lead);
    }
}
