package com.pitstop.ordemservico.exception;

import java.util.UUID;

/**
 * Exception lançada quando tenta-se entregar uma OS que não está quitada.
 */
public class OrdemServicoNaoPagaException extends RuntimeException {

    public OrdemServicoNaoPagaException(UUID id) {
        super(String.format("Ordem de Serviço ID %s não pode ser entregue pois não está quitada", id));
    }

    public OrdemServicoNaoPagaException(Long numero) {
        super(String.format("Ordem de Serviço #%d não pode ser entregue pois não está quitada", numero));
    }
}
