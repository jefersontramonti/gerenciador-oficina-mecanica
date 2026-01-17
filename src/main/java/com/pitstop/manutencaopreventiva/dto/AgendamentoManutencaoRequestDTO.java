package com.pitstop.manutencaopreventiva.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO para criar/atualizar agendamento de manutenção.
 */
public record AgendamentoManutencaoRequestDTO(
    UUID planoId,

    @NotNull(message = "Veículo é obrigatório")
    UUID veiculoId,

    @NotNull(message = "Cliente é obrigatório")
    UUID clienteId,

    @NotNull(message = "Data do agendamento é obrigatória")
    LocalDate dataAgendamento,

    @NotNull(message = "Hora do agendamento é obrigatória")
    LocalTime horaAgendamento,

    @Min(value = 15, message = "Duração mínima é 15 minutos")
    Integer duracaoEstimadaMinutos,

    @NotBlank(message = "Tipo de manutenção é obrigatório")
    @Size(max = 50, message = "Tipo de manutenção deve ter no máximo 50 caracteres")
    String tipoManutencao,

    String descricao,

    String observacoes,

    String observacoesInternas,

    Boolean enviarConfirmacao
) {}
