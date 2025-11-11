package com.pitstop.estoque.exception;

import java.util.UUID;

/**
 * Exception lançada quando uma peça não é encontrada no sistema.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public class PecaNotFoundException extends RuntimeException {

    private final UUID pecaId;
    private final String codigo;

    /**
     * Construtor com ID da peça.
     *
     * @param pecaId ID da peça não encontrada
     */
    public PecaNotFoundException(UUID pecaId) {
        super(String.format("Peça não encontrada com ID: %s", pecaId));
        this.pecaId = pecaId;
        this.codigo = null;
    }

    /**
     * Construtor com código da peça.
     *
     * @param codigo código da peça não encontrada
     */
    public PecaNotFoundException(String codigo) {
        super(String.format("Peça não encontrada com código: %s", codigo));
        this.pecaId = null;
        this.codigo = codigo;
    }

    public UUID getPecaId() {
        return pecaId;
    }

    public String getCodigo() {
        return codigo;
    }
}
