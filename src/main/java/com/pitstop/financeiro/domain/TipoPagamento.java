package com.pitstop.financeiro.domain;

/**
 * Tipo de pagamento aceito no sistema PitStop.
 *
 * <p>Enum que define os métodos de pagamento disponíveis para
 * quitação de ordens de serviço.</p>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
public enum TipoPagamento {

    /**
     * Pagamento em dinheiro (espécie).
     */
    DINHEIRO("Dinheiro"),

    /**
     * Pagamento via PIX.
     * Pagamento instantâneo do Banco Central.
     */
    PIX("PIX"),

    /**
     * Pagamento com cartão de crédito.
     * Pode ter parcelas.
     */
    CARTAO_CREDITO("Cartão de Crédito"),

    /**
     * Pagamento com cartão de débito.
     * Sempre à vista.
     */
    CARTAO_DEBITO("Cartão de Débito"),

    /**
     * Pagamento via boleto bancário.
     * Geralmente com vencimento futuro.
     */
    BOLETO("Boleto Bancário"),

    /**
     * Transferência bancária (TED/DOC).
     */
    TRANSFERENCIA("Transferência Bancária"),

    /**
     * Cheque.
     * @deprecated Em desuso, mantido por compatibilidade.
     */
    @Deprecated
    CHEQUE("Cheque");

    private final String descricao;

    TipoPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se o tipo de pagamento aceita parcelamento.
     *
     * @return true se aceita parcelas
     */
    public boolean aceitaParcelas() {
        return this == CARTAO_CREDITO || this == BOLETO;
    }

    /**
     * Verifica se é pagamento instantâneo.
     *
     * @return true se pagamento é imediato
     */
    public boolean isPagamentoInstantaneo() {
        return this == DINHEIRO || this == PIX || this == CARTAO_DEBITO;
    }
}
