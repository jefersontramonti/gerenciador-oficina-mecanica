/**
 * React Query hook para faturamento mensal
 */

import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '../services/dashboardService';
import { dashboardKeys } from './useDashboardStats';

export const useFaturamentoMensal = () => {
  return useQuery({
    queryKey: dashboardKeys.faturamento(),
    queryFn: dashboardService.getFaturamentoMensal,
    staleTime: 30 * 1000, // 30 segundos
    refetchInterval: 30 * 1000, // Auto-refresh a cada 30s
  });
};
