package com.pitstop.financeiro.controller;

import com.pitstop.financeiro.domain.StatusPagamento;
import com.pitstop.financeiro.domain.TipoPagamento;
import com.pitstop.financeiro.dto.ConfirmarPagamentoDTO;
import com.pitstop.financeiro.dto.PagamentoRequestDTO;
import com.pitstop.financeiro.dto.PagamentoResponseDTO;
import com.pitstop.financeiro.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de pagamentos.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@RestController
@RequestMapping("/api/pagamentos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pagamentos", description = "Endpoints para gestão de pagamentos")
@SecurityRequirement(name = "bearer-jwt")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Criar pagamento", description = "Registra um novo pagamento para uma ordem de serviço")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pagamento criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
        @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content)
    })
    public ResponseEntity<PagamentoResponseDTO> criar(@RequestBody @Valid PagamentoRequestDTO dto) {
        log.info("POST /api/pagamentos - Criando pagamento para OS: {}", dto.ordemServicoId());

        PagamentoResponseDTO response = pagamentoService.criar(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar pagamento por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pagamento encontrado"),
        @ApiResponse(responseCode = "404", description = "Pagamento não encontrado", content = @Content)
    })
    public ResponseEntity<PagamentoResponseDTO> buscarPorId(@PathVariable UUID id) {
        log.info("GET /api/pagamentos/{} - Buscando pagamento", id);

        PagamentoResponseDTO response = pagamentoService.buscarPorId(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar pagamentos", description = "Lista pagamentos com paginação e filtros")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    })
    public ResponseEntity<Page<PagamentoResponseDTO>> listar(
        @Parameter(description = "Tipo de pagamento") @RequestParam(required = false) TipoPagamento tipo,
        @Parameter(description = "Status do pagamento") @RequestParam(required = false) StatusPagamento status,
        @Parameter(description = "Data inicial") @RequestParam(required = false) LocalDateTime dataInicio,
        @Parameter(description = "Data final") @RequestParam(required = false) LocalDateTime dataFim,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("GET /api/pagamentos - Listando pagamentos");

        Page<PagamentoResponseDTO> response = pagamentoService.buscarComFiltros(
            tipo, status, dataInicio, dataFim, pageable
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ordem-servico/{ordemServicoId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar pagamentos por OS", description = "Retorna todos os pagamentos de uma ordem de serviço")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    })
    public ResponseEntity<List<PagamentoResponseDTO>> listarPorOrdemServico(@PathVariable UUID ordemServicoId) {
        log.info("GET /api/pagamentos/ordem-servico/{} - Listando pagamentos da OS", ordemServicoId);

        List<PagamentoResponseDTO> response = pagamentoService.buscarPorOrdemServico(ordemServicoId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Confirmar pagamento", description = "Marca um pagamento como pago")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pagamento confirmado"),
        @ApiResponse(responseCode = "400", description = "Pagamento não pode ser confirmado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Pagamento não encontrado", content = @Content)
    })
    public ResponseEntity<PagamentoResponseDTO> confirmar(
        @PathVariable UUID id,
        @RequestBody @Valid ConfirmarPagamentoDTO dto
    ) {
        log.info("PUT /api/pagamentos/{}/confirmar - Confirmando pagamento", id);

        PagamentoResponseDTO response = pagamentoService.confirmar(id, dto);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Cancelar pagamento", description = "Cancela um pagamento pendente")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Pagamento cancelado"),
        @ApiResponse(responseCode = "400", description = "Pagamento não pode ser cancelado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Pagamento não encontrado", content = @Content)
    })
    public ResponseEntity<Void> cancelar(@PathVariable UUID id) {
        log.info("DELETE /api/pagamentos/{}/cancelar - Cancelando pagamento", id);

        pagamentoService.cancelar(id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/estornar")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Estornar pagamento", description = "Estorna um pagamento já realizado (apenas ADMIN)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Pagamento estornado"),
        @ApiResponse(responseCode = "400", description = "Pagamento não pode ser estornado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Pagamento não encontrado", content = @Content)
    })
    public ResponseEntity<Void> estornar(@PathVariable UUID id) {
        log.info("PUT /api/pagamentos/{}/estornar - Estornando pagamento", id);

        pagamentoService.estornar(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ordem-servico/{ordemServicoId}/resumo")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Resumo financeiro da OS", description = "Retorna totais de pagamentos da OS")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resumo retornado")
    })
    public ResponseEntity<Map<String, Object>> resumoFinanceiroOS(@PathVariable UUID ordemServicoId) {
        log.info("GET /api/pagamentos/ordem-servico/{}/resumo - Resumo financeiro", ordemServicoId);

        BigDecimal totalPago = pagamentoService.calcularTotalPago(ordemServicoId);
        BigDecimal totalPendente = pagamentoService.calcularTotalPendente(ordemServicoId);
        boolean quitada = pagamentoService.isOrdemServicoQuitada(ordemServicoId);

        Map<String, Object> resumo = Map.of(
            "totalPago", totalPago,
            "totalPendente", totalPendente,
            "quitada", quitada
        );

        return ResponseEntity.ok(resumo);
    }
}
