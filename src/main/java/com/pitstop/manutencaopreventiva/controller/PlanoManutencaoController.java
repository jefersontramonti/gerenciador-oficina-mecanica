package com.pitstop.manutencaopreventiva.controller;

import com.pitstop.manutencaopreventiva.domain.StatusPlanoManutencao;
import com.pitstop.manutencaopreventiva.dto.*;
import com.pitstop.manutencaopreventiva.service.PlanoManutencaoService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/manutencao-preventiva/planos")
@RequiredArgsConstructor
@Tag(name = "Manutenção Preventiva - Planos", description = "Gerenciamento de planos de manutenção preventiva")
@PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
@RequiresFeature("MANUTENCAO_PREVENTIVA")
public class PlanoManutencaoController {

    private final PlanoManutencaoService planoService;

    @GetMapping
    @Operation(summary = "Listar planos de manutenção")
    public ResponseEntity<Page<PlanoManutencaoResponseDTO>> listar(
            @RequestParam(required = false) UUID veiculoId,
            @RequestParam(required = false) StatusPlanoManutencao status,
            @RequestParam(required = false) String tipoManutencao,
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 20, sort = "proximaPrevisaoData", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(planoService.listar(veiculoId, status, tipoManutencao, busca, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar plano por ID")
    public ResponseEntity<PlanoManutencaoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(planoService.buscarPorId(id));
    }

    @GetMapping("/veiculo/{veiculoId}")
    @Operation(summary = "Listar planos de um veículo")
    public ResponseEntity<List<PlanoManutencaoResponseDTO>> listarPorVeiculo(@PathVariable UUID veiculoId) {
        return ResponseEntity.ok(planoService.listarPorVeiculo(veiculoId));
    }

    @GetMapping("/vencidos")
    @Operation(summary = "Listar planos vencidos")
    public ResponseEntity<List<PlanoManutencaoResponseDTO>> listarVencidos() {
        return ResponseEntity.ok(planoService.listarVencidos());
    }

    @GetMapping("/proximos-vencer")
    @Operation(summary = "Listar planos próximos a vencer")
    public ResponseEntity<List<PlanoManutencaoResponseDTO>> listarProximosAVencer(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(planoService.listarProximosAVencer(dias));
    }

    @PostMapping
    @Operation(summary = "Criar plano de manutenção")
    public ResponseEntity<PlanoManutencaoResponseDTO> criar(
            @Valid @RequestBody PlanoManutencaoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planoService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar plano de manutenção")
    public ResponseEntity<PlanoManutencaoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PlanoManutencaoRequestDTO request) {
        return ResponseEntity.ok(planoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Ativar plano")
    public ResponseEntity<PlanoManutencaoResponseDTO> ativar(@PathVariable UUID id) {
        return ResponseEntity.ok(planoService.ativar(id));
    }

    @PatchMapping("/{id}/pausar")
    @Operation(summary = "Pausar plano")
    public ResponseEntity<PlanoManutencaoResponseDTO> pausar(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String motivo = body != null ? body.get("motivo") : null;
        return ResponseEntity.ok(planoService.pausar(id, motivo));
    }

    @PatchMapping("/{id}/concluir")
    @Operation(summary = "Concluir plano")
    public ResponseEntity<PlanoManutencaoResponseDTO> concluir(@PathVariable UUID id) {
        return ResponseEntity.ok(planoService.concluir(id));
    }

    @PostMapping("/{id}/executar")
    @Operation(summary = "Registrar execução de manutenção")
    public ResponseEntity<PlanoManutencaoResponseDTO> executar(
            @PathVariable UUID id,
            @Valid @RequestBody ExecutarPlanoRequestDTO request) {
        return ResponseEntity.ok(planoService.executar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar plano de manutenção")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        planoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
