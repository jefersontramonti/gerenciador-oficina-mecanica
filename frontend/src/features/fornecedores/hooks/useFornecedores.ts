import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fornecedorService } from '../services/fornecedorService';
import type {
  CreateFornecedorRequest,
  UpdateFornecedorRequest,
  FornecedorFilters,
} from '../types';

export const fornecedorKeys = {
  all: ['fornecedores'] as const,
  lists: () => [...fornecedorKeys.all, 'list'] as const,
  list: (filters: FornecedorFilters) => [...fornecedorKeys.lists(), filters] as const,
  details: () => [...fornecedorKeys.all, 'detail'] as const,
  detail: (id: string) => [...fornecedorKeys.details(), id] as const,
  resumo: () => [...fornecedorKeys.all, 'resumo'] as const,
};

export const useFornecedores = (filters: FornecedorFilters = {}) => {
  return useQuery({
    queryKey: fornecedorKeys.list(filters),
    queryFn: () => fornecedorService.findAll(filters),
    staleTime: 1 * 60 * 1000,
  });
};

export const useFornecedor = (id?: string) => {
  return useQuery({
    queryKey: fornecedorKeys.detail(id || ''),
    queryFn: () => fornecedorService.findById(id!),
    enabled: !!id,
    staleTime: 2 * 60 * 1000,
  });
};

export const useFornecedoresResumo = () => {
  return useQuery({
    queryKey: fornecedorKeys.resumo(),
    queryFn: () => fornecedorService.findAllResumo(),
    staleTime: 5 * 60 * 1000,
  });
};

export const useCreateFornecedor = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateFornecedorRequest) => fornecedorService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: fornecedorKeys.lists() });
      queryClient.invalidateQueries({ queryKey: fornecedorKeys.resumo() });
    },
  });
};

export const useUpdateFornecedor = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateFornecedorRequest }) =>
      fornecedorService.update(id, data),
    onSuccess: (updatedFornecedor) => {
      queryClient.setQueryData(fornecedorKeys.detail(updatedFornecedor.id), updatedFornecedor);
      queryClient.invalidateQueries({ queryKey: fornecedorKeys.lists() });
      queryClient.invalidateQueries({ queryKey: fornecedorKeys.resumo() });
    },
  });
};

export const useDeleteFornecedor = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => fornecedorService.delete(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: fornecedorKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: fornecedorKeys.lists() });
      queryClient.invalidateQueries({ queryKey: fornecedorKeys.resumo() });
    },
  });
};

export const useReativarFornecedor = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => fornecedorService.reativar(id),
    onSuccess: (reactivatedFornecedor) => {
      queryClient.setQueryData(fornecedorKeys.detail(reactivatedFornecedor.id), reactivatedFornecedor);
      queryClient.invalidateQueries({ queryKey: fornecedorKeys.lists() });
      queryClient.invalidateQueries({ queryKey: fornecedorKeys.resumo() });
    },
  });
};
