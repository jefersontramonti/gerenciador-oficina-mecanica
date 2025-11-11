/**
 * Hooks React Query para operações com Peças/Estoque
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import pecaService from '../services/pecaService';
import type {
  CreatePecaRequest,
  UpdatePecaRequest,
  PecaFilters,
} from '../types';

// ==================== QUERY KEYS ====================

export const pecaKeys = {
  all: ['pecas'] as const,
  lists: () => [...pecaKeys.all, 'list'] as const,
  list: (filters: PecaFilters) => [...pecaKeys.lists(), filters] as const,
  details: () => [...pecaKeys.all, 'detail'] as const,
  detail: (id: string) => [...pecaKeys.details(), id] as const,
  codigo: (codigo: string) => [...pecaKeys.all, 'codigo', codigo] as const,
  alertas: () => [...pecaKeys.all, 'alertas'] as const,
  alertaBaixo: (page: number) => [...pecaKeys.alertas(), 'baixo', page] as const,
  alertaZerado: (page: number) => [...pecaKeys.alertas(), 'zerado', page] as const,
  marcas: () => [...pecaKeys.all, 'marcas'] as const,
  valorTotal: () => [...pecaKeys.all, 'valor-total'] as const,
  contadorBaixo: () => [...pecaKeys.all, 'contador-baixo'] as const,
  semLocalizacao: (page: number) => [...pecaKeys.all, 'sem-localizacao', page] as const,
  contadorSemLocalizacao: () => [...pecaKeys.all, 'contador-sem-localizacao'] as const,
};

// ==================== QUERIES ====================

/**
 * Listar peças com filtros
 */
export const usePecas = (filters: PecaFilters = {}) => {
  return useQuery({
    queryKey: pecaKeys.list(filters),
    queryFn: () => pecaService.listarPecas(filters),
    staleTime: 5 * 60 * 1000, // 5 minutos
  });
};

/**
 * Buscar peça por ID
 */
export const usePeca = (id?: string) => {
  return useQuery({
    queryKey: pecaKeys.detail(id!),
    queryFn: () => pecaService.buscarPecaPorId(id!),
    enabled: !!id,
    staleTime: 5 * 60 * 1000,
  });
};

/**
 * Buscar peça por código (SKU)
 */
export const usePecaPorCodigo = (codigo?: string) => {
  return useQuery({
    queryKey: pecaKeys.codigo(codigo!),
    queryFn: () => pecaService.buscarPecaPorCodigo(codigo!),
    enabled: !!codigo && codigo.length >= 3,
    staleTime: 5 * 60 * 1000,
  });
};

/**
 * Listar alertas de estoque baixo
 */
export const useAlertasEstoqueBaixo = (page = 0, size = 20) => {
  return useQuery({
    queryKey: pecaKeys.alertaBaixo(page),
    queryFn: () => pecaService.listarAlertasEstoqueBaixo(page, size),
    staleTime: 2 * 60 * 1000, // 2 minutos
  });
};

/**
 * Listar alertas de estoque zerado
 */
export const useAlertasEstoqueZerado = (page = 0, size = 20) => {
  return useQuery({
    queryKey: pecaKeys.alertaZerado(page),
    queryFn: () => pecaService.listarAlertasEstoqueZerado(page, size),
    staleTime: 2 * 60 * 1000,
  });
};

/**
 * Listar marcas disponíveis (para filtros)
 */
export const useMarcas = () => {
  return useQuery({
    queryKey: pecaKeys.marcas(),
    queryFn: () => pecaService.listarMarcas(),
    staleTime: 30 * 60 * 1000, // 30 minutos (raramente muda)
  });
};

/**
 * Obter valor total do inventário
 */
export const useValorTotalInventario = () => {
  return useQuery({
    queryKey: pecaKeys.valorTotal(),
    queryFn: () => pecaService.obterValorTotalInventario(),
    staleTime: 5 * 60 * 1000,
  });
};

/**
 * Contar peças com estoque baixo (para badge)
 */
export const useContadorEstoqueBaixo = () => {
  return useQuery({
    queryKey: pecaKeys.contadorBaixo(),
    queryFn: () => pecaService.contarEstoqueBaixo(),
    staleTime: 60 * 1000, // 1 minuto
    refetchInterval: 60 * 1000, // Auto-refresh a cada 1 minuto
  });
};

/**
 * Listar peças sem localização definida
 */
export const usePecasSemLocalizacao = (page = 0, size = 20) => {
  return useQuery({
    queryKey: pecaKeys.semLocalizacao(page),
    queryFn: () => pecaService.listarPecasSemLocalizacao(page, size),
    staleTime: 2 * 60 * 1000, // 2 minutos
  });
};

/**
 * Contar peças sem localização (para badge/alerta)
 */
export const useContadorPecasSemLocalizacao = () => {
  return useQuery({
    queryKey: pecaKeys.contadorSemLocalizacao(),
    queryFn: () => pecaService.contarPecasSemLocalizacao(),
    staleTime: 60 * 1000, // 1 minuto
    refetchInterval: 60 * 1000, // Auto-refresh a cada 1 minuto
  });
};

// ==================== MUTATIONS ====================

/**
 * Criar nova peça
 */
export const useCreatePeca = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreatePecaRequest) => pecaService.criarPeca(request),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: pecaKeys.lists() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.marcas() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.valorTotal() });
      toast.success('Peça criada com sucesso!', {
        description: `Código: ${data.codigo}`,
      });
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao criar peça';
      toast.error('Erro ao criar peça', {
        description: message,
      });
    },
  });
};

/**
 * Atualizar peça existente
 */
export const useUpdatePeca = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdatePecaRequest }) =>
      pecaService.atualizarPeca(id, data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: pecaKeys.detail(data.id) });
      queryClient.invalidateQueries({ queryKey: pecaKeys.lists() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.marcas() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.valorTotal() });
      toast.success('Peça atualizada com sucesso!');
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao atualizar peça';
      toast.error('Erro ao atualizar peça', {
        description: message,
      });
    },
  });
};

/**
 * Desativar peça (soft delete)
 */
export const useDesativarPeca = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => pecaService.desativarPeca(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: pecaKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: pecaKeys.lists() });
      toast.success('Peça desativada com sucesso!');
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao desativar peça';
      toast.error('Erro ao desativar peça', {
        description: message,
      });
    },
  });
};

/**
 * Reativar peça desativada
 */
export const useReativarPeca = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => pecaService.reativarPeca(id),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: pecaKeys.detail(data.id) });
      queryClient.invalidateQueries({ queryKey: pecaKeys.lists() });
      toast.success('Peça reativada com sucesso!');
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao reativar peça';
      toast.error('Erro ao reativar peça', {
        description: message,
      });
    },
  });
};

/**
 * Definir/alterar localização de uma peça
 */
export const useDefinirLocalizacao = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ pecaId, localId }: { pecaId: string; localId?: string }) =>
      pecaService.definirLocalizacaoPeca(pecaId, localId),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: pecaKeys.detail(data.id) });
      queryClient.invalidateQueries({ queryKey: pecaKeys.lists() });
      queryClient.invalidateQueries({ queryKey: pecaKeys.semLocalizacao(0) });
      queryClient.invalidateQueries({ queryKey: pecaKeys.contadorSemLocalizacao() });
      toast.success('Localização definida com sucesso!', {
        description: data.localArmazenamento
          ? `Local: ${data.localArmazenamento.descricao}`
          : 'Localização removida',
      });
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Erro ao definir localização';
      toast.error('Erro ao definir localização', {
        description: message,
      });
    },
  });
};
