package com.pitstop.ordemservico.exception;

import com.pitstop.ordemservico.domain.StatusOS;

/**
 * Exception lançada quando tenta-se editar uma OS que não está mais editável.
 *
 * <p>OS só podem ser editadas nos status: ORCAMENTO e APROVADO.</p>
 *
 * <p>Deve ser tratada pelo {@code GlobalExceptionHandler} retornando HTTP 400 (Bad Request).</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class OrdemServicoNaoEditavelException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE = "Ordem de Serviço não pode ser editada no status: %s";

    /**
     * Construtor com status atual.
     *
     * @param status status atual da OS
     */
    public OrdemServicoNaoEditavelException(StatusOS status) {
        super(String.format(MESSAGE_TEMPLATE, status));
    }

    /**
     * Construtor com mensagem customizada.
     *
     * @param message mensagem de erro personalizada
     */
    public OrdemServicoNaoEditavelException(String message) {
        super(message);
    }
}
