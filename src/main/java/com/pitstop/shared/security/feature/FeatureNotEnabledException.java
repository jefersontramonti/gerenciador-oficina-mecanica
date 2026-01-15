package com.pitstop.shared.security.feature;

import lombok.Getter;

/**
 * Exceção lançada quando uma funcionalidade (feature flag) não está habilitada
 * para a oficina do usuário atual.
 *
 * <p>Esta exceção é usada pelo sistema de gating para bloquear acesso a
 * funcionalidades não disponíveis no plano atual da oficina.</p>
 */
@Getter
public class FeatureNotEnabledException extends RuntimeException {

    private final String featureCode;
    private final String featureName;
    private final String requiredPlan;

    public FeatureNotEnabledException(String featureCode) {
        super(String.format("A funcionalidade '%s' não está disponível no seu plano atual", featureCode));
        this.featureCode = featureCode;
        this.featureName = null;
        this.requiredPlan = null;
    }

    public FeatureNotEnabledException(String featureCode, String featureName) {
        super(String.format("A funcionalidade '%s' não está disponível no seu plano atual", featureName));
        this.featureCode = featureCode;
        this.featureName = featureName;
        this.requiredPlan = null;
    }

    public FeatureNotEnabledException(String featureCode, String featureName, String requiredPlan) {
        super(String.format(
            "A funcionalidade '%s' está disponível apenas no plano %s ou superior. " +
            "Faça upgrade do seu plano para ter acesso.",
            featureName,
            requiredPlan
        ));
        this.featureCode = featureCode;
        this.featureName = featureName;
        this.requiredPlan = requiredPlan;
    }
}
