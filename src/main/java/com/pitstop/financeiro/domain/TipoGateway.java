package com.pitstop.financeiro.domain;

/**
 * Tipos de gateway de pagamento suportados pelo sistema.
 */
public enum TipoGateway {

    MERCADO_PAGO("Mercado Pago", "mercadopago"),
    PAGSEGURO("PagSeguro", "pagseguro"),
    STRIPE("Stripe", "stripe"),
    ASAAS("Asaas", "asaas"),
    PAGARME("Pagar.me", "pagarme");

    private final String descricao;
    private final String codigo;

    TipoGateway(String descricao, String codigo) {
        this.descricao = descricao;
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getCodigo() {
        return codigo;
    }
}
