package com.pitstop.estoque.controller;

import com.pitstop.estoque.domain.Peca;
import com.pitstop.estoque.domain.UnidadeMedida;
import com.pitstop.estoque.dto.CreatePecaDTO;
import com.pitstop.estoque.dto.PecaResponseDTO;
import com.pitstop.estoque.dto.UpdatePecaDTO;
import com.pitstop.estoque.mapper.PecaMapper;
import com.pitstop.estoque.service.EstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de peças do estoque.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Estoque", description = "Gerenciamento de peças e inventário")
@SecurityRequirement(name = "bearer-jwt")
public class EstoqueController {

    private final EstoqueService estoqueService;
    private final PecaMapper pecaMapper;

    /**
     * Cria nova peça no catálogo.
     * POST /api/estoque
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Criar nova peça", description = "Adiciona nova peça ao catálogo de estoque")
    public ResponseEntity<PecaResponseDTO> criar(@Valid @RequestBody CreatePecaDTO dto) {
        log.info("POST /api/estoque - Criando peça: {}", dto.codigo());

        Peca peca = pecaMapper.toEntity(dto);
        Peca pecaCriada = estoqueService.criar(peca);

        // Define localização se informada
        if (dto.localArmazenamentoId() != null) {
            pecaCriada = estoqueService.definirLocalizacaoPeca(pecaCriada.getId(), dto.localArmazenamentoId());
        }

        PecaResponseDTO response = pecaMapper.toResponseDTO(pecaCriada);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista todas as peças com filtros opcionais.
     * GET /api/estoque
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar peças", description = "Lista peças com filtros e paginação")
    public ResponseEntity<Page<PecaResponseDTO>> listar(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) UnidadeMedida unidadeMedida,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Boolean estoqueBaixo,
            @RequestParam(required = false) UUID localArmazenamentoId,
            @PageableDefault(size = 20, sort = "descricao") Pageable pageable
    ) {

        Page<Peca> pecas = estoqueService.listarComFiltros(codigo, descricao, marca, unidadeMedida, ativo, estoqueBaixo, localArmazenamentoId, pageable);
        Page<PecaResponseDTO> response = pecas.map(pecaMapper::toResponseDTO);

        return ResponseEntity.ok(response);
    }

    /**
     * Busca peça por ID.
     * GET /api/estoque/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar peça por ID", description = "Retorna detalhes completos da peça")
    public ResponseEntity<PecaResponseDTO> buscarPorId(@PathVariable UUID id) {

        Peca peca = estoqueService.buscarPorId(id);
        PecaResponseDTO response = pecaMapper.toResponseDTO(peca);

        return ResponseEntity.ok(response);
    }

    /**
     * Busca peça por código (SKU).
     * GET /api/estoque/codigo/{codigo}
     */
    @GetMapping("/codigo/{codigo}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar peça por código", description = "Busca peça pelo código único (SKU)")
    public ResponseEntity<PecaResponseDTO> buscarPorCodigo(@PathVariable String codigo) {

        Peca peca = estoqueService.buscarPorCodigo(codigo);
        PecaResponseDTO response = pecaMapper.toResponseDTO(peca);

        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza peça existente.
     * PUT /api/estoque/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Atualizar peça", description = "Atualiza dados da peça (exceto quantidade em estoque)")
    public ResponseEntity<PecaResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePecaDTO dto
    ) {
        log.info("PUT /api/estoque/{} - Atualizando peça", id);

        Peca pecaAtualizada = pecaMapper.toEntity(dto);
        Peca peca = estoqueService.atualizar(id, pecaAtualizada);

        // Atualiza localização (pode ser null para remover)
        peca = estoqueService.definirLocalizacaoPeca(id, dto.localArmazenamentoId());

        PecaResponseDTO response = pecaMapper.toResponseDTO(peca);

        return ResponseEntity.ok(response);
    }

    /**
     * Desativa peça (soft delete).
     * DELETE /api/estoque/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Desativar peça", description = "Desativa peça do catálogo (soft delete)")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        log.info("DELETE /api/estoque/{} - Desativando peça", id);

        estoqueService.desativar(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Reativa peça desativada.
     * PATCH /api/estoque/{id}/reativar
     */
    @PatchMapping("/{id}/reativar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Reativar peça", description = "Reativa peça desativada")
    public ResponseEntity<PecaResponseDTO> reativar(@PathVariable UUID id) {
        log.info("PATCH /api/estoque/{}/reativar - Reativando peça", id);

        estoqueService.reativar(id);
        Peca peca = estoqueService.buscarPorId(id);
        PecaResponseDTO response = pecaMapper.toResponseDTO(peca);

        return ResponseEntity.ok(response);
    }

    // ========== ALERTAS E RELATÓRIOS ==========

