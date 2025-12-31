package com.pitstop.financeiro.domain;

/**
 * Ambiente de execução do gateway de pagamento.
 */
public enum AmbienteGateway {

    SANDBOX("Sandbox (Testes)"),
    PRODUCAO("Produção");

    private final String descricao;

    AmbienteGateway(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
