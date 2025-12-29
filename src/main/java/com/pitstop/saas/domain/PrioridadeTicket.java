package com.pitstop.saas.domain;

public enum PrioridadeTicket {
    BAIXA("Baixa", 4320),      // SLA: 72 horas
    MEDIA("Média", 1440),      // SLA: 24 horas
    ALTA("Alta", 480),         // SLA: 8 horas
    URGENTE("Urgente", 120);   // SLA: 2 horas

    private final String descricao;
    private final int slaMinutos;

    PrioridadeTicket(String descricao, int slaMinutos) {
        this.descricao = descricao;
        this.slaMinutos = slaMinutos;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getSlaMinutos() {
        return slaMinutos;
    }
}
