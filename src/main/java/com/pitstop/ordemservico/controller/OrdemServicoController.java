package com.pitstop.ordemservico.controller;

import com.pitstop.ordemservico.domain.StatusOS;
import com.pitstop.ordemservico.dto.AguardarPecaDTO;
import com.pitstop.ordemservico.dto.CancelarOrdemServicoDTO;
import com.pitstop.ordemservico.dto.CreateOrdemServicoDTO;
import com.pitstop.ordemservico.dto.FinalizarOSDTO;
import com.pitstop.ordemservico.dto.HistoricoStatusOSDTO;
import com.pitstop.ordemservico.dto.OrdemServicoResponseDTO;
import com.pitstop.ordemservico.dto.UpdateOrdemServicoDTO;
import com.pitstop.ordemservico.service.OrdemServicoService;
import com.pitstop.ordemservico.service.OrdemServicoPDFService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de Ordens de Serviço.
 *
 * <p>Base path: {@code /api/ordens-servico}</p>
 *
 * <p>RBAC:</p>
 * <ul>
 *   <li>ADMIN/GERENTE/ATENDENTE: acesso completo (criar, editar, aprovar, cancelar)</li>
 *   <li>MECANICO: visualizar + atualizar status das OS atribuídas a ele</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/ordens-servico")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ordens de Serviço", description = "Endpoints para gerenciamento de ordens de serviço")
public class OrdemServicoController {

    private final OrdemServicoService service;
    private final OrdemServicoPDFService pdfService;

    // ===== CREATE =====

