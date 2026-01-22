package com.pitstop.manutencaopreventiva.dto;

import com.pitstop.manutencaopreventiva.domain.StatusAgendamento;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para exibição pública do agendamento (cliente via link).
 */
public record AgendamentoPublicoDTO(
    String status,
    String mensagem,
    boolean podeConfirmar,
    // Dados do agendamento
    LocalDate dataAgendamento,
    String dataFormatada,
    LocalTime horaAgendamento,
    String horaFormatada,
    Integer duracaoEstimadaMinutos,
    String tipoManutencao,
    String tipoManutencaoDescricao,
    String descricao,
    String observacoes,
    StatusAgendamento statusAgendamento,
    String statusDescricao,
    // Veículo
    String veiculoPlaca,
    String veiculoMarca,
    String veiculoModelo,
    Integer veiculoAno,
    // Oficina
    String oficinaNome,
    String oficinaTelefone,
    String oficinaEndereco
) {

    public static AgendamentoPublicoDTO jaConfirmado(String mensagem) {
        return new AgendamentoPublicoDTO(
            "JA_CONFIRMADO", mensagem, false,
            null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null,
            null, null, null
        );
    }

    public static AgendamentoPublicoDTO erro(String mensagem) {
        return new AgendamentoPublicoDTO(
            "ERRO", mensagem, false,
            null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null,
            null, null, null
        );
    }
}
