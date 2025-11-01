/**
 * Hooks React Query para gerenciamento de usuários
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usuarioService } from '../services/usuarioService';
import type {
  UsuarioFilters,
  CreateUsuarioRequest,
  UpdateUsuarioRequest,
  PerfilUsuario,
} from '../types';

/**
 * Query keys para cache
 */
export const usuarioKeys = {
  all: ['usuarios'] as const,
  lists: () => [...usuarioKeys.all, 'list'] as const,
  list: (filters: UsuarioFilters) => [...usuarioKeys.lists(), filters] as const,
  details: () => [...usuarioKeys.all, 'detail'] as const,
  detail: (id: string) => [...usuarioKeys.details(), id] as const,
  byPerfil: (perfil: PerfilUsuario) => [...usuarioKeys.all, 'perfil', perfil] as const,
  ativos: () => [...usuarioKeys.all, 'ativos'] as const,
};

/**
 * Hook para listar usuários com filtros
 */
export const useUsuarios = (filters: UsuarioFilters = {}) => {
  return useQuery({
    queryKey: usuarioKeys.list(filters),
    queryFn: () => usuarioService.findAll(filters),
    staleTime: 2 * 60 * 1000, // 2 minutos
  });
};

/**
 * Hook para buscar usuário por ID
 */
export const useUsuario = (id?: string) => {
  return useQuery({
    queryKey: usuarioKeys.detail(id || ''),
    queryFn: () => usuarioService.findById(id!),
    enabled: !!id,
    staleTime: 2 * 60 * 1000,
  });
};

/**
 * Hook para listar usuários por perfil
 */
export const useUsuariosPorPerfil = (perfil: PerfilUsuario) => {
  return useQuery({
    queryKey: usuarioKeys.byPerfil(perfil),
    queryFn: () => usuarioService.findByPerfil(perfil),
    staleTime: 5 * 60 * 1000, // 5 minutos (muda pouco)
  });
};

/**
 * Hook para listar usuários ativos
 */
export const useUsuariosAtivos = () => {
  return useQuery({
    queryKey: usuarioKeys.ativos(),
    queryFn: () => usuarioService.findAllAtivos(),
    staleTime: 5 * 60 * 1000,
  });
};

/**
 * Hook para criar usuário
 */
export const useCreateUsuario = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateUsuarioRequest) => usuarioService.create(data),
    onSuccess: () => {
      // Invalidar todas as listas de usuários
      queryClient.invalidateQueries({ queryKey: usuarioKeys.lists() });
      queryClient.invalidateQueries({ queryKey: usuarioKeys.ativos() });
    },
  });
};

/**
 * Hook para atualizar usuário
 */
export const useUpdateUsuario = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUsuarioRequest }) =>
      usuarioService.update(id, data),
    onSuccess: (updated) => {
      // Invalidar o usuário específico
      queryClient.invalidateQueries({ queryKey: usuarioKeys.detail(updated.id) });

      // Invalidar listas
      queryClient.invalidateQueries({ queryKey: usuarioKeys.lists() });
      queryClient.invalidateQueries({ queryKey: usuarioKeys.ativos() });
    },
  });
};

/**
 * Hook para desativar usuário
 */
export const useDeleteUsuario = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => usuarioService.delete(id),
    onSuccess: (_, deletedId) => {
      // Invalidar o usuário específico
      queryClient.invalidateQueries({ queryKey: usuarioKeys.detail(deletedId) });

      // Invalidar listas
      queryClient.invalidateQueries({ queryKey: usuarioKeys.lists() });
      queryClient.invalidateQueries({ queryKey: usuarioKeys.ativos() });
    },
  });
};

/**
 * Hook para reativar usuário
 */
export const useReactivateUsuario = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => usuarioService.reactivate(id),
    onSuccess: (reactivated) => {
      // Invalidar o usuário específico
      queryClient.invalidateQueries({ queryKey: usuarioKeys.detail(reactivated.id) });

      // Invalidar listas
      queryClient.invalidateQueries({ queryKey: usuarioKeys.lists() });
      queryClient.invalidateQueries({ queryKey: usuarioKeys.ativos() });
    },
  });
};
