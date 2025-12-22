import { api } from '@/shared/services/api';
import type { PaginatedResponse } from '@/shared/types/api';
import type {
  Cliente,
  CreateClienteRequest,
  UpdateClienteRequest,
  ClienteFilters,
  ClienteEstatisticas,
} from '../types';

/**
 * Cliente service
 * Handles all cliente-related API calls
 */
export const clienteService = {
  /**
   * List clientes with filters and pagination
   */
  async findAll(filters: ClienteFilters = {}): Promise<PaginatedResponse<Cliente>> {
    const params = new URLSearchParams();

    if (filters.nome) params.append('nome', filters.nome);
    if (filters.tipo) params.append('tipo', filters.tipo);
    if (filters.ativo !== undefined) params.append('ativo', String(filters.ativo));
    if (filters.cidade) params.append('cidade', filters.cidade);
    if (filters.estado) params.append('estado', filters.estado);
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));
    if (filters.sort) params.append('sort', filters.sort);

    const response = await api.get<PaginatedResponse<Cliente>>(
      `/clientes?${params.toString()}`
    );

    return response.data;
  },

  /**
   * Find cliente by ID
   */
  async findById(id: string): Promise<Cliente> {
    const response = await api.get<Cliente>(`/clientes/${id}`);
    return response.data;
  },

  /**
   * Find cliente by CPF/CNPJ
   */
  async findByCpfCnpj(cpfCnpj: string): Promise<Cliente> {
    const response = await api.get<Cliente>(`/clientes/cpf-cnpj/${cpfCnpj}`);
    return response.data;
  },

  /**
   * Create new cliente
   */
  async create(data: CreateClienteRequest): Promise<Cliente> {
    const response = await api.post<Cliente>('/clientes', data);
    return response.data;
  },

  /**
   * Update existing cliente
   */
  async update(id: string, data: UpdateClienteRequest): Promise<Cliente> {
    const response = await api.put<Cliente>(`/clientes/${id}`, data);
    return response.data;
  },

  /**
   * Deactivate cliente (soft delete)
   */
  async delete(id: string): Promise<void> {
    await api.delete(`/clientes/${id}`);
  },

  /**
   * Reactivate cliente
   */
  async reativar(id: string): Promise<Cliente> {
    const response = await api.patch<Cliente>(`/clientes/${id}/reativar`);
    return response.data;
  },

  /**
   * Get client statistics
   */
  async getEstatisticas(): Promise<ClienteEstatisticas> {
    const response = await api.get<ClienteEstatisticas>('/clientes/estatisticas');
    return response.data;
  },

  /**
   * Get list of cities (for filters)
   */
  async getCidades(): Promise<string[]> {
    const response = await api.get<string[]>('/clientes/filtros/cidades');
    return response.data;
  },

  /**
   * Get list of states (for filters)
   */
  async getEstados(): Promise<string[]> {
    const response = await api.get<string[]>('/clientes/filtros/estados');
    return response.data;
  },
};
