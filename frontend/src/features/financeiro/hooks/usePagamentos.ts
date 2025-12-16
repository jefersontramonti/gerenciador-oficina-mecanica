/**
 * Custom hooks para operações de pagamentos usando React Query
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { pagamentosApi } from '../api/pagamentosApi';
import type {
  PagamentoRequestDTO,
  ConfirmarPagamentoDTO,
  FiltrosPagamento
} from '../types/pagamento';

// Query keys
export const pagamentosKeys = {
  all: ['pagamentos'] as const,
  lists: () => [...pagamentosKeys.all, 'list'] as const,
  list: (filtros?: FiltrosPagamento, page?: number) =>
    [...pagamentosKeys.lists(), filtros, page] as const,
  details: () => [...pagamentosKeys.all, 'detail'] as const,
  detail: (id: string) => [...pagamentosKeys.details(), id] as const,
  porOS: (ordemServicoId: string) =>
    [...pagamentosKeys.all, 'ordem-servico', ordemServicoId] as const,
  resumo: (ordemServicoId: string) =>
    [...pagamentosKeys.all, 'resumo', ordemServicoId] as const
};

/**
 * Hook para listar pagamentos com filtros
 */
export function usePagamentos(
  filtros?: FiltrosPagamento,
  page = 0,
  size = 20
) {
  return useQuery({
    queryKey: pagamentosKeys.list(filtros, page),
    queryFn: () => pagamentosApi.listar(filtros, page, size),
    staleTime: 30000 // 30 segundos
  });
}

/**
 * Hook para buscar um pagamento por ID
 */
export function usePagamento(id: string) {
  return useQuery({
    queryKey: pagamentosKeys.detail(id),
    queryFn: () => pagamentosApi.buscarPorId(id),
    enabled: !!id
  });
}

/**
 * Hook para listar pagamentos de uma OS
 */
export function usePagamentosPorOS(ordemServicoId: string) {
  return useQuery({
    queryKey: pagamentosKeys.porOS(ordemServicoId),
    queryFn: () => pagamentosApi.listarPorOrdemServico(ordemServicoId),
    enabled: !!ordemServicoId
  });
}

/**
 * Hook para buscar resumo financeiro de uma OS
 * Com auto-refresh a cada 10 segundos para atualização em tempo real
 */
export function useResumoFinanceiro(ordemServicoId: string) {
  return useQuery({
    queryKey: pagamentosKeys.resumo(ordemServicoId),
    queryFn: () => pagamentosApi.resumoFinanceiro(ordemServicoId),
    enabled: !!ordemServicoId,
    refetchInterval: 10000, // Atualiza a cada 10 segundos
    staleTime: 5000 // Considera dados frescos por 5 segundos
  });
}

/**
 * Hook para criar um pagamento
 */
export function useCriarPagamento() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: PagamentoRequestDTO) => pagamentosApi.criar(data),
    onSuccess: (data) => {
      // Invalidar queries relacionadas
      queryClient.invalidateQueries({ queryKey: pagamentosKeys.lists() });
      queryClient.invalidateQueries({
        queryKey: pagamentosKeys.porOS(data.ordemServicoId)
      });
      queryClient.invalidateQueries({
        queryKey: pagamentosKeys.resumo(data.ordemServicoId)
      });

      toast.success('Pagamento criado com sucesso!');
    },
    onError: (error: any) => {
      toast.error(
        error.response?.data?.message || 'Erro ao criar pagamento'
      );
    }
  });
}

/**
 * Hook para confirmar um pagamento
 */
export function useConfirmarPagamento() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ConfirmarPagamentoDTO }) =>
      pagamentosApi.confirmar(id, data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: pagamentosKeys.lists() });
      queryClient.invalidateQueries({
        queryKey: pagamentosKeys.detail(data.id)
      });
      queryClient.invalidateQueries({
        queryKey: pagamentosKeys.porOS(data.ordemServicoId)
      });
      queryClient.invalidateQueries({
        queryKey: pagamentosKeys.resumo(data.ordemServicoId)
      });

      toast.success('Pagamento confirmado com sucesso!');
    },
    onError: (error: any) => {
      toast.error(
        error.response?.data?.message || 'Erro ao confirmar pagamento'
      );
    }
  });
}

/**
 * Hook para cancelar um pagamento
 */
export function useCancelarPagamento() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => pagamentosApi.cancelar(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: pagamentosKeys.lists() });
      queryClient.invalidateQueries({ queryKey: pagamentosKeys.detail(id) });

      toast.success('Pagamento cancelado com sucesso!');
    },
    onError: (error: any) => {
      toast.error(
        error.response?.data?.message || 'Erro ao cancelar pagamento'
      );
    }
  });
}

/**
 * Hook para estornar um pagamento
 */
export function useEstornarPagamento() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => pagamentosApi.estornar(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: pagamentosKeys.lists() });
      queryClient.invalidateQueries({ queryKey: pagamentosKeys.detail(id) });

      toast.success('Pagamento estornado com sucesso!');
    },
    onError: (error: any) => {
      toast.error(
        error.response?.data?.message || 'Erro ao estornar pagamento'
      );
    }
  });
}
