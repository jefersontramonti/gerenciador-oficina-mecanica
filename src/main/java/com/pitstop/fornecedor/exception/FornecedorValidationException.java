package com.pitstop.fornecedor.exception;

/**
 * Exceção lançada quando uma validação de fornecedor falha.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class FornecedorValidationException extends RuntimeException {

    public FornecedorValidationException(String message) {
        super(message);
    }
}
