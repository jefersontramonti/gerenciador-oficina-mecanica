import { api } from '@/shared/services/api';
import type {
  PlanoManutencao,
  PlanoManutencaoRequest,
  PlanoFilters,
  ExecutarPlanoRequest,
  AplicarTemplateRequest,
  TemplateManutencao,
  TemplateManutencaoRequest,
  TemplateFilters,
  AgendamentoManutencao,
  AgendamentoManutencaoRequest,
  AgendamentoFilters,
  RemarcarAgendamentoRequest,
  CalendarioEvento,
  DashboardManutencao,
  EstatisticasManutencao,
} from '../types';

const BASE_URL = '/manutencao-preventiva';

// ==================== PLANOS ====================

export const planoService = {
  async listar(filters: PlanoFilters = {}) {
    const params = new URLSearchParams();
    if (filters.veiculoId) params.append('veiculoId', filters.veiculoId);
    if (filters.status) params.append('status', filters.status);
    if (filters.tipoManutencao) params.append('tipoManutencao', filters.tipoManutencao);
    if (filters.busca) params.append('busca', filters.busca);
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));

    const { data } = await api.get<PageResponse<PlanoManutencao>>(`${BASE_URL}/planos?${params}`);
    return data;
  },

  async buscarPorId(id: string) {
    const { data } = await api.get<PlanoManutencao>(`${BASE_URL}/planos/${id}`);
    return data;
  },

  async listarPorVeiculo(veiculoId: string) {
    const { data } = await api.get<PlanoManutencao[]>(`${BASE_URL}/planos/veiculo/${veiculoId}`);
    return data;
  },

  async listarVencidos() {
    const { data } = await api.get<PlanoManutencao[]>(`${BASE_URL}/planos/vencidos`);
    return data;
  },

  async listarProximosAVencer(dias = 30) {
    const { data } = await api.get<PlanoManutencao[]>(`${BASE_URL}/planos/proximos-vencer?dias=${dias}`);
    return data;
  },

  async criar(request: PlanoManutencaoRequest) {
    const { data } = await api.post<PlanoManutencao>(`${BASE_URL}/planos`, request);
    return data;
  },

  async atualizar(id: string, request: PlanoManutencaoRequest) {
    const { data } = await api.put<PlanoManutencao>(`${BASE_URL}/planos/${id}`, request);
    return data;
  },

  async ativar(id: string) {
    const { data } = await api.patch<PlanoManutencao>(`${BASE_URL}/planos/${id}/ativar`);
    return data;
  },

  async pausar(id: string, motivo?: string) {
    const { data } = await api.patch<PlanoManutencao>(`${BASE_URL}/planos/${id}/pausar`, { motivo });
    return data;
  },

  async concluir(id: string) {
    const { data } = await api.patch<PlanoManutencao>(`${BASE_URL}/planos/${id}/concluir`);
    return data;
  },

  async executar(id: string, request: ExecutarPlanoRequest) {
    const { data } = await api.post<PlanoManutencao>(`${BASE_URL}/planos/${id}/executar`, request);
    return data;
  },

  async deletar(id: string) {
    await api.delete(`${BASE_URL}/planos/${id}`);
  },
};

// ==================== TEMPLATES ====================

export const templateService = {
  async listar(filters: TemplateFilters = {}) {
    const params = new URLSearchParams();
    if (filters.tipoManutencao) params.append('tipoManutencao', filters.tipoManutencao);
    if (filters.busca) params.append('busca', filters.busca);
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));

    const { data } = await api.get<PageResponse<TemplateManutencao>>(`${BASE_URL}/templates?${params}`);
    return data;
  },

  async listarDisponiveis() {
    const { data } = await api.get<TemplateManutencao[]>(`${BASE_URL}/templates/disponiveis`);
    return data;
  },

  async listarGlobais() {
    const { data } = await api.get<TemplateManutencao[]>(`${BASE_URL}/templates/globais`);
    return data;
  },

  async listarTiposManutencao() {
    const { data } = await api.get<string[]>(`${BASE_URL}/templates/tipos-manutencao`);
    return data;
  },

  async buscarPorId(id: string) {
    const { data } = await api.get<TemplateManutencao>(`${BASE_URL}/templates/${id}`);
    return data;
  },

  async criar(request: TemplateManutencaoRequest) {
    const { data } = await api.post<TemplateManutencao>(`${BASE_URL}/templates`, request);
    return data;
  },

  async atualizar(id: string, request: TemplateManutencaoRequest) {
    const { data } = await api.put<TemplateManutencao>(`${BASE_URL}/templates/${id}`, request);
    return data;
  },

  async deletar(id: string) {
    await api.delete(`${BASE_URL}/templates/${id}`);
  },

  async aplicar(templateId: string, request: AplicarTemplateRequest) {
    const { data } = await api.post<PlanoManutencao>(`${BASE_URL}/templates/${templateId}/aplicar`, request);
    return data;
  },
};

