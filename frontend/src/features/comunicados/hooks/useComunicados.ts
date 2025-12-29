/**
 * React Query hooks for Comunicados (Oficina side)
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { comunicadoOficinaService } from '../services/comunicadoService';

export const comunicadoKeys = {
  all: ['comunicados-oficina'] as const,
  list: (page: number, size: number) => [...comunicadoKeys.all, 'list', page, size] as const,
  detail: (id: string) => [...comunicadoKeys.all, 'detail', id] as const,
  naoLidos: () => [...comunicadoKeys.all, 'nao-lidos'] as const,
  alerta: () => [...comunicadoKeys.all, 'alerta'] as const,
  login: () => [...comunicadoKeys.all, 'login'] as const,
};

/**
 * Hook para listar comunicados
 */
export const useComunicadosOficina = (page = 0, size = 20) => {
  return useQuery({
    queryKey: comunicadoKeys.list(page, size),
    queryFn: () => comunicadoOficinaService.findAll(page, size),
    staleTime: 30 * 1000, // 30 seconds
  });
};

/**
 * Hook para buscar detalhes de um comunicado
 */
export const useComunicadoDetail = (id?: string) => {
  return useQuery({
    queryKey: comunicadoKeys.detail(id || ''),
    queryFn: () => comunicadoOficinaService.findById(id!),
    enabled: !!id,
    staleTime: 30 * 1000,
  });
};

/**
 * Hook para contar não lidos
 */
export const useComunicadosNaoLidos = () => {
  return useQuery({
    queryKey: comunicadoKeys.naoLidos(),
    queryFn: () => comunicadoOficinaService.contarNaoLidos(),
    staleTime: 30 * 1000,
    refetchInterval: 60 * 1000, // Refresh every minute
  });
};

/**
 * Hook para alerta do dashboard
 */
export const useComunicadoAlerta = () => {
  return useQuery({
    queryKey: comunicadoKeys.alerta(),
    queryFn: () => comunicadoOficinaService.getAlerta(),
    staleTime: 30 * 1000,
    refetchInterval: 60 * 1000, // Refresh every minute
  });
};

/**
 * Hook para comunicados do login
 */
export const useComunicadosLogin = () => {
  return useQuery({
    queryKey: comunicadoKeys.login(),
    queryFn: () => comunicadoOficinaService.getComunicadosLogin(),
    staleTime: 30 * 1000,
  });
};

/**
 * Hook para confirmar leitura
 */
export const useConfirmarComunicado = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => comunicadoOficinaService.confirmar(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: comunicadoKeys.all });
      queryClient.invalidateQueries({ queryKey: comunicadoKeys.detail(id) });
    },
  });
};

/**
 * Hook para marcar todos como lidos
 */
export const useMarcarTodosLidos = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => comunicadoOficinaService.marcarTodosLidos(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: comunicadoKeys.all });
    },
  });
};
