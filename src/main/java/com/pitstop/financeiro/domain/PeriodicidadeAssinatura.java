package com.pitstop.financeiro.domain;

/**
 * Periodicidade de cobrança de uma assinatura.
 */
public enum PeriodicidadeAssinatura {

    SEMANAL("Semanal", 7),
    QUINZENAL("Quinzenal", 15),
    MENSAL("Mensal", 30),
    TRIMESTRAL("Trimestral", 90),
    SEMESTRAL("Semestral", 180),
    ANUAL("Anual", 365);

    private final String descricao;
    private final int diasIntervalo;

    PeriodicidadeAssinatura(String descricao, int diasIntervalo) {
        this.descricao = descricao;
        this.diasIntervalo = diasIntervalo;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getDiasIntervalo() {
        return diasIntervalo;
    }
}
