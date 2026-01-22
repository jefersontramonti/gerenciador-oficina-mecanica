package com.pitstop.saas.controller;

import com.pitstop.saas.dto.*;
import com.pitstop.saas.service.MinhaContaService;
import com.pitstop.shared.security.CustomUserDetails;
import com.pitstop.shared.security.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for workshops to manage their account and invoices.
 * Accessible by all authenticated workshop users (not SUPER_ADMIN).
 */
@RestController
@RequestMapping("/api/minha-conta")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Minha Conta", description = "Endpoints para oficinas gerenciarem sua conta e faturas")
public class MinhaContaController {

    private final MinhaContaService minhaContaService;

    /**
     * Get financial summary for the logged-in workshop.
     */
    @GetMapping("/resumo")
    @PreAuthorize("isAuthenticated() and !hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Resumo financeiro da oficina")
    public ResponseEntity<MinhaContaResumoDTO> getResumo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID oficinaId = TenantContext.getTenantIdOrNull();
        if (oficinaId == null) {
            return ResponseEntity.badRequest().build();
        }

        log.debug("GET /api/minha-conta/resumo - Oficina: {}", oficinaId);
        MinhaContaResumoDTO resumo = minhaContaService.getResumo(oficinaId);
        return ResponseEntity.ok(resumo);
    }

    /**
     * List invoices for the logged-in workshop.
     */
    @GetMapping("/faturas")
    @PreAuthorize("isAuthenticated() and !hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Listar faturas da oficina")
    public ResponseEntity<Page<FaturaResumoDTO>> listarFaturas(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 12, sort = "dataVencimento", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID oficinaId = TenantContext.getTenantIdOrNull();
        if (oficinaId == null) {
            return ResponseEntity.badRequest().build();
        }

        log.debug("GET /api/minha-conta/faturas - Oficina: {}, Status: {}", oficinaId, status);
        Page<FaturaResumoDTO> faturas = minhaContaService.listarFaturas(oficinaId, status, pageable);
        return ResponseEntity.ok(faturas);
    }

    /**
     * Get invoice detail.
     */
    @GetMapping("/faturas/{id}")
    @PreAuthorize("isAuthenticated() and !hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Detalhe de uma fatura")
    public ResponseEntity<FaturaDTO> getFatura(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID oficinaId = TenantContext.getTenantIdOrNull();
        if (oficinaId == null) {
            return ResponseEntity.badRequest().build();
        }

        log.debug("GET /api/minha-conta/faturas/{} - Oficina: {}", id, oficinaId);
        FaturaDTO fatura = minhaContaService.getFatura(oficinaId, id);
        return ResponseEntity.ok(fatura);
    }

    /**
     * Download invoice as PDF.
     */
    @GetMapping("/faturas/{id}/pdf")
    @PreAuthorize("isAuthenticated() and !hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Download PDF da fatura")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID oficinaId = TenantContext.getTenantIdOrNull();
        if (oficinaId == null) {
            return ResponseEntity.badRequest().build();
        }

        log.debug("GET /api/minha-conta/faturas/{}/pdf - Oficina: {}", id, oficinaId);
        byte[] pdf = minhaContaService.gerarPdfFatura(oficinaId, id);

        return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .header("Content-Disposition", "attachment; filename=fatura-" + id + ".pdf")
            .body(pdf);
    }

    /**
     * Initiate payment for an invoice.
     * Returns Mercado Pago checkout URL and/or PIX QR code.
     */
    @PostMapping("/faturas/{id}/pagar")
    @PreAuthorize("isAuthenticated() and !hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Iniciar pagamento de uma fatura")
    public ResponseEntity<IniciarPagamentoFaturaDTO> iniciarPagamento(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "PIX") String metodoPagamento,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID oficinaId = TenantContext.getTenantIdOrNull();
        if (oficinaId == null) {
            return ResponseEntity.badRequest().build();
        }

        log.info("POST /api/minha-conta/faturas/{}/pagar - Oficina: {}, Metodo: {}",
            id, oficinaId, metodoPagamento);

        IniciarPagamentoFaturaDTO resultado = minhaContaService.iniciarPagamento(
            oficinaId, id, metodoPagamento, userDetails.getUsuario().getEmail()
        );

        return ResponseEntity.ok(resultado);
    }

    /**
     * Get payment history for the logged-in workshop.
     */
    @GetMapping("/pagamentos")
    @PreAuthorize("isAuthenticated() and !hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Histórico de pagamentos da oficina")
    public ResponseEntity<Page<FaturaResumoDTO>> listarPagamentos(
            @PageableDefault(size = 12, sort = "dataPagamento", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID oficinaId = TenantContext.getTenantIdOrNull();
        if (oficinaId == null) {
            return ResponseEntity.badRequest().build();
        }

        log.debug("GET /api/minha-conta/pagamentos - Oficina: {}", oficinaId);
        Page<FaturaResumoDTO> pagamentos = minhaContaService.listarPagamentos(oficinaId, pageable);
        return ResponseEntity.ok(pagamentos);
    }
}
