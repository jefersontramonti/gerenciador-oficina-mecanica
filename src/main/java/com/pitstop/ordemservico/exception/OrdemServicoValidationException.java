package com.pitstop.ordemservico.exception;

/**
 * Exception genérica para erros de validação de Ordem de Serviço.
 *
 * <p>Deve ser tratada pelo {@code GlobalExceptionHandler} retornando HTTP 400 (Bad Request).</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class OrdemServicoValidationException extends RuntimeException {

    /**
     * Construtor com mensagem de erro.
     *
     * @param message mensagem descrevendo o erro de validação
     */
    public OrdemServicoValidationException(String message) {
        super(message);
    }

    /**
     * Construtor com mensagem e causa.
     *
     * @param message mensagem descrevendo o erro de validação
     * @param cause causa raiz do erro
     */
    public OrdemServicoValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
