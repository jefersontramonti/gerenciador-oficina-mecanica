package com.pitstop.estoque.domain;

public enum CategoriaPeca {

    FILTRO("Filtro"),
    CORREIA("Correia"),
    PASTILHA_FREIO("Pastilha de Freio"),
    DISCO_FREIO("Disco de Freio"),
    AMORTECEDOR("Amortecedor"),
    OLEO_LUBRIFICANTE("Óleo/Lubrificante"),
    FLUIDO("Fluido"),
    VELA_IGNICAO("Vela de Ignição"),
    BATERIA("Bateria"),
    PNEU("Pneu"),
    LAMPADA("Lâmpada"),
    ROLAMENTO("Rolamento"),
    JUNTA("Junta"),
    RETENTOR("Retentor"),
    SENSOR("Sensor"),
    BOMBA("Bomba"),
    EMBREAGEM("Embreagem"),
    SUSPENSAO("Suspensão"),
    POLIA("Polia"),
    MANGUEIRA("Mangueira"),
    ELETRICO("Elétrico"),
    FUNILARIA("Funilaria"),
    ACESSORIO("Acessório"),
    OUTROS("Outros");

    private final String descricao;

    CategoriaPeca(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
