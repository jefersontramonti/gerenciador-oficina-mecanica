package com.pitstop.financeiro.controller;

import com.pitstop.financeiro.domain.StatusAssinatura;
import com.pitstop.financeiro.domain.StatusFaturaAssinatura;
import com.pitstop.financeiro.dto.*;
import com.pitstop.financeiro.service.AssinaturaService;
import com.pitstop.shared.security.feature.RequiresFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de assinaturas e cobranças recorrentes.
 */
@Slf4j
@RestController
@RequestMapping("/api/financeiro/assinaturas")
@RequiredArgsConstructor
@RequiresFeature("COBRANCA_RECORRENTE")
@Tag(name = "Assinaturas", description = "Gerenciamento de assinaturas e cobranças recorrentes")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    // ========== PLANOS ==========

    @GetMapping("/planos")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar planos", description = "Lista todos os planos de assinatura da oficina")
    public ResponseEntity<List<PlanoAssinaturaDTO>> listarPlanos() {
        return ResponseEntity.ok(assinaturaService.listarPlanos());
    }

    @GetMapping("/planos/ativos")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar planos ativos", description = "Lista planos ativos para seleção")
    public ResponseEntity<List<PlanoAssinaturaDTO>> listarPlanosAtivos() {
        return ResponseEntity.ok(assinaturaService.listarPlanosAtivos());
    }

    @GetMapping("/planos/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Buscar plano", description = "Busca um plano por ID")
    public ResponseEntity<PlanoAssinaturaDTO> buscarPlano(
            @Parameter(description = "ID do plano") @PathVariable UUID id) {
        return ResponseEntity.ok(assinaturaService.buscarPlano(id));
    }

    @PostMapping("/planos")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Criar plano", description = "Cria um novo plano de assinatura")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Plano criado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<PlanoAssinaturaDTO> criarPlano(
            @Valid @RequestBody PlanoAssinaturaDTO dto) {
        PlanoAssinaturaDTO plano = assinaturaService.criarPlano(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(plano);
    }

    @PutMapping("/planos/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Atualizar plano", description = "Atualiza um plano existente")
    public ResponseEntity<PlanoAssinaturaDTO> atualizarPlano(
            @Parameter(description = "ID do plano") @PathVariable UUID id,
            @Valid @RequestBody PlanoAssinaturaDTO dto) {
        return ResponseEntity.ok(assinaturaService.atualizarPlano(id, dto));
    }

    @DeleteMapping("/planos/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Desativar plano", description = "Desativa um plano (não exclui)")
    public ResponseEntity<Void> desativarPlano(
            @Parameter(description = "ID do plano") @PathVariable UUID id) {
        assinaturaService.desativarPlano(id);
        return ResponseEntity.noContent().build();
    }

    // ========== ASSINATURAS ==========

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar assinaturas", description = "Lista assinaturas com filtros")
    public ResponseEntity<Page<AssinaturaDTO>> listarAssinaturas(
            @RequestParam(required = false) StatusAssinatura status,
            @RequestParam(required = false) UUID planoId,
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(assinaturaService.listarAssinaturas(status, planoId, busca, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Buscar assinatura", description = "Busca uma assinatura por ID")
    public ResponseEntity<AssinaturaDTO> buscarAssinatura(
            @Parameter(description = "ID da assinatura") @PathVariable UUID id) {
        return ResponseEntity.ok(assinaturaService.buscarAssinatura(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Criar assinatura", description = "Cria uma nova assinatura para um cliente")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Assinatura criada"),
        @ApiResponse(responseCode = "400", description = "Cliente já tem assinatura ativa ou dados inválidos")
    })
    public ResponseEntity<AssinaturaDTO> criarAssinatura(
            @Valid @RequestBody AssinaturaDTO.CreateAssinaturaDTO dto) {
        AssinaturaDTO assinatura = assinaturaService.criarAssinatura(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(assinatura);
    }

    @PostMapping("/{id}/pausar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Pausar assinatura", description = "Pausa uma assinatura ativa")
    public ResponseEntity<AssinaturaDTO> pausarAssinatura(
            @Parameter(description = "ID da assinatura") @PathVariable UUID id) {
        return ResponseEntity.ok(assinaturaService.pausarAssinatura(id));
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Reativar assinatura", description = "Reativa uma assinatura pausada ou inadimplente")
    public ResponseEntity<AssinaturaDTO> reativarAssinatura(
            @Parameter(description = "ID da assinatura") @PathVariable UUID id) {
        return ResponseEntity.ok(assinaturaService.reativarAssinatura(id));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Cancelar assinatura", description = "Cancela uma assinatura permanentemente")
    public ResponseEntity<AssinaturaDTO> cancelarAssinatura(
            @Parameter(description = "ID da assinatura") @PathVariable UUID id,
            @Valid @RequestBody AssinaturaDTO.CancelarAssinaturaDTO dto) {
        return ResponseEntity.ok(assinaturaService.cancelarAssinatura(id, dto));
    }

    // ========== FATURAS ==========

    @GetMapping("/faturas")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar faturas", description = "Lista faturas de assinatura com filtros")
    public ResponseEntity<Page<FaturaAssinaturaDTO>> listarFaturas(
            @RequestParam(required = false) StatusFaturaAssinatura status,
            @RequestParam(required = false) UUID assinaturaId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(assinaturaService.listarFaturas(status, assinaturaId, pageable));
    }

    @GetMapping("/faturas/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Buscar fatura", description = "Busca uma fatura por ID")
    public ResponseEntity<FaturaAssinaturaDTO> buscarFatura(
            @Parameter(description = "ID da fatura") @PathVariable UUID id) {
        return ResponseEntity.ok(assinaturaService.buscarFatura(id));
    }

    @GetMapping("/{id}/faturas")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar faturas da assinatura", description = "Lista faturas de uma assinatura específica")
    public ResponseEntity<List<FaturaAssinaturaDTO>> listarFaturasAssinatura(
            @Parameter(description = "ID da assinatura") @PathVariable UUID id) {
        return ResponseEntity.ok(assinaturaService.listarFaturasAssinatura(id));
    }

    @PostMapping("/faturas/{id}/pagar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Registrar pagamento", description = "Registra pagamento manual de uma fatura")
    public ResponseEntity<FaturaAssinaturaDTO> registrarPagamento(
            @Parameter(description = "ID da fatura") @PathVariable UUID id,
            @RequestBody(required = false) FaturaAssinaturaDTO.RegistrarPagamentoDTO dto) {
        return ResponseEntity.ok(assinaturaService.registrarPagamento(id, dto));
    }

    @PostMapping("/faturas/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Cancelar fatura", description = "Cancela uma fatura pendente")
    public ResponseEntity<FaturaAssinaturaDTO> cancelarFatura(
            @Parameter(description = "ID da fatura") @PathVariable UUID id,
            @RequestParam(required = false) String observacao) {
        return ResponseEntity.ok(assinaturaService.cancelarFatura(id, observacao));
    }
}
