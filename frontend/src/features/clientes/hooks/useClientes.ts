import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { clienteService } from '../services/clienteService';
import type {
  CreateClienteRequest,
  UpdateClienteRequest,
  ClienteFilters,
} from '../types';

/**
 * Query keys for React Query
 */
export const clienteKeys = {
  all: ['clientes'] as const,
  lists: () => [...clienteKeys.all, 'list'] as const,
  list: (filters: ClienteFilters) => [...clienteKeys.lists(), filters] as const,
  details: () => [...clienteKeys.all, 'detail'] as const,
  detail: (id: string) => [...clienteKeys.details(), id] as const,
  byCpfCnpj: (cpfCnpj: string) => [...clienteKeys.all, 'cpfCnpj', cpfCnpj] as const,
  estatisticas: () => [...clienteKeys.all, 'estatisticas'] as const,
  cidades: () => [...clienteKeys.all, 'cidades'] as const,
  estados: () => [...clienteKeys.all, 'estados'] as const,
};

/**
 * Hook to fetch list of clientes with filters
 */
export const useClientes = (filters: ClienteFilters = {}) => {
  return useQuery({
    queryKey: clienteKeys.list(filters),
    queryFn: () => clienteService.findAll(filters),
    staleTime: 1 * 60 * 1000, // 1 minute
  });
};

/**
 * Hook to fetch single cliente by ID
 */
export const useCliente = (id?: string) => {
  return useQuery({
    queryKey: clienteKeys.detail(id || ''),
    queryFn: () => clienteService.findById(id!),
    enabled: !!id,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
};

/**
 * Hook to fetch cliente by CPF/CNPJ
 */
export const useClienteByCpfCnpj = (cpfCnpj: string, enabled = true) => {
  return useQuery({
    queryKey: clienteKeys.byCpfCnpj(cpfCnpj),
    queryFn: () => clienteService.findByCpfCnpj(cpfCnpj),
    enabled: enabled && !!cpfCnpj,
    retry: false, // Don't retry on 404
  });
};

/**
 * Hook to fetch client statistics
 */
export const useClienteEstatisticas = () => {
  return useQuery({
    queryKey: clienteKeys.estatisticas(),
    queryFn: () => clienteService.getEstatisticas(),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};

/**
 * Hook to fetch cities for filters
 */
export const useCidades = () => {
  return useQuery({
    queryKey: clienteKeys.cidades(),
    queryFn: () => clienteService.getCidades(),
    staleTime: 30 * 60 * 1000, // 30 minutes (rarely changes)
  });
};

/**
 * Hook to fetch states for filters
 */
export const useEstados = () => {
  return useQuery({
    queryKey: clienteKeys.estados(),
    queryFn: () => clienteService.getEstados(),
    staleTime: 30 * 60 * 1000, // 30 minutes (rarely changes)
  });
};

/**
 * Hook to create new cliente
 */
export const useCreateCliente = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateClienteRequest) => clienteService.create(data),
    onSuccess: () => {
      // Invalidate all cliente lists to refetch
      queryClient.invalidateQueries({ queryKey: clienteKeys.lists() });
      queryClient.invalidateQueries({ queryKey: clienteKeys.estatisticas() });
    },
  });
};

/**
 * Hook to update existing cliente
 */
export const useUpdateCliente = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateClienteRequest }) =>
      clienteService.update(id, data),
    onSuccess: (updatedCliente) => {
      // Update the specific cliente in cache
      queryClient.setQueryData(clienteKeys.detail(updatedCliente.id), updatedCliente);

      // Invalidate lists to refetch
      queryClient.invalidateQueries({ queryKey: clienteKeys.lists() });
    },
  });
};

/**
 * Hook to deactivate (delete) cliente
 */
export const useDeleteCliente = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => clienteService.delete(id),
    onSuccess: (_, id) => {
      // Invalidate the specific cliente
      queryClient.invalidateQueries({ queryKey: clienteKeys.detail(id) });

      // Invalidate all lists
      queryClient.invalidateQueries({ queryKey: clienteKeys.lists() });
      queryClient.invalidateQueries({ queryKey: clienteKeys.estatisticas() });
    },
  });
};

/**
 * Hook to reactivate cliente
 */
export const useReativarCliente = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => clienteService.reativar(id),
    onSuccess: (reactivatedCliente) => {
      // Update the specific cliente in cache
      queryClient.setQueryData(clienteKeys.detail(reactivatedCliente.id), reactivatedCliente);

      // Invalidate lists to refetch
      queryClient.invalidateQueries({ queryKey: clienteKeys.lists() });
      queryClient.invalidateQueries({ queryKey: clienteKeys.estatisticas() });
    },
  });
};
