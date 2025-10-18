package com.pitstop.usuario.exception;

/**
 * Exceção lançada quando tenta-se desativar o último administrador ativo do sistema.
 * Esta proteção garante que o sistema sempre terá pelo menos um admin ativo.
 */
public class CannotDeleteLastAdminException extends RuntimeException {

    public CannotDeleteLastAdminException() {
        super("Não é possível desativar o último administrador ativo do sistema");
    }

    public CannotDeleteLastAdminException(String message) {
        super(message);
    }

    public CannotDeleteLastAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
