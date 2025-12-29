package com.pitstop.saas.dto;

import com.pitstop.saas.domain.StatusTicket;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusTicketRequest(
    @NotNull(message = "Status é obrigatório")
    StatusTicket status
) {}
