/**
 * Custom hooks para operações de Notas Fiscais usando React Query
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { notasFiscaisApi } from '../api/notasFiscaisApi';
import type {
  NotaFiscalRequestDTO,
  FiltrosNotaFiscal,
  StatusNotaFiscal,
} from '../types/notaFiscal';

// Query keys
export const notasFiscaisKeys = {
  all: ['notas-fiscais'] as const,
  lists: () => [...notasFiscaisKeys.all, 'list'] as const,
  list: (filtros?: FiltrosNotaFiscal, page?: number) =>
    [...notasFiscaisKeys.lists(), filtros, page] as const,
  details: () => [...notasFiscaisKeys.all, 'detail'] as const,
  detail: (id: string) => [...notasFiscaisKeys.details(), id] as const,
  porOS: (ordemServicoId: string) =>
    [...notasFiscaisKeys.all, 'ordem-servico', ordemServicoId] as const,
  porStatus: (status: StatusNotaFiscal, page?: number) =>
    [...notasFiscaisKeys.all, 'status', status, page] as const,
};

/**
 * Hook para listar notas fiscais com filtros
 */
export function useNotasFiscais(
  filtros?: FiltrosNotaFiscal,
  page = 0,
  size = 20
) {
  return useQuery({
    queryKey: notasFiscaisKeys.list(filtros, page),
    queryFn: () => notasFiscaisApi.listar(filtros, page, size),
    staleTime: 30000, // 30 segundos
  });
}

/**
 * Hook para buscar uma nota fiscal por ID
 */
export function useNotaFiscal(id?: string) {
  return useQuery({
    queryKey: notasFiscaisKeys.detail(id || ''),
    queryFn: () => notasFiscaisApi.buscarPorId(id!),
    enabled: !!id,
    staleTime: 60000, // 1 minuto
  });
}

/**
 * Hook para listar notas fiscais de uma OS
 */
export function useNotasFiscaisPorOS(ordemServicoId?: string) {
  return useQuery({
    queryKey: notasFiscaisKeys.porOS(ordemServicoId || ''),
    queryFn: () => notasFiscaisApi.listarPorOrdemServico(ordemServicoId!),
    enabled: !!ordemServicoId,
  });
}

/**
 * Hook para buscar notas fiscais por status
 */
export function useNotasFiscaisPorStatus(
  status: StatusNotaFiscal,
  page = 0,
  size = 20
) {
  return useQuery({
    queryKey: notasFiscaisKeys.porStatus(status, page),
    queryFn: () => notasFiscaisApi.buscarPorStatus(status, page, size),
  });
}

/**
 * Hook para criar uma nota fiscal
 */
export function useCriarNotaFiscal() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: NotaFiscalRequestDTO) => notasFiscaisApi.criar(data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: notasFiscaisKeys.lists() });
      queryClient.invalidateQueries({
        queryKey: notasFiscaisKeys.porOS(data.ordemServicoId),
      });

      toast.success('Nota fiscal criada com sucesso!');
    },
    onError: (error: any) => {
      toast.error(
        error.response?.data?.message || 'Erro ao criar nota fiscal'
      );
    },
  });
}

/**
 * Hook para atualizar uma nota fiscal
 */
export function useAtualizarNotaFiscal() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: NotaFiscalRequestDTO }) =>
      notasFiscaisApi.atualizar(id, data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: notasFiscaisKeys.lists() });
      queryClient.invalidateQueries({
        queryKey: notasFiscaisKeys.detail(data.id),
      });
      queryClient.invalidateQueries({
        queryKey: notasFiscaisKeys.porOS(data.ordemServicoId),
      });

      toast.success('Nota fiscal atualizada com sucesso!');
    },
    onError: (error: any) => {
      toast.error(
        error.response?.data?.message || 'Erro ao atualizar nota fiscal'
      );
    },
  });
}

/**
 * Hook para deletar uma nota fiscal
 */
export function useDeletarNotaFiscal() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => notasFiscaisApi.deletar(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: notasFiscaisKeys.lists() });
      queryClient.removeQueries({ queryKey: notasFiscaisKeys.detail(id) });

      toast.success('Nota fiscal deletada com sucesso!');
    },
    onError: (error: any) => {
      toast.error(
        error.response?.data?.message || 'Erro ao deletar nota fiscal'
      );
    },
  });
}

/**
 * Hook para verificar se existe nota fiscal para uma OS
 */
export function useExisteNotaFiscalParaOS(ordemServicoId?: string) {
  return useQuery({
    queryKey: [...notasFiscaisKeys.all, 'existe', ordemServicoId],
    queryFn: () => notasFiscaisApi.existeParaOS(ordemServicoId!),
    enabled: !!ordemServicoId,
  });
}

/**
 * Hook para buscar próximo número disponível
 */
export function useProximoNumero(serie: number) {
  return useQuery({
    queryKey: [...notasFiscaisKeys.all, 'proximo-numero', serie],
    queryFn: () => notasFiscaisApi.buscarProximoNumero(serie),
    enabled: !!serie,
  });
}
