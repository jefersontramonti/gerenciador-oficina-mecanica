package com.pitstop.saas.exception;

import com.pitstop.shared.exception.BusinessException;

/**
 * Exception thrown when workshop tries to create more service orders than allowed monthly limit.
 *
 * @author PitStop Team
 */
public class LimiteOsMesExcedidoException extends BusinessException {

    public LimiteOsMesExcedidoException(String plano, int limite, long atual) {
        super(String.format(
            "Limite de ordens de serviço atingido para o plano %s. Limite mensal: %d, Criadas este mês: %d. Faça upgrade para criar mais OS.",
            plano, limite, atual
        ));
    }
}
