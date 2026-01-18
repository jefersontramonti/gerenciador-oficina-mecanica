/**
 * React Query hooks para os novos widgets do dashboard
 */

import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '../services/dashboardService';
import { dashboardKeys } from './useDashboardStats';

/**
 * Hook para estatísticas com variação percentual vs mês anterior
 */
export const useDashboardStatsComTrend = () => {
  return useQuery({
    queryKey: dashboardKeys.statsTrend(),
    queryFn: dashboardService.getStatsComTrend,
    staleTime: 60 * 1000, // 1 minuto
    refetchInterval: 60 * 1000, // Auto-refresh a cada 1 min
  });
};

/**
 * Hook para alertas dinâmicos do dashboard
 */
export const useDashboardAlertas = () => {
  return useQuery({
    queryKey: dashboardKeys.alertas(),
    queryFn: dashboardService.getAlertas,
    staleTime: 30 * 1000, // 30 segundos
    refetchInterval: 30 * 1000, // Auto-refresh a cada 30s
  });
};

/**
 * Hook para resumo de pagamentos (widget expansível)
 */
export const usePagamentosResumo = () => {
  return useQuery({
    queryKey: dashboardKeys.pagamentosResumo(),
    queryFn: dashboardService.getPagamentosResumo,
    staleTime: 60 * 1000,
    refetchInterval: 60 * 1000,
  });
};

/**
 * Hook para pagamentos agrupados por tipo (gráfico)
 */
export const usePagamentosPorTipo = () => {
  return useQuery({
    queryKey: dashboardKeys.pagamentosPorTipo(),
    queryFn: dashboardService.getPagamentosPorTipo,
    staleTime: 60 * 1000,
    refetchInterval: 60 * 1000,
  });
};

/**
 * Hook para resumo de manutenção preventiva (widget)
 */
export const useManutencaoResumo = () => {
  return useQuery({
    queryKey: dashboardKeys.manutencaoResumo(),
    queryFn: dashboardService.getManutencaoResumo,
    staleTime: 60 * 1000,
    refetchInterval: 60 * 1000,
  });
};

/**
 * Hook para próximas manutenções
 * @param dias Dias à frente para buscar (padrão 7)
 * @param limite Quantidade máxima de resultados (padrão 5)
 */
export const useProximasManutencoes = (dias = 7, limite = 5) => {
  return useQuery({
    queryKey: dashboardKeys.proximasManutencoes(dias, limite),
    queryFn: () => dashboardService.getProximasManutencoes(dias, limite),
    staleTime: 5 * 60 * 1000, // 5 minutos
    refetchInterval: 5 * 60 * 1000,
  });
};

/**
 * Hook para resumo de notas fiscais (widget)
 */
export const useNotasFiscaisResumo = () => {
  return useQuery({
    queryKey: dashboardKeys.notasFiscaisResumo(),
    queryFn: dashboardService.getNotasFiscaisResumo,
    staleTime: 60 * 1000,
    refetchInterval: 60 * 1000,
  });
};
