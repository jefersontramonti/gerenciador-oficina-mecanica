import type { ReactNode } from 'react';
import { useFeatureFlag, useFeatureFlagMultiple } from '@/shared/hooks/useFeatureFlag';

interface FeatureGateProps {
  /**
   * Código da feature flag a verificar.
   * Use este prop para verificar uma única feature.
   */
  feature?: string;

  /**
   * Array de códigos de features.
   * Por padrão, exige que TODAS estejam habilitadas (mode="all").
   */
  features?: string[];

  /**
   * Modo de verificação quando `features` é fornecido:
   * - "all": Todas as features devem estar habilitadas (padrão)
   * - "any": Pelo menos uma feature deve estar habilitada
   */
  mode?: 'all' | 'any';

  /**
   * Conteúdo a exibir quando a feature está habilitada.
   */
  children: ReactNode;

  /**
   * Conteúdo a exibir quando a feature NÃO está habilitada.
   * Se não fornecido, não renderiza nada.
   */
  fallback?: ReactNode;
}

/**
 * Componente para renderizar conteúdo condicionalmente baseado em feature flags.
 *
 * @example Single feature
 * ```tsx
 * <FeatureGate feature="EMAIL_MARKETING">
 *   <EmailCampaignButton />
 * </FeatureGate>
 * ```
 *
 * @example Single feature with fallback
 * ```tsx
 * <FeatureGate
 *   feature="EMISSAO_NFE"
 *   fallback={<PlanUpgradePrompt feature="EMISSAO_NFE" requiredPlan="PROFISSIONAL" />}
 * >
 *   <NfeEmissionPanel />
 * </FeatureGate>
 * ```
 *
 * @example Multiple features (all required)
 * ```tsx
 * <FeatureGate features={['RELATORIOS_GERENCIAIS', 'PDF_EXPORT_AVANCADO']}>
 *   <AdvancedReportsPanel />
 * </FeatureGate>
 * ```
 *
 * @example Multiple features (any)
 * ```tsx
 * <FeatureGate
 *   features={['INTEGRACAO_MERCADO_PAGO', 'INTEGRACAO_STRIPE', 'INTEGRACAO_PAGSEGURO']}
 *   mode="any"
 *   fallback={<PlanUpgradePrompt requiredPlan="PROFISSIONAL" />}
 * >
 *   <PaymentIntegrationsPanel />
 * </FeatureGate>
 * ```
 */
export function FeatureGate({
  feature,
  features,
  mode = 'all',
  children,
  fallback = null,
}: FeatureGateProps): ReactNode {
  // Single feature check
  const singleFeatureEnabled = useFeatureFlag(feature || '');

  // Multiple features check
  const { all: allEnabled, any: anyEnabled } = useFeatureFlagMultiple(features || []);

  // Determine if we should render children
  let isEnabled = false;

  if (feature) {
    // Single feature mode
    isEnabled = singleFeatureEnabled;
  } else if (features && features.length > 0) {
    // Multiple features mode
    isEnabled = mode === 'all' ? allEnabled : anyEnabled;
  } else {
    // No feature specified, always enabled
    isEnabled = true;
  }

  if (isEnabled) {
    return <>{children}</>;
  }

  return <>{fallback}</>;
}

/**
 * HOC para envolver um componente com verificação de feature flag.
 *
 * @example
 * ```tsx
 * const ProtectedEmailCampaign = withFeatureGate(
 *   EmailCampaignPanel,
 *   'EMAIL_MARKETING',
 *   <PlanUpgradePrompt feature="EMAIL_MARKETING" />
 * );
 *
 * // Uso
 * <ProtectedEmailCampaign />
 * ```
 */
export function withFeatureGate<P extends object>(
  Component: React.ComponentType<P>,
  feature: string,
  fallback?: ReactNode
) {
  return function WrappedComponent(props: P) {
    return (
      <FeatureGate feature={feature} fallback={fallback}>
        <Component {...props} />
      </FeatureGate>
    );
  };
}
