/**
 * SaaS Admin Service - API calls for SUPER_ADMIN
 */

import { api } from '@/shared/services/api';
import type { PaginatedResponse } from '@/shared/types/api';
import type {
  DashboardStats,
  DashboardMetrics,
  MRRBreakdown,
  MRREvolution,
  ChurnEvolution,
  SignupsVsCancellations,
  OficinaResumo,
  OficinaDetail,
  OficinaFilters,
  CreateOficinaFullRequest,
  UpdateOficinaFullRequest,
  OficinaMetricas,
  ImpersonateResponse,
  UpdateLimitesRequest,
  Pagamento,
  PagamentoFilters,
  RegistrarPagamentoRequest,
  AuditLog,
  AuditFilters,
  StatusOficina,
  Plano,
  CreatePlanoRequest,
  UpdatePlanoRequest,
  AlterarPlanoOficinaRequest,
  PlanoStatistics,
  Fatura,
  FaturaResumo,
  FaturaFilters,
  CreateFaturaRequest,
  RegistrarPagamentoFaturaRequest,
  FaturaSummary,
  InadimplenciaDashboard,
  OficinaInadimplente,
  AcaoMassaInadimplenciaRequest,
  AcaoMassaResult,
  Acordo,
  AcordoFilters,
  CriarAcordoRequest,
  RelatorioSummary,
  RelatorioFinanceiro,
  RelatorioOperacional,
  RelatorioCrescimento,
  FormatoExport,
} from '../types';

// ===== DASHBOARD =====

export const dashboardService = {
  async getStats(): Promise<DashboardStats> {
    const response = await api.get<DashboardStats>('/saas/dashboard/stats');
    return response.data;
  },

  async getMRRBreakdown(): Promise<MRRBreakdown[]> {
    const response = await api.get<MRRBreakdown[]>('/saas/dashboard/mrr');
    return response.data;
  },

  async getTrialsExpiring(page = 0, size = 10): Promise<PaginatedResponse<OficinaResumo>> {
    const response = await api.get<PaginatedResponse<OficinaResumo>>(
      `/saas/dashboard/trials-expiring?page=${page}&size=${size}`
    );
    return response.data;
  },

  // Advanced metrics
  async getMetrics(): Promise<DashboardMetrics> {
    const response = await api.get<DashboardMetrics>('/saas/dashboard/metrics');
    return response.data;
  },

  async getMRREvolution(months = 12): Promise<MRREvolution> {
    const response = await api.get<MRREvolution>(`/saas/dashboard/mrr-evolution?months=${months}`);
    return response.data;
  },

  async getChurnEvolution(months = 12): Promise<ChurnEvolution> {
    const response = await api.get<ChurnEvolution>(`/saas/dashboard/churn-evolution?months=${months}`);
    return response.data;
  },

  async getSignupsVsCancellations(months = 12): Promise<SignupsVsCancellations> {
    const response = await api.get<SignupsVsCancellations>(`/saas/dashboard/signups-vs-cancellations?months=${months}`);
    return response.data;
  },
};

// ===== OFICINAS =====

export const oficinasService = {
  async findAll(filters: OficinaFilters = {}): Promise<PaginatedResponse<OficinaResumo>> {
    const params = new URLSearchParams();

    if (filters.status) params.append('status', filters.status);
    if (filters.plano) params.append('plano', filters.plano);
    if (filters.searchTerm) params.append('nome', filters.searchTerm);
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));
    if (filters.sort) params.append('sort', filters.sort);

    const response = await api.get<PaginatedResponse<OficinaResumo>>(
      `/saas/oficinas?${params.toString()}`
    );
    return response.data;
  },

  async findByStatus(status: StatusOficina, page = 0, size = 20): Promise<PaginatedResponse<OficinaResumo>> {
    const response = await api.get<PaginatedResponse<OficinaResumo>>(
      `/saas/dashboard/oficinas?status=${status}&page=${page}&size=${size}`
    );
    return response.data;
  },

  async findById(id: string): Promise<OficinaDetail> {
    const response = await api.get<OficinaDetail>(`/saas/oficinas/${id}`);
    return response.data;
  },

  async create(data: CreateOficinaFullRequest): Promise<OficinaDetail> {
    const response = await api.post<OficinaDetail>('/saas/oficinas', data);
    return response.data;
  },

  async update(id: string, data: UpdateOficinaFullRequest): Promise<OficinaDetail> {
    const response = await api.put<OficinaDetail>(`/saas/oficinas/${id}`, data);
    return response.data;
  },

  async activate(id: string): Promise<void> {
    await api.post(`/saas/oficinas/${id}/activate`);
  },

  async suspend(id: string): Promise<void> {
    await api.post(`/saas/oficinas/${id}/suspend`);
  },

  async cancel(id: string): Promise<void> {
    await api.post(`/saas/oficinas/${id}/cancel`);
  },

  async getMetricas(id: string): Promise<OficinaMetricas> {
    const response = await api.get<OficinaMetricas>(`/saas/oficinas/${id}/metricas`);
    return response.data;
  },

  async impersonate(id: string): Promise<ImpersonateResponse> {
    const response = await api.post<ImpersonateResponse>(`/saas/oficinas/${id}/impersonate`);
    return response.data;
  },

  async updateLimites(id: string, data: UpdateLimitesRequest): Promise<OficinaDetail> {
    const response = await api.put<OficinaDetail>(`/saas/oficinas/${id}/limites`, data);
    return response.data;
  },
};

