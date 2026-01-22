package com.pitstop.financeiro.controller;

import com.pitstop.financeiro.dto.ConciliacaoRequestDTO;
import com.pitstop.financeiro.dto.ExtratoBancarioDTO;
import com.pitstop.financeiro.dto.TransacaoExtratoDTO;
import com.pitstop.financeiro.service.ConciliacaoService;
import com.pitstop.shared.security.feature.RequiresFeature;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST para conciliação bancária.
 *
 * <p>Permite:</p>
 * <ul>
 *   <li>Importar extratos bancários (OFX)</li>
 *   <li>Listar e visualizar extratos e transações</li>
 *   <li>Conciliar transações com pagamentos</li>
 *   <li>Matching automático com sugestões</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/financeiro/conciliacao")
@RequiredArgsConstructor
@RequiresFeature("CONCILIACAO_BANCARIA")
@Tag(name = "Conciliação Bancária", description = "Gerenciamento de conciliação bancária e importação de extratos")
public class ConciliacaoController {

    private final ConciliacaoService conciliacaoService;

    // ========== Importação de Extrato ==========

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Importar extrato bancário",
               description = "Importa um arquivo OFX e executa matching automático inicial")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Extrato importado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Arquivo inválido ou já importado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public ResponseEntity<ExtratoBancarioDTO> importarExtrato(
            @Parameter(description = "Arquivo OFX do extrato bancário", required = true)
            @RequestParam("arquivo") MultipartFile arquivo,
            @Parameter(description = "ID da conta bancária (opcional)")
            @RequestParam(value = "contaBancariaId", required = false) UUID contaBancariaId) {

        try {
            log.info("Recebendo arquivo para importação: {}", arquivo.getOriginalFilename());

            // Validar tipo do arquivo
            String filename = arquivo.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".ofx")) {
                return ResponseEntity.badRequest().build();
            }

            ExtratoBancarioDTO result = conciliacaoService.importarExtrato(arquivo, contaBancariaId);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação ao importar extrato: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro ao importar extrato: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar arquivo OFX: " + e.getMessage());
        }
    }

    // ========== Listagem de Extratos ==========

    @GetMapping("/extratos")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar extratos", description = "Lista todos os extratos importados da oficina")
    public ResponseEntity<Page<ExtratoBancarioDTO>> listarExtratos(
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(conciliacaoService.listarExtratos(pageable));
    }

    @GetMapping("/extratos/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Buscar extrato", description = "Busca um extrato específico com suas transações")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Extrato encontrado"),
        @ApiResponse(responseCode = "404", description = "Extrato não encontrado")
    })
    public ResponseEntity<ExtratoBancarioDTO> buscarExtrato(
            @Parameter(description = "ID do extrato", required = true)
            @PathVariable UUID id) {

        return ResponseEntity.ok(conciliacaoService.buscarExtrato(id));
    }

    @GetMapping("/extratos/{id}/transacoes")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar transações com sugestões",
               description = "Lista transações do extrato com sugestões de conciliação para créditos não conciliados")
    public ResponseEntity<List<TransacaoExtratoDTO>> listarTransacoesComSugestoes(
            @Parameter(description = "ID do extrato", required = true)
            @PathVariable UUID id) {

        return ResponseEntity.ok(conciliacaoService.listarTransacoesComSugestoes(id));
    }

    // ========== Operações de Conciliação ==========

    @PostMapping("/transacoes/{id}/conciliar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Conciliar transação", description = "Vincula uma transação do extrato a um pagamento")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transação conciliada"),
        @ApiResponse(responseCode = "400", description = "Transação já conciliada ou inválida"),
        @ApiResponse(responseCode = "404", description = "Transação ou pagamento não encontrado")
    })
    public ResponseEntity<TransacaoExtratoDTO> conciliarTransacao(
            @Parameter(description = "ID da transação do extrato", required = true)
            @PathVariable UUID id,
            @Parameter(description = "ID do pagamento para vincular", required = true)
            @RequestParam UUID pagamentoId) {

        return ResponseEntity.ok(conciliacaoService.conciliarTransacao(id, pagamentoId));
    }

    @PostMapping("/transacoes/{id}/ignorar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Ignorar transação",
               description = "Marca uma transação como ignorada (não tem correspondência no sistema)")
    public ResponseEntity<TransacaoExtratoDTO> ignorarTransacao(
            @Parameter(description = "ID da transação do extrato", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Observação explicando o motivo")
            @RequestParam(required = false) String observacao) {

        return ResponseEntity.ok(conciliacaoService.ignorarTransacao(id, observacao));
    }

    @PostMapping("/transacoes/{id}/desconciliar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Desfazer conciliação",
               description = "Remove a conciliação de uma transação, voltando-a para status não conciliada")
    public ResponseEntity<TransacaoExtratoDTO> desconciliarTransacao(
            @Parameter(description = "ID da transação do extrato", required = true)
            @PathVariable UUID id) {

        return ResponseEntity.ok(conciliacaoService.desconciliarTransacao(id));
    }

    // ========== Operações em Lote ==========

    @PostMapping("/lote")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Conciliação em lote",
               description = "Processa múltiplas conciliações e transações a ignorar de uma vez")
    @ApiResponse(responseCode = "200", description = "Resultado do processamento em lote",
                 content = @Content(schema = @Schema(implementation = Map.class)))
    public ResponseEntity<Map<String, Object>> conciliarEmLote(
            @Valid @RequestBody ConciliacaoRequestDTO.ConciliacaoLoteDTO request) {

        return ResponseEntity.ok(conciliacaoService.conciliarEmLote(request));
    }

    // ========== Estatísticas ==========

    @GetMapping("/extratos/{id}/resumo")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Resumo do extrato", description = "Retorna estatísticas de conciliação do extrato")
    public ResponseEntity<Map<String, Object>> resumoExtrato(
            @Parameter(description = "ID do extrato", required = true)
            @PathVariable UUID id) {

        ExtratoBancarioDTO extrato = conciliacaoService.buscarExtrato(id);

        Map<String, Object> resumo = Map.of(
            "totalTransacoes", extrato.getTotalTransacoes(),
            "totalConciliadas", extrato.getTotalConciliadas(),
            "totalPendentes", extrato.getTotalPendentes(),
            "percentualConciliado", extrato.getPercentualConciliado(),
            "status", extrato.getStatus(),
            "periodo", Map.of(
                "inicio", extrato.getDataInicio(),
                "fim", extrato.getDataFim()
            ),
            "saldos", Map.of(
                "inicial", extrato.getSaldoInicial() != null ? extrato.getSaldoInicial() : 0,
                "final", extrato.getSaldoFinal() != null ? extrato.getSaldoFinal() : 0
            )
        );

        return ResponseEntity.ok(resumo);
    }
}
