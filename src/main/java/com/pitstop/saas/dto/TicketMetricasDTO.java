package com.pitstop.saas.dto;

import java.math.BigDecimal;

public record TicketMetricasDTO(
    // Totais
    long totalTickets,
    long ticketsAbertos,
    long ticketsEmAndamento,
    long ticketsAguardando,
    long ticketsResolvidos,
    long ticketsFechados,

    // Por prioridade (abertos)
    long urgentes,
    long alta,
    long media,
    long baixa,

    // SLA
    long comSlaVencido,
    long dentroSla,
    BigDecimal percentualDentroSla,

    // Tempos
    Double tempoMedioRespostaMinutos,
    Double tempoMedioResolucaoMinutos,

    // Período (últimos 30 dias)
    long novosUltimos30d,
    long resolvidosUltimos30d,

    // Não atribuídos
    long naoAtribuidos
) {}
