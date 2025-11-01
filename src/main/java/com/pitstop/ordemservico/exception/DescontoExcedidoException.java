package com.pitstop.ordemservico.exception;

import java.math.BigDecimal;

/**
 * Exception lançada quando o desconto aplicado excede os limites permitidos.
 *
 * <p>Regra de negócio: Desconto percentual não pode exceder 100% e desconto em valor
 * não pode ser maior que o valor total da OS.</p>
 *
 * <p>Deve ser tratada pelo {@code GlobalExceptionHandler} retornando HTTP 400 (Bad Request).</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class DescontoExcedidoException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE = "Desconto excedido: %s";

    /**
     * Construtor com desconto e limite.
     *
     * @param desconto valor do desconto aplicado
     * @param limite valor máximo permitido
     */
    public DescontoExcedidoException(BigDecimal desconto, BigDecimal limite) {
        super(String.format("Desconto de R$ %.2f excede o limite de R$ %.2f", desconto, limite));
    }

    /**
     * Construtor com mensagem customizada.
     *
     * @param message mensagem de erro personalizada
     */
    public DescontoExcedidoException(String message) {
        super(String.format(MESSAGE_TEMPLATE, message));
    }
}
