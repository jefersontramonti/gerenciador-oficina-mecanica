import { api } from '@/shared/services/api';
import type {
  DespesaCreateRequest,
  DespesaUpdateRequest,
  DespesaPagamentoRequest,
  DespesaResponse,
  DespesaListItem,
  DespesaResumo,
  DespesaFiltros,
  DespesaPageResponse,
  CategoriaInfo,
} from '../types/despesa';

const BASE_URL = '/financeiro/despesas';

/**
 * Service para gerenciamento de despesas
 */
export const despesaService = {
  /**
   * Cria uma nova despesa
   */
  async criar(data: DespesaCreateRequest): Promise<DespesaResponse> {
    const response = await api.post<DespesaResponse>(BASE_URL, data);
    return response.data;
  },

  /**
   * Busca despesa por ID
   */
  async buscarPorId(id: string): Promise<DespesaResponse> {
    const response = await api.get<DespesaResponse>(`${BASE_URL}/${id}`);
    return response.data;
  },

  /**
   * Lista despesas com filtros e paginação
   */
  async listar(filtros: DespesaFiltros = {}): Promise<DespesaPageResponse> {
    const params = new URLSearchParams();

    if (filtros.status) params.append('status', filtros.status);
    if (filtros.categoria) params.append('categoria', filtros.categoria);
    if (filtros.dataInicio) params.append('dataInicio', filtros.dataInicio);
    if (filtros.dataFim) params.append('dataFim', filtros.dataFim);
    if (filtros.page !== undefined) params.append('page', String(filtros.page));
    if (filtros.size !== undefined) params.append('size', String(filtros.size));

    const response = await api.get<DespesaPageResponse>(`${BASE_URL}?${params.toString()}`);
    return response.data;
  },

  /**
   * Atualiza uma despesa
   */
  async atualizar(id: string, data: DespesaUpdateRequest): Promise<DespesaResponse> {
    const response = await api.put<DespesaResponse>(`${BASE_URL}/${id}`, data);
    return response.data;
  },

  /**
   * Exclui uma despesa
   */
  async excluir(id: string): Promise<void> {
    await api.delete(`${BASE_URL}/${id}`);
  },

  /**
   * Registra pagamento de uma despesa
   */
  async pagar(id: string, data: DespesaPagamentoRequest): Promise<DespesaResponse> {
    const response = await api.patch<DespesaResponse>(`${BASE_URL}/${id}/pagar`, data);
    return response.data;
  },

  /**
   * Cancela uma despesa
   */
  async cancelar(id: string): Promise<DespesaResponse> {
    const response = await api.patch<DespesaResponse>(`${BASE_URL}/${id}/cancelar`);
    return response.data;
  },

  /**
   * Lista despesas vencidas
   */
  async listarVencidas(): Promise<DespesaListItem[]> {
    const response = await api.get<DespesaListItem[]>(`${BASE_URL}/vencidas`);
    return response.data;
  },

  /**
   * Lista despesas a vencer nos próximos dias
   */
  async listarAVencer(dias: number = 7): Promise<DespesaListItem[]> {
    const response = await api.get<DespesaListItem[]>(`${BASE_URL}/a-vencer?dias=${dias}`);
    return response.data;
  },

  /**
   * Busca resumo das despesas para dashboard
   */
  async getResumo(): Promise<DespesaResumo> {
    const response = await api.get<DespesaResumo>(`${BASE_URL}/resumo`);
    return response.data;
  },

  /**
   * Lista todas as categorias disponíveis
   */
  async listarCategorias(): Promise<CategoriaInfo[]> {
    const response = await api.get<CategoriaInfo[]>(`${BASE_URL}/categorias`);
    return response.data;
  },
};

export default despesaService;
