package com.pitstop.usuario.exception;

/**
 * Exceção lançada quando tenta-se criar ou atualizar um usuário
 * com um email que já está em uso por outro usuário.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Já existe um usuário cadastrado com o email: " + email);
    }

    public EmailAlreadyExistsException(String email, Throwable cause) {
        super("Já existe um usuário cadastrado com o email: " + email, cause);
    }
}
