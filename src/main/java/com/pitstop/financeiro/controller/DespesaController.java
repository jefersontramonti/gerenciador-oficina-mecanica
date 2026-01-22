package com.pitstop.financeiro.controller;

import com.pitstop.financeiro.domain.CategoriaDespesa;
import com.pitstop.financeiro.domain.StatusDespesa;
import com.pitstop.financeiro.dto.DespesaDTO;
import com.pitstop.financeiro.service.DespesaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller para gerenciamento de despesas operacionais.
 */
@Slf4j
@RestController
@RequestMapping("/api/financeiro/despesas")
@RequiredArgsConstructor
@Tag(name = "Despesas", description = "Gerenciamento de despesas operacionais")
@PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
public class DespesaController {

    private final DespesaService despesaService;

    // ==================== CRUD ====================

    @PostMapping
    @Operation(summary = "Criar nova despesa")
    @ApiResponse(responseCode = "201", description = "Despesa criada com sucesso")
    public ResponseEntity<DespesaDTO.Response> criar(
            @Valid @RequestBody DespesaDTO.CreateRequest request
    ) {
        log.info("POST /api/financeiro/despesas - Criando despesa: {}", request.getDescricao());
        DespesaDTO.Response response = despesaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar despesa por ID")
    @ApiResponse(responseCode = "200", description = "Despesa encontrada")
    @ApiResponse(responseCode = "404", description = "Despesa não encontrada")
    public ResponseEntity<DespesaDTO.Response> buscarPorId(
            @PathVariable UUID id
    ) {
        log.debug("GET /api/financeiro/despesas/{}", id);
        DespesaDTO.Response response = despesaService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar despesas com filtros")
    @ApiResponse(responseCode = "200", description = "Lista de despesas")
    public ResponseEntity<Page<DespesaDTO.ListItem>> listar(
            @Parameter(description = "Filtrar por status")
            @RequestParam(required = false) StatusDespesa status,

            @Parameter(description = "Filtrar por categoria")
            @RequestParam(required = false) CategoriaDespesa categoria,

            @Parameter(description = "Data início (formato: yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,

            @Parameter(description = "Data fim (formato: yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,

            @PageableDefault(size = 20, sort = "dataVencimento", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.debug("GET /api/financeiro/despesas - status={}, categoria={}", status, categoria);
        Page<DespesaDTO.ListItem> despesas = despesaService.listar(
            status, categoria, dataInicio, dataFim, pageable
        );
        return ResponseEntity.ok(despesas);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar despesa")
    @ApiResponse(responseCode = "200", description = "Despesa atualizada")
    @ApiResponse(responseCode = "400", description = "Despesa já paga não pode ser alterada")
    @ApiResponse(responseCode = "404", description = "Despesa não encontrada")
    public ResponseEntity<DespesaDTO.Response> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody DespesaDTO.UpdateRequest request
    ) {
        log.info("PUT /api/financeiro/despesas/{}", id);
        DespesaDTO.Response response = despesaService.atualizar(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir despesa")
    @ApiResponse(responseCode = "204", description = "Despesa excluída")
    @ApiResponse(responseCode = "400", description = "Despesa paga não pode ser excluída")
    @ApiResponse(responseCode = "404", description = "Despesa não encontrada")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        log.info("DELETE /api/financeiro/despesas/{}", id);
        despesaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== AÇÕES ====================

    @PatchMapping("/{id}/pagar")
    @Operation(summary = "Registrar pagamento de despesa")
    @ApiResponse(responseCode = "200", description = "Pagamento registrado")
    @ApiResponse(responseCode = "400", description = "Despesa já paga ou cancelada")
    @ApiResponse(responseCode = "404", description = "Despesa não encontrada")
    public ResponseEntity<DespesaDTO.Response> pagar(
            @PathVariable UUID id,
            @Valid @RequestBody DespesaDTO.PagamentoRequest request
    ) {
        log.info("PATCH /api/financeiro/despesas/{}/pagar", id);
        DespesaDTO.Response response = despesaService.pagar(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar despesa")
    @ApiResponse(responseCode = "200", description = "Despesa cancelada")
    @ApiResponse(responseCode = "400", description = "Despesa paga não pode ser cancelada")
    @ApiResponse(responseCode = "404", description = "Despesa não encontrada")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    public ResponseEntity<DespesaDTO.Response> cancelar(@PathVariable UUID id) {
        log.info("PATCH /api/financeiro/despesas/{}/cancelar", id);
        DespesaDTO.Response response = despesaService.cancelar(id);
        return ResponseEntity.ok(response);
    }

    // ==================== CONSULTAS ESPECIAIS ====================

    @GetMapping("/vencidas")
    @Operation(summary = "Listar despesas vencidas")
    @ApiResponse(responseCode = "200", description = "Lista de despesas vencidas")
    public ResponseEntity<List<DespesaDTO.ListItem>> listarVencidas() {
        log.debug("GET /api/financeiro/despesas/vencidas");
        List<DespesaDTO.ListItem> despesas = despesaService.listarVencidas();
        return ResponseEntity.ok(despesas);
    }

    @GetMapping("/a-vencer")
    @Operation(summary = "Listar despesas a vencer")
    @ApiResponse(responseCode = "200", description = "Lista de despesas a vencer")
    public ResponseEntity<List<DespesaDTO.ListItem>> listarAVencer(
            @Parameter(description = "Número de dias para projeção (padrão: 7)")
            @RequestParam(defaultValue = "7") int dias
    ) {
        log.debug("GET /api/financeiro/despesas/a-vencer?dias={}", dias);
        List<DespesaDTO.ListItem> despesas = despesaService.listarAVencer(dias);
        return ResponseEntity.ok(despesas);
    }

    @GetMapping("/resumo")
    @Operation(summary = "Obter resumo das despesas")
    @ApiResponse(responseCode = "200", description = "Resumo das despesas")
    public ResponseEntity<DespesaDTO.Resumo> getResumo() {
        log.debug("GET /api/financeiro/despesas/resumo");
        DespesaDTO.Resumo resumo = despesaService.getResumo();
        return ResponseEntity.ok(resumo);
    }

    @GetMapping("/categorias")
    @Operation(summary = "Listar categorias de despesa disponíveis")
    @ApiResponse(responseCode = "200", description = "Lista de categorias")
    public ResponseEntity<List<DespesaDTO.CategoriaInfo>> listarCategorias() {
        log.debug("GET /api/financeiro/despesas/categorias");
        List<DespesaDTO.CategoriaInfo> categorias = despesaService.listarCategorias();
        return ResponseEntity.ok(categorias);
    }
}
