/**
 * Service para o módulo Minha Conta - Faturas SaaS
 */

import { api } from '@/shared/services/api';
import type {
  MinhaContaResumo,
  FaturaResumo,
  Fatura,
  IniciarPagamentoResponse,
  FiltrosFatura,
} from '../types/fatura';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const minhaContaService = {
  /**
   * Busca o resumo financeiro da oficina.
   */
  async getResumo(): Promise<MinhaContaResumo> {
    const { data } = await api.get<MinhaContaResumo>('/minha-conta/resumo');
    return data;
  },

  /**
   * Lista faturas da oficina.
   */
  async listarFaturas(
    filtros: FiltrosFatura = {},
    page: number = 0,
    size: number = 12
  ): Promise<PageResponse<FaturaResumo>> {
    const { data } = await api.get<PageResponse<FaturaResumo>>('/minha-conta/faturas', {
      params: {
        ...filtros,
        page,
        size,
        sort: 'dataVencimento,desc',
      },
    });
    return data;
  },

  /**
   * Busca detalhes de uma fatura.
   */
  async getFatura(id: string): Promise<Fatura> {
    const { data } = await api.get<Fatura>(`/minha-conta/faturas/${id}`);
    return data;
  },

  /**
   * Baixa o PDF de uma fatura.
   */
  async downloadPdf(id: string): Promise<Blob> {
    const { data } = await api.get(`/minha-conta/faturas/${id}/pdf`, {
      responseType: 'blob',
    });
    return data;
  },

  /**
   * Inicia o pagamento de uma fatura.
   */
  async iniciarPagamento(
    faturaId: string,
    metodoPagamento: string = 'PIX'
  ): Promise<IniciarPagamentoResponse> {
    const { data } = await api.post<IniciarPagamentoResponse>(
      `/minha-conta/faturas/${faturaId}/pagar`,
      null,
      {
        params: { metodoPagamento },
      }
    );
    return data;
  },

  /**
   * Lista histórico de pagamentos (faturas pagas).
   */
  async listarPagamentos(
    page: number = 0,
    size: number = 12
  ): Promise<PageResponse<FaturaResumo>> {
    const { data } = await api.get<PageResponse<FaturaResumo>>('/minha-conta/pagamentos', {
      params: {
        page,
        size,
        sort: 'dataPagamento,desc',
      },
    });
    return data;
  },
};
