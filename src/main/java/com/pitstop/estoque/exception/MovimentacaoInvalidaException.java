package com.pitstop.estoque.exception;

/**
 * Exception lançada quando uma movimentação de estoque é inválida por regras de negócio.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public class MovimentacaoInvalidaException extends RuntimeException {

    /**
     * Construtor com mensagem personalizada.
     *
     * @param mensagem descrição do erro
     */
    public MovimentacaoInvalidaException(String mensagem) {
        super(mensagem);
    }

    /**
     * Construtor com mensagem e causa.
     *
     * @param mensagem descrição do erro
     * @param causa exceção original
     */
    public MovimentacaoInvalidaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
