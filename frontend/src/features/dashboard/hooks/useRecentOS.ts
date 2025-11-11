/**
 * React Query hook para OS recentes
 */

import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '../services/dashboardService';
import { dashboardKeys } from './useDashboardStats';

export const useRecentOS = () => {
  return useQuery({
    queryKey: dashboardKeys.recentOS(),
    queryFn: dashboardService.getRecentOS,
    staleTime: 30 * 1000, // 30 segundos
    refetchInterval: 30 * 1000, // Auto-refresh a cada 30s
  });
};
