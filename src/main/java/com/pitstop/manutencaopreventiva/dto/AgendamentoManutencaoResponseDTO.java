package com.pitstop.manutencaopreventiva.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pitstop.manutencaopreventiva.domain.StatusAgendamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO de resposta do agendamento de manutenção.
 */
public record AgendamentoManutencaoResponseDTO(
    UUID id,
    PlanoResumoDTO plano,
    VeiculoResumoDTO veiculo,
    ClienteResumoDTO cliente,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dataAgendamento,

    @JsonFormat(pattern = "HH:mm")
    LocalTime horaAgendamento,

    Integer duracaoEstimadaMinutos,
    String tipoManutencao,
    String descricao,
    StatusAgendamento status,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime confirmadoEm,
    String confirmadoVia,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime canceladoEm,
    String motivoCancelamento,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime realizadoEm,
    UUID ordemServicoId,

    Boolean lembreteEnviado,
    String observacoes,

    Boolean hoje,
    Boolean passado,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt
) {
    public record PlanoResumoDTO(
        UUID id,
        String nome,
        String tipoManutencao
    ) {}

    public record VeiculoResumoDTO(
        UUID id,
        String placa,
        String placaFormatada,
        String marca,
        String modelo,
        Integer ano
    ) {}

    public record ClienteResumoDTO(
        UUID id,
        String nome,
        String telefone,
        String email
    ) {}
}
