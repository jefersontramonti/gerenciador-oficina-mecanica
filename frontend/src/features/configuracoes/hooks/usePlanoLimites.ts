import { useQuery } from '@tanstack/react-query';
import { planoLimitesService } from '../services/planoLimitesService';
import type { UsoLimites } from '../types/planoLimites';

export const PLANO_LIMITES_QUERY_KEY = ['plano-limites'];

/**
 * Hook para obter uso atual vs limites do plano
 */
export function usePlanoLimites() {
  return useQuery<UsoLimites>({
    queryKey: PLANO_LIMITES_QUERY_KEY,
    queryFn: () => planoLimitesService.obterUsoAtual(),
    staleTime: 5 * 60 * 1000, // 5 minutos
    refetchOnWindowFocus: false,
  });
}
