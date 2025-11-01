package com.pitstop.ordemservico.exception;

import java.util.UUID;

/**
 * Exception lançada quando uma Ordem de Serviço não é encontrada no sistema.
 *
 * <p>Deve ser tratada pelo {@code GlobalExceptionHandler} retornando HTTP 404.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class OrdemServicoNotFoundException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE_ID = "Ordem de Serviço não encontrada com ID: %s";
    private static final String MESSAGE_TEMPLATE_NUMERO = "Ordem de Serviço não encontrada com número: %s";

    /**
     * Construtor para busca por ID.
     *
     * @param id identificador único da OS
     */
    public OrdemServicoNotFoundException(UUID id) {
        super(String.format(MESSAGE_TEMPLATE_ID, id));
    }

    /**
     * Construtor para busca por número.
     *
     * @param numero número sequencial da OS
     */
    public OrdemServicoNotFoundException(Long numero) {
        super(String.format(MESSAGE_TEMPLATE_NUMERO, numero));
    }

    /**
     * Construtor com mensagem customizada.
     *
     * @param message mensagem de erro personalizada
     */
    public OrdemServicoNotFoundException(String message) {
        super(message);
    }
}
