package com.pitstop.estoque.controller;

import com.pitstop.estoque.domain.LocalArmazenamento;
import com.pitstop.estoque.domain.TipoLocal;
import com.pitstop.estoque.dto.CreateLocalArmazenamentoDTO;
import com.pitstop.estoque.dto.LocalArmazenamentoResponseDTO;
import com.pitstop.estoque.dto.UpdateLocalArmazenamentoDTO;
import com.pitstop.estoque.mapper.LocalArmazenamentoMapper;
import com.pitstop.estoque.service.LocalArmazenamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller REST para gerenciamento de locais de armazenamento físico.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@RestController
@RequestMapping("/api/locais-armazenamento")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Locais de Armazenamento", description = "Gerenciamento de locais físicos para organização do estoque")
@SecurityRequirement(name = "bearer-jwt")
public class LocalArmazenamentoController {

    private final LocalArmazenamentoService localService;
    private final LocalArmazenamentoMapper localMapper;

    /**
     * Cria novo local de armazenamento.
     * POST /api/locais-armazenamento
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Criar local", description = "Cria novo local de armazenamento com validações de hierarquia")
    public ResponseEntity<LocalArmazenamentoResponseDTO> criar(@Valid @RequestBody CreateLocalArmazenamentoDTO dto) {
        log.info("POST /api/locais-armazenamento - Criando local: código={}", dto.codigo());

        LocalArmazenamento local = localService.criar(dto);
        LocalArmazenamentoResponseDTO response = localMapper.toResponseDTO(local);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista todos os locais ativos.
     * GET /api/locais-armazenamento
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar todos", description = "Lista todos os locais ativos")
    public ResponseEntity<List<LocalArmazenamentoResponseDTO>> listarTodos() {

        List<LocalArmazenamento> locais = localService.listarTodos();
        List<LocalArmazenamentoResponseDTO> response = locais.stream()
                .map(localMapper::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Busca local por ID.
     * GET /api/locais-armazenamento/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar por ID", description = "Retorna detalhes de um local específico")
    public ResponseEntity<LocalArmazenamentoResponseDTO> buscarPorId(@PathVariable UUID id) {

        LocalArmazenamento local = localService.buscarPorId(id);
        LocalArmazenamentoResponseDTO response = localMapper.toResponseDTO(local);

        return ResponseEntity.ok(response);
    }

    /**
     * Busca local por código.
     * GET /api/locais-armazenamento/codigo/{codigo}
     */
    @GetMapping("/codigo/{codigo}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar por código", description = "Busca local pelo código único")
    public ResponseEntity<LocalArmazenamentoResponseDTO> buscarPorCodigo(@PathVariable String codigo) {

        LocalArmazenamento local = localService.buscarPorCodigo(codigo);
        LocalArmazenamentoResponseDTO response = localMapper.toResponseDTO(local);

        return ResponseEntity.ok(response);
    }

    /**
     * Lista locais raiz (sem pai).
     * GET /api/locais-armazenamento/raiz
     */
    @GetMapping("/raiz")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar locais raiz", description = "Lista locais sem pai (normalmente depósitos, vitrines)")
    public ResponseEntity<List<LocalArmazenamentoResponseDTO>> listarLocaisRaiz() {

        List<LocalArmazenamento> locais = localService.listarLocaisRaiz();
        List<LocalArmazenamentoResponseDTO> response = locais.stream()
                .map(localMapper::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Lista locais filhos de um pai específico.
     * GET /api/locais-armazenamento/filhos/{paiId}
     */
    @GetMapping("/filhos/{paiId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar filhos", description = "Lista locais filhos de um local pai")
    public ResponseEntity<List<LocalArmazenamentoResponseDTO>> listarFilhos(@PathVariable UUID paiId) {

        List<LocalArmazenamento> locais = localService.listarFilhos(paiId);
        List<LocalArmazenamentoResponseDTO> response = locais.stream()
                .map(localMapper::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Lista locais por tipo.
     * GET /api/locais-armazenamento/tipo/{tipo}
     */
    @GetMapping("/tipo/{tipo}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar por tipo", description = "Lista locais de um tipo específico")
    public ResponseEntity<List<LocalArmazenamentoResponseDTO>> listarPorTipo(@PathVariable TipoLocal tipo) {

        List<LocalArmazenamento> locais = localService.listarPorTipo(tipo);
        List<LocalArmazenamentoResponseDTO> response = locais.stream()
                .map(localMapper::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Busca locais por descrição (parcial).
     * GET /api/locais-armazenamento/buscar?descricao=...
     */
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar por descrição", description = "Busca locais por descrição parcial (case insensitive)")
    public ResponseEntity<List<LocalArmazenamentoResponseDTO>> buscarPorDescricao(
            @RequestParam String descricao
    ) {

        List<LocalArmazenamento> locais = localService.buscarPorDescricao(descricao);
        List<LocalArmazenamentoResponseDTO> response = locais.stream()
                .map(localMapper::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza local existente.
     * PUT /api/locais-armazenamento/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Atualizar local", description = "Atualiza dados do local com validações de hierarquia")
    public ResponseEntity<LocalArmazenamentoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLocalArmazenamentoDTO dto
    ) {
        log.info("PUT /api/locais-armazenamento/{} - Atualizando local", id);

        LocalArmazenamento local = localService.atualizar(id, dto);
        LocalArmazenamentoResponseDTO response = localMapper.toResponseDTO(local);

        return ResponseEntity.ok(response);
    }

    /**
     * Desativa local (soft delete).
     * PATCH /api/locais-armazenamento/{id}/desativar
     */
    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Desativar local", description = "Desativa local (soft delete) - sempre permitido")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        log.info("PATCH /api/locais-armazenamento/{}/desativar - Desativando local", id);

        localService.desativar(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Reativa local desativado.
     * PATCH /api/locais-armazenamento/{id}/reativar
     */
    @PatchMapping("/{id}/reativar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Reativar local", description = "Reativa local previamente desativado")
    public ResponseEntity<LocalArmazenamentoResponseDTO> reativar(@PathVariable UUID id) {
        log.info("PATCH /api/locais-armazenamento/{}/reativar - Reativando local", id);

        localService.reativar(id);
        LocalArmazenamento local = localService.buscarPorId(id);
        LocalArmazenamentoResponseDTO response = localMapper.toResponseDTO(local);

        return ResponseEntity.ok(response);
    }

    /**
     * Exclui local permanentemente (hard delete).
     * DELETE /api/locais-armazenamento/{id}
     *
     * Só permitido se não houver peças vinculadas.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Excluir permanentemente", description = "Exclui local do banco (hard delete) - só permitido sem peças vinculadas")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        log.info("DELETE /api/locais-armazenamento/{} - Excluindo permanentemente", id);

        localService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}