    /**
     * Lista peças com estoque baixo.
     * GET /api/estoque/alertas/baixo
     */
    @GetMapping("/alertas/baixo")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Peças com estoque baixo", description = "Lista peças com quantidade <= quantidade mínima")
    public ResponseEntity<Page<PecaResponseDTO>> listarEstoqueBaixo(
            @PageableDefault(size = 20) Pageable pageable
    ) {

        Page<Peca> pecas = estoqueService.listarEstoqueBaixo(pageable);
        Page<PecaResponseDTO> response = pecas.map(pecaMapper::toResponseDTO);

        return ResponseEntity.ok(response);
    }

    /**
     * Lista peças com estoque zerado.
     * GET /api/estoque/alertas/zerado
     */
    @GetMapping("/alertas/zerado")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Peças sem estoque", description = "Lista peças com quantidade = 0")
    public ResponseEntity<Page<PecaResponseDTO>> listarEstoqueZerado(
            @PageableDefault(size = 20) Pageable pageable
    ) {

        Page<Peca> pecas = estoqueService.listarEstoqueZerado(pageable);
        Page<PecaResponseDTO> response = pecas.map(pecaMapper::toResponseDTO);

        return ResponseEntity.ok(response);
    }

    /**
     * Calcula valor total do inventário.
     * GET /api/estoque/relatorios/valor-total
     */
    @GetMapping("/relatorios/valor-total")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Valor total do inventário", description = "Calcula valor total de todas as peças em estoque")
    public ResponseEntity<BigDecimal> calcularValorTotalInventario() {

        BigDecimal valorTotal = estoqueService.calcularValorTotalInventario();

        return ResponseEntity.ok(valorTotal);
    }

    /**
     * Conta peças com estoque baixo.
     * GET /api/estoque/dashboard/estoque-baixo
     * Retorna 0 para SUPER_ADMIN (não tem estoque próprio).
     */
    @GetMapping("/dashboard/estoque-baixo")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'SUPER_ADMIN')")
    @Operation(summary = "Quantidade de peças com estoque baixo", description = "Retorna contador para dashboard")
    public ResponseEntity<Long> contarEstoqueBaixo() {
        // SUPER_ADMIN não tem oficina/estoque próprio
        UUID oficinaId = com.pitstop.shared.security.tenant.TenantContext.getTenantIdOrNull();
        if (oficinaId == null) {
            return ResponseEntity.ok(0L);
        }
        long count = estoqueService.contarEstoqueBaixo();
        log.info("GET /api/estoque/dashboard/estoque-baixo - Retornando contagem: {}", count);
        return ResponseEntity.ok(count);
    }

    /**
     * Lista marcas distintas cadastradas.
     * GET /api/estoque/filtros/marcas
     */
    @GetMapping("/filtros/marcas")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar marcas", description = "Lista todas as marcas cadastradas (para filtros)")
    public ResponseEntity<List<String>> listarMarcas() {
        List<String> marcas = estoqueService.listarMarcas();
        return ResponseEntity.ok(marcas);
    }

    // ========== LOCATION MANAGEMENT ==========

    /**
     * Lista peças sem localização definida.
     * GET /api/estoque/sem-localizacao
     */
    @GetMapping("/sem-localizacao")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar peças sem localização", description = "Lista peças que ainda não têm local físico definido")
    public ResponseEntity<Page<PecaResponseDTO>> listarPecasSemLocalizacao(
            @PageableDefault(size = 20) Pageable pageable
    ) {

        Page<Peca> pecas = estoqueService.listarPecasSemLocalizacao(pageable);
        Page<PecaResponseDTO> response = pecas.map(pecaMapper::toResponseDTO);

        return ResponseEntity.ok(response);
    }

    /**
     * Conta peças sem localização.
     * GET /api/estoque/dashboard/sem-localizacao
     */
    @GetMapping("/dashboard/sem-localizacao")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Contar peças sem localização", description = "Retorna quantidade de peças sem local físico")
    public ResponseEntity<Long> contarPecasSemLocalizacao() {
        long count = estoqueService.contarPecasSemLocalizacao();
        return ResponseEntity.ok(count);
    }

    /**
     * Define localização física de uma peça.
     * POST /api/estoque/{pecaId}/definir-localizacao
     */
    @PostMapping("/{pecaId}/definir-localizacao")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Definir localização", description = "Define ou altera o local físico de uma peça")
    public ResponseEntity<PecaResponseDTO> definirLocalizacao(
            @PathVariable UUID pecaId,
            @RequestParam(required = false) UUID localId
    ) {
        log.info("POST /api/estoque/{}/definir-localizacao - Local: {}", pecaId, localId);

        Peca peca = estoqueService.definirLocalizacaoPeca(pecaId, localId);
        PecaResponseDTO response = pecaMapper.toResponseDTO(peca);

        return ResponseEntity.ok(response);
    }
}
