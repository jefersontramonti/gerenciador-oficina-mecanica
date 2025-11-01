/**
 * Service para comunicação com a API de Veículos
 * Endpoints: /api/veiculos
 */

import { api } from '@/shared/services/api';
import type { PaginatedResponse } from '@/shared/types/api';
import type {
  Veiculo,
  CreateVeiculoRequest,
  UpdateVeiculoRequest,
  UpdateQuilometragemRequest,
  VeiculoFilters,
  VeiculoEstatisticasCliente,
} from '../types';

export const veiculoService = {
  /**
   * Listar veículos com filtros e paginação
   */
  async findAll(filters: VeiculoFilters = {}): Promise<PaginatedResponse<Veiculo>> {
    const params = new URLSearchParams();

    if (filters.clienteId) params.append('clienteId', filters.clienteId);
    if (filters.placa) params.append('placa', filters.placa);
    if (filters.marca) params.append('marca', filters.marca);
    if (filters.modelo) params.append('modelo', filters.modelo);
    if (filters.ano) params.append('ano', filters.ano.toString());

    params.append('page', (filters.page ?? 0).toString());
    params.append('size', (filters.size ?? 20).toString());
    params.append('sort', filters.sort ?? 'placa,asc');

    const response = await api.get<PaginatedResponse<Veiculo>>(`/veiculos?${params.toString()}`);
    return response.data;
  },

  /**
   * Buscar veículo por ID
   */
  async findById(id: string): Promise<Veiculo> {
    const response = await api.get<Veiculo>(`/veiculos/${id}`);
    return response.data;
  },

  /**
   * Buscar veículo por placa
   */
  async findByPlaca(placa: string): Promise<Veiculo> {
    const response = await api.get<Veiculo>(`/veiculos/placa/${placa}`);
    return response.data;
  },

  /**
   * Listar veículos de um cliente específico
   */
  async findByClienteId(
    clienteId: string,
    page = 0,
    size = 20,
    sort = 'placa,asc'
  ): Promise<PaginatedResponse<Veiculo>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sort,
    });

    const response = await api.get<PaginatedResponse<Veiculo>>(
      `/veiculos/cliente/${clienteId}?${params.toString()}`
    );
    return response.data;
  },

  /**
   * Criar novo veículo
   */
  async create(data: CreateVeiculoRequest): Promise<Veiculo> {
    const response = await api.post<Veiculo>('/veiculos', data);
    return response.data;
  },

  /**
   * Atualizar veículo
   */
  async update(id: string, data: UpdateVeiculoRequest): Promise<Veiculo> {
    const response = await api.put<Veiculo>(`/veiculos/${id}`, data);
    return response.data;
  },

  /**
   * Atualizar apenas quilometragem
   */
  async updateQuilometragem(id: string, data: UpdateQuilometragemRequest): Promise<Veiculo> {
    const response = await api.patch<Veiculo>(`/veiculos/${id}/quilometragem`, data);
    return response.data;
  },

  /**
   * Remover veículo
   * Nota: Não permitido se houver ordens de serviço vinculadas
   */
  async delete(id: string): Promise<void> {
    await api.delete(`/veiculos/${id}`);
  },

  /**
   * Obter estatísticas de veículos de um cliente
   */
  async getEstatisticasCliente(clienteId: string): Promise<VeiculoEstatisticasCliente> {
    const response = await api.get<VeiculoEstatisticasCliente>(
      `/veiculos/cliente/${clienteId}/estatisticas`
    );
    return response.data;
  },

  /**
   * Listar marcas disponíveis (para filtros)
   */
  async getMarcas(): Promise<string[]> {
    const response = await api.get<string[]>('/veiculos/filtros/marcas');
    return response.data;
  },

  /**
   * Listar modelos disponíveis (para filtros)
   */
  async getModelos(): Promise<string[]> {
    const response = await api.get<string[]>('/veiculos/filtros/modelos');
    return response.data;
  },

  /**
   * Listar anos disponíveis (para filtros)
   */
  async getAnos(): Promise<number[]> {
    const response = await api.get<number[]>('/veiculos/filtros/anos');
    return response.data;
  },
};
