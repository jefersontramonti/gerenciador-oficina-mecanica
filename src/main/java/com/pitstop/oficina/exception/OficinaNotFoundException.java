package com.pitstop.oficina.exception;

import com.pitstop.shared.exception.ResourceNotFoundException;

import java.util.UUID;

/**
 * Exceção lançada quando uma oficina não é encontrada.
 *
 * @author PitStop Team
 */
public class OficinaNotFoundException extends ResourceNotFoundException {

    public OficinaNotFoundException(UUID id) {
        super("Oficina não encontrada com o ID: " + id);
    }

    public OficinaNotFoundException(String cnpj) {
        super("Oficina não encontrada com o CNPJ: " + cnpj);
    }

    public OficinaNotFoundException(String message, Object... args) {
        super(String.format(message, args));
    }
}
