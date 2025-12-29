package com.pitstop.saas.dto;

import com.pitstop.saas.domain.PrioridadeTicket;
import com.pitstop.saas.domain.StatusTicket;
import com.pitstop.saas.domain.TipoTicket;

import java.util.UUID;

public record TicketFilterRequest(
    UUID oficinaId,
    StatusTicket status,
    TipoTicket tipo,
    PrioridadeTicket prioridade,
    UUID atribuidoA,
    String busca,
    int page,
    int size
) {
    public TicketFilterRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
    }
}
