package com.pitstop.usuario.exception;

import java.util.UUID;

/**
 * Exceção lançada quando tenta-se realizar operações com um usuário inativo.
 * Por exemplo: login de usuário desativado.
 */
public class UsuarioInativoException extends RuntimeException {

    public UsuarioInativoException(UUID id) {
        super("O usuário com ID " + id + " está inativo");
    }

    public UsuarioInativoException(String email) {
        super("O usuário com email " + email + " está inativo");
    }

    public UsuarioInativoException(String message, Throwable cause) {
        super(message, cause);
    }
}
