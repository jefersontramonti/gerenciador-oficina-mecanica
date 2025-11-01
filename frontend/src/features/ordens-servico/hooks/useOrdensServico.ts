/**
 * React Query hooks para gerenciamento de ordens de serviço
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ordemServicoService } from '../services/ordemServicoService';
import type {
  OrdemServicoFilters,
  CreateOrdemServicoRequest,
  UpdateOrdemServicoRequest,
  CancelarOrdemServicoRequest,
} from '../types';

/**
 * Query keys para cache do React Query
 */
export const ordemServicoKeys = {
  all: ['ordens-servico'] as const,
  lists: () => [...ordemServicoKeys.all, 'list'] as const,
  list: (filters: OrdemServicoFilters) => [...ordemServicoKeys.lists(), filters] as const,
  details: () => [...ordemServicoKeys.all, 'detail'] as const,
  detail: (id: string) => [...ordemServicoKeys.details(), id] as const,
  byNumero: (numero: number) => [...ordemServicoKeys.all, 'numero', numero] as const,
  historicoVeiculo: (veiculoId: string) => [
    ...ordemServicoKeys.all,
    'historico',
    veiculoId,
  ] as const,
  dashboardContagem: () => [...ordemServicoKeys.all, 'dashboard', 'contagem'] as const,
  dashboardFaturamento: (dataInicio: string, dataFim: string) =>
    [...ordemServicoKeys.all, 'dashboard', 'faturamento', dataInicio, dataFim] as const,
};

/**
 * Hook para listar ordens de serviço com filtros
 */
export const useOrdensServico = (filters: OrdemServicoFilters = {}) => {
  return useQuery({
    queryKey: ordemServicoKeys.list(filters),
    queryFn: () => ordemServicoService.findAll(filters),
    staleTime: 1 * 60 * 1000, // 1 minuto
  });
};

/**
 * Hook para buscar ordem de serviço por ID
 */
export const useOrdemServico = (id?: string) => {
  return useQuery({
    queryKey: ordemServicoKeys.detail(id || ''),
    queryFn: () => ordemServicoService.findById(id!),
    enabled: !!id,
    staleTime: 2 * 60 * 1000, // 2 minutos
  });
};

/**
 * Hook para buscar ordem de serviço por número
 */
export const useOrdemServicoByNumero = (numero: number, enabled = true) => {
  return useQuery({
    queryKey: ordemServicoKeys.byNumero(numero),
    queryFn: () => ordemServicoService.findByNumero(numero),
    enabled: enabled && !!numero,
    staleTime: 2 * 60 * 1000,
  });
};

/**
 * Hook para buscar histórico de OS de um veículo
 */
export const useHistoricoVeiculo = (
  veiculoId: string,
  page = 0,
  size = 20,
  sort = 'numero,desc',
  enabled = true
) => {
  return useQuery({
    queryKey: ordemServicoKeys.historicoVeiculo(veiculoId),
    queryFn: () => ordemServicoService.findHistoricoVeiculo(veiculoId, page, size, sort),
    enabled: enabled && !!veiculoId,
    staleTime: 1 * 60 * 1000,
  });
};

/**
 * Hook para obter contagem de OS por status (dashboard)
 */
export const useDashboardContagem = () => {
  return useQuery({
    queryKey: ordemServicoKeys.dashboardContagem(),
    queryFn: () => ordemServicoService.getDashboardContagem(),
    staleTime: 30 * 1000, // 30 segundos (dados dinâmicos)
  });
};

/**
 * Hook para obter faturamento do período (dashboard)
 */
export const useDashboardFaturamento = (dataInicio: string, dataFim: string, enabled = true) => {
  return useQuery({
    queryKey: ordemServicoKeys.dashboardFaturamento(dataInicio, dataFim),
    queryFn: () => ordemServicoService.getDashboardFaturamento(dataInicio, dataFim),
    enabled: enabled && !!dataInicio && !!dataFim,
    staleTime: 1 * 60 * 1000, // 1 minuto
  });
};

/**
 * Hook para criar nova ordem de serviço
 */
export const useCreateOrdemServico = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateOrdemServicoRequest) => ordemServicoService.create(data),
    onSuccess: (newOS) => {
      // Invalidar listas
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.lists() });

      // Invalidar histórico do veículo
      queryClient.invalidateQueries({
        queryKey: ordemServicoKeys.historicoVeiculo(newOS.veiculoId),
      });

      // Invalidar dashboard
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.dashboardContagem() });
    },
  });
};

/**
 * Hook para atualizar ordem de serviço
 */
export const useUpdateOrdemServico = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateOrdemServicoRequest }) =>
      ordemServicoService.update(id, data),
    onSuccess: (updatedOS) => {
      // Invalidar a OS específica
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.detail(updatedOS.id) });

      // Invalidar listas
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.lists() });

      // Invalidar histórico do veículo
      queryClient.invalidateQueries({
        queryKey: ordemServicoKeys.historicoVeiculo(updatedOS.veiculoId),
      });

      // Invalidar dashboard
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.dashboardContagem() });
    },
  });
};

/**
 * Hook para aprovar ordem de serviço
 */
export const useAprovarOrdemServico = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, aprovadoPeloCliente }: { id: string; aprovadoPeloCliente?: boolean }) =>
      ordemServicoService.aprovar(id, aprovadoPeloCliente),
    onSuccess: (_, { id }) => {
      // Invalidar a OS específica
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.detail(id) });

      // Invalidar listas
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.lists() });

      // Invalidar dashboard
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.dashboardContagem() });
    },
  });
};

/**
 * Hook para iniciar ordem de serviço
 */
export const useIniciarOrdemServico = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => ordemServicoService.iniciar(id),
    onSuccess: (_, id) => {
      // Invalidar a OS específica
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.detail(id) });

      // Invalidar listas
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.lists() });

      // Invalidar dashboard
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.dashboardContagem() });
    },
  });
};

/**
 * Hook para finalizar ordem de serviço
 */
export const useFinalizarOrdemServico = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => ordemServicoService.finalizar(id),
    onSuccess: (_, id) => {
      // Invalidar a OS específica
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.detail(id) });

      // Invalidar listas
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.lists() });

      // Invalidar dashboard
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.dashboardContagem() });
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.all }); // Faturamento pode ter mudado
    },
  });
};

/**
 * Hook para entregar ordem de serviço
 */
export const useEntregarOrdemServico = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => ordemServicoService.entregar(id),
    onSuccess: (_, id) => {
      // Invalidar a OS específica
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.detail(id) });

      // Invalidar listas
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.lists() });

      // Invalidar dashboard
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.dashboardContagem() });
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.all }); // Faturamento pode ter mudado
    },
  });
};

/**
 * Hook para cancelar ordem de serviço
 */
export const useCancelarOrdemServico = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CancelarOrdemServicoRequest }) =>
      ordemServicoService.cancelar(id, data),
    onSuccess: (_, { id }) => {
      // Invalidar a OS específica
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.detail(id) });

      // Invalidar listas
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.lists() });

      // Invalidar dashboard
      queryClient.invalidateQueries({ queryKey: ordemServicoKeys.dashboardContagem() });
    },
  });
};

/**
 * Hook para gerar PDF da ordem de serviço
 */
export const useGerarPDF = () => {
  return useMutation({
    mutationFn: (id: string) => ordemServicoService.gerarPDF(id),
  });
};
