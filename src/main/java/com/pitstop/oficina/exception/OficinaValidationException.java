package com.pitstop.oficina.exception;

/**
 * Exceção lançada quando há falha de validação de regras de negócio da oficina.
 *
 * @author PitStop Team
 */
public class OficinaValidationException extends RuntimeException {

    public OficinaValidationException(String message) {
        super(message);
    }

    public OficinaValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
