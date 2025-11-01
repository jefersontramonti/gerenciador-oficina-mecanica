package com.pitstop.cliente.exception;

/**
 * Exception lançada quando há falha em validações customizadas de cliente.
 *
 * <p>Exemplos: tipo de documento incompatível com tipo de cliente,
 * CPF/CNPJ com dígitos verificadores inválidos, etc.</p>
 *
 * <p>Deve ser tratada pelo {@code GlobalExceptionHandler} retornando HTTP 400 (Bad Request).</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class ClienteValidationException extends RuntimeException {

    /**
     * Construtor com mensagem de validação.
     *
     * @param message mensagem descrevendo a falha de validação
     */
    public ClienteValidationException(String message) {
        super(message);
    }

    /**
     * Construtor com mensagem e causa raiz.
     *
     * @param message mensagem descrevendo a falha de validação
     * @param cause exceção original que causou a validação falhar
     */
    public ClienteValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
