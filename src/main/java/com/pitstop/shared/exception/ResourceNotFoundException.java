package com.pitstop.shared.exception;

/**
 * Generic exception for when a requested resource is not found.
 * Can be used across all modules for NOT_FOUND scenarios.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Construtor com mensagem.
     *
     * @param mensagem mensagem de erro
     */
    public ResourceNotFoundException(String mensagem) {
        super(mensagem);
    }

    /**
     * Construtor com mensagem e causa.
     *
     * @param mensagem mensagem de erro
     * @param causa causa da exception
     */
    public ResourceNotFoundException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
