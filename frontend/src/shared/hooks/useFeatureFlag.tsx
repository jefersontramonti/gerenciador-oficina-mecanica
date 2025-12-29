/**
 * Hook para verificar Feature Flags no frontend das oficinas
 *
 * Uso:
 * const { isEnabled, isLoading } = useFeatureFlag('WHATSAPP_NOTIFICATIONS');
 *
 * if (isEnabled) {
 *   // Mostrar funcionalidade
 * }
 */

import { useQuery } from '@tanstack/react-query';
import { featureService } from '../services/featureService';

// Query keys
export const featureKeys = {
  all: ['features'] as const,
  myFeatures: () => [...featureKeys.all, 'me'] as const,
  check: (codigo: string) => [...featureKeys.all, 'check', codigo] as const,
  checkBatch: (codigos: string[]) => [...featureKeys.all, 'batch', codigos.sort().join(',')] as const,
};

/**
 * Hook para obter todas as features habilitadas para a oficina
 */
export const useMyFeatures = () => {
  return useQuery({
    queryKey: featureKeys.myFeatures(),
    queryFn: () => featureService.getMyFeatures(),
    staleTime: 5 * 60 * 1000, // 5 minutos - features não mudam frequentemente
    gcTime: 30 * 60 * 1000, // 30 minutos
    retry: 1,
  });
};

/**
 * Hook para verificar se uma feature específica está habilitada
 *
 * @param codigo - Código da feature (ex: 'WHATSAPP_NOTIFICATIONS')
 * @returns { isEnabled, isLoading, error }
 */
export const useFeatureFlag = (codigo: string) => {
  const { data, isLoading, error } = useQuery({
    queryKey: featureKeys.check(codigo),
    queryFn: () => featureService.checkFeature(codigo),
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    retry: 1,
    enabled: !!codigo,
  });

  return {
    isEnabled: data?.enabled ?? false,
    isLoading,
    error,
    data,
  };
};

/**
 * Hook para verificar múltiplas features de uma vez
 *
 * @param codigos - Array de códigos de features
 * @returns { features, isLoading, error, isEnabled(codigo) }
 */
export const useFeatureFlags = (codigos: string[]) => {
  const { data, isLoading, error } = useQuery({
    queryKey: featureKeys.checkBatch(codigos),
    queryFn: () => featureService.checkFeaturesBatch(codigos),
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    retry: 1,
    enabled: codigos.length > 0,
  });

  return {
    features: data ?? {},
    isLoading,
    error,
    isEnabled: (codigo: string) => data?.[codigo] ?? false,
  };
};

/**
 * Hook que usa o cache de todas as features para verificar uma específica
 * Mais eficiente quando você precisa verificar múltiplas features na mesma página
 */
export const useFeatureFlagFromCache = (codigo: string) => {
  const { data, isLoading, error } = useMyFeatures();

  return {
    isEnabled: data?.features?.[codigo] ?? false,
    isLoading,
    error,
  };
};

/**
 * Componente helper para renderização condicional baseada em feature flag
 *
 * Uso:
 * <FeatureGate feature="WHATSAPP_NOTIFICATIONS">
 *   <WhatsAppButton />
 * </FeatureGate>
 *
 * Ou com fallback:
 * <FeatureGate feature="PREMIUM_REPORTS" fallback={<UpgradePrompt />}>
 *   <PremiumReports />
 * </FeatureGate>
 */
import type { ReactNode } from 'react';

interface FeatureGateProps {
  feature: string;
  children: ReactNode;
  fallback?: ReactNode;
  loadingFallback?: ReactNode;
}

export const FeatureGate = ({
  feature,
  children,
  fallback = null,
  loadingFallback = null,
}: FeatureGateProps) => {
  const { isEnabled, isLoading } = useFeatureFlag(feature);

  if (isLoading) {
    return <>{loadingFallback}</>;
  }

  if (!isEnabled) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
};

/**
 * Componente para mostrar conteúdo apenas para features desabilitadas
 * Útil para mostrar prompts de upgrade
 */
interface FeatureDisabledProps {
  feature: string;
  children: ReactNode;
}

export const FeatureDisabled = ({ feature, children }: FeatureDisabledProps) => {
  const { isEnabled, isLoading } = useFeatureFlag(feature);

  if (isLoading || isEnabled) {
    return null;
  }

  return <>{children}</>;
};
