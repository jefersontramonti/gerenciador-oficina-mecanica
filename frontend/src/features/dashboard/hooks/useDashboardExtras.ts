/**
 * Hook para buscar indicadores extras do dashboard
 */

import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '../services/dashboardService';

export const useDashboardExtras = () => {
  return useQuery({
    queryKey: ['dashboard', 'extras'],
    queryFn: () => dashboardService.getExtras(),
    staleTime: 5 * 60 * 1000, // 5 minutos
    refetchOnWindowFocus: false,
  });
};
