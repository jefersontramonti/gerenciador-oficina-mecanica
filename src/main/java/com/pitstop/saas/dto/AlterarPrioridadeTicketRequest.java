package com.pitstop.saas.dto;

import com.pitstop.saas.domain.PrioridadeTicket;
import jakarta.validation.constraints.NotNull;

public record AlterarPrioridadeTicketRequest(
    @NotNull(message = "Prioridade é obrigatória")
    PrioridadeTicket prioridade
) {}
