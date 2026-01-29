package com.pitstop.fornecedor.exception;

import java.util.UUID;

/**
 * Exceção lançada quando um fornecedor não é encontrado.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class FornecedorNotFoundException extends RuntimeException {

    public FornecedorNotFoundException(UUID id) {
        super("Fornecedor não encontrado com ID: " + id);
    }

    public FornecedorNotFoundException(String message) {
        super(message);
    }
}
