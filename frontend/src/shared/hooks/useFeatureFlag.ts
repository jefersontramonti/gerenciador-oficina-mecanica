import { useMemo } from 'react';
import { useFeatureFlags } from '@/shared/contexts/FeatureFlagContext';

/**
 * Hook para verificar se uma feature específica está habilitada.
 *
 * @param featureCode - Código da feature flag (ex: "EMAIL_MARKETING")
 * @returns true se a feature está habilitada, false caso contrário
 *
 * @example
 * ```tsx
 * function EmailCampaignButton() {
 *   const isEnabled = useFeatureFlag('EMAIL_MARKETING');
 *
 *   if (!isEnabled) {
 *     return null; // or show upgrade prompt
 *   }
 *
 *   return <Button>Criar Campanha</Button>;
 * }
 * ```
 */
export function useFeatureFlag(featureCode: string): boolean {
  const { isFeatureEnabled } = useFeatureFlags();

  return useMemo(() => isFeatureEnabled(featureCode), [isFeatureEnabled, featureCode]);
}

/**
 * Hook para verificar múltiplas features de uma vez.
 *
 * @param featureCodes - Array de códigos de features
 * @returns Object com as verificações { all: boolean, any: boolean, map: Record<string, boolean> }
 *
 * @example
 * ```tsx
 * function IntegrationPanel() {
 *   const { any: hasAnyIntegration, map } = useFeatureFlags([
 *     'INTEGRACAO_MERCADO_PAGO',
 *     'INTEGRACAO_STRIPE',
 *     'INTEGRACAO_PAGSEGURO',
 *   ]);
 *
 *   if (!hasAnyIntegration) {
 *     return <UpgradePrompt />;
 *   }
 *
 *   return (
 *     <div>
 *       {map.INTEGRACAO_MERCADO_PAGO && <MercadoPagoIntegration />}
 *       {map.INTEGRACAO_STRIPE && <StripeIntegration />}
 *       {map.INTEGRACAO_PAGSEGURO && <PagSeguroIntegration />}
 *     </div>
 *   );
 * }
 * ```
 */
export function useFeatureFlagMultiple(featureCodes: string[]): {
  /** true se TODAS as features estão habilitadas */
  all: boolean;
  /** true se PELO MENOS UMA feature está habilitada */
  any: boolean;
  /** Map de código -> habilitado */
  map: Record<string, boolean>;
} {
  const { isFeatureEnabled, areAllFeaturesEnabled, isAnyFeatureEnabled } = useFeatureFlags();

  return useMemo(() => {
    const map: Record<string, boolean> = {};
    for (const code of featureCodes) {
      map[code] = isFeatureEnabled(code);
    }

    return {
      all: areAllFeaturesEnabled(featureCodes),
      any: isAnyFeatureEnabled(featureCodes),
      map,
    };
  }, [featureCodes, isFeatureEnabled, areAllFeaturesEnabled, isAnyFeatureEnabled]);
}
