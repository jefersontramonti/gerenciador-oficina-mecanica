package com.pitstop.notificacao.controller;

import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.StatusNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import com.pitstop.notificacao.dto.HistoricoNotificacaoDTO;
import com.pitstop.notificacao.dto.NotificacaoMetricasDTO;
import com.pitstop.notificacao.service.HistoricoNotificacaoService;
import com.pitstop.notificacao.service.WhatsAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST para historico de notificacoes.
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/notificacoes/historico")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Historico de Notificacoes", description = "Consulta e gerenciamento do historico de notificacoes")
@SecurityRequirement(name = "bearer-jwt")
public class HistoricoNotificacaoController {

    private final HistoricoNotificacaoService historicoService;
    private final WhatsAppService whatsAppService;

    /**
     * Lista historico com paginacao e filtros opcionais.
     * GET /api/notificacoes/historico
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar historico", description = "Lista historico de notificacoes com paginacao e filtros")
    public ResponseEntity<Page<HistoricoNotificacaoDTO.Resumido>> listar(
        @RequestParam(required = false) TipoNotificacao tipo,
        @RequestParam(required = false) StatusNotificacao status,
        @RequestParam(required = false) EventoNotificacao evento,
        @RequestParam(required = false) String destinatario,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        log.debug("GET /api/notificacoes/historico - tipo={}, status={}, evento={}, destinatario={}",
            tipo, status, evento, destinatario);

        // Usa metodo com filtros combinados
        Page<HistoricoNotificacaoDTO.Resumido> page = historicoService.listarComFiltros(tipo, status, evento, pageable);

        return ResponseEntity.ok(page);
    }

    /**
     * Lista historico por status.
     * GET /api/notificacoes/historico/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar por status", description = "Lista notificacoes filtradas por status")
    public ResponseEntity<Page<HistoricoNotificacaoDTO.Resumido>> listarPorStatus(
        @PathVariable StatusNotificacao status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        log.debug("GET /api/notificacoes/historico/status/{}", status);
        Page<HistoricoNotificacaoDTO.Resumido> page = historicoService.listarPorStatus(status, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Lista historico por evento.
     * GET /api/notificacoes/historico/evento/{evento}
     */
    @GetMapping("/evento/{evento}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar por evento", description = "Lista notificacoes filtradas por evento")
    public ResponseEntity<Page<HistoricoNotificacaoDTO.Resumido>> listarPorEvento(
        @PathVariable EventoNotificacao evento,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        log.debug("GET /api/notificacoes/historico/evento/{}", evento);
        Page<HistoricoNotificacaoDTO.Resumido> page = historicoService.listarPorEvento(evento, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Lista historico por canal.
     * GET /api/notificacoes/historico/canal/{canal}
     */
    @GetMapping("/canal/{canal}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Listar por canal", description = "Lista notificacoes filtradas por canal")
    public ResponseEntity<Page<HistoricoNotificacaoDTO.Resumido>> listarPorCanal(
        @PathVariable TipoNotificacao canal,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        log.debug("GET /api/notificacoes/historico/canal/{}", canal);
        Page<HistoricoNotificacaoDTO.Resumido> page = historicoService.listarPorCanal(canal, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Busca detalhes de uma notificacao.
     * GET /api/notificacoes/historico/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Buscar por ID", description = "Retorna detalhes completos de uma notificacao")
    public ResponseEntity<HistoricoNotificacaoDTO> buscarPorId(@PathVariable UUID id) {
        log.debug("GET /api/notificacoes/historico/{}", id);
        HistoricoNotificacaoDTO historico = historicoService.buscarPorId(id);
        return ResponseEntity.ok(historico);
    }

    /**
     * Lista notificacoes de uma OS.
     * GET /api/notificacoes/historico/os/{ordemServicoId}
     */
    @GetMapping("/os/{ordemServicoId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Notificacoes da OS", description = "Lista todas as notificacoes de uma ordem de servico")
    public ResponseEntity<List<HistoricoNotificacaoDTO.Resumido>> listarPorOrdemServico(
        @PathVariable UUID ordemServicoId
    ) {
        log.debug("GET /api/notificacoes/historico/os/{}", ordemServicoId);
        List<HistoricoNotificacaoDTO.Resumido> lista = historicoService.listarPorOrdemServico(ordemServicoId);
        return ResponseEntity.ok(lista);
    }

    /**
     * Lista notificacoes de um cliente.
     * GET /api/notificacoes/historico/cliente/{clienteId}
     */
    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Notificacoes do cliente", description = "Lista notificacoes enviadas para um cliente")
    public ResponseEntity<Page<HistoricoNotificacaoDTO.Resumido>> listarPorCliente(
        @PathVariable UUID clienteId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        log.debug("GET /api/notificacoes/historico/cliente/{}", clienteId);
        Page<HistoricoNotificacaoDTO.Resumido> page = historicoService.listarPorCliente(clienteId, pageable);
        return ResponseEntity.ok(page);
    }

    // ===== REENVIO =====

    /**
     * Reenvia uma notificacao que falhou.
     * POST /api/notificacoes/historico/{id}/reenviar
     */
    @PostMapping("/{id}/reenviar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Reenviar notificacao", description = "Reenvia uma notificacao que falhou")
    public ResponseEntity<HistoricoNotificacaoDTO> reenviar(@PathVariable UUID id) {
        log.info("POST /api/notificacoes/historico/{}/reenviar", id);

        HistoricoNotificacaoDTO historico = historicoService.buscarPorId(id);

        // Verifica o canal para saber qual servico usar
        if (historico.tipoNotificacao() == TipoNotificacao.WHATSAPP) {
            var resultado = whatsAppService.reenviar(id);
            return ResponseEntity.ok(HistoricoNotificacaoDTO.fromEntity(resultado));
        }

        // Para outros canais (email), retorna erro por enquanto
        throw new UnsupportedOperationException("Reenvio ainda nao suportado para " + historico.tipoNotificacao());
    }

    // ===== METRICAS =====

    /**
     * Obtem metricas de notificacoes.
     * GET /api/notificacoes/historico/metricas
     */
    @GetMapping("/metricas")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Metricas", description = "Retorna metricas de notificacoes para um periodo")
    public ResponseEntity<NotificacaoMetricasDTO> getMetricas(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        log.debug("GET /api/notificacoes/historico/metricas - {} a {}", dataInicio, dataFim);
        NotificacaoMetricasDTO metricas = historicoService.getMetricas(dataInicio, dataFim);
        return ResponseEntity.ok(metricas);
    }

    /**
     * Obtem contadores para dashboard.
     * GET /api/notificacoes/historico/dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Contadores dashboard", description = "Retorna contadores para exibicao no dashboard")
    public ResponseEntity<Map<String, Long>> getDashboard() {
        log.debug("GET /api/notificacoes/historico/dashboard");
        Map<String, Long> contadores = Map.of(
            "total", historicoService.contarTotal(),
            "enviadas", historicoService.contarEnviadas(),
            "falhas", historicoService.contarFalhas()
        );
        return ResponseEntity.ok(contadores);
    }

    // ===== LIMPEZA =====

    /**
     * Limpa historico antigo.
     * DELETE /api/notificacoes/historico/limpeza
     */
    @DeleteMapping("/limpeza")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Limpar historico", description = "Remove registros antigos do historico")
    public ResponseEntity<Map<String, Integer>> limparHistorico(
        @RequestParam(defaultValue = "90") int diasRetencao
    ) {
        log.info("DELETE /api/notificacoes/historico/limpeza - Dias retencao: {}", diasRetencao);
        int removidos = historicoService.limparHistoricoAntigo(diasRetencao);
        return ResponseEntity.ok(Map.of("removidos", removidos));
    }
}