// ===== PAGAMENTOS =====

export const pagamentosService = {
  async findAll(filters: PagamentoFilters = {}): Promise<PaginatedResponse<Pagamento>> {
    const params = new URLSearchParams();

    if (filters.oficinaId) params.append('oficinaId', filters.oficinaId);
    if (filters.status) params.append('status', filters.status);
    if (filters.dataInicio) params.append('dataInicio', filters.dataInicio);
    if (filters.dataFim) params.append('dataFim', filters.dataFim);
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));

    const response = await api.get<PaginatedResponse<Pagamento>>(
      `/saas/pagamentos?${params.toString()}`
    );
    return response.data;
  },

  async getPendentes(page = 0, size = 20): Promise<PaginatedResponse<OficinaResumo>> {
    const response = await api.get<PaginatedResponse<OficinaResumo>>(
      `/saas/pagamentos/pendentes?page=${page}&size=${size}`
    );
    return response.data;
  },

  async getInadimplentes(page = 0, size = 20): Promise<PaginatedResponse<OficinaResumo>> {
    const response = await api.get<PaginatedResponse<OficinaResumo>>(
      `/saas/pagamentos/inadimplentes?page=${page}&size=${size}`
    );
    return response.data;
  },

  async registrar(data: RegistrarPagamentoRequest): Promise<Pagamento> {
    const response = await api.post<Pagamento>('/saas/pagamentos', data);
    return response.data;
  },
};

// ===== AUDITORIA =====

export const auditService = {
  async findAll(filters: AuditFilters = {}): Promise<PaginatedResponse<AuditLog>> {
    const params = new URLSearchParams();

    if (filters.acao) params.append('acao', filters.acao);
    if (filters.entidade) params.append('entidade', filters.entidade);
    if (filters.usuarioEmail) params.append('usuarioEmail', filters.usuarioEmail);
    if (filters.ipAddress) params.append('ipAddress', filters.ipAddress);
    if (filters.dataInicio) params.append('dataInicio', filters.dataInicio);
    if (filters.dataFim) params.append('dataFim', filters.dataFim);
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));

    const response = await api.get<PaginatedResponse<AuditLog>>(
      `/saas/audit?${params.toString()}`
    );
    return response.data;
  },

  async exportCsv(filters: AuditFilters = {}): Promise<Blob> {
    const params = new URLSearchParams();

    if (filters.acao) params.append('acao', filters.acao);
    if (filters.entidade) params.append('entidade', filters.entidade);
    if (filters.usuarioEmail) params.append('usuarioEmail', filters.usuarioEmail);
    if (filters.ipAddress) params.append('ipAddress', filters.ipAddress);
    if (filters.dataInicio) params.append('dataInicio', filters.dataInicio);
    if (filters.dataFim) params.append('dataFim', filters.dataFim);

    const response = await api.get(`/saas/audit/export?${params.toString()}`, {
      responseType: 'blob',
    });
    return response.data;
  },

  async count(filters: AuditFilters = {}): Promise<number> {
    const params = new URLSearchParams();

    if (filters.acao) params.append('acao', filters.acao);
    if (filters.entidade) params.append('entidade', filters.entidade);
    if (filters.usuarioEmail) params.append('usuarioEmail', filters.usuarioEmail);
    if (filters.ipAddress) params.append('ipAddress', filters.ipAddress);
    if (filters.dataInicio) params.append('dataInicio', filters.dataInicio);
    if (filters.dataFim) params.append('dataFim', filters.dataFim);

    const response = await api.get<{ count: number }>(`/saas/audit/count?${params.toString()}`);
    return response.data.count;
  },
};

