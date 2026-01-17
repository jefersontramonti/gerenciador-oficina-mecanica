package com.pitstop.manutencaopreventiva.controller;

import com.pitstop.manutencaopreventiva.domain.StatusAgendamento;
import com.pitstop.manutencaopreventiva.dto.*;
import com.pitstop.manutencaopreventiva.service.AgendamentoManutencaoService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/manutencao-preventiva/agendamentos")
@RequiredArgsConstructor
@Tag(name = "Manutenção Preventiva - Agendamentos", description = "Gerenciamento de agendamentos de manutenção")
@PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
@RequiresFeature("MANUTENCAO_PREVENTIVA")
public class AgendamentoManutencaoController {

    private final AgendamentoManutencaoService agendamentoService;

    @GetMapping
    @Operation(summary = "Listar agendamentos com filtros")
    public ResponseEntity<Page<AgendamentoManutencaoResponseDTO>> listar(
            @RequestParam(required = false) UUID veiculoId,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @PageableDefault(size = 20, sort = "dataAgendamento", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(agendamentoService.listar(
            veiculoId, clienteId, status, dataInicio, dataFim, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar agendamento por ID")
    public ResponseEntity<AgendamentoManutencaoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(agendamentoService.buscarPorId(id));
    }

    @GetMapping("/hoje")
    @Operation(summary = "Listar agendamentos de hoje")
    public ResponseEntity<List<AgendamentoManutencaoResponseDTO>> listarHoje() {
        return ResponseEntity.ok(agendamentoService.listarAgendamentosDoDia());
    }

    @GetMapping("/proximos")
    @Operation(summary = "Listar próximos agendamentos")
    public ResponseEntity<List<AgendamentoManutencaoResponseDTO>> listarProximos(
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(agendamentoService.listarProximos(limite));
    }

    @GetMapping("/calendario")
    @Operation(summary = "Listar eventos para calendário")
    public ResponseEntity<List<CalendarioEventoDTO>> listarCalendario(
            @RequestParam int mes,
            @RequestParam int ano) {
        return ResponseEntity.ok(agendamentoService.listarCalendario(mes, ano));
    }

    @PostMapping
    @Operation(summary = "Criar agendamento")
    public ResponseEntity<AgendamentoManutencaoResponseDTO> criar(
            @Valid @RequestBody AgendamentoManutencaoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamentoService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar agendamento")
    public ResponseEntity<AgendamentoManutencaoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AgendamentoManutencaoRequestDTO request) {
        return ResponseEntity.ok(agendamentoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar agendamento (interno)")
    public ResponseEntity<AgendamentoManutencaoResponseDTO> confirmar(@PathVariable UUID id) {
        return ResponseEntity.ok(agendamentoService.confirmar(id));
    }

    @GetMapping("/confirmar/{token}")
    @Operation(summary = "Confirmar agendamento via link (público)")
    @PreAuthorize("permitAll()")
    public ResponseEntity<AgendamentoManutencaoResponseDTO> confirmarPorToken(@PathVariable String token) {
        return ResponseEntity.ok(agendamentoService.confirmarPorToken(token));
    }

    @PatchMapping("/{id}/remarcar")
    @Operation(summary = "Remarcar agendamento")
    public ResponseEntity<AgendamentoManutencaoResponseDTO> remarcar(
            @PathVariable UUID id,
            @Valid @RequestBody RemarcarAgendamentoRequestDTO request) {
        return ResponseEntity.ok(agendamentoService.remarcar(id, request));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar agendamento")
    public ResponseEntity<AgendamentoManutencaoResponseDTO> cancelar(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String motivo = body != null ? body.get("motivo") : null;
        return ResponseEntity.ok(agendamentoService.cancelar(id, motivo));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar agendamento")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        agendamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
