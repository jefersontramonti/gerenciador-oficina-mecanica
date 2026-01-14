package com.pitstop.anexo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção base para erros relacionados a anexos.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AnexoException extends RuntimeException {

    public AnexoException(String message) {
        super(message);
    }

    public AnexoException(String message, Throwable cause) {
        super(message, cause);
    }
}
