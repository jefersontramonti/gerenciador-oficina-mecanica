/**
 * React Query hooks para Pagamentos Online e Configuração de Gateways
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { gatewayService, pagamentoOnlineService } from '../services/pagamentoOnlineService';
import type { ConfiguracaoGatewayRequest, CriarCheckoutRequest } from '../types/pagamentoOnline';
import { TipoGateway } from '../types/pagamentoOnline';

// Query Keys
export const gatewayKeys = {
  all: ['gateways'] as const,
  lists: () => [...gatewayKeys.all, 'list'] as const,
  detail: (id: string) => [...gatewayKeys.all, 'detail', id] as const,
  byTipo: (tipo: TipoGateway) => [...gatewayKeys.all, 'tipo', tipo] as const,
};

export const pagamentoOnlineKeys = {
  all: ['pagamentos-online'] as const,
  lists: () => [...pagamentoOnlineKeys.all, 'list'] as const,
  detail: (id: string) => [...pagamentoOnlineKeys.all, 'detail', id] as const,
  byOS: (osId: string) => [...pagamentoOnlineKeys.all, 'os', osId] as const,
};

// ========== Gateway Hooks ==========

export function useGateways() {
  return useQuery({
    queryKey: gatewayKeys.lists(),
    queryFn: gatewayService.listar,
    staleTime: 5 * 60 * 1000, // 5 minutos
  });
}

export function useGateway(id: string) {
  return useQuery({
    queryKey: gatewayKeys.detail(id),
    queryFn: () => gatewayService.buscarPorId(id),
    enabled: !!id,
  });
}

export function useGatewayPorTipo(tipo: TipoGateway) {
  return useQuery({
    queryKey: gatewayKeys.byTipo(tipo),
    queryFn: () => gatewayService.buscarPorTipo(tipo),
    enabled: !!tipo,
  });
}

export function useCriarGateway() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: ConfiguracaoGatewayRequest) => gatewayService.criar(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: gatewayKeys.all });
    },
  });
}

export function useAtualizarGateway() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ConfiguracaoGatewayRequest }) =>
      gatewayService.atualizar(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: gatewayKeys.all });
      queryClient.invalidateQueries({ queryKey: gatewayKeys.detail(id) });
    },
  });
}

export function useValidarGateway() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => gatewayService.validar(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: gatewayKeys.all });
      queryClient.invalidateQueries({ queryKey: gatewayKeys.detail(id) });
    },
  });
}

export function useAtivarGateway() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => gatewayService.ativar(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: gatewayKeys.all });
      queryClient.invalidateQueries({ queryKey: gatewayKeys.detail(id) });
    },
  });
}

export function useDesativarGateway() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => gatewayService.desativar(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: gatewayKeys.all });
      queryClient.invalidateQueries({ queryKey: gatewayKeys.detail(id) });
    },
  });
}

export function useRemoverGateway() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => gatewayService.remover(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: gatewayKeys.all });
    },
  });
}

// Hook para buscar Public Key (necessário para Checkout Bricks)
export function usePublicKey() {
  return useQuery({
    queryKey: [...gatewayKeys.all, 'public-key'] as const,
    queryFn: gatewayService.getPublicKey,
    staleTime: 30 * 60 * 1000, // 30 minutos (raramente muda)
    retry: false, // Não ficar tentando se não configurou
  });
}

// ========== Pagamento Online Hooks ==========

export function usePagamentosOnlinePorOS(osId: string) {
  return useQuery({
    queryKey: pagamentoOnlineKeys.byOS(osId),
    queryFn: () => pagamentoOnlineService.listarPorOS(osId),
    enabled: !!osId,
    refetchInterval: 10000, // Atualiza a cada 10 segundos para verificar status
  });
}

export function usePagamentoOnline(id: string) {
  return useQuery({
    queryKey: pagamentoOnlineKeys.detail(id),
    queryFn: () => pagamentoOnlineService.buscarPorId(id),
    enabled: !!id,
    refetchInterval: 5000, // Atualiza a cada 5 segundos enquanto pendente
  });
}

export function useCriarCheckout() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CriarCheckoutRequest) => pagamentoOnlineService.criarCheckout(data),
    onSuccess: (_, { ordemServicoId }) => {
      queryClient.invalidateQueries({ queryKey: pagamentoOnlineKeys.byOS(ordemServicoId) });
    },
  });
}

export function useAtualizarStatusPagamento() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => pagamentoOnlineService.atualizarStatus(id),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: pagamentoOnlineKeys.all });
      queryClient.invalidateQueries({ queryKey: pagamentoOnlineKeys.byOS(result.ordemServicoId) });
    },
  });
}
