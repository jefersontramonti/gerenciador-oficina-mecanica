package com.pitstop.veiculo.exception;

import java.util.UUID;

/**
 * Exception lançada quando um veículo não é encontrado no sistema.
 *
 * <p>Deve ser tratada pelo {@code GlobalExceptionHandler} retornando HTTP 404.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class VeiculoNotFoundException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE_ID = "Veículo não encontrado com ID: %s";
    private static final String MESSAGE_TEMPLATE_PLACA = "Veículo não encontrado com placa: %s";

    /**
     * Construtor para busca por ID.
     *
     * @param id identificador único do veículo
     */
    public VeiculoNotFoundException(UUID id) {
        super(String.format(MESSAGE_TEMPLATE_ID, id));
    }

    /**
     * Construtor para busca por placa.
     *
     * @param placa placa do veículo
     */
    public VeiculoNotFoundException(String placa) {
        super(String.format(MESSAGE_TEMPLATE_PLACA, placa));
    }

    /**
     * Construtor com mensagem customizada.
     *
     * @param message mensagem de erro personalizada
     */
    public VeiculoNotFoundException(String message, boolean customMessage) {
        super(message);
    }
}