// ===== JOBS =====

export const jobsService = {
  async suspendOverdue(): Promise<void> {
    await api.post('/saas/jobs/suspend-overdue');
  },

  async alertTrials(): Promise<void> {
    await api.post('/saas/jobs/alert-trials');
  },

  async refreshStats(): Promise<void> {
    await api.post('/saas/jobs/refresh-stats');
  },

  async runAll(): Promise<void> {
    await api.post('/saas/jobs/run-all');
  },
};

// ===== PLANOS =====

export const planosService = {
  async findAll(): Promise<Plano[]> {
    const response = await api.get<Plano[]>('/saas/planos');
    return response.data;
  },

  async findAllActive(): Promise<Plano[]> {
    const response = await api.get<Plano[]>('/saas/planos/ativos');
    return response.data;
  },

  async findVisiblePlans(): Promise<Plano[]> {
    const response = await api.get<Plano[]>('/saas/planos/visiveis');
    return response.data;
  },

  async findById(id: string): Promise<Plano> {
    const response = await api.get<Plano>(`/saas/planos/${id}`);
    return response.data;
  },

  async findByCodigo(codigo: string): Promise<Plano> {
    const response = await api.get<Plano>(`/saas/planos/codigo/${codigo}`);
    return response.data;
  },

  async create(data: CreatePlanoRequest): Promise<Plano> {
    const response = await api.post<Plano>('/saas/planos', data);
    return response.data;
  },

  async update(id: string, data: UpdatePlanoRequest): Promise<Plano> {
    const response = await api.put<Plano>(`/saas/planos/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/saas/planos/${id}`);
  },

  async toggleVisibility(id: string): Promise<Plano> {
    const response = await api.post<Plano>(`/saas/planos/${id}/toggle-visibilidade`);
    return response.data;
  },

  async getStatistics(): Promise<PlanoStatistics> {
    const response = await api.get<PlanoStatistics>('/saas/planos/estatisticas');
    return response.data;
  },

  async alterarPlanoOficina(oficinaId: string, data: AlterarPlanoOficinaRequest): Promise<void> {
    await api.post(`/saas/planos/oficinas/${oficinaId}/alterar-plano`, data);
  },
};

// ===== FATURAS =====

export const faturasService = {
  async findAll(filters: FaturaFilters = {}): Promise<PaginatedResponse<FaturaResumo>> {
    const params = new URLSearchParams();

    if (filters.oficinaId) params.append('oficinaId', filters.oficinaId);
    if (filters.status) params.append('status', filters.status);
    if (filters.dataInicio) params.append('dataInicio', filters.dataInicio);
    if (filters.dataFim) params.append('dataFim', filters.dataFim);
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));

    const response = await api.get<PaginatedResponse<FaturaResumo>>(
      `/saas/faturas?${params.toString()}`
    );
    return response.data;
  },

  async findById(id: string): Promise<Fatura> {
    const response = await api.get<Fatura>(`/saas/faturas/${id}`);
    return response.data;
  },

  async findByNumero(numero: string): Promise<Fatura> {
    const response = await api.get<Fatura>(`/saas/faturas/numero/${numero}`);
    return response.data;
  },

  async findByOficina(oficinaId: string, page = 0, size = 20): Promise<PaginatedResponse<FaturaResumo>> {
    const response = await api.get<PaginatedResponse<FaturaResumo>>(
      `/saas/faturas/oficina/${oficinaId}?page=${page}&size=${size}`
    );
    return response.data;
  },

  async create(data: CreateFaturaRequest): Promise<Fatura> {
    const response = await api.post<Fatura>('/saas/faturas', data);
    return response.data;
  },

  async gerarParaOficina(oficinaId: string, mesReferencia?: string): Promise<Fatura> {
    const params = mesReferencia ? `?mesReferencia=${mesReferencia}` : '';
    const response = await api.post<Fatura>(`/saas/faturas/gerar/${oficinaId}${params}`);
    return response.data;
  },

  async gerarMensais(): Promise<{ message: string; count: number }> {
    const response = await api.post<{ message: string; count: number }>('/saas/faturas/gerar-mensais');
    return response.data;
  },

  async registrarPagamento(id: string, data: RegistrarPagamentoFaturaRequest): Promise<Fatura> {
    const response = await api.post<Fatura>(`/saas/faturas/${id}/registrar-pagamento`, data);
    return response.data;
  },

  async cancelar(id: string, motivo?: string): Promise<Fatura> {
    const response = await api.post<Fatura>(`/saas/faturas/${id}/cancelar`, { motivo });
    return response.data;
  },

  async processarVencidas(): Promise<{ message: string; count: number }> {
    const response = await api.post<{ message: string; count: number }>('/saas/faturas/processar-vencidas');
    return response.data;
  },

  async getSummary(): Promise<FaturaSummary> {
    const response = await api.get<FaturaSummary>('/saas/faturas/summary');
    return response.data;
  },
};

