package com.pitstop.oficina.exception;

import com.pitstop.oficina.domain.PlanoAssinatura;

/**
 * Exceção lançada quando há falha em operações de upgrade/downgrade de plano.
 *
 * @author PitStop Team
 */
public class PlanUpgradeException extends RuntimeException {

    public PlanUpgradeException(String message) {
        super(message);
    }

    public PlanUpgradeException(PlanoAssinatura planoAtual, PlanoAssinatura novoPlano) {
        super(String.format(
            "Não é possível alterar do plano %s para %s. O novo plano deve ser superior ao atual.",
            planoAtual.name(),
            novoPlano.name()
        ));
    }
}
