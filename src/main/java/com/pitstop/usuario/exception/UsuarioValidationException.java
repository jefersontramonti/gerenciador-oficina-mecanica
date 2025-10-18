package com.pitstop.usuario.exception;

/**
 * Exceção genérica para erros de validação de dados do usuário
 * que não se enquadram nas outras categorias específicas.
 */
public class UsuarioValidationException extends RuntimeException {

    public UsuarioValidationException(String message) {
        super(message);
    }

    public UsuarioValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
