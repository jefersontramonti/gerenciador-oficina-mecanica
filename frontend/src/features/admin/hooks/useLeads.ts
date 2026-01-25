import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { leadService } from '../services/leadService';
import type {
  Lead,
  LeadResumo,
  UpdateLeadRequest,
  LeadStats,
  LeadFilters,
} from '../types/lead';
import type { PaginatedResponse } from '@/shared/types/api';
import { toast } from 'react-hot-toast';

const QUERY_KEYS = {
  leads: (filters?: LeadFilters, page?: number) => ['leads', filters, page] as const,
  lead: (id: string) => ['lead', id] as const,
  stats: () => ['leads', 'stats'] as const,
};

/**
 * Hook para listar leads com paginação
 */
export function useLeads(filters: LeadFilters = {}, page = 0, size = 20) {
  return useQuery<PaginatedResponse<LeadResumo>>({
    queryKey: QUERY_KEYS.leads(filters, page),
    queryFn: () => leadService.listar(filters, page, size),
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
}

/**
 * Hook para buscar lead por ID
 */
export function useLead(id: string) {
  return useQuery<Lead>({
    queryKey: QUERY_KEYS.lead(id),
    queryFn: () => leadService.buscarPorId(id),
    enabled: !!id,
  });
}

/**
 * Hook para estatísticas de leads
 */
export function useLeadStats() {
  return useQuery<LeadStats>({
    queryKey: QUERY_KEYS.stats(),
    queryFn: leadService.getEstatisticas,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

/**
 * Hook para atualizar lead
 */
export function useUpdateLead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateLeadRequest }) =>
      leadService.atualizar(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['leads'] });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.lead(variables.id) });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.stats() });
      toast.success('Lead atualizado com sucesso!');
    },
    onError: (error: Error & { response?: { data?: { message?: string } } }) => {
      toast.error(error?.response?.data?.message || 'Erro ao atualizar lead');
    },
  });
}