    /**
     * Cria nova Ordem de Serviço.
     *
     * @param dto dados da OS
     * @return OS criada (HTTP 201)
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Criar nova OS", description = "Cria uma nova ordem de serviço no status ORCAMENTO")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "OS criada com sucesso",
            content = @Content(schema = @Schema(implementation = OrdemServicoResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
        @ApiResponse(responseCode = "404", description = "Veículo ou mecânico não encontrado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content)
    })
    public ResponseEntity<OrdemServicoResponseDTO> criar(@Valid @RequestBody CreateOrdemServicoDTO dto) {
        log.info("POST /api/ordens-servico - Criando nova OS");
        OrdemServicoResponseDTO response = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ===== READ =====

    /**
     * Busca OS por ID.
     *
     * @param id ID da OS
     * @return OS encontrada (HTTP 200)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar OS por ID", description = "Retorna dados completos de uma OS")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OS encontrada",
            content = @Content(schema = @Schema(implementation = OrdemServicoResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content),
        @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content)
    })
    public ResponseEntity<OrdemServicoResponseDTO> buscarPorId(
        @Parameter(description = "ID da OS", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        log.info("GET /api/ordens-servico/{}", id);
        OrdemServicoResponseDTO response = service.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Busca OS por número sequencial.
     *
     * @param numero número da OS
     * @return OS encontrada (HTTP 200)
     */
    @GetMapping("/numero/{numero}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar OS por número", description = "Retorna OS pelo número sequencial")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OS encontrada",
            content = @Content(schema = @Schema(implementation = OrdemServicoResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content)
    })
    public ResponseEntity<OrdemServicoResponseDTO> buscarPorNumero(
        @Parameter(description = "Número da OS", example = "123")
        @PathVariable Long numero
    ) {
        log.info("GET /api/ordens-servico/numero/{}", numero);
        OrdemServicoResponseDTO response = service.buscarPorNumero(numero);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista OS com filtros opcionais e paginação.
     *
     * @param status status da OS (opcional)
     * @param veiculoId ID do veículo (opcional)
     * @param usuarioId ID do mecânico (opcional)
     * @param dataInicio data inicial do período (opcional)
     * @param dataFim data final do período (opcional)
     * @param pageable configuração de paginação e ordenação
     * @return página de OS (HTTP 200)
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar OS", description = "Lista OS com filtros opcionais e paginação")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de OS",
            content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content)
    })
    public ResponseEntity<Page<OrdemServicoResponseDTO>> listar(
        @Parameter(description = "Status da OS", example = "EM_ANDAMENTO")
        @RequestParam(required = false) StatusOS status,

        @Parameter(description = "ID do veículo", example = "123e4567-e89b-12d3-a456-426614174000")
        @RequestParam(required = false) UUID veiculoId,

        @Parameter(description = "ID do mecânico", example = "123e4567-e89b-12d3-a456-426614174000")
        @RequestParam(required = false) UUID usuarioId,

        @Parameter(description = "Data inicial (formato ISO 8601)", example = "2025-11-01T00:00:00")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,

        @Parameter(description = "Data final (formato ISO 8601)", example = "2025-11-30T23:59:59")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,

        @PageableDefault(size = 20, sort = "dataAbertura", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("GET /api/ordens-servico - Filtros: status={}, veiculoId={}, usuarioId={}", status, veiculoId, usuarioId);
        Page<OrdemServicoResponseDTO> page = service.listar(status, veiculoId, usuarioId, dataInicio, dataFim, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Busca histórico de OS de um veículo.
     *
     * @param veiculoId ID do veículo
     * @param pageable configuração de paginação
     * @return página de OS do veículo (HTTP 200)
     */
    @GetMapping("/veiculo/{veiculoId}/historico")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Histórico de OS do veículo", description = "Lista todas as OS de um veículo")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Histórico de OS",
            content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content)
    })
    public ResponseEntity<Page<OrdemServicoResponseDTO>> buscarHistoricoVeiculo(
        @Parameter(description = "ID do veículo", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID veiculoId,

        @PageableDefault(size = 20, sort = "dataAbertura", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("GET /api/ordens-servico/veiculo/{}/historico", veiculoId);
        Page<OrdemServicoResponseDTO> page = service.buscarHistoricoVeiculo(veiculoId, pageable);
        return ResponseEntity.ok(page);
    }

    // ===== UPDATE =====

    /**
     * Atualiza OS existente.
     * Apenas OS em status ORCAMENTO ou APROVADO podem ser editadas.
     *
     * @param id ID da OS
     * @param dto dados para atualização
     * @return OS atualizada (HTTP 200)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Atualizar OS", description = "Atualiza dados de uma OS editável (status ORCAMENTO ou APROVADO)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OS atualizada com sucesso",
            content = @Content(schema = @Schema(implementation = OrdemServicoResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "OS não está editável", content = @Content),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content),
        @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content)
    })
    public ResponseEntity<OrdemServicoResponseDTO> atualizar(
        @Parameter(description = "ID da OS", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id,

        @Valid @RequestBody UpdateOrdemServicoDTO dto
    ) {
        log.info("PUT /api/ordens-servico/{}", id);
        OrdemServicoResponseDTO response = service.atualizar(id, dto);
        return ResponseEntity.ok(response);
    }

    // ===== STATUS TRANSITIONS =====

    /**
     * Aprova orçamento (ORCAMENTO → APROVADO).
     *
     * @param id ID da OS
     * @param aprovadoPeloCliente indicador de aprovação do cliente
     * @return HTTP 204 (No Content)
     */
    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Aprovar orçamento", description = "Aprova orçamento (transição para APROVADO)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "OS aprovada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Transição inválida", content = @Content),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content)
    })
    public ResponseEntity<Void> aprovar(
        @Parameter(description = "ID da OS", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id,

        @Parameter(description = "Cliente aprovou o orçamento?", example = "true")
        @RequestParam Boolean aprovadoPeloCliente
    ) {
        log.info("PATCH /api/ordens-servico/{}/aprovar", id);
        service.aprovar(id, aprovadoPeloCliente);
        return ResponseEntity.noContent().build();
    }

    /**
     * Inicia execução (APROVADO → EM_ANDAMENTO).
     *
     * @param id ID da OS
     * @return HTTP 204 (No Content)
     */
    @PatchMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'MECANICO')")
    @Operation(summary = "Iniciar execução", description = "Inicia trabalhos na OS (transição para EM_ANDAMENTO)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "OS iniciada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Transição inválida", content = @Content),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content)
    })
    public ResponseEntity<Void> iniciar(
        @Parameter(description = "ID da OS", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        log.info("PATCH /api/ordens-servico/{}/iniciar", id);
        service.iniciar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Coloca OS em aguardando peça (EM_ANDAMENTO → AGUARDANDO_PECA).
     *
     * @param id ID da OS
     * @param dto dados com descrição da peça aguardada
     * @return HTTP 204 (No Content)
     */
    @PatchMapping("/{id}/aguardar-peca")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'MECANICO')")
    @Operation(summary = "Aguardar peça", description = "Coloca OS em aguardando peça (transição para AGUARDANDO_PECA)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "OS em aguardando peça"),
        @ApiResponse(responseCode = "400", description = "Transição inválida", content = @Content),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content)
    })
    public ResponseEntity<Void> aguardarPeca(
        @Parameter(description = "ID da OS", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id,

        @Valid @RequestBody AguardarPecaDTO dto
    ) {
        log.info("PATCH /api/ordens-servico/{}/aguardar-peca", id);
        service.aguardarPeca(id, dto.descricaoPeca());
        return ResponseEntity.noContent().build();
    }

    /**
     * Retoma execução após recebimento de peça (AGUARDANDO_PECA → EM_ANDAMENTO).
     *
     * @param id ID da OS
     * @return HTTP 204 (No Content)
     */
    @PatchMapping("/{id}/retomar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'MECANICO')")
    @Operation(summary = "Retomar execução", description = "Retoma execução após recebimento de peça (transição para EM_ANDAMENTO)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Execução retomada"),
        @ApiResponse(responseCode = "400", description = "Transição inválida", content = @Content),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content)
    })
    public ResponseEntity<Void> retomarExecucao(
        @Parameter(description = "ID da OS", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        log.info("PATCH /api/ordens-servico/{}/retomar", id);
        service.retomarExecucao(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Finaliza OS sem informar horas (modelo VALOR_FIXO).
     * Para OS com cobrança POR_HORA, use o endpoint POST com body.
     *
     * @param id ID da OS
     * @return HTTP 204 (No Content)
     */
    @PatchMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'MECANICO')")
    @Operation(summary = "Finalizar OS (VALOR_FIXO)",
               description = "Finaliza serviços com mão de obra fixa (transição para FINALIZADO, baixa estoque)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "OS finalizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Transição inválida", content = @Content),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content)
    })
    public ResponseEntity<Void> finalizar(
        @Parameter(description = "ID da OS", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        log.info("PATCH /api/ordens-servico/{}/finalizar", id);
        service.finalizar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Finaliza OS informando horas trabalhadas (modelo POR_HORA).
     *
     * <p>Valida que as horas informadas não excedem o limite aprovado pelo cliente.</p>
     *
     * @param id ID da OS
     * @param dto dados de finalização (horas trabalhadas, observações opcionais)
     * @return OS finalizada com valores calculados (HTTP 200)
     */
    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'MECANICO')")
    @Operation(summary = "Finalizar OS (POR_HORA)",
               description = "Finaliza serviços informando horas trabalhadas. Calcula mão de obra automaticamente.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OS finalizada com sucesso",
            content = @Content(schema = @Schema(implementation = OrdemServicoResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Transição inválida ou limite de horas excedido", content = @Content),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content),
        @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content)
    })
    public ResponseEntity<OrdemServicoResponseDTO> finalizarComHoras(
        @Parameter(description = "ID da OS", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id,

        @Valid @RequestBody FinalizarOSDTO dto
    ) {
        log.info("POST /api/ordens-servico/{}/finalizar - {} horas trabalhadas", id, dto.horasTrabalhadas());
        OrdemServicoResponseDTO response = service.finalizar(id, dto);
        return ResponseEntity.ok(response);
    }

    /**
     * Entrega veículo ao cliente (FINALIZADO → ENTREGUE).
     *
     * @param id ID da OS
     * @return HTTP 204 (No Content)
     */
    @PatchMapping("/{id}/entregar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Entregar veículo", description = "Registra entrega do veículo (transição para ENTREGUE)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Veículo entregue com sucesso"),
        @ApiResponse(responseCode = "400", description = "Transição inválida ou pagamento pendente", content = @Content),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content)
    })
    public ResponseEntity<Void> entregar(
        @Parameter(description = "ID da OS", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        log.info("PATCH /api/ordens-servico/{}/entregar", id);
        service.entregar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cancela OS (qualquer status exceto ENTREGUE).
     *
     * @param id ID da OS
     * @param dto dados do cancelamento (motivo obrigatório)
     * @return HTTP 204 (No Content)
     */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Cancelar OS", description = "Cancela uma OS (transição para CANCELADO)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "OS cancelada com sucesso"),
        @ApiResponse(responseCode = "400", description = "OS não pode ser cancelada", content = @Content),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content)
    })
    public ResponseEntity<Void> cancelar(
        @Parameter(description = "ID da OS", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id,

        @Valid @RequestBody CancelarOrdemServicoDTO dto
    ) {
        log.info("PATCH /api/ordens-servico/{}/cancelar", id);
        service.cancelar(id, dto);
        return ResponseEntity.noContent().build();
    }

    // ===== DASHBOARD & STATISTICS =====

    /**
     * Retorna contagem de OS por status (para dashboard).
     *
     * @return mapa com status e quantidade (HTTP 200)
     */
    @GetMapping("/dashboard/contagem-por-status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Contagem por status", description = "Retorna quantidade de OS em cada status (KPI)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estatísticas de status"),
        @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content)
    })
    public ResponseEntity<Map<StatusOS, Long>> contarPorStatus() {
        log.info("GET /api/ordens-servico/dashboard/contagem-por-status");
        Map<StatusOS, Long> stats = service.contarPorStatus();
        return ResponseEntity.ok(stats);
    }

    /**
     * Calcula faturamento no período.
     *
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return valor total faturado (HTTP 200)
     */
    @GetMapping("/dashboard/faturamento")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Faturamento do período", description = "Calcula faturamento total (OS entregues)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Valor do faturamento"),
        @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content)
    })
    public ResponseEntity<Map<String, BigDecimal>> calcularFaturamento(
        @Parameter(description = "Data inicial", example = "2025-11-01T00:00:00")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,

        @Parameter(description = "Data final", example = "2025-11-30T23:59:59")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim
    ) {
        log.info("GET /api/ordens-servico/dashboard/faturamento");
        BigDecimal faturamento = service.calcularFaturamento(dataInicio, dataFim);
        return ResponseEntity.ok(Map.of("faturamento", faturamento));
    }

    /**
     * Calcula ticket médio no período.
     *
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return ticket médio (HTTP 200)
     */
    @GetMapping("/dashboard/ticket-medio")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Ticket médio do período", description = "Calcula ticket médio das OS entregues")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ticket médio"),
        @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content)
    })
    public ResponseEntity<Map<String, BigDecimal>> calcularTicketMedio(
        @Parameter(description = "Data inicial", example = "2025-11-01T00:00:00")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,

        @Parameter(description = "Data final", example = "2025-11-30T23:59:59")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim
    ) {
        log.info("GET /api/ordens-servico/dashboard/ticket-medio");
        BigDecimal ticketMedio = service.calcularTicketMedio(dataInicio, dataFim);
        return ResponseEntity.ok(Map.of("ticketMedio", ticketMedio));
    }

    // ===== STATUS HISTORY =====

    /**
     * Busca o histórico de mudanças de status de uma OS.
     *
     * @param id ID da OS
     * @return lista de mudanças de status em ordem cronológica (HTTP 200)
     */
    @GetMapping("/{id}/historico-status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Histórico de status", description = "Retorna todas as mudanças de status de uma OS")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Histórico de status"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content),
        @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content)
    })
    public ResponseEntity<List<HistoricoStatusOSDTO>> buscarHistoricoStatus(
        @Parameter(description = "ID da OS", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        log.info("GET /api/ordens-servico/{}/historico-status", id);
        List<HistoricoStatusOSDTO> historico = service.buscarHistoricoStatus(id);
        return ResponseEntity.ok(historico);
    }

    // ===== PDF GENERATION =====

    /**
     * Gera e baixa PDF da Ordem de Serviço.
     *
     * @param id ID da OS
     * @return PDF da OS (HTTP 200)
     */
    @PostMapping("/{id}/gerar-pdf")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Gerar PDF da OS", description = "Gera e baixa PDF profissional da ordem de serviço")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "PDF gerado com sucesso",
            content = @Content(mediaType = "application/pdf")),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content),
        @ApiResponse(responseCode = "403", description = "Sem permissão", content = @Content),
        @ApiResponse(responseCode = "500", description = "Erro ao gerar PDF", content = @Content)
    })
    public ResponseEntity<byte[]> gerarPDF(
        @Parameter(description = "ID da OS", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        log.info("POST /api/ordens-servico/{}/gerar-pdf", id);

        byte[] pdfBytes = pdfService.gerarPDF(id);

        return ResponseEntity.ok()
            .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
            .header("Content-Disposition", "attachment; filename=\"OS-" + id + ".pdf\"")
            .body(pdfBytes);
    }
}
