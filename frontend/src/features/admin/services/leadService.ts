import api from '@/shared/services/api';
import {
  Lead,
  LeadResumo,
  CreateLeadRequest,
  UpdateLeadRequest,
  LeadStats,
  LeadFilters,
} from '../types/lead';
import { Page } from '@/shared/types/pagination';

const BASE_URL = '/saas/leads';

export const leadService = {
  /**
   * Lista leads com paginação e filtros
   */
  async listar(
    filters: LeadFilters = {},
    page = 0,
    size = 20
  ): Promise<Page<LeadResumo>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (filters.status) params.append('status', filters.status);
    if (filters.origem) params.append('origem', filters.origem);
    if (filters.nome) params.append('nome', filters.nome);
    if (filters.email) params.append('email', filters.email);

    const response = await api.get<Page<LeadResumo>>(`${BASE_URL}?${params}`);
    return response.data;
  },

  /**
   * Busca lead por ID
   */
  async buscarPorId(id: string): Promise<Lead> {
    const response = await api.get<Lead>(`${BASE_URL}/${id}`);
    return response.data;
  },

  /**
   * Atualiza lead (status e observações)
   */
  async atualizar(id: string, data: UpdateLeadRequest): Promise<Lead> {
    const response = await api.patch<Lead>(`${BASE_URL}/${id}`, data);
    return response.data;
  },

  /**
   * Obtém estatísticas dos leads
   */
  async getEstatisticas(): Promise<LeadStats> {
    const response = await api.get<LeadStats>(`${BASE_URL}/stats`);
    return response.data;
  },

  /**
   * Cria um novo lead (endpoint público)
   */
  async criar(data: CreateLeadRequest): Promise<Lead> {
    const response = await api.post<Lead>('/public/leads', data);
    return response.data;
  },
};