// ==================== AGENDAMENTOS ====================

export const agendamentoService = {
  async listar(filters: AgendamentoFilters = {}) {
    const params = new URLSearchParams();
    if (filters.veiculoId) params.append('veiculoId', filters.veiculoId);
    if (filters.clienteId) params.append('clienteId', filters.clienteId);
    if (filters.status) params.append('status', filters.status);
    if (filters.dataInicio) params.append('dataInicio', filters.dataInicio);
    if (filters.dataFim) params.append('dataFim', filters.dataFim);
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));

    const { data } = await api.get<PageResponse<AgendamentoManutencao>>(`${BASE_URL}/agendamentos?${params}`);
    return data;
  },

  async buscarPorId(id: string) {
    const { data } = await api.get<AgendamentoManutencao>(`${BASE_URL}/agendamentos/${id}`);
    return data;
  },

  async listarHoje() {
    const { data } = await api.get<AgendamentoManutencao[]>(`${BASE_URL}/agendamentos/hoje`);
    return data;
  },

  async listarProximos(limite = 10) {
    const { data } = await api.get<AgendamentoManutencao[]>(`${BASE_URL}/agendamentos/proximos?limite=${limite}`);
    return data;
  },

  async listarCalendario(mes: number, ano: number) {
    const { data } = await api.get<CalendarioEvento[]>(`${BASE_URL}/agendamentos/calendario?mes=${mes}&ano=${ano}`);
    return data;
  },

  async criar(request: AgendamentoManutencaoRequest) {
    const { data } = await api.post<AgendamentoManutencao>(`${BASE_URL}/agendamentos`, request);
    return data;
  },

  async atualizar(id: string, request: AgendamentoManutencaoRequest) {
    const { data } = await api.put<AgendamentoManutencao>(`${BASE_URL}/agendamentos/${id}`, request);
    return data;
  },

  async confirmar(id: string) {
    const { data } = await api.patch<AgendamentoManutencao>(`${BASE_URL}/agendamentos/${id}/confirmar`);
    return data;
  },

  async remarcar(id: string, request: RemarcarAgendamentoRequest) {
    const { data } = await api.patch<AgendamentoManutencao>(`${BASE_URL}/agendamentos/${id}/remarcar`, request);
    return data;
  },

  async cancelar(id: string, motivo?: string) {
    const { data } = await api.patch<AgendamentoManutencao>(`${BASE_URL}/agendamentos/${id}/cancelar`, { motivo });
    return data;
  },

  async deletar(id: string) {
    await api.delete(`${BASE_URL}/agendamentos/${id}`);
  },
};

// ==================== DASHBOARD ====================

export const dashboardService = {
  async getDashboard() {
    const { data } = await api.get<DashboardManutencao>(`${BASE_URL}/dashboard`);
    return data;
  },

  async getEstatisticas() {
    const { data } = await api.get<EstatisticasManutencao>(`${BASE_URL}/dashboard/estatisticas`);
    return data;
  },

  async getProximasManutencoes(limite = 10) {
    const { data } = await api.get<PlanoManutencao[]>(`${BASE_URL}/dashboard/proximas-manutencoes?limite=${limite}`);
    return data;
  },

  async getAgendamentosHoje() {
    const { data } = await api.get<AgendamentoManutencao[]>(`${BASE_URL}/dashboard/agendamentos-hoje`);
    return data;
  },
};

// Type helper for paginated responses
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
