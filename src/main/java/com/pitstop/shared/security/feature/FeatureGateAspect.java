package com.pitstop.shared.security.feature;

import com.pitstop.saas.service.FeatureFlagService;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Aspect que intercepta métodos/classes anotadas com {@link RequiresFeature}
 * e verifica se a funcionalidade está habilitada para a oficina atual.
 *
 * <p>Ordem de execução:</p>
 * <ol>
 *   <li>Spring Security (autenticação/autorização)</li>
 *   <li>FeatureGateAspect (verificação de feature flags)</li>
 *   <li>Método do controller/service</li>
 * </ol>
 *
 * <p>Regras de verificação:</p>
 * <ul>
 *   <li>Se TenantContext não está definido (SUPER_ADMIN), permite acesso</li>
 *   <li>Se a feature está habilitada para a oficina, permite acesso</li>
 *   <li>Se a feature não está habilitada, lança {@link FeatureNotEnabledException}</li>
 * </ul>
 *
 * @see RequiresFeature
 * @see FeatureNotEnabledException
 * @see FeatureFlagService
 */
@Aspect
@Component
@Order(2) // Executa após a segurança do Spring (Order 1)
@RequiredArgsConstructor
@Slf4j
public class FeatureGateAspect {

    private final FeatureFlagService featureFlagService;

    /**
     * Intercepta métodos anotados com @RequiresFeature.
     * Verifica se a feature está habilitada antes da execução.
     */
    @Before("@annotation(requiresFeature)")
    public void checkFeatureOnMethod(JoinPoint joinPoint, RequiresFeature requiresFeature) {
        checkFeature(requiresFeature, joinPoint);
    }

    /**
     * Intercepta todos os métodos públicos de classes anotadas com @RequiresFeature.
     * A annotation da classe se aplica a todos os métodos públicos.
     */
    @Before("@within(requiresFeature) && execution(public * *(..))")
    public void checkFeatureOnClass(JoinPoint joinPoint, RequiresFeature requiresFeature) {
        // Verifica se o método tem sua própria annotation (prioridade)
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequiresFeature methodAnnotation = method.getAnnotation(RequiresFeature.class);
        if (methodAnnotation != null) {
            // Método tem sua própria annotation, usa ela (será processada pelo outro advice)
            return;
        }

        checkFeature(requiresFeature, joinPoint);
    }

    /**
     * Verifica se a feature está habilitada para a oficina atual.
     *
     * @param requiresFeature Annotation com os dados da feature
     * @param joinPoint Ponto de junção para logging
     * @throws FeatureNotEnabledException se a feature não está habilitada
     */
    private void checkFeature(RequiresFeature requiresFeature, JoinPoint joinPoint) {
        String featureCode = requiresFeature.value();
        String featureName = requiresFeature.name().isEmpty() ? featureCode : requiresFeature.name();
        String requiredPlan = requiresFeature.requiredPlan();

        // Obtém o tenant ID (oficina) do contexto atual
        UUID oficinaId = TenantContext.getTenantIdOrNull();

        // Se não há tenant (SUPER_ADMIN ou endpoint público), permite acesso
        if (oficinaId == null) {
            log.debug("Feature check skipped (no tenant context) for {} at {}",
                    featureCode, joinPoint.getSignature().toShortString());
            return;
        }

        // Verifica se a feature está habilitada para esta oficina
        boolean isEnabled = featureFlagService.isEnabled(featureCode, oficinaId);

        if (!isEnabled) {
            log.warn("Feature '{}' not enabled for oficina {}. Method: {}",
                    featureCode, oficinaId, joinPoint.getSignature().toShortString());

            if (!requiredPlan.isEmpty()) {
                throw new FeatureNotEnabledException(featureCode, featureName, requiredPlan);
            } else if (!featureName.equals(featureCode)) {
                throw new FeatureNotEnabledException(featureCode, featureName);
            } else {
                throw new FeatureNotEnabledException(featureCode);
            }
        }

        log.debug("Feature '{}' enabled for oficina {}. Proceeding with method: {}",
                featureCode, oficinaId, joinPoint.getSignature().toShortString());
    }
}
