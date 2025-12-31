import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSelector } from 'react-redux';
import type { RootState } from '@/shared/store';
import { oficinaService } from '../services/oficinaService';
import type { UpdateOficinaRequest } from '../types';

/**
 * Query keys for React Query
 */
export const oficinaKeys = {
  all: ['oficina'] as const,
  detail: (id: string) => [...oficinaKeys.all, 'detail', id] as const,
  current: () => [...oficinaKeys.all, 'current'] as const,
};

/**
 * Hook to get the current user's oficinaId from Redux
 */
export const useCurrentOficinaId = (): string | null => {
  return useSelector((state: RootState) => state.auth.user?.oficinaId ?? null);
};

/**
 * Hook to fetch the current user's oficina
 *
 * Automatically uses the oficinaId from the authenticated user.
 * Returns null for SUPER_ADMIN (who has no oficina).
 */
export const useOficina = () => {
  const oficinaId = useCurrentOficinaId();

  return useQuery({
    queryKey: oficinaKeys.detail(oficinaId || ''),
    queryFn: () => oficinaService.findById(oficinaId!),
    enabled: !!oficinaId,
    staleTime: 5 * 60 * 1000, // 5 minutes (oficina data rarely changes)
  });
};

/**
 * Hook to fetch oficina by specific ID
 *
 * For admin pages that may need to view other oficinas.
 *
 * @param id - Oficina ID (optional)
 */
export const useOficinaById = (id?: string) => {
  return useQuery({
    queryKey: oficinaKeys.detail(id || ''),
    queryFn: () => oficinaService.findById(id!),
    enabled: !!id,
    staleTime: 5 * 60 * 1000,
  });
};

/**
 * Hook to update oficina data
 *
 * Generic update that can handle any partial update.
 */
export const useUpdateOficina = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateOficinaRequest }) =>
      oficinaService.update(id, data),
    onSuccess: (updatedOficina) => {
      // Update the specific oficina in cache
      queryClient.setQueryData(
        oficinaKeys.detail(updatedOficina.id),
        updatedOficina
      );
    },
  });
};

/**
 * Hook to update basic oficina info
 *
 * Used by the "Dados Basicos" form tab.
 */
export const useUpdateOficinaBasico = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: Parameters<typeof oficinaService.updateBasico>[1];
    }) => oficinaService.updateBasico(id, data),
    onSuccess: (updatedOficina) => {
      queryClient.setQueryData(
        oficinaKeys.detail(updatedOficina.id),
        updatedOficina
      );
    },
  });
};

/**
 * Hook to update operational oficina info
 *
 * Used by the "Operacional" form tab.
 */
export const useUpdateOficinaOperacional = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: Parameters<typeof oficinaService.updateOperacional>[1];
    }) => oficinaService.updateOperacional(id, data),
    onSuccess: (updatedOficina) => {
      queryClient.setQueryData(
        oficinaKeys.detail(updatedOficina.id),
        updatedOficina
      );
    },
  });
};

/**
 * Hook to update financial oficina info
 *
 * Used by the "Financeiro" form tab.
 */
export const useUpdateOficinaFinanceiro = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: Parameters<typeof oficinaService.updateFinanceiro>[1];
    }) => oficinaService.updateFinanceiro(id, data),
    onSuccess: (updatedOficina) => {
      queryClient.setQueryData(
        oficinaKeys.detail(updatedOficina.id),
        updatedOficina
      );
    },
  });
};

/**
 * Hook to update fiscal oficina info
 *
 * Used by the "Fiscal" form tab.
 */
export const useUpdateOficinaFiscal = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: Parameters<typeof oficinaService.updateFiscal>[1];
    }) => oficinaService.updateFiscal(id, data),
    onSuccess: (updatedOficina) => {
      queryClient.setQueryData(
        oficinaKeys.detail(updatedOficina.id),
        updatedOficina
      );
    },
  });
};
