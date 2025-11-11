/**
 * React Query hook para contagem de OS por status
 */

import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '../services/dashboardService';
import { dashboardKeys } from './useDashboardStats';

export const useOSByStatus = () => {
  return useQuery({
    queryKey: dashboardKeys.osStatus(),
    queryFn: dashboardService.getOSByStatus,
    staleTime: 30 * 1000, // 30 segundos
    refetchInterval: 30 * 1000, // Auto-refresh a cada 30s
  });
};
