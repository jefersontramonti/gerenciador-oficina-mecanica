package com.pitstop.saas.controller;

import com.pitstop.saas.domain.PrioridadeTicket;
import com.pitstop.saas.domain.StatusTicket;
import com.pitstop.saas.domain.TipoTicket;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/saas/tickets")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class SaasTicketController {

    private final TicketService ticketService;

    /**
     * Lista todos os tickets com filtros
     */
    @GetMapping
    public ResponseEntity<Page<TicketDTO>> listarTickets(
        @RequestParam(required = false) UUID oficinaId,
        @RequestParam(required = false) StatusTicket status,
        @RequestParam(required = false) TipoTicket tipo,
        @RequestParam(required = false) PrioridadeTicket prioridade,
        @RequestParam(required = false) UUID atribuidoA,
        @RequestParam(required = false) String busca,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Listando tickets - page: {}, size: {}, status: {}, tipo: {}", page, size, status, tipo);

        TicketFilterRequest filter = new TicketFilterRequest(
            oficinaId, status, tipo, prioridade, atribuidoA, busca, page, size
        );

        Page<TicketDTO> tickets = ticketService.listarTickets(filter);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Busca ticket por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TicketDetailDTO> buscarPorId(@PathVariable UUID id) {
        log.info("Buscando ticket por ID: {}", id);
        TicketDetailDTO ticket = ticketService.buscarTicketPorId(id);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Busca ticket por número
     */
    @GetMapping("/numero/{numero}")
    public ResponseEntity<TicketDetailDTO> buscarPorNumero(@PathVariable String numero) {
        log.info("Buscando ticket por número: {}", numero);
        TicketDetailDTO ticket = ticketService.buscarTicketPorNumero(numero);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Cria um novo ticket
     */
    @PostMapping
    public ResponseEntity<TicketDTO> criarTicket(@Valid @RequestBody CreateTicketRequest request) {
        log.info("Criando ticket para: {}", request.usuarioEmail());
        TicketDTO ticket = ticketService.criarTicket(request);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Responde a um ticket
     */
    @PostMapping("/{id}/responder")
    public ResponseEntity<MensagemTicketDTO> responderTicket(
        @PathVariable UUID id,
        @Valid @RequestBody ResponderTicketRequest request
    ) {
        log.info("Respondendo ticket: {}", id);
        MensagemTicketDTO mensagem = ticketService.responderTicket(id, request);
        return ResponseEntity.ok(mensagem);
    }

    /**
     * Atribui um ticket a um usuário
     */
    @PostMapping("/{id}/atribuir")
    public ResponseEntity<TicketDTO> atribuirTicket(
        @PathVariable UUID id,
        @RequestBody AtribuirTicketRequest request
    ) {
        log.info("Atribuindo ticket {} para {}", id, request.atribuidoA());
        TicketDTO ticket = ticketService.atribuirTicket(id, request);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Altera o status de um ticket
     */
    @PostMapping("/{id}/alterar-status")
    public ResponseEntity<TicketDTO> alterarStatus(
        @PathVariable UUID id,
        @Valid @RequestBody AlterarStatusTicketRequest request
    ) {
        log.info("Alterando status do ticket {} para {}", id, request.status());
        TicketDTO ticket = ticketService.alterarStatus(id, request);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Altera a prioridade de um ticket
     */
    @PostMapping("/{id}/alterar-prioridade")
    public ResponseEntity<TicketDTO> alterarPrioridade(
        @PathVariable UUID id,
        @Valid @RequestBody AlterarPrioridadeTicketRequest request
    ) {
        log.info("Alterando prioridade do ticket {} para {}", id, request.prioridade());
        TicketDTO ticket = ticketService.alterarPrioridade(id, request);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Retorna métricas dos tickets
     */
    @GetMapping("/metricas")
    public ResponseEntity<TicketMetricasDTO> getMetricas() {
        log.info("Buscando métricas de tickets");
        TicketMetricasDTO metricas = ticketService.getMetricas();
        return ResponseEntity.ok(metricas);
    }

    /**
     * Retorna os enums para uso no frontend
     */
    @GetMapping("/enums")
    public ResponseEntity<TicketEnumsDTO> getEnums() {
        return ResponseEntity.ok(new TicketEnumsDTO(
            TipoTicket.values(),
            PrioridadeTicket.values(),
            StatusTicket.values()
        ));
    }

    public record TicketEnumsDTO(
        TipoTicket[] tipos,
        PrioridadeTicket[] prioridades,
        StatusTicket[] status
    ) {}
}
