/**
 * React Query hooks for plan information
 */

import { useQuery } from '@tanstack/react-query';
import { planoService } from '../services/planoService';

export const planoKeys = {
  all: ['plano'] as const,
  meuPlano: () => [...planoKeys.all, 'meu-plano'] as const,
  feature: (codigo: string) => [...planoKeys.all, 'feature', codigo] as const,
  features: (codigos: string[]) => [...planoKeys.all, 'features', codigos] as const,
};

/**
 * Hook to fetch the current workshop's plan information
 */
export const useMeuPlano = () => {
  return useQuery({
    queryKey: planoKeys.meuPlano(),
    queryFn: () => planoService.getMeuPlano(),
    staleTime: 5 * 60 * 1000, // 5 minutes - plan info doesn't change often
    refetchOnWindowFocus: false,
  });
};

/**
 * Hook to check if a specific feature is enabled
 */
export const useCheckFeature = (codigo: string, enabled = true) => {
  return useQuery({
    queryKey: planoKeys.feature(codigo),
    queryFn: () => planoService.checkFeature(codigo),
    enabled: enabled && !!codigo,
    staleTime: 5 * 60 * 1000,
  });
};

/**
 * Hook to check multiple features at once
 */
export const useCheckFeatures = (codigos: string[], enabled = true) => {
  return useQuery({
    queryKey: planoKeys.features(codigos),
    queryFn: () => planoService.checkFeatures(codigos),
    enabled: enabled && codigos.length > 0,
    staleTime: 5 * 60 * 1000,
  });
};
