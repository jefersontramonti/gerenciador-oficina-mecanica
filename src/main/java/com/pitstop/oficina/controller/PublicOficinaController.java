package com.pitstop.oficina.controller;

import com.pitstop.oficina.dto.RegisterOficinaRequest;
import com.pitstop.oficina.dto.RegisterOficinaResponse;
import com.pitstop.oficina.service.OficinaRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public controller for oficina self-registration (SaaS onboarding).
 *
 * <p>This endpoint allows new workshops to register in the PitStop platform.
 * It creates:
 * <ul>
 *   <li>A new Oficina (workshop) with TESTE plan (30-day trial)</li>
 *   <li>A default ADMIN user for the oficina</li>
 *   <li>Returns JWT tokens for immediate login</li>
 * </ul>
 *
 * <p><b>Security:</b> This is a PUBLIC endpoint (no authentication required).
 * Rate limiting should be implemented in production to prevent abuse.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/public/oficinas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Registro Público", description = "Endpoints públicos para registro de novas oficinas")
public class PublicOficinaController {

    private final OficinaRegistrationService oficinaRegistrationService;

    /**
     * Registers a new oficina in the platform (SaaS self-service).
     *
     * <p><b>Process:</b>
     * <ol>
     *   <li>Validates CNPJ/CPF uniqueness</li>
     *   <li>Creates Oficina with TESTE plan (30-day trial)</li>
     *   <li>Creates first ADMIN user for the oficina</li>
     *   <li>Generates JWT tokens for immediate login</li>
     *   <li>Sends welcome email (future)</li>
     * </ol>
     *
     * <p><b>Default values:</b>
     * <ul>
     *   <li>Plan: TESTE</li>
     *   <li>Status: ATIVA</li>
     *   <li>Trial period: 30 days</li>
     *   <li>Admin profile: ADMIN</li>
     * </ul>
     *
     * @param request registration data (oficina + admin user data)
     * @return registration response with JWT tokens and oficina/user IDs
     */
    @PostMapping("/register")
    @Operation(
            summary = "Registrar nova oficina",
            description = "Cria uma nova oficina no sistema SaaS com plano de teste de 30 dias e usuário admin"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Oficina e usuário admin criados com sucesso",
                    content = @Content(schema = @Schema(implementation = RegisterOficinaResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "CNPJ/CPF ou email já cadastrados",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos",
                    content = @Content
            )
    })
    public ResponseEntity<RegisterOficinaResponse> registerOficina(
            @Valid @RequestBody RegisterOficinaRequest request
    ) {
        log.info("POST /api/public/oficinas/register - CNPJ/CPF: {}, Admin Email: {}",
                request.cnpjCpf(), request.adminEmail());

        RegisterOficinaResponse response = oficinaRegistrationService.register(request);

        log.info("Oficina registered successfully - ID: {}, Admin Email: {}",
                response.oficinaId(), request.adminEmail());

        return ResponseEntity.status(201).body(response);
    }
}
