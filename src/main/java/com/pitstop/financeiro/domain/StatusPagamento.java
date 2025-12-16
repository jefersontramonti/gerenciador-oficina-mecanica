package com.pitstop.financeiro.domain;

/**
 * Status de um pagamento no sistema PitStop.
 *
 * <p>Define o ciclo de vida de um pagamento desde sua criação
 * até sua quitação, cancelamento ou estorno.</p>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
public enum StatusPagamento {

    /**
     * Pagamento aguardando quitação.
     * Estado inicial de todo pagamento.
     */
    PENDENTE("Pendente", false),

    /**
     * Pagamento realizado e confirmado.
     * Estado final bem-sucedido.
     */
    PAGO("Pago", true),

    /**
     * Pagamento cancelado antes da quitação.
     * Estado final (cancelamento).
     */
    CANCELADO("Cancelado", true),

    /**
     * Pagamento estornado após ter sido pago.
     * Estado final (estorno).
     */
    ESTORNADO("Estornado", true),

    /**
     * Pagamento vencido (não quitado no prazo).
     * Ainda pode ser pago com multa/juros.
     */
    VENCIDO("Vencido", false);

    private final String descricao;
    private final boolean estadoFinal;

    StatusPagamento(String descricao, boolean estadoFinal) {
        this.descricao = descricao;
        this.estadoFinal = estadoFinal;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se é um estado final (não pode mais mudar).
     *
     * @return true se é estado final
     */
    public boolean isEstadoFinal() {
        return estadoFinal;
    }

    /**
     * Verifica se o pagamento está pendente (aguardando quitação).
     *
     * @return true se status é PENDENTE ou VENCIDO
     */
    public boolean isPendente() {
        return this == PENDENTE || this == VENCIDO;
    }

    /**
     * Verifica se o pagamento foi quitado com sucesso.
     *
     * @return true se status é PAGO
     */
    public boolean isPago() {
        return this == PAGO;
    }
}
