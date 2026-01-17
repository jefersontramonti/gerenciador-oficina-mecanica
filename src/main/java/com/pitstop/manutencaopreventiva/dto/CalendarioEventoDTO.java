package com.pitstop.manutencaopreventiva.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pitstop.manutencaopreventiva.domain.StatusAgendamento;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para evento no calendário de manutenções.
 */
public record CalendarioEventoDTO(
    UUID id,
    String titulo,
    String descricao,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime inicio,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime fim,

    StatusAgendamento status,
    String cor,

    UUID veiculoId,
    String veiculoPlaca,
    String veiculoDescricao,

    UUID clienteId,
    String clienteNome,

    String tipoManutencao
) {}
