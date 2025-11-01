package com.pitstop.veiculo.exception;

/**
 * Exception lançada quando tenta-se criar/atualizar um veículo com placa já cadastrada.
 *
 * <p>Deve ser tratada pelo {@code GlobalExceptionHandler} retornando HTTP 409 (Conflict).</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class PlacaJaExisteException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE = "Já existe um veículo cadastrado com placa: %s";

    /**
     * Construtor com placa duplicada.
     *
     * @param placa placa que já existe no sistema
     */
    public PlacaJaExisteException(String placa) {
        super(String.format(MESSAGE_TEMPLATE, placa));
    }
}
