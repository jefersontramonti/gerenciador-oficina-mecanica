package com.pitstop.saas.exception;

import com.pitstop.shared.exception.BusinessException;

/**
 * Exception thrown when workshop tries to create more users than allowed by plan.
 *
 * @author PitStop Team
 */
public class LimiteUsuariosExcedidoException extends BusinessException {

    public LimiteUsuariosExcedidoException(String plano, int limite, long atual) {
        super(String.format(
            "Limite de usuários atingido para o plano %s. Limite: %d, Atual: %d. Faça upgrade para adicionar mais usuários.",
            plano, limite, atual
        ));
    }
}
