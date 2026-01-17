package com.pitstop.manutencaopreventiva.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para remarcar agendamento.
 */
public record RemarcarAgendamentoRequestDTO(
    @NotNull(message = "Nova data é obrigatória")
    LocalDate novaData,

    @NotNull(message = "Nova hora é obrigatória")
    LocalTime novaHora,

    String motivo
) {}
