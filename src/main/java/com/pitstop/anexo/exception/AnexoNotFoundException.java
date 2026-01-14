package com.pitstop.anexo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Exceção lançada quando um anexo não é encontrado.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AnexoNotFoundException extends RuntimeException {

    public AnexoNotFoundException(UUID id) {
        super("Anexo não encontrado com ID: " + id);
    }

    public AnexoNotFoundException(String message) {
        super(message);
    }
}
