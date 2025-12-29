package com.pitstop.saas.controller;

import com.pitstop.saas.domain.PrioridadeComunicado;
import com.pitstop.saas.domain.StatusComunicado;
import com.pitstop.saas.domain.TipoComunicado;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.service.ComunicadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/saas/comunicados")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class ComunicadoController {

    private final ComunicadoService comunicadoService;

    /**
     * Lista todos os comunicados com filtros
     */
    @GetMapping
    public ResponseEntity<Page<ComunicadoDTO>> listarComunicados(
        @RequestParam(required = false) StatusComunicado status,
        @RequestParam(required = false) TipoComunicado tipo,
        @RequestParam(required = false) PrioridadeComunicado prioridade,
        @RequestParam(required = false) String busca,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Listando comunicados - page: {}, size: {}, status: {}", page, size, status);

        ComunicadoFilterRequest filter = new ComunicadoFilterRequest(
            status, tipo, prioridade, busca, page, size
        );

        Page<ComunicadoDTO> comunicados = comunicadoService.listarComunicados(filter);
        return ResponseEntity.ok(comunicados);
    }

    /**
     * Busca comunicado por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ComunicadoDetailDTO> buscarPorId(@PathVariable UUID id) {
        log.info("Buscando comunicado por ID: {}", id);
        ComunicadoDetailDTO comunicado = comunicadoService.buscarPorId(id);
        return ResponseEntity.ok(comunicado);
    }

    /**
     * Cria um novo comunicado
     */
    @PostMapping
    public ResponseEntity<ComunicadoDTO> criarComunicado(@Valid @RequestBody CreateComunicadoRequest request) {
        log.info("Criando comunicado: {}", request.titulo());
        ComunicadoDTO comunicado = comunicadoService.criarComunicado(request);
        return ResponseEntity.ok(comunicado);
    }

    /**
     * Atualiza um comunicado
     */
    @PutMapping("/{id}")
    public ResponseEntity<ComunicadoDTO> atualizarComunicado(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateComunicadoRequest request
    ) {
        log.info("Atualizando comunicado: {}", id);
        ComunicadoDTO comunicado = comunicadoService.atualizarComunicado(id, request);
        return ResponseEntity.ok(comunicado);
    }

    /**
     * Envia um comunicado imediatamente
     */
    @PostMapping("/{id}/enviar")
    public ResponseEntity<ComunicadoDTO> enviarComunicado(@PathVariable UUID id) {
        log.info("Enviando comunicado: {}", id);
        ComunicadoDTO comunicado = comunicadoService.enviarComunicado(id);
        return ResponseEntity.ok(comunicado);
    }

    /**
     * Agenda um comunicado
     */
    @PostMapping("/{id}/agendar")
    public ResponseEntity<ComunicadoDTO> agendarComunicado(
        @PathVariable UUID id,
        @RequestBody AgendarComunicadoRequest request
    ) {
        log.info("Agendando comunicado {} para {}", id, request.dataAgendamento());
        ComunicadoDTO comunicado = comunicadoService.agendarComunicado(id, request.dataAgendamento());
        return ResponseEntity.ok(comunicado);
    }

    /**
     * Cancela um comunicado agendado
     */
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ComunicadoDTO> cancelarComunicado(@PathVariable UUID id) {
        log.info("Cancelando comunicado: {}", id);
        ComunicadoDTO comunicado = comunicadoService.cancelarComunicado(id);
        return ResponseEntity.ok(comunicado);
    }

    /**
     * Exclui um comunicado (apenas rascunhos)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirComunicado(@PathVariable UUID id) {
        log.info("Excluindo comunicado: {}", id);
        comunicadoService.excluirComunicado(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retorna métricas dos comunicados
     */
    @GetMapping("/metricas")
    public ResponseEntity<ComunicadoMetricasDTO> getMetricas() {
        log.info("Buscando métricas de comunicados");
        ComunicadoMetricasDTO metricas = comunicadoService.getMetricas();
        return ResponseEntity.ok(metricas);
    }

    /**
     * Retorna os enums para uso no frontend
     */
    @GetMapping("/enums")
    public ResponseEntity<ComunicadoService.ComunicadoEnumsDTO> getEnums() {
        return ResponseEntity.ok(comunicadoService.getEnums());
    }

    /**
     * Processa comunicados agendados manualmente
     */
    @PostMapping("/processar-agendados")
    public ResponseEntity<ProcessarAgendadosResponse> processarAgendados() {
        log.info("Processando comunicados agendados manualmente");
        int count = comunicadoService.processarAgendados();
        return ResponseEntity.ok(new ProcessarAgendadosResponse(count));
    }

    // DTOs internos
    public record AgendarComunicadoRequest(OffsetDateTime dataAgendamento) {}
    public record ProcessarAgendadosResponse(int comunicadosEnviados) {}
}
