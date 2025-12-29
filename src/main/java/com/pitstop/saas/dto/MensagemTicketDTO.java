package com.pitstop.saas.dto;

import com.pitstop.saas.domain.MensagemTicket;
import com.pitstop.saas.domain.TipoAutorMensagem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MensagemTicketDTO(
    UUID id,
    UUID ticketId,
    UUID autorId,
    String autorNome,
    TipoAutorMensagem autorTipo,
    String autorTipoDescricao,
    boolean isInterno,
    String conteudo,
    List<String> anexos,
    LocalDateTime criadoEm
) {
    public static MensagemTicketDTO fromEntity(MensagemTicket mensagem) {
        return new MensagemTicketDTO(
            mensagem.getId(),
            mensagem.getTicket() != null ? mensagem.getTicket().getId() : null,
            mensagem.getAutorId(),
            mensagem.getAutorNome(),
            mensagem.getAutorTipo(),
            mensagem.getAutorTipo().getDescricao(),
            mensagem.getIsInterno(),
            mensagem.getConteudo(),
            mensagem.getAnexos(),
            mensagem.getCriadoEm()
        );
    }
}
