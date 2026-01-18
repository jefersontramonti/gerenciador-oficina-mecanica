/**
 * React Query hook para estatísticas gerais do dashboard
 */

import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '../services/dashboardService';

export const dashboardKeys = {
  all: ['dashboard'] as const,
  stats: () => [...dashboardKeys.all, 'stats'] as const,
  statsTrend: () => [...dashboardKeys.all, 'stats-trend'] as const,
  alertas: () => [...dashboardKeys.all, 'alertas'] as const,
  osStatus: () => [...dashboardKeys.all, 'os-status'] as const,
  faturamento: () => [...dashboardKeys.all, 'faturamento'] as const,
  recentOS: () => [...dashboardKeys.all, 'recent-os'] as const,
  pagamentosResumo: () => [...dashboardKeys.all, 'pagamentos-resumo'] as const,
  pagamentosPorTipo: () => [...dashboardKeys.all, 'pagamentos-por-tipo'] as const,
  manutencaoResumo: () => [...dashboardKeys.all, 'manutencao-resumo'] as const,
  proximasManutencoes: (dias: number, limite: number) =>
    [...dashboardKeys.all, 'proximas-manutencoes', { dias, limite }] as const,
  notasFiscaisResumo: () => [...dashboardKeys.all, 'notas-fiscais-resumo'] as const,
};

export const useDashboardStats = () => {
  return useQuery({
    queryKey: dashboardKeys.stats(),
    queryFn: dashboardService.getStats,
    staleTime: 30 * 1000, // 30 segundos
    refetchInterval: 30 * 1000, // Auto-refresh a cada 30s
  });
};
