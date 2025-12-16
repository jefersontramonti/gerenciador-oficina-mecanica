/**
 * Hooks React Query para operações com Movimentações de Estoque
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import movimentacaoService from '../services/movimentacaoService';
import { pecaKeys } from './usePecas';
import type {
  CreateEntradaRequest,
  CreateSaidaRequest,
  CreateAjusteRequest,
  MovimentacaoFilters,
} from '../types';

// ==================== QUERY KEYS ====================

export const movimentacaoKeys = {
  all: ['movimentacoes'] as const,
  lists: () => [...movimentacaoKeys.all, 'list'] as const,
  list: (filters: MovimentacaoFilters) => [...movimentacaoKeys.lists(), filters] as const,
  historicoPeca: (pecaId: string, page: number) =>
    [...movimentacaoKeys.all, 'historico', pecaId, page] as const,
  porOS: (osId: string) =>
    [...movimentacaoKeys.all, 'os', osId] as const,
};

// ==================== QUERIES ====================

/**
 * Listar movimentações com filtros
 */
export const useMovimentacoes = (filters: MovimentacaoFilters = {}) => {
  return useQuery({
    queryKey: movimentacaoKeys.list(filters),
    queryFn: () => movimentacaoService.listarMovimentacoes(filters),
    staleTime: 2 * 60 * 1000, // 2 minutos
  });
};

/**
 * Obter histórico de movimentações de uma peça
 */
export const useHistoricoPeca = (pecaId?: string, page = 0, size = 20) => {
  return useQuery({
    queryKey: movimentacaoKeys.historicoPeca(pecaId!, page),
    queryFn: () => movimentacaoService.obterHistoricoPeca(pecaId!, page, size),
    enabled: !!pecaId,
    staleTime: 2 * 60 * 1000,
  });
};

/**
 * Obter movimentações de uma Ordem de Serviço
 * Nota: Este endpoint retorna lista completa (não paginada)
 */
export const useMovimentacoesPorOS = (osId?: string) => {
  return useQuery({
    queryKey: movimentacaoKeys.porOS(osId!),
    queryFn: () => movimentacaoService.obterMovimentacoesPorOS(osId!),
    enabled: !!osId,
    staleTime: 2 * 60 * 1000,
  });
};

// ==================== MUTATIONS ====================

/**
 * Registrar entrada de estoque
 */
export const useRegistrarEntrada = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateEntradaRequest) =>
      movimentacaoService.registrarEntrada(request),
    onSuccess: (data) => {
      // Invalidar cache da peça
      queryClient.invalidateQueries({ queryKey: pecaKeys.detail(data.peca.id) });
      queryClient.invalidateQueries({ queryKey: pecaKeys.lists() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.alertas() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.valorTotal() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.contadorBaixo() });

      // Invalidar cache de movimentações
      queryClient.invalidateQueries({ queryKey: movimentacaoKeys.lists() });
      queryClient.invalidateQueries({
        queryKey: movimentacaoKeys.historicoPeca(data.peca.id, 0)
      });

      toast.success('Entrada registrada com sucesso!', {
        description: `+${data.quantidade} ${data.peca.codigo}`,
      });
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao registrar entrada';
      toast.error('Erro ao registrar entrada', {
        description: message,
      });
    },
  });
};

/**
 * Registrar saída de estoque
 */
export const useRegistrarSaida = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateSaidaRequest) =>
      movimentacaoService.registrarSaida(request),
    onSuccess: (data) => {
      // Invalidar cache da peça
      queryClient.invalidateQueries({ queryKey: pecaKeys.detail(data.peca.id) });
      queryClient.invalidateQueries({ queryKey: pecaKeys.lists() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.alertas() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.valorTotal() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.contadorBaixo() });

      // Invalidar cache de movimentações
      queryClient.invalidateQueries({ queryKey: movimentacaoKeys.lists() });
      queryClient.invalidateQueries({
        queryKey: movimentacaoKeys.historicoPeca(data.peca.id, 0)
      });

      toast.success('Saída registrada com sucesso!', {
        description: `-${data.quantidade} ${data.peca.codigo}`,
      });
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao registrar saída';
      toast.error('Erro ao registrar saída', {
        description: message,
      });
    },
  });
};

/**
 * Registrar ajuste de inventário
 */
export const useRegistrarAjuste = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateAjusteRequest) =>
      movimentacaoService.registrarAjuste(request),
    onSuccess: (data) => {
      // Invalidar cache da peça
      queryClient.invalidateQueries({ queryKey: pecaKeys.detail(data.peca.id) });
      queryClient.invalidateQueries({ queryKey: pecaKeys.lists() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.alertas() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.valorTotal() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.contadorBaixo() });

      // Invalidar cache de movimentações
      queryClient.invalidateQueries({ queryKey: movimentacaoKeys.lists() });
      queryClient.invalidateQueries({
        queryKey: movimentacaoKeys.historicoPeca(data.peca.id, 0)
      });

      const diff = data.quantidadeAtual - data.quantidadeAnterior;
      const sinal = diff >= 0 ? '+' : '';

      toast.success('Ajuste de inventário registrado!', {
        description: `${data.peca.codigo}: ${data.quantidadeAnterior} → ${data.quantidadeAtual} (${sinal}${diff})`,
      });
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao registrar ajuste';
      toast.error('Erro ao registrar ajuste', {
        description: message,
      });
    },
  });
};
