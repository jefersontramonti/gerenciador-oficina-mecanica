/**
 * API client para operações de pagamentos
 */

import { api } from '@/shared/services/api';
import type {
  Pagamento,
  PagamentoRequestDTO,
  ConfirmarPagamentoDTO,
  ResumoFinanceiro,
  FiltrosPagamento
} from '../types/pagamento';

export interface PaginatedResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

export const pagamentosApi = {
  /**
   * Cria um novo pagamento
   */
  criar: async (data: PagamentoRequestDTO): Promise<Pagamento> => {
    const response = await api.post<Pagamento>('/pagamentos', data);
    return response.data;
  },

  /**
   * Busca pagamento por ID
   */
  buscarPorId: async (id: string): Promise<Pagamento> => {
    const response = await api.get<Pagamento>(`/pagamentos/${id}`);
    return response.data;
  },

  /**
   * Lista pagamentos com filtros e paginação
   */
  listar: async (
    filtros?: FiltrosPagamento,
    page = 0,
    size = 20
  ): Promise<PaginatedResponse<Pagamento>> => {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sort', 'createdAt,desc');

    if (filtros?.tipo) params.append('tipo', filtros.tipo);
    if (filtros?.status) params.append('status', filtros.status);
    if (filtros?.dataInicio) params.append('dataInicio', filtros.dataInicio);
    if (filtros?.dataFim) params.append('dataFim', filtros.dataFim);

    const response = await api.get<PaginatedResponse<Pagamento>>(
      `/pagamentos?${params.toString()}`
    );
    return response.data;
  },

  /**
   * Lista pagamentos por ordem de serviço
   */
  listarPorOrdemServico: async (ordemServicoId: string): Promise<Pagamento[]> => {
    const response = await api.get<Pagamento[]>(
      `/pagamentos/ordem-servico/${ordemServicoId}`
    );
    return response.data;
  },

  /**
   * Confirma um pagamento (marca como PAGO)
   */
  confirmar: async (
    id: string,
    data: ConfirmarPagamentoDTO
  ): Promise<Pagamento> => {
    const response = await api.put<Pagamento>(`/pagamentos/${id}/confirmar`, data);
    return response.data;
  },

  /**
   * Cancela um pagamento
   */
  cancelar: async (id: string): Promise<void> => {
    await api.delete(`/pagamentos/${id}/cancelar`);
  },

  /**
   * Estorna um pagamento (ADMIN only)
   */
  estornar: async (id: string): Promise<void> => {
    await api.put(`/pagamentos/${id}/estornar`);
  },

  /**
   * Busca resumo financeiro de uma OS
   */
  resumoFinanceiro: async (ordemServicoId: string): Promise<ResumoFinanceiro> => {
    const response = await api.get<ResumoFinanceiro>(
      `/pagamentos/ordem-servico/${ordemServicoId}/resumo`
    );
    return response.data;
  }
};
