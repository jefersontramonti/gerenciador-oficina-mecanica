package com.pitstop.ordemservico.exception;

import java.math.BigDecimal;

/**
 * Exception lançada quando as horas trabalhadas excedem o limite aprovado pelo cliente.
 *
 * <p>No modelo de cobrança POR_HORA, o cliente aprova um limite máximo de horas.
 * Se o mecânico informar mais horas do que o limite aprovado, esta exception é lançada.</p>
 *
 * <p>Deve ser tratada pelo {@code GlobalExceptionHandler} retornando HTTP 400 (Bad Request).</p>
 *
 * <p>Exemplo:</p>
 * <pre>
 * Limite aprovado: 5 horas
 * Horas trabalhadas: 7 horas
 * → LimiteHorasExcedidoException: "Horas trabalhadas (7.00) excedem limite aprovado (5.00)"
 * </pre>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class LimiteHorasExcedidoException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE =
        "Horas trabalhadas (%.2f) excedem limite aprovado (%.2f). " +
        "É necessário solicitar nova aprovação do cliente.";

    private final BigDecimal horasTrabalhadas;
    private final BigDecimal limiteAprovado;

    /**
     * Construtor com horas trabalhadas e limite aprovado.
     *
     * @param horasTrabalhadas horas informadas pelo mecânico
     * @param limiteAprovado limite máximo aprovado pelo cliente
     */
    public LimiteHorasExcedidoException(BigDecimal horasTrabalhadas, BigDecimal limiteAprovado) {
        super(String.format(MESSAGE_TEMPLATE, horasTrabalhadas, limiteAprovado));
        this.horasTrabalhadas = horasTrabalhadas;
        this.limiteAprovado = limiteAprovado;
    }

    /**
     * Construtor com mensagem customizada.
     *
     * @param message mensagem de erro personalizada
     */
    public LimiteHorasExcedidoException(String message) {
        super(message);
        this.horasTrabalhadas = null;
        this.limiteAprovado = null;
    }

    /**
     * Retorna as horas trabalhadas informadas.
     *
     * @return horas trabalhadas
     */
    public BigDecimal getHorasTrabalhadas() {
        return horasTrabalhadas;
    }

    /**
     * Retorna o limite de horas aprovado pelo cliente.
     *
     * @return limite aprovado
     */
    public BigDecimal getLimiteAprovado() {
        return limiteAprovado;
    }
}
