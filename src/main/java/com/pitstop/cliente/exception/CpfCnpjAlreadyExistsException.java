package com.pitstop.cliente.exception;

/**
 * Exception lançada quando tenta-se criar/atualizar um cliente com CPF/CNPJ já cadastrado.
 *
 * <p>Deve ser tratada pelo {@code GlobalExceptionHandler} retornando HTTP 409 (Conflict).</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class CpfCnpjAlreadyExistsException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE = "Já existe um cliente cadastrado com CPF/CNPJ: %s";

    /**
     * Construtor com CPF/CNPJ duplicado.
     *
     * @param cpfCnpj CPF ou CNPJ que já existe no sistema
     */
    public CpfCnpjAlreadyExistsException(String cpfCnpj) {
        super(String.format(MESSAGE_TEMPLATE, cpfCnpj));
    }
}
