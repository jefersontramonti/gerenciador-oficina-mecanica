package com.pitstop.usuario.exception;

/**
 * Exceção lançada quando as credenciais de login são inválidas.
 * Usada no processo de autenticação.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email ou senha inválidos");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
