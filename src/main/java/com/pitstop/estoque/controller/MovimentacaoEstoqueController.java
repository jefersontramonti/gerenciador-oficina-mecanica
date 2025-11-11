package com.pitstop.estoque.controller;

import com.pitstop.estoque.domain.MovimentacaoEstoque;
import com.pitstop.estoque.domain.TipoMovimentacao;
import com.pitstop.estoque.dto.*;
import com.pitstop.estoque.mapper.MovimentacaoEstoqueMapper;
import com.pitstop.estoque.service.MovimentacaoEstoqueService;
import com.pitstop.shared.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller REST para gerenciamento de movimentações de estoque.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@RestController
@RequestMapping("/api/movimentacoes-estoque")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Movimentações de Estoque", description = "Gerenciamento de entradas, saídas e ajustes de estoque")
@SecurityRequirement(name = "bearer-jwt")
public class MovimentacaoEstoqueController {

    private final MovimentacaoEstoqueService movimentacaoService;
    private final MovimentacaoEstoqueMapper movimentacaoMapper;

    /**
     * Registra entrada de estoque.
     * POST /api/movimentacoes-estoque/entrada
     */
    @PostMapping("/entrada")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Registrar entrada", description = "Registra entrada de peças no estoque")
    public ResponseEntity<MovimentacaoEstoqueResponseDTO> registrarEntrada(
            @Valid @RequestBody CreateEntradaEstoqueDTO dto
    ) {
        log.info("POST /api/movimentacoes-estoque/entrada - Peça ID: {}, Qtd: {}",
                dto.pecaId(), dto.quantidade());

        UUID usuarioId = SecurityUtils.getCurrentUserId();

        MovimentacaoEstoque movimentacao = movimentacaoService.registrarEntrada(
                dto.pecaId(),
                dto.quantidade(),
                dto.valorUnitario(),
                usuarioId,
                dto.motivo(),
                dto.observacao()
        );

        MovimentacaoEstoqueResponseDTO response = movimentacaoMapper.toResponseDTO(movimentacao);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Registra saída manual de estoque.
     * POST /api/movimentacoes-estoque/saida
     */
    @PostMapping("/saida")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Registrar saída", description = "Registra saída manual de peças do estoque")
    public ResponseEntity<MovimentacaoEstoqueResponseDTO> registrarSaida(
            @Valid @RequestBody CreateSaidaEstoqueDTO dto
    ) {
        log.info("POST /api/movimentacoes-estoque/saida - Peça ID: {}, Qtd: {}",
                dto.pecaId(), dto.quantidade());

        UUID usuarioId = SecurityUtils.getCurrentUserId();

        MovimentacaoEstoque movimentacao = movimentacaoService.registrarSaida(
                dto.pecaId(),
                dto.quantidade(),
                dto.valorUnitario(),
                usuarioId,
                dto.motivo(),
                dto.observacao()
        );

        MovimentacaoEstoqueResponseDTO response = movimentacaoMapper.toResponseDTO(movimentacao);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Registra ajuste de inventário.
     * POST /api/movimentacoes-estoque/ajuste
     */
    @PostMapping("/ajuste")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Registrar ajuste", description = "Registra ajuste de inventário (correção de estoque)")
    public ResponseEntity<MovimentacaoEstoqueResponseDTO> registrarAjuste(
            @Valid @RequestBody CreateAjusteEstoqueDTO dto
    ) {
        log.info("POST /api/movimentacoes-estoque/ajuste - Peça ID: {}, Nova Qtd: {}",
                dto.pecaId(), dto.quantidadeNova());

        UUID usuarioId = SecurityUtils.getCurrentUserId();

        MovimentacaoEstoque movimentacao = movimentacaoService.registrarAjuste(
                dto.pecaId(),
                dto.quantidadeNova(),
                dto.valorUnitario(),
                usuarioId,
                dto.motivo(),
                dto.observacao()
        );

        MovimentacaoEstoqueResponseDTO response = movimentacaoMapper.toResponseDTO(movimentacao);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========== CONSULTAS ==========

    /**
     * Lista movimentações com filtros.
     * GET /api/movimentacoes-estoque
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar movimentações", description = "Lista movimentações com filtros e paginação")
    public ResponseEntity<Page<MovimentacaoEstoqueResponseDTO>> listar(
            @RequestParam(required = false) UUID pecaId,
            @RequestParam(required = false) TipoMovimentacao tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(required = false) UUID usuarioId,
            @PageableDefault(size = 20, sort = "dataMovimentacao") Pageable pageable
    ) {
        log.debug("GET /api/movimentacoes-estoque - Listando movimentações");

        Page<MovimentacaoEstoque> movimentacoes = movimentacaoService.buscarComFiltros(
                pecaId, tipo, dataInicio, dataFim, usuarioId, pageable
        );

        Page<MovimentacaoEstoqueResponseDTO> response = movimentacoes.map(movimentacaoMapper::toResponseDTO);

        return ResponseEntity.ok(response);
    }

    /**
     * Busca histórico de movimentações de uma peça.
     * GET /api/movimentacoes-estoque/peca/{pecaId}
     */
    @GetMapping("/peca/{pecaId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Histórico de peça", description = "Retorna histórico completo de movimentações de uma peça")
    public ResponseEntity<Page<MovimentacaoEstoqueResponseDTO>> buscarHistoricoPeca(
            @PathVariable UUID pecaId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.debug("GET /api/movimentacoes-estoque/peca/{} - Buscando histórico", pecaId);

        Page<MovimentacaoEstoque> movimentacoes = movimentacaoService.buscarHistoricoPeca(pecaId, pageable);
        Page<MovimentacaoEstoqueResponseDTO> response = movimentacoes.map(movimentacaoMapper::toResponseDTO);

        return ResponseEntity.ok(response);
    }

    /**
     * Busca movimentações de uma Ordem de Serviço.
     * GET /api/movimentacoes-estoque/ordem-servico/{ordemServicoId}
     */
    @GetMapping("/ordem-servico/{ordemServicoId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Movimentações de OS", description = "Retorna movimentações vinculadas a uma Ordem de Serviço")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> buscarPorOS(
            @PathVariable UUID ordemServicoId
    ) {
        log.debug("GET /api/movimentacoes-estoque/ordem-servico/{} - Buscando movimentações da OS", ordemServicoId);

        List<MovimentacaoEstoque> movimentacoes = movimentacaoService.buscarPorOS(ordemServicoId);
        List<MovimentacaoEstoqueResponseDTO> response = movimentacoes.stream()
                .map(movimentacaoMapper::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
