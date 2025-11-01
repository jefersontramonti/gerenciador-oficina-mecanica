/**
 * React Query hooks para gerenciamento de veículos
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { veiculoService } from '../services/veiculoService';
import type {
  VeiculoFilters,
  CreateVeiculoRequest,
  UpdateVeiculoRequest,
  UpdateQuilometragemRequest,
} from '../types';

/**
 * Query keys para cache do React Query
 */
export const veiculoKeys = {
  all: ['veiculos'] as const,
  lists: () => [...veiculoKeys.all, 'list'] as const,
  list: (filters: VeiculoFilters) => [...veiculoKeys.lists(), filters] as const,
  details: () => [...veiculoKeys.all, 'detail'] as const,
  detail: (id: string) => [...veiculoKeys.details(), id] as const,
  byPlaca: (placa: string) => [...veiculoKeys.all, 'placa', placa] as const,
  byCliente: (clienteId: string) => [...veiculoKeys.all, 'cliente', clienteId] as const,
  estatisticasCliente: (clienteId: string) =>
    [...veiculoKeys.all, 'estatisticas', clienteId] as const,
  marcas: () => [...veiculoKeys.all, 'marcas'] as const,
  modelos: () => [...veiculoKeys.all, 'modelos'] as const,
  anos: () => [...veiculoKeys.all, 'anos'] as const,
};

/**
 * Hook para listar veículos com filtros
 */
export const useVeiculos = (filters: VeiculoFilters = {}) => {
  return useQuery({
    queryKey: veiculoKeys.list(filters),
    queryFn: () => veiculoService.findAll(filters),
    staleTime: 1 * 60 * 1000, // 1 minuto
  });
};

/**
 * Hook para buscar veículo por ID
 */
export const useVeiculo = (id?: string) => {
  return useQuery({
    queryKey: veiculoKeys.detail(id || ''),
    queryFn: () => veiculoService.findById(id!),
    enabled: !!id,
    staleTime: 2 * 60 * 1000, // 2 minutos
  });
};

/**
 * Hook para buscar veículo por placa
 */
export const useVeiculoByPlaca = (placa: string, enabled = true) => {
  return useQuery({
    queryKey: veiculoKeys.byPlaca(placa),
    queryFn: () => veiculoService.findByPlaca(placa),
    enabled: enabled && !!placa,
    staleTime: 2 * 60 * 1000,
  });
};

/**
 * Hook para listar veículos de um cliente
 */
export const useVeiculosByCliente = (
  clienteId: string,
  page = 0,
  size = 20,
  sort = 'placa,asc',
  enabled = true
) => {
  return useQuery({
    queryKey: veiculoKeys.byCliente(clienteId),
    queryFn: () => veiculoService.findByClienteId(clienteId, page, size, sort),
    enabled: enabled && !!clienteId,
    staleTime: 1 * 60 * 1000,
  });
};

/**
 * Hook para obter estatísticas de veículos de um cliente
 */
export const useVeiculoEstatisticasCliente = (clienteId: string, enabled = true) => {
  return useQuery({
    queryKey: veiculoKeys.estatisticasCliente(clienteId),
    queryFn: () => veiculoService.getEstatisticasCliente(clienteId),
    enabled: enabled && !!clienteId,
    staleTime: 5 * 60 * 1000, // 5 minutos
  });
};

/**
 * Hook para obter lista de marcas
 */
export const useMarcas = () => {
  return useQuery({
    queryKey: veiculoKeys.marcas(),
    queryFn: () => veiculoService.getMarcas(),
    staleTime: 30 * 60 * 1000, // 30 minutos (dados estáticos)
  });
};

/**
 * Hook para obter lista de modelos
 */
export const useModelos = () => {
  return useQuery({
    queryKey: veiculoKeys.modelos(),
    queryFn: () => veiculoService.getModelos(),
    staleTime: 30 * 60 * 1000,
  });
};

/**
 * Hook para obter lista de anos
 */
export const useAnos = () => {
  return useQuery({
    queryKey: veiculoKeys.anos(),
    queryFn: () => veiculoService.getAnos(),
    staleTime: 30 * 60 * 1000,
  });
};

/**
 * Hook para criar novo veículo
 */
export const useCreateVeiculo = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateVeiculoRequest) => veiculoService.create(data),
    onSuccess: (newVeiculo) => {
      // Invalidar lista geral
      queryClient.invalidateQueries({ queryKey: veiculoKeys.lists() });

      // Invalidar estatísticas do cliente
      queryClient.invalidateQueries({
        queryKey: veiculoKeys.estatisticasCliente(newVeiculo.clienteId),
      });

      // Invalidar lista de veículos do cliente
      queryClient.invalidateQueries({
        queryKey: veiculoKeys.byCliente(newVeiculo.clienteId),
      });

      // Invalidar listas de filtros
      queryClient.invalidateQueries({ queryKey: veiculoKeys.marcas() });
      queryClient.invalidateQueries({ queryKey: veiculoKeys.modelos() });
      queryClient.invalidateQueries({ queryKey: veiculoKeys.anos() });
    },
  });
};

/**
 * Hook para atualizar veículo
 */
export const useUpdateVeiculo = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateVeiculoRequest }) =>
      veiculoService.update(id, data),
    onSuccess: (updatedVeiculo) => {
      // Invalidar o veículo específico
      queryClient.invalidateQueries({ queryKey: veiculoKeys.detail(updatedVeiculo.id) });

      // Invalidar listas
      queryClient.invalidateQueries({ queryKey: veiculoKeys.lists() });

      // Invalidar lista de veículos do cliente
      queryClient.invalidateQueries({
        queryKey: veiculoKeys.byCliente(updatedVeiculo.clienteId),
      });

      // Invalidar listas de filtros
      queryClient.invalidateQueries({ queryKey: veiculoKeys.marcas() });
      queryClient.invalidateQueries({ queryKey: veiculoKeys.modelos() });
      queryClient.invalidateQueries({ queryKey: veiculoKeys.anos() });
    },
  });
};

/**
 * Hook para atualizar apenas quilometragem
 */
export const useUpdateQuilometragem = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateQuilometragemRequest }) =>
      veiculoService.updateQuilometragem(id, data),
    onSuccess: (updatedVeiculo) => {
      // Invalidar o veículo específico
      queryClient.invalidateQueries({ queryKey: veiculoKeys.detail(updatedVeiculo.id) });

      // Invalidar listas
      queryClient.invalidateQueries({ queryKey: veiculoKeys.lists() });

      // Invalidar lista de veículos do cliente
      queryClient.invalidateQueries({
        queryKey: veiculoKeys.byCliente(updatedVeiculo.clienteId),
      });
    },
  });
};

/**
 * Hook para deletar veículo
 */
export const useDeleteVeiculo = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => veiculoService.delete(id),
    onSuccess: (_, deletedId) => {
      // Invalidar listas
      queryClient.invalidateQueries({ queryKey: veiculoKeys.lists() });

      // Remover do cache
      queryClient.removeQueries({ queryKey: veiculoKeys.detail(deletedId) });

      // Invalidar estatísticas (será recarregado quando necessário)
      queryClient.invalidateQueries({ queryKey: veiculoKeys.all });
    },
  });
};
