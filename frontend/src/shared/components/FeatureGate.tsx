import type { ReactNode } from 'react';
import { useFeatureFlag, useFeatureFlagMultiple, useFeatureFlagsReady } from '@/shared/hooks/useFeatureFlag';

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

  /**
   * Conteúdo a exibir enquanto as features estão sendo carregadas.
   * Se não fornecido, mostra um spinner de loading padrão.
   * Use `null` para não mostrar nada durante o carregamento.
   */
  loadingFallback?: ReactNode;

  /**
   * Se true, mostra o fallback imediatamente enquanto carrega.
   * Se false (padrão), mostra loading enquanto carrega.
   */
  showFallbackWhileLoading?: boolean;
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
// Default loading spinner component
const DefaultLoadingSpinner = () => (
  <div className="flex items-center justify-center py-8">
    <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 dark:border-gray-700 border-t-blue-600 dark:border-t-blue-400" />
  </div>
);

export function FeatureGate({
  feature,
  features,
  mode = 'all',
  children,
  fallback = null,
  loadingFallback,
  showFallbackWhileLoading = false,
}: FeatureGateProps): ReactNode {
  // Check if features are ready (loaded from API)
  const isReady = useFeatureFlagsReady();

  // Single feature check
  const singleFeatureEnabled = useFeatureFlag(feature || '');

  // Multiple features check
  const { all: allEnabled, any: anyEnabled } = useFeatureFlagMultiple(features || []);

  // If features are not ready yet, show loading or fallback based on config
  if (!isReady && (feature || (features && features.length > 0))) {
    if (showFallbackWhileLoading) {
      return <>{fallback}</>;
    }
    // Show loading fallback (default spinner if not provided, or custom, or nothing if null)
    if (loadingFallback === undefined) {
      return <DefaultLoadingSpinner />;
    }
    return <>{loadingFallback}</>;
  }

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