// ===== INADIMPLÊNCIA =====

export const inadimplenciaService = {
  async getDashboard(): Promise<InadimplenciaDashboard> {
    const response = await api.get<InadimplenciaDashboard>('/saas/inadimplencia/dashboard');
    return response.data;
  },

  async listarInadimplentes(page = 0, size = 20): Promise<PaginatedResponse<OficinaInadimplente>> {
    const response = await api.get<PaginatedResponse<OficinaInadimplente>>(
      `/saas/inadimplencia/oficinas?page=${page}&size=${size}`
    );
    return response.data;
  },

  async executarAcaoMassa(data: AcaoMassaInadimplenciaRequest): Promise<AcaoMassaResult> {
    const response = await api.post<AcaoMassaResult>('/saas/inadimplencia/acao-massa', data);
    return response.data;
  },

  // Agreements
  async criarAcordo(oficinaId: string, data: CriarAcordoRequest): Promise<Acordo> {
    const response = await api.post<Acordo>(`/saas/inadimplencia/oficinas/${oficinaId}/acordo`, data);
    return response.data;
  },

  async getAcordo(id: string): Promise<Acordo> {
    const response = await api.get<Acordo>(`/saas/inadimplencia/acordos/${id}`);
    return response.data;
  },

  async listarAcordos(filters: AcordoFilters = {}): Promise<PaginatedResponse<Acordo>> {
    const params = new URLSearchParams();

    if (filters.oficinaId) params.append('oficinaId', filters.oficinaId);
    if (filters.status) params.append('status', filters.status);
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));

    const response = await api.get<PaginatedResponse<Acordo>>(
      `/saas/inadimplencia/acordos?${params.toString()}`
    );
    return response.data;
  },

  async cancelarAcordo(id: string, motivo: string): Promise<Acordo> {
    const response = await api.post<Acordo>(
      `/saas/inadimplencia/acordos/${id}/cancelar?motivo=${encodeURIComponent(motivo)}`
    );
    return response.data;
  },

  async registrarPagamentoParcela(
    acordoId: string,
    parcelaId: string,
    metodoPagamento: string,
    transacaoId?: string
  ): Promise<Acordo> {
    let url = `/saas/inadimplencia/acordos/${acordoId}/parcelas/${parcelaId}/pagar?metodoPagamento=${metodoPagamento}`;
    if (transacaoId) {
      url += `&transacaoId=${encodeURIComponent(transacaoId)}`;
    }
    const response = await api.post<Acordo>(url);
    return response.data;
  },
};

// ===== RELATÓRIOS =====

export const relatorioService = {
  async getSummary(): Promise<RelatorioSummary> {
    const response = await api.get<RelatorioSummary>('/saas/relatorios');
    return response.data;
  },

  async getRelatorioFinanceiro(
    dataInicio: string,
    dataFim: string
  ): Promise<RelatorioFinanceiro> {
    const response = await api.get<RelatorioFinanceiro>(
      `/saas/relatorios/financeiro?dataInicio=${dataInicio}&dataFim=${dataFim}`
    );
    return response.data;
  },

  async getRelatorioOperacional(
    dataInicio: string,
    dataFim: string
  ): Promise<RelatorioOperacional> {
    const response = await api.get<RelatorioOperacional>(
      `/saas/relatorios/operacional?dataInicio=${dataInicio}&dataFim=${dataFim}`
    );
    return response.data;
  },

  async getRelatorioCrescimento(
    dataInicio: string,
    dataFim: string
  ): Promise<RelatorioCrescimento> {
    const response = await api.get<RelatorioCrescimento>(
      `/saas/relatorios/crescimento?dataInicio=${dataInicio}&dataFim=${dataFim}`
    );
    return response.data;
  },

  async exportarRelatorio(
    tipo: 'financeiro' | 'operacional' | 'crescimento',
    dataInicio: string,
    dataFim: string,
    formato: FormatoExport = 'PDF'
  ): Promise<Blob> {
    const response = await api.get(
      `/saas/relatorios/${tipo}/export?dataInicio=${dataInicio}&dataFim=${dataFim}&formato=${formato}`,
      { responseType: 'blob' }
    );
    return response.data;
  },
};

