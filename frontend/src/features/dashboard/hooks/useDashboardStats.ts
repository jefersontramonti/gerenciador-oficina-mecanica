/**
 * React Query hook para estatísticas gerais do dashboard
 */

import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '../services/dashboardService';

export const dashboardKeys = {
  all: ['dashboard'] as const,
  stats: () => [...dashboardKeys.all, 'stats'] as const,
  osStatus: () => [...dashboardKeys.all, 'os-status'] as const,
  faturamento: () => [...dashboardKeys.all, 'faturamento'] as const,
  recentOS: () => [...dashboardKeys.all, 'recent-os'] as const,
};

export const useDashboardStats = () => {
  return useQuery({
    queryKey: dashboardKeys.stats(),
    queryFn: dashboardService.getStats,
    staleTime: 30 * 1000, // 30 segundos
    refetchInterval: 30 * 1000, // Auto-refresh a cada 30s
  });
};
