import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import conciliacaoService from '../services/conciliacaoService';
import type { PageResponse } from '../services/conciliacaoService';
import type {
  ExtratoBancarioDTO,
  TransacaoExtratoDTO,
  ConciliacaoLoteDTO,
  ConciliacaoLoteResult,
  ExtratoResumo,
} from '../types/conciliacao';

// Query keys
export const conciliacaoKeys = {
  all: ['conciliacao'] as const,
  extratos: () => [...conciliacaoKeys.all, 'extratos'] as const,
  extratosLista: (page: number, size: number) =>
    [...conciliacaoKeys.extratos(), { page, size }] as const,
  extrato: (id: string) => [...conciliacaoKeys.extratos(), id] as const,
  transacoes: (extratoId: string) =>
    [...conciliacaoKeys.all, 'transacoes', extratoId] as const,
  resumo: (extratoId: string) =>
    [...conciliacaoKeys.all, 'resumo', extratoId] as const,
};

/**
 * Hook para listar extratos bancários
 */
export function useExtratos(page = 0, size = 20) {
  return useQuery<PageResponse<ExtratoBancarioDTO>>({
    queryKey: conciliacaoKeys.extratosLista(page, size),
    queryFn: () => conciliacaoService.listarExtratos(page, size),
  });
}

/**
 * Hook para buscar um extrato específico
 */
export function useExtrato(id: string) {
  return useQuery<ExtratoBancarioDTO>({
    queryKey: conciliacaoKeys.extrato(id),
    queryFn: () => conciliacaoService.buscarExtrato(id),
    enabled: !!id,
  });
}

/**
 * Hook para listar transações com sugestões
 */
export function useTransacoesComSugestoes(extratoId: string) {
  return useQuery<TransacaoExtratoDTO[]>({
    queryKey: conciliacaoKeys.transacoes(extratoId),
    queryFn: () => conciliacaoService.listarTransacoesComSugestoes(extratoId),
    enabled: !!extratoId,
  });
}

/**
 * Hook para buscar resumo do extrato
 */
export function useExtratoResumo(extratoId: string) {
  return useQuery<ExtratoResumo>({
    queryKey: conciliacaoKeys.resumo(extratoId),
    queryFn: () => conciliacaoService.buscarResumo(extratoId),
    enabled: !!extratoId,
  });
}

/**
 * Hook para importar extrato OFX
 */
export function useImportarExtrato() {
  const queryClient = useQueryClient();

  return useMutation<
    ExtratoBancarioDTO,
    Error,
    { arquivo: File; contaBancariaId?: string }
  >({
    mutationFn: ({ arquivo, contaBancariaId }) =>
      conciliacaoService.importarExtrato(arquivo, contaBancariaId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: conciliacaoKeys.extratos() });
      toast.success(
        `Extrato importado: ${data.totalTransacoes} transações, ${data.totalConciliadas} conciliadas automaticamente`
      );
    },
    onError: (error: Error & { response?: { data?: { message?: string } } }) => {
      const message =
        error.response?.data?.message ||
        error.message ||
        'Erro ao importar extrato';
      toast.error(message);
    },
  });
}

/**
 * Hook para conciliar uma transação
 */
export function useConciliarTransacao() {
  const queryClient = useQueryClient();

  return useMutation<
    TransacaoExtratoDTO,
    Error,
    { transacaoId: string; pagamentoId: string; extratoId: string }
  >({
    mutationFn: ({ transacaoId, pagamentoId }) =>
      conciliacaoService.conciliarTransacao(transacaoId, pagamentoId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: conciliacaoKeys.transacoes(variables.extratoId),
      });
      queryClient.invalidateQueries({
        queryKey: conciliacaoKeys.extrato(variables.extratoId),
      });
      queryClient.invalidateQueries({
        queryKey: conciliacaoKeys.resumo(variables.extratoId),
      });
      toast.success('Transação conciliada');
    },
    onError: (error: Error & { response?: { data?: { message?: string } } }) => {
      const message =
        error.response?.data?.message ||
        error.message ||
        'Erro ao conciliar transação';
      toast.error(message);
    },
  });
}

/**
 * Hook para ignorar uma transação
 */
export function useIgnorarTransacao() {
  const queryClient = useQueryClient();

  return useMutation<
    TransacaoExtratoDTO,
    Error,
    { transacaoId: string; observacao?: string; extratoId: string }
  >({
    mutationFn: ({ transacaoId, observacao }) =>
      conciliacaoService.ignorarTransacao(transacaoId, observacao),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: conciliacaoKeys.transacoes(variables.extratoId),
      });
      queryClient.invalidateQueries({
        queryKey: conciliacaoKeys.extrato(variables.extratoId),
      });
      queryClient.invalidateQueries({
        queryKey: conciliacaoKeys.resumo(variables.extratoId),
      });
      toast.success('Transação ignorada');
    },
    onError: (error: Error & { response?: { data?: { message?: string } } }) => {
      const message =
        error.response?.data?.message ||
        error.message ||
        'Erro ao ignorar transação';
      toast.error(message);
    },
  });
}

/**
 * Hook para desconciliar uma transação
 */
export function useDesconciliarTransacao() {
  const queryClient = useQueryClient();

  return useMutation<
    TransacaoExtratoDTO,
    Error,
    { transacaoId: string; extratoId: string }
  >({
    mutationFn: ({ transacaoId }) =>
      conciliacaoService.desconciliarTransacao(transacaoId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: conciliacaoKeys.transacoes(variables.extratoId),
      });
      queryClient.invalidateQueries({
        queryKey: conciliacaoKeys.extrato(variables.extratoId),
      });
      queryClient.invalidateQueries({
        queryKey: conciliacaoKeys.resumo(variables.extratoId),
      });
      toast.success('Conciliação desfeita');
    },
    onError: (error: Error & { response?: { data?: { message?: string } } }) => {
      const message =
        error.response?.data?.message ||
        error.message ||
        'Erro ao desfazer conciliação';
      toast.error(message);
    },
  });
}

/**
 * Hook para conciliação em lote
 */
export function useConciliarEmLote() {
  const queryClient = useQueryClient();

  return useMutation<
    ConciliacaoLoteResult,
    Error,
    { request: ConciliacaoLoteDTO; extratoId: string }
  >({
    mutationFn: ({ request }) => conciliacaoService.conciliarEmLote(request),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({
        queryKey: conciliacaoKeys.transacoes(variables.extratoId),
      });
      queryClient.invalidateQueries({
        queryKey: conciliacaoKeys.extrato(variables.extratoId),
      });
      queryClient.invalidateQueries({
        queryKey: conciliacaoKeys.resumo(variables.extratoId),
      });
      toast.success(
        `Lote processado: ${data.conciliadas} conciliadas, ${data.ignoradas} ignoradas, ${data.erros} erros`
      );
    },
    onError: (error: Error & { response?: { data?: { message?: string } } }) => {
      const message =
        error.response?.data?.message ||
        error.message ||
        'Erro ao processar lote';
      toast.error(message);
    },
  });
}
