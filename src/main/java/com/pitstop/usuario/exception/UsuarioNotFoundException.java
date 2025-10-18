package com.pitstop.usuario.exception;

import java.util.UUID;

/**
 * Exceção lançada quando um usuário não é encontrado no sistema.
 */
public class UsuarioNotFoundException extends RuntimeException {

    public UsuarioNotFoundException(UUID id) {
        super("Usuário não encontrado com ID: " + id);
    }

    public UsuarioNotFoundException(String email) {
        super("Usuário não encontrado com email: " + email);
    }

    public UsuarioNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
