package com.pitstop.cliente.exception;

import java.util.UUID;

/**
 * Exception lançada quando um cliente não é encontrado no sistema.
 *
 * <p>Deve ser tratada pelo {@code GlobalExceptionHandler} retornando HTTP 404.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class ClienteNotFoundException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE_ID = "Cliente não encontrado com ID: %s";
    private static final String MESSAGE_TEMPLATE_CPF_CNPJ = "Cliente não encontrado com CPF/CNPJ: %s";

    /**
     * Construtor para busca por ID.
     *
     * @param id identificador único do cliente
     */
    public ClienteNotFoundException(UUID id) {
        super(String.format(MESSAGE_TEMPLATE_ID, id));
    }

    /**
     * Construtor para busca por CPF/CNPJ.
     *
     * @param cpfCnpj CPF ou CNPJ do cliente
     */
    public ClienteNotFoundException(String cpfCnpj) {
        super(String.format(MESSAGE_TEMPLATE_CPF_CNPJ, cpfCnpj));
    }

    /**
     * Construtor com mensagem customizada.
     *
     * @param message mensagem de erro personalizada
     */
    public ClienteNotFoundException(String message, boolean customMessage) {
        super(message);
    }
}
