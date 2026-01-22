package com.pitstop.saas.domain;

/**
 * Supported payment gateways.
 */
public enum TipoGateway {
    MERCADO_PAGO("Mercado Pago", "mercadopago"),
    // Future: STRIPE, PAGSEGURO, etc.
    ;

    private final String nome;
    private final String codigo;

    TipoGateway(String nome, String codigo) {
        this.nome = nome;
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getCodigo() {
        return codigo;
    }
}