// ===== TICKETS =====

import type {
  Ticket,
  TicketDetail,
  TicketFilters,
  TicketMetricas,
  CreateTicketRequest,
  ResponderTicketRequest,
  AtribuirTicketRequest,
  AlterarStatusTicketRequest,
  AlterarPrioridadeTicketRequest,
  MensagemTicket,
  TicketEnums,
} from '../types';

export const ticketService = {
  async findAll(filters: TicketFilters = {}): Promise<PaginatedResponse<Ticket>> {
    const params = new URLSearchParams();

    if (filters.oficinaId) params.append('oficinaId', filters.oficinaId);
    if (filters.status) params.append('status', filters.status);
    if (filters.tipo) params.append('tipo', filters.tipo);
    if (filters.prioridade) params.append('prioridade', filters.prioridade);
    if (filters.atribuidoA) params.append('atribuidoA', filters.atribuidoA);
    if (filters.busca) params.append('busca', filters.busca);
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));

    const response = await api.get<PaginatedResponse<Ticket>>(
      `/saas/tickets?${params.toString()}`
    );
    return response.data;
  },

  async findById(id: string): Promise<TicketDetail> {
    const response = await api.get<TicketDetail>(`/saas/tickets/${id}`);
    return response.data;
  },

  async findByNumero(numero: string): Promise<TicketDetail> {
    const response = await api.get<TicketDetail>(`/saas/tickets/numero/${numero}`);
    return response.data;
  },

  async create(data: CreateTicketRequest): Promise<Ticket> {
    const response = await api.post<Ticket>('/saas/tickets', data);
    return response.data;
  },

  async responder(id: string, data: ResponderTicketRequest): Promise<MensagemTicket> {
    const response = await api.post<MensagemTicket>(`/saas/tickets/${id}/responder`, data);
    return response.data;
  },

  async atribuir(id: string, data: AtribuirTicketRequest): Promise<Ticket> {
    const response = await api.post<Ticket>(`/saas/tickets/${id}/atribuir`, data);
    return response.data;
  },

  async alterarStatus(id: string, data: AlterarStatusTicketRequest): Promise<Ticket> {
    const response = await api.post<Ticket>(`/saas/tickets/${id}/alterar-status`, data);
    return response.data;
  },

  async alterarPrioridade(id: string, data: AlterarPrioridadeTicketRequest): Promise<Ticket> {
    const response = await api.post<Ticket>(`/saas/tickets/${id}/alterar-prioridade`, data);
    return response.data;
  },

  async getMetricas(): Promise<TicketMetricas> {
    const response = await api.get<TicketMetricas>('/saas/tickets/metricas');
    return response.data;
  },

  async getEnums(): Promise<TicketEnums> {
    const response = await api.get<TicketEnums>('/saas/tickets/enums');
    return response.data;
  },
};

// ===== SUPER ADMINS =====

import type { SuperAdmin } from '../types';

export const superAdminService = {
  async findAll(): Promise<SuperAdmin[]> {
    const response = await api.get<SuperAdmin[]>('/saas/super-admins');
    return response.data;
  },
};

// ===== COMUNICADOS =====

import type {
  Comunicado,
  ComunicadoDetail,
  ComunicadoFilters,
  ComunicadoMetricas,
  ComunicadoEnums,
  CreateComunicadoRequest,
  UpdateComunicadoRequest,
} from '../types';

