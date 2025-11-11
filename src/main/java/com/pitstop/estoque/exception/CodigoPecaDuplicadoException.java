package com.pitstop.estoque.exception;

/**
 * Exception lançada quando há tentativa de criar peça com código já existente.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public class CodigoPecaDuplicadoException extends RuntimeException {

    private final String codigo;

    /**
     * Construtor com código duplicado.
     *
     * @param codigo código que já existe no sistema
     */
    public CodigoPecaDuplicadoException(String codigo) {
        super(String.format("Já existe uma peça cadastrada com o código: %s", codigo));
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
