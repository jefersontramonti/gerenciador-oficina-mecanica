package com.pitstop.fornecedor.controller;

import com.pitstop.fornecedor.domain.TipoFornecedor;
import com.pitstop.fornecedor.dto.*;
import com.pitstop.fornecedor.service.FornecedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * Controller REST para gerenciamento de fornecedores.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/fornecedores")
@RequiredArgsConstructor
@Tag(name = "Fornecedores", description = "Gerenciamento de fornecedores de peças")
public class FornecedorController {

    private final FornecedorService fornecedorService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Criar novo fornecedor")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Fornecedor criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "409", description = "CPF/CNPJ já cadastrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<FornecedorResponse> create(@Valid @RequestBody CreateFornecedorRequest request) {
        FornecedorResponse response = fornecedorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar fornecedores", description = "Lista fornecedores com filtros e paginação")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<Page<FornecedorResponse>> findAll(
        @Parameter(description = "Filtro por nome fantasia") @RequestParam(required = false) String nome,
        @Parameter(description = "Filtro por tipo") @RequestParam(required = false) TipoFornecedor tipo,
        @Parameter(description = "Filtro por cidade") @RequestParam(required = false) String cidade,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<FornecedorResponse> fornecedores = fornecedorService.findByFiltros(nome, tipo, cidade, pageable);
        return ResponseEntity.ok(fornecedores);
    }

    @GetMapping("/resumo")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar fornecedores (resumo)", description = "Lista resumida para selects e autocomplete")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<FornecedorResumoResponse>> findAllResumo() {
        List<FornecedorResumoResponse> fornecedores = fornecedorService.findAllResumo();
        return ResponseEntity.ok(fornecedores);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar fornecedor por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fornecedor encontrado"),
        @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<FornecedorResponse> findById(
        @Parameter(description = "ID do fornecedor") @PathVariable UUID id
    ) {
        FornecedorResponse fornecedor = fornecedorService.findById(id);
        return ResponseEntity.ok(fornecedor);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Atualizar fornecedor")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fornecedor atualizado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado"),
        @ApiResponse(responseCode = "409", description = "CPF/CNPJ duplicado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<FornecedorResponse> update(
        @Parameter(description = "ID do fornecedor") @PathVariable UUID id,
        @Valid @RequestBody UpdateFornecedorRequest request
    ) {
        FornecedorResponse fornecedor = fornecedorService.update(id, request);
        return ResponseEntity.ok(fornecedor);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Desativar fornecedor", description = "Soft delete")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Fornecedor desativado"),
        @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID do fornecedor") @PathVariable UUID id
    ) {
        fornecedorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Reativar fornecedor")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fornecedor reativado"),
        @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado"),
        @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<FornecedorResponse> reativar(
        @Parameter(description = "ID do fornecedor") @PathVariable UUID id
    ) {
        FornecedorResponse fornecedor = fornecedorService.reativar(id);
        return ResponseEntity.ok(fornecedor);
    }
}
