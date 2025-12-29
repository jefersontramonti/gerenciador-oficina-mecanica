package com.pitstop.saas.dto;

import com.pitstop.saas.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TicketDetailDTO(
    UUID id,
    String numero,

    // Oficina
    UUID oficinaId,
    String oficinaNome,
    String oficinaCnpj,
    String oficinaEmail,

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
    String atribuidoAEmail,

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
    List<MensagemTicketDTO> mensagens,
    int totalMensagens
) {
    public static TicketDetailDTO fromEntity(Ticket ticket) {
        List<MensagemTicketDTO> mensagensDTO = ticket.getMensagens() != null
            ? ticket.getMensagens().stream().map(MensagemTicketDTO::fromEntity).toList()
            : List.of();

        return new TicketDetailDTO(
            ticket.getId(),
            ticket.getNumero(),
            ticket.getOficina() != null ? ticket.getOficina().getId() : null,
            ticket.getOficina() != null ? ticket.getOficina().getNomeFantasia() : null,
            ticket.getOficina() != null ? ticket.getOficina().getCnpjCpf() : null,
            ticket.getOficina() != null && ticket.getOficina().getContato() != null ? ticket.getOficina().getContato().getEmail() : null,
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
            ticket.getAtribuidoA() != null ? ticket.getAtribuidoA().getEmail() : null,
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
            mensagensDTO,
            mensagensDTO.size()
        );
    }
}
