package com.pitstop.manutencaopreventiva.controller;

import com.pitstop.manutencaopreventiva.dto.*;
import com.pitstop.manutencaopreventiva.service.PlanoManutencaoService;
import com.pitstop.manutencaopreventiva.service.TemplateManutencaoService;
import com.pitstop.shared.security.feature.RequiresFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/manutencao-preventiva/templates")
@RequiredArgsConstructor
@Tag(name = "Manutenção Preventiva - Templates", description = "Gerenciamento de templates de manutenção")
@PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
@RequiresFeature("MANUTENCAO_PREVENTIVA")
public class TemplateManutencaoController {

    private final TemplateManutencaoService templateService;
    private final PlanoManutencaoService planoService;

    @GetMapping
    @Operation(summary = "Listar templates com filtros")
    public ResponseEntity<Page<TemplateManutencaoResponseDTO>> listar(
            @RequestParam(required = false) String tipoManutencao,
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(templateService.listar(tipoManutencao, busca, pageable));
    }

    @GetMapping("/disponiveis")
    @Operation(summary = "Listar templates disponíveis (globais + oficina)")
    public ResponseEntity<List<TemplateManutencaoResponseDTO>> listarDisponiveis() {
        return ResponseEntity.ok(templateService.listarDisponiveis());
    }

    @GetMapping("/globais")
    @Operation(summary = "Listar templates globais")
    public ResponseEntity<List<TemplateManutencaoResponseDTO>> listarGlobais() {
        return ResponseEntity.ok(templateService.listarGlobais());
    }

    @GetMapping("/tipos-manutencao")
    @Operation(summary = "Listar tipos de manutenção disponíveis")
    public ResponseEntity<List<String>> listarTiposManutencao() {
        return ResponseEntity.ok(templateService.listarTiposManutencao());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar template por ID")
    public ResponseEntity<TemplateManutencaoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Criar template personalizado")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    public ResponseEntity<TemplateManutencaoResponseDTO> criar(
            @Valid @RequestBody TemplateManutencaoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar template")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    public ResponseEntity<TemplateManutencaoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody TemplateManutencaoRequestDTO request) {
        return ResponseEntity.ok(templateService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar template")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        templateService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/aplicar")
    @Operation(summary = "Aplicar template em um veículo (cria plano)")
    public ResponseEntity<PlanoManutencaoResponseDTO> aplicarTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody AplicarTemplateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(planoService.criarAPartirDeTemplate(id, request));
    }
}
