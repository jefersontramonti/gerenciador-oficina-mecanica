package com.pitstop.financeiro.controller;

import com.pitstop.financeiro.domain.StatusNotaFiscal;
import com.pitstop.financeiro.dto.NotaFiscalRequestDTO;
import com.pitstop.financeiro.dto.NotaFiscalResponseDTO;
import com.pitstop.financeiro.dto.NotaFiscalResumoDTO;
import com.pitstop.financeiro.service.NotaFiscalService;
import io.swagger.v3.oas.annotations.Operation;
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
 * Controller REST para Notas Fiscais.
 *
 * <p><strong>⚠️ IMPLEMENTAÇÃO BÁSICA - MVP ⚠️</strong></p>
 * <p>Esta implementação fornece apenas CRUD básico para Notas Fiscais.
 * Não inclui integração com SEFAZ (emissão, autorização, cancelamento).
 * A integração completa está planejada para Phase 3.</p>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-23
 */
@RestController
@RequestMapping("/api/notas-fiscais")
@RequiredArgsConstructor
@Tag(name = "Notas Fiscais", description = "Endpoints para gerenciamento de notas fiscais (CRUD básico)")
public class NotaFiscalController {

    private final NotaFiscalService notaFiscalService;

    /**
     * Cria uma nova nota fiscal.
     *
     * @param dto dados da nota fiscal
     * @return nota fiscal criada
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Criar nota fiscal", description = "Cria uma nova nota fiscal em modo digitação")
    public ResponseEntity<NotaFiscalResponseDTO> criar(@Valid @RequestBody NotaFiscalRequestDTO dto) {
        NotaFiscalResponseDTO response = notaFiscalService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Busca nota fiscal por ID.
     *
     * @param id ID da nota fiscal
     * @return nota fiscal encontrada
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar nota fiscal por ID")
    public ResponseEntity<NotaFiscalResponseDTO> buscarPorId(@PathVariable UUID id) {
        NotaFiscalResponseDTO response = notaFiscalService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista todas as notas fiscais com paginação.
     *
     * @param pageable paginação
     * @return página de notas fiscais
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar notas fiscais", description = "Lista todas as notas fiscais com paginação")
    public ResponseEntity<Page<NotaFiscalResumoDTO>> listar(
        @PageableDefault(size = 20, sort = "dataEmissao") Pageable pageable
    ) {
        Page<NotaFiscalResumoDTO> response = notaFiscalService.listar(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Busca notas fiscais por ordem de serviço.
     *
     * @param ordemServicoId ID da OS
     * @return lista de notas fiscais
     */
    @GetMapping("/ordem-servico/{ordemServicoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Buscar notas fiscais por OS")
    public ResponseEntity<List<NotaFiscalResponseDTO>> buscarPorOrdemServico(
        @PathVariable UUID ordemServicoId
    ) {
        List<NotaFiscalResponseDTO> response = notaFiscalService.buscarPorOrdemServico(ordemServicoId);
        return ResponseEntity.ok(response);
    }

    /**
     * Busca notas fiscais por status.
     *
     * @param status status da nota
     * @param pageable paginação
     * @return página de notas fiscais
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Buscar notas fiscais por status")
    public ResponseEntity<Page<NotaFiscalResumoDTO>> buscarPorStatus(
        @PathVariable StatusNotaFiscal status,
        @PageableDefault(size = 20, sort = "dataEmissao") Pageable pageable
    ) {
        Page<NotaFiscalResumoDTO> response = notaFiscalService.buscarPorStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Busca nota fiscal por número e série.
     *
     * @param numero número da nota
     * @param serie série da nota
     * @return nota fiscal encontrada
     */
    @GetMapping("/numero/{numero}/serie/{serie}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Buscar nota fiscal por número e série")
    public ResponseEntity<NotaFiscalResponseDTO> buscarPorNumeroESerie(
        @PathVariable Long numero,
        @PathVariable Integer serie
    ) {
        NotaFiscalResponseDTO response = notaFiscalService.buscarPorNumeroESerie(numero, serie);
        return ResponseEntity.ok(response);
    }

    /**
     * Busca nota fiscal por chave de acesso.
     *
     * @param chaveAcesso chave de acesso da NFe
     * @return nota fiscal encontrada
     */
    @GetMapping("/chave-acesso/{chaveAcesso}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Buscar nota fiscal por chave de acesso")
    public ResponseEntity<NotaFiscalResponseDTO> buscarPorChaveAcesso(
        @PathVariable String chaveAcesso
    ) {
        NotaFiscalResponseDTO response = notaFiscalService.buscarPorChaveAcesso(chaveAcesso);
        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza uma nota fiscal.
     *
     * @param id ID da nota fiscal
     * @param dto dados atualizados
     * @return nota fiscal atualizada
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Atualizar nota fiscal", description = "Atualiza uma nota fiscal (apenas em digitação)")
    public ResponseEntity<NotaFiscalResponseDTO> atualizar(
        @PathVariable UUID id,
        @Valid @RequestBody NotaFiscalRequestDTO dto
    ) {
        NotaFiscalResponseDTO response = notaFiscalService.atualizar(id, dto);
        return ResponseEntity.ok(response);
    }

    /**
     * Deleta uma nota fiscal.
     *
     * @param id ID da nota fiscal
     * @return resposta vazia
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Deletar nota fiscal", description = "Deleta uma nota fiscal (apenas em digitação)")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        notaFiscalService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verifica se existe nota fiscal para uma OS.
     *
     * @param ordemServicoId ID da OS
     * @return true se existe
     */
    @GetMapping("/existe/ordem-servico/{ordemServicoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Verificar se existe nota fiscal para OS")
    public ResponseEntity<Boolean> existeNotaFiscalParaOS(@PathVariable UUID ordemServicoId) {
        boolean existe = notaFiscalService.existeNotaFiscalParaOS(ordemServicoId);
        return ResponseEntity.ok(existe);
    }

    /**
     * Busca o próximo número disponível para uma série.
     *
     * @param serie série da nota
     * @return próximo número disponível
     */
    @GetMapping("/proximo-numero/serie/{serie}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Buscar próximo número disponível")
    public ResponseEntity<Long> buscarProximoNumero(@PathVariable Integer serie) {
        Long proximoNumero = notaFiscalService.buscarProximoNumero(serie);
        return ResponseEntity.ok(proximoNumero);
    }
}