export const comunicadoService = {
  async findAll(filters: ComunicadoFilters = {}): Promise<PaginatedResponse<Comunicado>> {
    const params = new URLSearchParams();

    if (filters.status) params.append('status', filters.status);
    if (filters.tipo) params.append('tipo', filters.tipo);
    if (filters.prioridade) params.append('prioridade', filters.prioridade);
    if (filters.busca) params.append('busca', filters.busca);
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));

    const response = await api.get<PaginatedResponse<Comunicado>>(
      `/saas/comunicados?${params.toString()}`
    );
    return response.data;
  },

  async findById(id: string): Promise<ComunicadoDetail> {
    const response = await api.get<ComunicadoDetail>(`/saas/comunicados/${id}`);
    return response.data;
  },

  async create(data: CreateComunicadoRequest): Promise<Comunicado> {
    const response = await api.post<Comunicado>('/saas/comunicados', data);
    return response.data;
  },

  async update(id: string, data: UpdateComunicadoRequest): Promise<Comunicado> {
    const response = await api.put<Comunicado>(`/saas/comunicados/${id}`, data);
    return response.data;
  },

  async enviar(id: string): Promise<Comunicado> {
    const response = await api.post<Comunicado>(`/saas/comunicados/${id}/enviar`);
    return response.data;
  },

  async agendar(id: string, dataAgendamento: string): Promise<Comunicado> {
    const response = await api.post<Comunicado>(`/saas/comunicados/${id}/agendar`, {
      dataAgendamento,
    });
    return response.data;
  },

  async cancelar(id: string): Promise<Comunicado> {
    const response = await api.post<Comunicado>(`/saas/comunicados/${id}/cancelar`);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/saas/comunicados/${id}`);
  },

  async getMetricas(): Promise<ComunicadoMetricas> {
    const response = await api.get<ComunicadoMetricas>('/saas/comunicados/metricas');
    return response.data;
  },

  async getEnums(): Promise<ComunicadoEnums> {
    const response = await api.get<ComunicadoEnums>('/saas/comunicados/enums');
    return response.data;
  },

  async processarAgendados(): Promise<{ comunicadosEnviados: number }> {
    const response = await api.post<{ comunicadosEnviados: number }>(
      '/saas/comunicados/processar-agendados'
    );
    return response.data;
  },
};

// ===== FEATURE FLAGS =====

import type {
  FeatureFlag,
  CreateFeatureFlagRequest,
  UpdateFeatureFlagRequest,
  ToggleFeatureFlagRequest,
  OficinaFeatureFlags,
  FeatureFlagStats,
} from '../types';

export const featureFlagService = {
  async findAll(): Promise<FeatureFlag[]> {
    const response = await api.get<FeatureFlag[]>('/saas/features');
    return response.data;
  },

  async findById(id: string): Promise<FeatureFlag> {
    const response = await api.get<FeatureFlag>(`/saas/features/${id}`);
    return response.data;
  },

  async findByCodigo(codigo: string): Promise<FeatureFlag> {
    const response = await api.get<FeatureFlag>(`/saas/features/codigo/${codigo}`);
    return response.data;
  },

  async findByCategoria(categoria: string): Promise<FeatureFlag[]> {
    const response = await api.get<FeatureFlag[]>(`/saas/features/categoria/${categoria}`);
    return response.data;
  },

  async getCategorias(): Promise<string[]> {
    const response = await api.get<string[]>('/saas/features/categorias');
    return response.data;
  },

  async create(request: CreateFeatureFlagRequest): Promise<FeatureFlag> {
    const response = await api.post<FeatureFlag>('/saas/features', request);
    return response.data;
  },

  async update(id: string, request: UpdateFeatureFlagRequest): Promise<FeatureFlag> {
    const response = await api.put<FeatureFlag>(`/saas/features/${id}`, request);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/saas/features/${id}`);
  },

  async toggle(id: string, request: ToggleFeatureFlagRequest): Promise<FeatureFlag> {
    const response = await api.post<FeatureFlag>(`/saas/features/${id}/toggle`, request);
    return response.data;
  },

  async toggleGlobal(id: string, habilitado: boolean): Promise<FeatureFlag> {
    const response = await api.post<FeatureFlag>(
      `/saas/features/${id}/toggle-global?habilitado=${habilitado}`
    );
    return response.data;
  },

  async getOficinaFeatures(oficinaId: string): Promise<OficinaFeatureFlags> {
    const response = await api.get<OficinaFeatureFlags>(`/saas/features/oficina/${oficinaId}`);
    return response.data;
  },

  async checkFeature(codigo: string, oficinaId: string): Promise<{ enabled: boolean }> {
    const response = await api.get<{ enabled: boolean }>(
      `/saas/features/check/${codigo}/oficina/${oficinaId}`
    );
    return response.data;
  },

  async getStats(id: string): Promise<FeatureFlagStats> {
    const response = await api.get<FeatureFlagStats>(`/saas/features/${id}/stats`);
    return response.data;
  },

  async getByPlano(): Promise<Record<string, FeatureFlag[]>> {
    const response = await api.get<Record<string, FeatureFlag[]>>('/saas/features/by-plano');
    return response.data;
  },
};
