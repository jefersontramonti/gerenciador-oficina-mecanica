package com.pitstop.ordemservico.exception;

import com.pitstop.ordemservico.domain.StatusOS;

/**
 * Exception lançada quando tenta-se fazer uma transição de status inválida.
 *
 * <p>Exemplo: Tentar mudar de FINALIZADO para EM_ANDAMENTO (não permitido pela máquina de estados).</p>
 *
 * <p>Deve ser tratada pelo {@code GlobalExceptionHandler} retornando HTTP 400 (Bad Request).</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class TransicaoStatusInvalidaException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE = "Transição de status inválida: %s → %s não é permitida";

    /**
     * Construtor com status atual e desejado.
     *
     * @param statusAtual status atual da OS
     * @param statusDesejado status para o qual se tentou transicionar
     */
    public TransicaoStatusInvalidaException(StatusOS statusAtual, StatusOS statusDesejado) {
        super(String.format(MESSAGE_TEMPLATE, statusAtual, statusDesejado));
    }

    /**
     * Construtor com mensagem customizada.
     *
     * @param message mensagem de erro personalizada
     */
    public TransicaoStatusInvalidaException(String message) {
        super(message);
    }
}
