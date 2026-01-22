/**
 * React Query hooks para Parcelamento
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { parcelamentoService } from '../services/parcelamentoService';
import type {
  ConfiguracaoParcelamentoRequest,
  TabelaJurosRequest,
} from '../types/parcelamento';

// Query Keys
export const parcelamentoKeys = {
  all: ['parcelamento'] as const,
  configuracao: () => [...parcelamentoKeys.all, 'configuracao'] as const,
  faixasJuros: () => [...parcelamentoKeys.all, 'faixas-juros'] as const,
  simulacao: (valor: number) => [...parcelamentoKeys.all, 'simulacao', valor] as const,
};

// ========== Configuração ==========

/**
 * Hook para buscar configuração de parcelamento
 */
export const useConfiguracaoParcelamento = () => {
  return useQuery({
    queryKey: parcelamentoKeys.configuracao(),
    queryFn: parcelamentoService.buscarConfiguracao,
    staleTime: 5 * 60 * 1000, // 5 minutos
  });
};

/**
 * Hook para salvar configuração de parcelamento
 */
export const useSalvarConfiguracaoParcelamento = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: ConfiguracaoParcelamentoRequest) =>
      parcelamentoService.salvarConfiguracao(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: parcelamentoKeys.configuracao() });
    },
  });
};

// ========== Faixas de Juros ==========

/**
 * Hook para listar faixas de juros
 */
export const useFaixasJuros = () => {
  return useQuery({
    queryKey: parcelamentoKeys.faixasJuros(),
    queryFn: parcelamentoService.listarFaixasJuros,
    staleTime: 5 * 60 * 1000,
  });
};

/**
 * Hook para criar faixa de juros
 */
export const useCriarFaixaJuros = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: TabelaJurosRequest) => parcelamentoService.criarFaixaJuros(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: parcelamentoKeys.faixasJuros() });
      queryClient.invalidateQueries({ queryKey: parcelamentoKeys.configuracao() });
    },
  });
};

/**
 * Hook para atualizar faixa de juros
 */
export const useAtualizarFaixaJuros = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: TabelaJurosRequest }) =>
      parcelamentoService.atualizarFaixaJuros(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: parcelamentoKeys.faixasJuros() });
      queryClient.invalidateQueries({ queryKey: parcelamentoKeys.configuracao() });
    },
  });
};

/**
 * Hook para remover faixa de juros
 */
export const useRemoverFaixaJuros = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => parcelamentoService.removerFaixaJuros(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: parcelamentoKeys.faixasJuros() });
      queryClient.invalidateQueries({ queryKey: parcelamentoKeys.configuracao() });
    },
  });
};

// ========== Simulação ==========

/**
 * Hook para simular parcelamento
 */
export const useSimulacaoParcelamento = (valor?: number) => {
  return useQuery({
    queryKey: parcelamentoKeys.simulacao(valor || 0),
    queryFn: () => parcelamentoService.simular(valor!),
    enabled: !!valor && valor > 0,
    staleTime: 1 * 60 * 1000, // 1 minuto
  });
};
