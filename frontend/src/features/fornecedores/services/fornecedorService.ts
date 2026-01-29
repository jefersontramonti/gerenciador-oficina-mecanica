import { api } from '@/shared/services/api';
import type { PaginatedResponse } from '@/shared/types/api';
import type {
  Fornecedor,
  FornecedorResumo,
  CreateFornecedorRequest,
  UpdateFornecedorRequest,
  FornecedorFilters,
} from '../types';

export const fornecedorService = {
  async findAll(filters: FornecedorFilters = {}): Promise<PaginatedResponse<Fornecedor>> {
    const params = new URLSearchParams();

    if (filters.nome) params.append('nome', filters.nome);
    if (filters.tipo) params.append('tipo', filters.tipo);
    if (filters.cidade) params.append('cidade', filters.cidade);
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));
    if (filters.sort) params.append('sort', filters.sort);

    const response = await api.get<PaginatedResponse<Fornecedor>>(
      `/fornecedores?${params.toString()}`
    );
    return response.data;
  },

  async findAllResumo(): Promise<FornecedorResumo[]> {
    const response = await api.get<FornecedorResumo[]>('/fornecedores/resumo');
    return response.data;
  },

  async findById(id: string): Promise<Fornecedor> {
    const response = await api.get<Fornecedor>(`/fornecedores/${id}`);
    return response.data;
  },

  async create(data: CreateFornecedorRequest): Promise<Fornecedor> {
    const response = await api.post<Fornecedor>('/fornecedores', data);
    return response.data;
  },

  async update(id: string, data: UpdateFornecedorRequest): Promise<Fornecedor> {
    const response = await api.put<Fornecedor>(`/fornecedores/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/fornecedores/${id}`);
  },

  async reativar(id: string): Promise<Fornecedor> {
    const response = await api.patch<Fornecedor>(`/fornecedores/${id}/reativar`);
    return response.data;
  },
};
