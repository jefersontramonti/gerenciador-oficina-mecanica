package com.pitstop.saas.dto;

import com.pitstop.saas.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TicketDTO(
    UUID id,
    String numero,

    // Oficina
    UUID oficinaId,
    String oficinaNome,

    // Solicitante
    UUID usuarioId,
    String usuarioNome,
    String usuarioEmail,

    // Classificação
    TipoTicket tipo,
    String tipoDescricao,
    PrioridadeTicket prioridade,
    String prioridadeDescricao,
    StatusTicket status,
    String statusDescricao,

    // Conteúdo
    String assunto,
    String descricao,
    List<String> anexos,

    // Atribuição
    UUID atribuidoAId,
    String atribuidoANome,

    // SLA
    Integer slaMinutos,
    LocalDateTime respostaInicialEm,
    Integer tempoRespostaMinutos,
    boolean slaCumprido,
    boolean slaVencido,
    Long minutosRestantesSla,

    // Datas
    LocalDateTime aberturaEm,
    LocalDateTime atualizadoEm,
    LocalDateTime resolvidoEm,
    LocalDateTime fechadoEm,

    // Mensagens
    int totalMensagens,
    MensagemTicketDTO ultimaMensagem
) {
    public static TicketDTO fromEntity(Ticket ticket) {
        return new TicketDTO(
            ticket.getId(),
            ticket.getNumero(),
            ticket.getOficina() != null ? ticket.getOficina().getId() : null,
            ticket.getOficina() != null ? ticket.getOficina().getNomeFantasia() : null,
            ticket.getUsuarioId(),
            ticket.getUsuarioNome(),
            ticket.getUsuarioEmail(),
            ticket.getTipo(),
            ticket.getTipo().getDescricao(),
            ticket.getPrioridade(),
            ticket.getPrioridade().getDescricao(),
            ticket.getStatus(),
            ticket.getStatus().getDescricao(),
            ticket.getAssunto(),
            ticket.getDescricao(),
            ticket.getAnexos(),
            ticket.getAtribuidoA() != null ? ticket.getAtribuidoA().getId() : null,
            ticket.getAtribuidoA() != null ? ticket.getAtribuidoA().getNome() : null,
            ticket.getSlaMinutos(),
            ticket.getRespostaInicialEm(),
            ticket.getTempoRespostaMinutos(),
            ticket.slaCumprido(),
            ticket.slaVencido(),
            ticket.minutosRestantesSla(),
            ticket.getAberturaEm(),
            ticket.getAtualizadoEm(),
            ticket.getResolvidoEm(),
            ticket.getFechadoEm(),
            ticket.getMensagens() != null ? ticket.getMensagens().size() : 0,
            ticket.getMensagens() != null && !ticket.getMensagens().isEmpty()
                ? MensagemTicketDTO.fromEntity(ticket.getMensagens().get(ticket.getMensagens().size() - 1))
                : null
        );
    }

    public static TicketDTO fromEntitySimples(Ticket ticket) {
        return new TicketDTO(
            ticket.getId(),
            ticket.getNumero(),
            ticket.getOficina() != null ? ticket.getOficina().getId() : null,
            ticket.getOficina() != null ? ticket.getOficina().getNomeFantasia() : null,
            ticket.getUsuarioId(),
            ticket.getUsuarioNome(),
            ticket.getUsuarioEmail(),
            ticket.getTipo(),
            ticket.getTipo().getDescricao(),
            ticket.getPrioridade(),
            ticket.getPrioridade().getDescricao(),
            ticket.getStatus(),
            ticket.getStatus().getDescricao(),
            ticket.getAssunto(),
            null, // Não incluir descrição na listagem
            null, // Não incluir anexos na listagem
            ticket.getAtribuidoA() != null ? ticket.getAtribuidoA().getId() : null,
            ticket.getAtribuidoA() != null ? ticket.getAtribuidoA().getNome() : null,
            ticket.getSlaMinutos(),
            ticket.getRespostaInicialEm(),
            ticket.getTempoRespostaMinutos(),
            ticket.slaCumprido(),
            ticket.slaVencido(),
            ticket.minutosRestantesSla(),
            ticket.getAberturaEm(),
            ticket.getAtualizadoEm(),
            ticket.getResolvidoEm(),
            ticket.getFechadoEm(),
            0,
            null
        );
    }
}
