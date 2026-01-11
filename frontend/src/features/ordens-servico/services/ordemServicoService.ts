/**
 * Service para comunicação com a API de Ordens de Serviço
 * Endpoints: /api/ordens-servico
 */

import { api } from '@/shared/services/api';
import type { PaginatedResponse } from '@/shared/types/api';
import type {
  OrdemServico,
  CreateOrdemServicoRequest,
  UpdateOrdemServicoRequest,
  CancelarOrdemServicoRequest,
  AguardarPecaRequest,
  FinalizarOSRequest,
  OrdemServicoFilters,
  DashboardContagem,
  DashboardFaturamento,
  DashboardTicketMedio,
  HistoricoStatusOS,
} from '../types';

export const ordemServicoService = {
  /**
   * Listar ordens de serviço com filtros e paginação
   */
  async findAll(filters: OrdemServicoFilters = {}): Promise<PaginatedResponse<OrdemServico>> {
    const params = new URLSearchParams();

    if (filters.status) params.append('status', filters.status);
    if (filters.veiculoId) params.append('veiculoId', filters.veiculoId);
    if (filters.usuarioId) params.append('usuarioId', filters.usuarioId);
    if (filters.dataInicio) params.append('dataInicio', filters.dataInicio);
    if (filters.dataFim) params.append('dataFim', filters.dataFim);

    params.append('page', (filters.page ?? 0).toString());
    params.append('size', (filters.size ?? 20).toString());
    params.append('sort', filters.sort ?? 'numero,desc');

    const response = await api.get<PaginatedResponse<OrdemServico>>(
      `/ordens-servico?${params.toString()}`
    );
    return response.data;
  },

  /**
   * Buscar ordem de serviço por ID
   */
  async findById(id: string): Promise<OrdemServico> {
    const response = await api.get<OrdemServico>(`/ordens-servico/${id}`);
    return response.data;
  },

  /**
   * Buscar ordem de serviço por número
   */
  async findByNumero(numero: number): Promise<OrdemServico> {
    const response = await api.get<OrdemServico>(`/ordens-servico/numero/${numero}`);
    return response.data;
  },

  /**
   * Buscar histórico de OS de um veículo
   */
  async findHistoricoVeiculo(
    veiculoId: string,
    page = 0,
    size = 20,
    sort = 'numero,desc'
  ): Promise<PaginatedResponse<OrdemServico>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sort,
    });

    const response = await api.get<PaginatedResponse<OrdemServico>>(
      `/ordens-servico/veiculo/${veiculoId}/historico?${params.toString()}`
    );
    return response.data;
  },

  /**
   * Criar nova ordem de serviço (status inicial: ORCAMENTO)
   */
  async create(data: CreateOrdemServicoRequest): Promise<OrdemServico> {
    const response = await api.post<OrdemServico>('/ordens-servico', data);
    return response.data;
  },

  /**
   * Atualizar ordem de serviço
   * Só permite editar se status = ORCAMENTO ou APROVADO
   */
  async update(id: string, data: UpdateOrdemServicoRequest): Promise<OrdemServico> {
    const response = await api.put<OrdemServico>(`/ordens-servico/${id}`, data);
    return response.data;
  },

  /**
   * Aprovar orçamento (transição ORCAMENTO → APROVADO)
   */
  async aprovar(id: string, aprovadoPeloCliente = true): Promise<void> {
    await api.patch(`/ordens-servico/${id}/aprovar`, null, {
      params: { aprovadoPeloCliente },
    });
  },

  /**
   * Iniciar execução (transição APROVADO → EM_ANDAMENTO)
   */
  async iniciar(id: string): Promise<void> {
    await api.patch(`/ordens-servico/${id}/iniciar`);
  },

  /**
   * Colocar OS em aguardando peça (transição EM_ANDAMENTO → AGUARDANDO_PECA)
   */
  async aguardarPeca(id: string, data: AguardarPecaRequest): Promise<void> {
    await api.patch(`/ordens-servico/${id}/aguardar-peca`, data);
  },

  /**
   * Retomar execução após receber peça (transição AGUARDANDO_PECA → EM_ANDAMENTO)
   */
  async retomar(id: string): Promise<void> {
    await api.patch(`/ordens-servico/${id}/retomar`);
  },

  /**
   * Finalizar serviços (transição EM_ANDAMENTO → FINALIZADO)
   * Para modelo VALOR_FIXO - sem corpo de requisição
   * Baixa estoque de peças
   */
  async finalizar(id: string): Promise<void> {
    await api.patch(`/ordens-servico/${id}/finalizar`);
  },

  /**
   * Finalizar serviços com horas trabalhadas (modelo POR_HORA)
   * Transição EM_ANDAMENTO → FINALIZADO
   * Calcula mão de obra baseada nas horas trabalhadas
   * Baixa estoque de peças
   */
  async finalizarComHoras(id: string, data: FinalizarOSRequest): Promise<OrdemServico> {
    const response = await api.post<OrdemServico>(`/ordens-servico/${id}/finalizar`, data);
    return response.data;
  },

  /**
   * Entregar veículo (transição FINALIZADO → ENTREGUE)
   * Requer pagamento completo
   */
  async entregar(id: string): Promise<void> {
    await api.patch(`/ordens-servico/${id}/entregar`);
  },

  /**
   * Cancelar ordem de serviço (qualquer status → CANCELADO)
   */
  async cancelar(id: string, data: CancelarOrdemServicoRequest): Promise<void> {
    await api.patch(`/ordens-servico/${id}/cancelar`, data);
  },

  /**
   * Obter contagem de OS por status (dashboard)
   */
  async getDashboardContagem(): Promise<DashboardContagem> {
    const response = await api.get<DashboardContagem>(
      '/ordens-servico/dashboard/contagem-por-status'
    );
    return response.data;
  },

  /**
   * Obter faturamento do período (dashboard)
   */
  async getDashboardFaturamento(dataInicio: string, dataFim: string): Promise<DashboardFaturamento> {
    const params = new URLSearchParams({
      dataInicio,
      dataFim,
    });

    const response = await api.get<DashboardFaturamento>(
      `/ordens-servico/dashboard/faturamento?${params.toString()}`
    );
    return response.data;
  },

  /**
   * Obter ticket médio do período (dashboard)
   */
  async getDashboardTicketMedio(dataInicio: string, dataFim: string): Promise<DashboardTicketMedio> {
    const params = new URLSearchParams({
      dataInicio,
      dataFim,
    });

    const response = await api.get<DashboardTicketMedio>(
      `/ordens-servico/dashboard/ticket-medio?${params.toString()}`
    );
    return response.data;
  },

  /**
   * Gerar PDF da OS
   */
  async gerarPDF(id: string): Promise<Blob> {
    const response = await api.post(`/ordens-servico/${id}/gerar-pdf`, null, {
      responseType: 'blob',
    });
    return response.data;
  },

  /**
   * Buscar histórico de mudanças de status de uma OS
   */
  async getHistoricoStatus(id: string): Promise<HistoricoStatusOS[]> {
    const response = await api.get<HistoricoStatusOS[]>(`/ordens-servico/${id}/historico-status`);
    return response.data;
  },
};
