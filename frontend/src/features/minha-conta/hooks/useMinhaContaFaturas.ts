/**
 * React Query hooks para o módulo Minha Conta
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { minhaContaService } from '../services/minhaContaService';
import type { FiltrosFatura } from '../types/fatura';

// Query keys
export const minhaContaKeys = {
  all: ['minha-conta'] as const,
  resumo: () => [...minhaContaKeys.all, 'resumo'] as const,
  faturas: () => [...minhaContaKeys.all, 'faturas'] as const,
  faturaList: (filtros: FiltrosFatura, page: number) =>
    [...minhaContaKeys.faturas(), filtros, page] as const,
  faturaDetail: (id: string) => [...minhaContaKeys.faturas(), 'detail', id] as const,
  pagamentos: () => [...minhaContaKeys.all, 'pagamentos'] as const,
  pagamentoList: (page: number) => [...minhaContaKeys.pagamentos(), page] as const,
};

/**
 * Hook para buscar o resumo financeiro da oficina.
 */
export const useMinhaContaResumo = () => {
  return useQuery({
    queryKey: minhaContaKeys.resumo(),
    queryFn: () => minhaContaService.getResumo(),
    staleTime: 5 * 60 * 1000, // 5 minutos
  });
};

/**
 * Hook para listar faturas da oficina.
 */
export const useMinhasFaturas = (
  filtros: FiltrosFatura = {},
  page: number = 0,
  size: number = 12
) => {
  return useQuery({
    queryKey: minhaContaKeys.faturaList(filtros, page),
    queryFn: () => minhaContaService.listarFaturas(filtros, page, size),
    staleTime: 2 * 60 * 1000, // 2 minutos
  });
};

/**
 * Hook para buscar detalhes de uma fatura.
 * @param id - ID da fatura
 * @param pollingEnabled - Se true, faz polling a cada 5 segundos (útil quando aguardando pagamento)
 */
export const useFaturaDetalhe = (id?: string, pollingEnabled: boolean = false) => {
  return useQuery({
    queryKey: minhaContaKeys.faturaDetail(id || ''),
    queryFn: () => minhaContaService.getFatura(id!),
    enabled: !!id,
    staleTime: pollingEnabled ? 0 : 2 * 60 * 1000, // Sem cache quando polling
    refetchInterval: pollingEnabled ? 5000 : false, // Poll a cada 5 segundos quando habilitado
  });
};

/**
 * Hook para iniciar pagamento de uma fatura.
 */
export const useIniciarPagamento = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      faturaId,
      metodoPagamento,
    }: {
      faturaId: string;
      metodoPagamento?: string;
    }) => minhaContaService.iniciarPagamento(faturaId, metodoPagamento),
    onSuccess: (_, { faturaId }) => {
      // Invalida caches relacionados
      queryClient.invalidateQueries({ queryKey: minhaContaKeys.resumo() });
      queryClient.invalidateQueries({ queryKey: minhaContaKeys.faturaDetail(faturaId) });
      queryClient.invalidateQueries({ queryKey: minhaContaKeys.faturas() });
    },
  });
};

/**
 * Hook para listar histórico de pagamentos.
 */
export const useMeusPagamentos = (page: number = 0, size: number = 12) => {
  return useQuery({
    queryKey: minhaContaKeys.pagamentoList(page),
    queryFn: () => minhaContaService.listarPagamentos(page, size),
    staleTime: 5 * 60 * 1000, // 5 minutos
  });
};

/**
 * Hook para download de PDF da fatura.
 */
export const useDownloadFaturaPdf = () => {
  return useMutation({
    mutationFn: (faturaId: string) => minhaContaService.downloadPdf(faturaId),
    onSuccess: (blob, faturaId) => {
      // Cria link temporário para download
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `fatura-${faturaId}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    },
  });
};
