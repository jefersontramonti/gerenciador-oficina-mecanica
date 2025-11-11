/**
 * Hooks React Query para operações com Locais de Armazenamento
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import localArmazenamentoService from '../services/localArmazenamentoService';
import type {
  LocalArmazenamento,
  CreateLocalArmazenamentoRequest,
  UpdateLocalArmazenamentoRequest,
  TipoLocal,
} from '../types';

// ==================== QUERY KEYS ====================

export const localKeys = {
  all: ['locais-armazenamento'] as const,
  lists: () => [...localKeys.all, 'list'] as const,
  details: () => [...localKeys.all, 'detail'] as const,
  detail: (id: string) => [...localKeys.details(), id] as const,
  codigo: (codigo: string) => [...localKeys.all, 'codigo', codigo] as const,
  raiz: () => [...localKeys.all, 'raiz'] as const,
  filhos: (paiId: string) => [...localKeys.all, 'filhos', paiId] as const,
  tipo: (tipo: TipoLocal) => [...localKeys.all, 'tipo', tipo] as const,
  busca: (descricao: string) => [...localKeys.all, 'busca', descricao] as const,
};

// ==================== QUERIES ====================

/**
 * Listar todos os locais ativos
 */
export const useLocaisArmazenamento = () => {
  return useQuery<LocalArmazenamento[]>({
    queryKey: localKeys.lists(),
    queryFn: () => localArmazenamentoService.listarTodos(),
    staleTime: 10 * 60 * 1000, // 10 minutos (dados relativamente estáticos)
    retry: 1, // Tentar apenas 1 vez em caso de erro
  });
};

/**
 * Buscar local por ID
 */
export const useLocalArmazenamento = (id?: string) => {
  return useQuery<LocalArmazenamento>({
    queryKey: localKeys.detail(id!),
    queryFn: () => localArmazenamentoService.buscarPorId(id!),
    enabled: !!id,
    staleTime: 10 * 60 * 1000,
  });
};

/**
 * Buscar local por código
 */
export const useLocalPorCodigo = (codigo?: string) => {
  return useQuery<LocalArmazenamento>({
    queryKey: localKeys.codigo(codigo!),
    queryFn: () => localArmazenamentoService.buscarPorCodigo(codigo!),
    enabled: !!codigo && codigo.length >= 2,
    staleTime: 10 * 60 * 1000,
  });
};

/**
 * Listar locais raiz (sem pai)
 */
export const useLocaisRaiz = () => {
  return useQuery<LocalArmazenamento[]>({
    queryKey: localKeys.raiz(),
    queryFn: () => localArmazenamentoService.listarLocaisRaiz(),
    staleTime: 10 * 60 * 1000,
    retry: 1, // Tentar apenas 1 vez em caso de erro
  });
};

/**
 * Listar locais filhos de um pai
 */
export const useLocaisFilhos = (paiId?: string) => {
  return useQuery<LocalArmazenamento[]>({
    queryKey: localKeys.filhos(paiId!),
    queryFn: () => localArmazenamentoService.listarFilhos(paiId!),
    enabled: !!paiId,
    staleTime: 10 * 60 * 1000,
  });
};

/**
 * Listar locais por tipo
 */
export const useLocaisPorTipo = (tipo?: TipoLocal) => {
  return useQuery<LocalArmazenamento[]>({
    queryKey: localKeys.tipo(tipo!),
    queryFn: () => localArmazenamentoService.listarPorTipo(tipo!),
    enabled: !!tipo,
    staleTime: 10 * 60 * 1000,
  });
};

/**
 * Buscar locais por descrição
 */
export const useBuscarLocais = (descricao: string) => {
  return useQuery<LocalArmazenamento[]>({
    queryKey: localKeys.busca(descricao),
    queryFn: () => localArmazenamentoService.buscarPorDescricao(descricao),
    enabled: descricao.length >= 3,
    staleTime: 5 * 60 * 1000,
  });
};

// ==================== MUTATIONS ====================

/**
 * Criar novo local
 */
export const useCreateLocal = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateLocalArmazenamentoRequest) =>
      localArmazenamentoService.criarLocal(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: localKeys.lists() });
      queryClient.invalidateQueries({ queryKey: localKeys.raiz() });
      toast.success('Local criado com sucesso!');
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao criar local';
      toast.error(message);
    },
  });
};

/**
 * Atualizar local
 */
export const useUpdateLocal = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateLocalArmazenamentoRequest }) =>
      localArmazenamentoService.atualizarLocal(id, data),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: localKeys.detail(updated.id) });
      queryClient.invalidateQueries({ queryKey: localKeys.lists() });
      queryClient.invalidateQueries({ queryKey: localKeys.raiz() });
      toast.success('Local atualizado com sucesso!');
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao atualizar local';
      toast.error(message);
    },
  });
};

/**
 * Desativar local (soft delete)
 */
export const useDesativarLocal = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => localArmazenamentoService.desativarLocal(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: localKeys.lists() });
      toast.success('Local desativado com sucesso!');
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao desativar local';
      toast.error(message);
    },
  });
};

/**
 * Reativar local
 */
export const useReativarLocal = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => localArmazenamentoService.reativarLocal(id),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: localKeys.detail(updated.id) });
      queryClient.invalidateQueries({ queryKey: localKeys.lists() });
      toast.success('Local reativado com sucesso!');
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao reativar local';
      toast.error(message);
    },
  });
};

/**
 * Excluir local permanentemente (hard delete)
 */
export const useExcluirLocal = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => localArmazenamentoService.excluirLocal(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: localKeys.lists() });
      queryClient.invalidateQueries({ queryKey: localKeys.raiz() });
      toast.success('Local excluído permanentemente!');
    },
    onError: (error: any) => {
      const message =
        error.response?.data?.message ||
        'Erro ao excluir local. Verifique se não há peças vinculadas.';
      toast.error(message);
    },
  });
};
