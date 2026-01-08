/**
 * React Query hooks para o módulo de IA
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { iaService } from '../services/iaService';
import type {
  ConfiguracaoIARequest,
  AtualizarApiKeyRequest,
  DiagnosticoIARequest,
} from '../types';

// ===== Query Keys =====

export const iaKeys = {
  all: ['ia'] as const,
  configuracao: () => [...iaKeys.all, 'configuracao'] as const,
  disponibilidade: () => [...iaKeys.all, 'disponibilidade'] as const,
  estatisticas: () => [...iaKeys.all, 'estatisticas'] as const,
};

// ===== Hooks de Configuração =====

/**
 * Hook para buscar configuração de IA
 */
export const useConfiguracaoIA = () => {
  return useQuery({
    queryKey: iaKeys.configuracao(),
    queryFn: () => iaService.getConfiguracao(),
    staleTime: 5 * 60 * 1000, // 5 minutos
    retry: false, // Não tenta novamente se falhar (pode não ter configuração ainda)
  });
};

/**
 * Hook para atualizar configuração de IA
 */
export const useAtualizarConfiguracaoIA = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: ConfiguracaoIARequest) =>
      iaService.atualizarConfiguracao(request),
    onSuccess: (data) => {
      queryClient.setQueryData(iaKeys.configuracao(), data);
      queryClient.invalidateQueries({ queryKey: iaKeys.disponibilidade() });
    },
  });
};

/**
 * Hook para atualizar API key
 */
export const useAtualizarApiKey = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: AtualizarApiKeyRequest) =>
      iaService.atualizarApiKey(request),
    onSuccess: (data) => {
      queryClient.setQueryData(iaKeys.configuracao(), data);
      queryClient.invalidateQueries({ queryKey: iaKeys.disponibilidade() });
    },
  });
};

/**
 * Hook para remover API key
 */
export const useRemoverApiKey = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => iaService.removerApiKey(),
    onSuccess: (data) => {
      queryClient.setQueryData(iaKeys.configuracao(), data);
      queryClient.invalidateQueries({ queryKey: iaKeys.disponibilidade() });
    },
  });
};

// ===== Hooks de Diagnóstico =====

/**
 * Hook para verificar disponibilidade da IA
 */
export const useIADisponivel = () => {
  return useQuery({
    queryKey: iaKeys.disponibilidade(),
    queryFn: () => iaService.verificarDisponibilidade(),
    staleTime: 1 * 60 * 1000, // 1 minuto
    retry: false,
  });
};

/**
 * Hook para gerar diagnóstico
 */
export const useGerarDiagnostico = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: DiagnosticoIARequest) =>
      iaService.gerarDiagnostico(request),
    onSuccess: () => {
      // Atualiza estatísticas após gerar diagnóstico
      queryClient.invalidateQueries({ queryKey: iaKeys.estatisticas() });
      queryClient.invalidateQueries({ queryKey: iaKeys.configuracao() });
    },
  });
};

/**
 * Hook para buscar estatísticas de uso
 */
export const useEstatisticasIA = () => {
  return useQuery({
    queryKey: iaKeys.estatisticas(),
    queryFn: () => iaService.getEstatisticas(),
    staleTime: 2 * 60 * 1000, // 2 minutos
  });
};
