/**
 * React Query hooks for SUPER_ADMIN SaaS Management
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  dashboardService,
  oficinasService,
  pagamentosService,
  auditService,
  jobsService,
  planosService,
  faturasService,
  inadimplenciaService,
  relatorioService,
  ticketService,
  superAdminService,
  comunicadoService,
} from '../services/saasService';
import type {
  OficinaFilters,
  CreateOficinaFullRequest,
  UpdateOficinaFullRequest,
  UpdateLimitesRequest,
  PagamentoFilters,
  RegistrarPagamentoRequest,
  AuditFilters,
  StatusOficina,
  CreatePlanoRequest,
  UpdatePlanoRequest,
  AlterarPlanoOficinaRequest,
  FaturaFilters,
  CreateFaturaRequest,
  RegistrarPagamentoFaturaRequest,
  AcordoFilters,
  CriarAcordoRequest,
  AcaoMassaInadimplenciaRequest,
  FormatoExport,
  TicketFilters,
  CreateTicketRequest,
  ResponderTicketRequest,
  AtribuirTicketRequest,
  AlterarStatusTicketRequest,
  AlterarPrioridadeTicketRequest,
  ComunicadoFilters,
  CreateComunicadoRequest,
  UpdateComunicadoRequest,
} from '../types';

// ===== QUERY KEYS =====

export const saasKeys = {
  all: ['saas'] as const,
  // Dashboard
  dashboard: () => [...saasKeys.all, 'dashboard'] as const,
  stats: () => [...saasKeys.dashboard(), 'stats'] as const,
  metrics: () => [...saasKeys.dashboard(), 'metrics'] as const,
  mrr: () => [...saasKeys.dashboard(), 'mrr'] as const,
  mrrEvolution: (months: number) => [...saasKeys.dashboard(), 'mrr-evolution', months] as const,
  churnEvolution: (months: number) => [...saasKeys.dashboard(), 'churn-evolution', months] as const,
  signupsVsCancellations: (months: number) => [...saasKeys.dashboard(), 'signups-vs-cancellations', months] as const,
  trialsExpiring: () => [...saasKeys.dashboard(), 'trials-expiring'] as const,
  // Oficinas
  oficinas: () => [...saasKeys.all, 'oficinas'] as const,
  oficinasList: (filters: OficinaFilters) => [...saasKeys.oficinas(), 'list', filters] as const,
  oficinasByStatus: (status: StatusOficina) => [...saasKeys.oficinas(), 'status', status] as const,
  oficinaDetail: (id: string) => [...saasKeys.oficinas(), 'detail', id] as const,
  oficinaMetricas: (id: string) => [...saasKeys.oficinas(), 'metricas', id] as const,
  // Pagamentos
  pagamentos: () => [...saasKeys.all, 'pagamentos'] as const,
  pagamentosList: (filters: PagamentoFilters) => [...saasKeys.pagamentos(), 'list', filters] as const,
  pagamentosPendentes: () => [...saasKeys.pagamentos(), 'pendentes'] as const,
  pagamentosInadimplentes: () => [...saasKeys.pagamentos(), 'inadimplentes'] as const,
  // Audit
  audit: () => [...saasKeys.all, 'audit'] as const,
  auditList: (filters: AuditFilters) => [...saasKeys.audit(), 'list', filters] as const,
  // Planos
  planos: () => [...saasKeys.all, 'planos'] as const,
  planosList: () => [...saasKeys.planos(), 'list'] as const,
  planosActive: () => [...saasKeys.planos(), 'active'] as const,
  planosVisible: () => [...saasKeys.planos(), 'visible'] as const,
  planoDetail: (id: string) => [...saasKeys.planos(), 'detail', id] as const,
  planoByCodigo: (codigo: string) => [...saasKeys.planos(), 'codigo', codigo] as const,
  planosStatistics: () => [...saasKeys.planos(), 'statistics'] as const,
  // Faturas
  faturas: () => [...saasKeys.all, 'faturas'] as const,
  faturasList: (filters: FaturaFilters) => [...saasKeys.faturas(), 'list', filters] as const,
  faturaDetail: (id: string) => [...saasKeys.faturas(), 'detail', id] as const,
  faturasByOficina: (oficinaId: string) => [...saasKeys.faturas(), 'oficina', oficinaId] as const,
  faturasSummary: () => [...saasKeys.faturas(), 'summary'] as const,
  // Inadimplência
  inadimplencia: () => [...saasKeys.all, 'inadimplencia'] as const,
  inadimplenciaDashboard: () => [...saasKeys.inadimplencia(), 'dashboard'] as const,
  inadimplentesLista: (page: number, size: number) => [...saasKeys.inadimplencia(), 'lista', page, size] as const,
  acordos: () => [...saasKeys.inadimplencia(), 'acordos'] as const,
  acordosList: (filters: AcordoFilters) => [...saasKeys.acordos(), 'list', filters] as const,
  acordoDetail: (id: string) => [...saasKeys.acordos(), 'detail', id] as const,
  // Relatórios
  relatorios: () => [...saasKeys.all, 'relatorios'] as const,
  relatoriosSummary: () => [...saasKeys.relatorios(), 'summary'] as const,
  relatorioFinanceiro: (dataInicio: string, dataFim: string) =>
    [...saasKeys.relatorios(), 'financeiro', dataInicio, dataFim] as const,
  relatorioOperacional: (dataInicio: string, dataFim: string) =>
    [...saasKeys.relatorios(), 'operacional', dataInicio, dataFim] as const,
  relatorioCrescimento: (dataInicio: string, dataFim: string) =>
    [...saasKeys.relatorios(), 'crescimento', dataInicio, dataFim] as const,
  // Tickets
  tickets: () => [...saasKeys.all, 'tickets'] as const,
  ticketsList: (filters: TicketFilters) => [...saasKeys.tickets(), 'list', filters] as const,
  ticketDetail: (id: string) => [...saasKeys.tickets(), 'detail', id] as const,
  ticketMetricas: () => [...saasKeys.tickets(), 'metricas'] as const,
  ticketEnums: () => [...saasKeys.tickets(), 'enums'] as const,
  // Super Admins
  superAdmins: () => [...saasKeys.all, 'super-admins'] as const,
  // Comunicados
  comunicados: () => [...saasKeys.all, 'comunicados'] as const,
  comunicadosList: (filters: ComunicadoFilters) => [...saasKeys.comunicados(), 'list', filters] as const,
  comunicadoDetail: (id: string) => [...saasKeys.comunicados(), 'detail', id] as const,
  comunicadoMetricas: () => [...saasKeys.comunicados(), 'metricas'] as const,
  comunicadoEnums: () => [...saasKeys.comunicados(), 'enums'] as const,
};

// ===== DASHBOARD HOOKS =====

export const useDashboardStats = () => {
  return useQuery({
    queryKey: saasKeys.stats(),
    queryFn: () => dashboardService.getStats(),
    staleTime: 30 * 1000, // 30 seconds
    refetchInterval: 60 * 1000, // Refresh every minute
  });
};

export const useMRRBreakdown = () => {
  return useQuery({
    queryKey: saasKeys.mrr(),
    queryFn: () => dashboardService.getMRRBreakdown(),
    staleTime: 60 * 1000,
  });
};

export const useTrialsExpiring = (page = 0, size = 10) => {
  return useQuery({
    queryKey: [...saasKeys.trialsExpiring(), page, size],
    queryFn: () => dashboardService.getTrialsExpiring(page, size),
    staleTime: 5 * 60 * 1000,
  });
};

// ===== ADVANCED METRICS HOOKS =====

export const useDashboardMetrics = () => {
  return useQuery({
    queryKey: saasKeys.metrics(),
    queryFn: () => dashboardService.getMetrics(),
    staleTime: 30 * 1000,
    refetchInterval: 60 * 1000,
  });
};

export const useMRREvolution = (months = 12) => {
  return useQuery({
    queryKey: saasKeys.mrrEvolution(months),
    queryFn: () => dashboardService.getMRREvolution(months),
    staleTime: 5 * 60 * 1000,
  });
};

export const useChurnEvolution = (months = 12) => {
  return useQuery({
    queryKey: saasKeys.churnEvolution(months),
    queryFn: () => dashboardService.getChurnEvolution(months),
    staleTime: 5 * 60 * 1000,
  });
};

export const useSignupsVsCancellations = (months = 12) => {
  return useQuery({
    queryKey: saasKeys.signupsVsCancellations(months),
    queryFn: () => dashboardService.getSignupsVsCancellations(months),
    staleTime: 5 * 60 * 1000,
  });
};

// ===== OFICINAS HOOKS =====

export const useOficinas = (filters: OficinaFilters = {}) => {
  return useQuery({
    queryKey: saasKeys.oficinasList(filters),
    queryFn: () => oficinasService.findAll(filters),
    staleTime: 1 * 60 * 1000,
  });
};

export const useOficinasByStatus = (status: StatusOficina, page = 0, size = 20) => {
  return useQuery({
    queryKey: [...saasKeys.oficinasByStatus(status), page, size],
    queryFn: () => oficinasService.findByStatus(status, page, size),
    staleTime: 1 * 60 * 1000,
  });
};

export const useOficinaDetail = (id?: string) => {
  return useQuery({
    queryKey: saasKeys.oficinaDetail(id || ''),
    queryFn: () => oficinasService.findById(id!),
    enabled: !!id,
    staleTime: 2 * 60 * 1000,
  });
};

export const useCreateOficina = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateOficinaFullRequest) => oficinasService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.oficinas() });
      queryClient.invalidateQueries({ queryKey: saasKeys.stats() });
    },
  });
};

export const useUpdateOficina = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateOficinaFullRequest }) =>
      oficinasService.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.oficinas() });
      queryClient.invalidateQueries({ queryKey: saasKeys.oficinaDetail(id) });
    },
  });
};

export const useOficinaMetricas = (id?: string) => {
  return useQuery({
    queryKey: saasKeys.oficinaMetricas(id || ''),
    queryFn: () => oficinasService.getMetricas(id!),
    enabled: !!id,
    staleTime: 2 * 60 * 1000,
  });
};

export const useImpersonateOficina = () => {
  return useMutation({
    mutationFn: (id: string) => oficinasService.impersonate(id),
  });
};

export const useUpdateLimites = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateLimitesRequest }) =>
      oficinasService.updateLimites(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.oficinaDetail(id) });
      queryClient.invalidateQueries({ queryKey: saasKeys.oficinaMetricas(id) });
    },
  });
};

export const useActivateOficina = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => oficinasService.activate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.oficinas() });
      queryClient.invalidateQueries({ queryKey: saasKeys.stats() });
    },
  });
};

export const useSuspendOficina = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => oficinasService.suspend(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.oficinas() });
      queryClient.invalidateQueries({ queryKey: saasKeys.stats() });
    },
  });
};

export const useCancelOficina = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => oficinasService.cancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.oficinas() });
      queryClient.invalidateQueries({ queryKey: saasKeys.stats() });
    },
  });
};

// ===== PAGAMENTOS HOOKS =====

export const usePagamentos = (filters: PagamentoFilters = {}) => {
  return useQuery({
    queryKey: saasKeys.pagamentosList(filters),
    queryFn: () => pagamentosService.findAll(filters),
    staleTime: 1 * 60 * 1000,
  });
};

export const usePagamentosPendentes = (page = 0, size = 20) => {
  return useQuery({
    queryKey: [...saasKeys.pagamentosPendentes(), page, size],
    queryFn: () => pagamentosService.getPendentes(page, size),
    staleTime: 2 * 60 * 1000,
  });
};

export const usePagamentosInadimplentes = (page = 0, size = 20) => {
  return useQuery({
    queryKey: [...saasKeys.pagamentosInadimplentes(), page, size],
    queryFn: () => pagamentosService.getInadimplentes(page, size),
    staleTime: 2 * 60 * 1000,
  });
};

export const useRegistrarPagamento = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: RegistrarPagamentoRequest) => pagamentosService.registrar(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.pagamentos() });
      queryClient.invalidateQueries({ queryKey: saasKeys.oficinas() });
      queryClient.invalidateQueries({ queryKey: saasKeys.stats() });
    },
  });
};

// ===== AUDIT HOOKS =====

export const useAuditLogs = (filters: AuditFilters = {}) => {
  return useQuery({
    queryKey: saasKeys.auditList(filters),
    queryFn: () => auditService.findAll(filters),
    staleTime: 30 * 1000,
  });
};

export const useExportAudit = () => {
  return useMutation({
    mutationFn: (filters: AuditFilters) => auditService.exportCsv(filters),
  });
};

// ===== JOBS HOOKS =====

export const useRunJob = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (job: 'suspend-overdue' | 'alert-trials' | 'refresh-stats' | 'run-all') => {
      switch (job) {
        case 'suspend-overdue':
          return jobsService.suspendOverdue();
        case 'alert-trials':
          return jobsService.alertTrials();
        case 'refresh-stats':
          return jobsService.refreshStats();
        case 'run-all':
          return jobsService.runAll();
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.all });
    },
  });
};

// ===== PLANOS HOOKS =====

export const usePlanos = () => {
  return useQuery({
    queryKey: saasKeys.planosList(),
    queryFn: () => planosService.findAll(),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};

export const usePlanosActive = () => {
  return useQuery({
    queryKey: saasKeys.planosActive(),
    queryFn: () => planosService.findAllActive(),
    staleTime: 5 * 60 * 1000,
  });
};

export const usePlanosVisible = () => {
  return useQuery({
    queryKey: saasKeys.planosVisible(),
    queryFn: () => planosService.findVisiblePlans(),
    staleTime: 5 * 60 * 1000,
  });
};

export const usePlanoDetail = (id?: string) => {
  return useQuery({
    queryKey: saasKeys.planoDetail(id || ''),
    queryFn: () => planosService.findById(id!),
    enabled: !!id,
    staleTime: 5 * 60 * 1000,
  });
};

export const usePlanoByCodigo = (codigo?: string) => {
  return useQuery({
    queryKey: saasKeys.planoByCodigo(codigo || ''),
    queryFn: () => planosService.findByCodigo(codigo!),
    enabled: !!codigo,
    staleTime: 5 * 60 * 1000,
  });
};

export const usePlanosStatistics = () => {
  return useQuery({
    queryKey: saasKeys.planosStatistics(),
    queryFn: () => planosService.getStatistics(),
    staleTime: 2 * 60 * 1000,
  });
};

export const useCreatePlano = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreatePlanoRequest) => planosService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.planos() });
    },
  });
};

export const useUpdatePlano = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdatePlanoRequest }) =>
      planosService.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.planos() });
      queryClient.invalidateQueries({ queryKey: saasKeys.planoDetail(id) });
    },
  });
};

export const useDeletePlano = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => planosService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.planos() });
    },
  });
};

export const useTogglePlanoVisibility = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => planosService.toggleVisibility(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.planos() });
      queryClient.invalidateQueries({ queryKey: saasKeys.planoDetail(id) });
    },
  });
};

export const useAlterarPlanoOficina = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ oficinaId, data }: { oficinaId: string; data: AlterarPlanoOficinaRequest }) =>
      planosService.alterarPlanoOficina(oficinaId, data),
    onSuccess: (_, { oficinaId }) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.oficinas() });
      queryClient.invalidateQueries({ queryKey: saasKeys.oficinaDetail(oficinaId) });
      queryClient.invalidateQueries({ queryKey: saasKeys.stats() });
    },
  });
};

// ===== FATURAS HOOKS =====

export const useFaturas = (filters: FaturaFilters = {}) => {
  return useQuery({
    queryKey: saasKeys.faturasList(filters),
    queryFn: () => faturasService.findAll(filters),
    staleTime: 30 * 1000,
  });
};

export const useFaturaDetail = (id?: string) => {
  return useQuery({
    queryKey: saasKeys.faturaDetail(id || ''),
    queryFn: () => faturasService.findById(id!),
    enabled: !!id,
    staleTime: 60 * 1000,
  });
};

export const useFaturasByOficina = (oficinaId?: string, page = 0, size = 20) => {
  return useQuery({
    queryKey: [...saasKeys.faturasByOficina(oficinaId || ''), page, size],
    queryFn: () => faturasService.findByOficina(oficinaId!, page, size),
    enabled: !!oficinaId,
    staleTime: 60 * 1000,
  });
};

export const useFaturasSummary = () => {
  return useQuery({
    queryKey: saasKeys.faturasSummary(),
    queryFn: () => faturasService.getSummary(),
    staleTime: 30 * 1000,
    refetchInterval: 60 * 1000,
  });
};

export const useCreateFatura = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateFaturaRequest) => faturasService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.faturas() });
      queryClient.invalidateQueries({ queryKey: saasKeys.stats() });
    },
  });
};

export const useGerarFaturaParaOficina = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ oficinaId, mesReferencia }: { oficinaId: string; mesReferencia?: string }) =>
      faturasService.gerarParaOficina(oficinaId, mesReferencia),
    onSuccess: (_, { oficinaId }) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.faturas() });
      queryClient.invalidateQueries({ queryKey: saasKeys.faturasByOficina(oficinaId) });
    },
  });
};

export const useGerarFaturasMensais = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => faturasService.gerarMensais(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.faturas() });
      queryClient.invalidateQueries({ queryKey: saasKeys.stats() });
    },
  });
};

export const useRegistrarPagamentoFatura = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RegistrarPagamentoFaturaRequest }) =>
      faturasService.registrarPagamento(id, data),
    onSuccess: (fatura) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.faturas() });
      queryClient.invalidateQueries({ queryKey: saasKeys.faturaDetail(fatura.id) });
      queryClient.invalidateQueries({ queryKey: saasKeys.faturasByOficina(fatura.oficinaId) });
      queryClient.invalidateQueries({ queryKey: saasKeys.stats() });
    },
  });
};

export const useCancelarFatura = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, motivo }: { id: string; motivo?: string }) =>
      faturasService.cancelar(id, motivo),
    onSuccess: (fatura) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.faturas() });
      queryClient.invalidateQueries({ queryKey: saasKeys.faturaDetail(fatura.id) });
    },
  });
};

export const useProcessarFaturasVencidas = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => faturasService.processarVencidas(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.faturas() });
    },
  });
};

// ===== INADIMPLÊNCIA HOOKS =====

export const useInadimplenciaDashboard = () => {
  return useQuery({
    queryKey: saasKeys.inadimplenciaDashboard(),
    queryFn: () => inadimplenciaService.getDashboard(),
    staleTime: 30 * 1000,
    refetchInterval: 60 * 1000,
  });
};

export const useInadimplentes = (page = 0, size = 20) => {
  return useQuery({
    queryKey: saasKeys.inadimplentesLista(page, size),
    queryFn: () => inadimplenciaService.listarInadimplentes(page, size),
    staleTime: 30 * 1000,
  });
};

export const useExecutarAcaoMassa = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: AcaoMassaInadimplenciaRequest) => inadimplenciaService.executarAcaoMassa(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.inadimplencia() });
      queryClient.invalidateQueries({ queryKey: saasKeys.oficinas() });
    },
  });
};

// ===== ACORDOS HOOKS =====

export const useAcordos = (filters: AcordoFilters = {}) => {
  return useQuery({
    queryKey: saasKeys.acordosList(filters),
    queryFn: () => inadimplenciaService.listarAcordos(filters),
    staleTime: 30 * 1000,
  });
};

export const useAcordoDetail = (id?: string) => {
  return useQuery({
    queryKey: saasKeys.acordoDetail(id || ''),
    queryFn: () => inadimplenciaService.getAcordo(id!),
    enabled: !!id,
    staleTime: 60 * 1000,
  });
};

export const useCriarAcordo = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ oficinaId, data }: { oficinaId: string; data: CriarAcordoRequest }) =>
      inadimplenciaService.criarAcordo(oficinaId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.inadimplencia() });
      queryClient.invalidateQueries({ queryKey: saasKeys.acordos() });
    },
  });
};

export const useCancelarAcordo = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, motivo }: { id: string; motivo: string }) =>
      inadimplenciaService.cancelarAcordo(id, motivo),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.acordos() });
      queryClient.invalidateQueries({ queryKey: saasKeys.acordoDetail(id) });
      queryClient.invalidateQueries({ queryKey: saasKeys.inadimplencia() });
    },
  });
};

export const useRegistrarPagamentoParcela = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      acordoId,
      parcelaId,
      metodoPagamento,
      transacaoId,
    }: {
      acordoId: string;
      parcelaId: string;
      metodoPagamento: string;
      transacaoId?: string;
    }) => inadimplenciaService.registrarPagamentoParcela(acordoId, parcelaId, metodoPagamento, transacaoId),
    onSuccess: (acordo) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.acordos() });
      queryClient.invalidateQueries({ queryKey: saasKeys.acordoDetail(acordo.id) });
      queryClient.invalidateQueries({ queryKey: saasKeys.inadimplencia() });
    },
  });
};

// ===== RELATÓRIO HOOKS =====

export const useRelatoriosSummary = () => {
  return useQuery({
    queryKey: saasKeys.relatoriosSummary(),
    queryFn: () => relatorioService.getSummary(),
    staleTime: 5 * 60 * 1000,
  });
};

export const useRelatorioFinanceiro = (dataInicio: string, dataFim: string, enabled = true) => {
  return useQuery({
    queryKey: saasKeys.relatorioFinanceiro(dataInicio, dataFim),
    queryFn: () => relatorioService.getRelatorioFinanceiro(dataInicio, dataFim),
    enabled: enabled && !!dataInicio && !!dataFim,
    staleTime: 5 * 60 * 1000,
  });
};

export const useRelatorioOperacional = (dataInicio: string, dataFim: string, enabled = true) => {
  return useQuery({
    queryKey: saasKeys.relatorioOperacional(dataInicio, dataFim),
    queryFn: () => relatorioService.getRelatorioOperacional(dataInicio, dataFim),
    enabled: enabled && !!dataInicio && !!dataFim,
    staleTime: 5 * 60 * 1000,
  });
};

export const useRelatorioCrescimento = (dataInicio: string, dataFim: string, enabled = true) => {
  return useQuery({
    queryKey: saasKeys.relatorioCrescimento(dataInicio, dataFim),
    queryFn: () => relatorioService.getRelatorioCrescimento(dataInicio, dataFim),
    enabled: enabled && !!dataInicio && !!dataFim,
    staleTime: 5 * 60 * 1000,
  });
};

export const useExportarRelatorio = () => {
  return useMutation({
    mutationFn: ({
      tipo,
      dataInicio,
      dataFim,
      formato,
    }: {
      tipo: 'financeiro' | 'operacional' | 'crescimento';
      dataInicio: string;
      dataFim: string;
      formato: FormatoExport;
    }) => relatorioService.exportarRelatorio(tipo, dataInicio, dataFim, formato),
    onSuccess: (blob, variables) => {
      // Download do arquivo
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `relatorio_${variables.tipo}_${variables.dataInicio}_${variables.dataFim}.${variables.formato.toLowerCase()}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    },
  });
};

// ===== TICKETS HOOKS =====

export const useTickets = (filters: TicketFilters = {}) => {
  return useQuery({
    queryKey: saasKeys.ticketsList(filters),
    queryFn: () => ticketService.findAll(filters),
    staleTime: 30 * 1000,
  });
};

export const useTicketDetail = (id?: string) => {
  return useQuery({
    queryKey: saasKeys.ticketDetail(id || ''),
    queryFn: () => ticketService.findById(id!),
    enabled: !!id,
    staleTime: 30 * 1000,
  });
};

export const useTicketMetricas = () => {
  return useQuery({
    queryKey: saasKeys.ticketMetricas(),
    queryFn: () => ticketService.getMetricas(),
    staleTime: 30 * 1000,
    refetchInterval: 60 * 1000,
  });
};

export const useTicketEnums = () => {
  return useQuery({
    queryKey: saasKeys.ticketEnums(),
    queryFn: () => ticketService.getEnums(),
    staleTime: 10 * 60 * 1000, // 10 minutes - static data
  });
};

export const useCreateTicket = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateTicketRequest) => ticketService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.tickets() });
    },
  });
};

export const useResponderTicket = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ResponderTicketRequest }) =>
      ticketService.responder(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.ticketDetail(id) });
      queryClient.invalidateQueries({ queryKey: saasKeys.tickets() });
    },
  });
};

export const useAtribuirTicket = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AtribuirTicketRequest }) =>
      ticketService.atribuir(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.ticketDetail(id) });
      queryClient.invalidateQueries({ queryKey: saasKeys.tickets() });
    },
  });
};

export const useAlterarStatusTicket = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AlterarStatusTicketRequest }) =>
      ticketService.alterarStatus(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.ticketDetail(id) });
      queryClient.invalidateQueries({ queryKey: saasKeys.tickets() });
      queryClient.invalidateQueries({ queryKey: saasKeys.ticketMetricas() });
    },
  });
};

export const useAlterarPrioridadeTicket = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AlterarPrioridadeTicketRequest }) =>
      ticketService.alterarPrioridade(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.ticketDetail(id) });
      queryClient.invalidateQueries({ queryKey: saasKeys.tickets() });
    },
  });
};

// ===== SUPER ADMINS HOOKS =====

export const useSuperAdmins = () => {
  return useQuery({
    queryKey: saasKeys.superAdmins(),
    queryFn: () => superAdminService.findAll(),
    staleTime: 5 * 60 * 1000, // 5 minutes - relatively static data
  });
};

// ===== COMUNICADOS HOOKS =====

export const useComunicados = (filters: ComunicadoFilters = {}) => {
  return useQuery({
    queryKey: saasKeys.comunicadosList(filters),
    queryFn: () => comunicadoService.findAll(filters),
    staleTime: 30 * 1000,
  });
};

export const useComunicadoDetail = (id?: string) => {
  return useQuery({
    queryKey: saasKeys.comunicadoDetail(id || ''),
    queryFn: () => comunicadoService.findById(id!),
    enabled: !!id,
    staleTime: 30 * 1000,
  });
};

export const useComunicadoMetricas = () => {
  return useQuery({
    queryKey: saasKeys.comunicadoMetricas(),
    queryFn: () => comunicadoService.getMetricas(),
    staleTime: 30 * 1000,
    refetchInterval: 60 * 1000,
  });
};

export const useComunicadoEnums = () => {
  return useQuery({
    queryKey: saasKeys.comunicadoEnums(),
    queryFn: () => comunicadoService.getEnums(),
    staleTime: 10 * 60 * 1000, // 10 minutes - static data
  });
};

export const useCreateComunicado = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateComunicadoRequest) => comunicadoService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicados() });
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicadoMetricas() });
    },
  });
};

export const useUpdateComunicado = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateComunicadoRequest }) =>
      comunicadoService.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicadoDetail(id) });
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicados() });
    },
  });
};

export const useEnviarComunicado = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => comunicadoService.enviar(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicadoDetail(id) });
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicados() });
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicadoMetricas() });
    },
  });
};

export const useAgendarComunicado = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, dataAgendamento }: { id: string; dataAgendamento: string }) =>
      comunicadoService.agendar(id, dataAgendamento),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicadoDetail(id) });
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicados() });
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicadoMetricas() });
    },
  });
};

export const useCancelarComunicado = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => comunicadoService.cancelar(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicadoDetail(id) });
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicados() });
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicadoMetricas() });
    },
  });
};

export const useDeleteComunicado = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => comunicadoService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicados() });
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicadoMetricas() });
    },
  });
};

export const useProcessarComunicadosAgendados = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => comunicadoService.processarAgendados(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicados() });
      queryClient.invalidateQueries({ queryKey: saasKeys.comunicadoMetricas() });
    },
  });
};

// ===== FEATURE FLAGS HOOKS =====

import { featureFlagService } from '../services/saasService';
import type {
  CreateFeatureFlagRequest,
  UpdateFeatureFlagRequest,
  ToggleFeatureFlagRequest,
} from '../types';

// Query keys for feature flags
export const featureFlagKeys = {
  all: ['feature-flags'] as const,
  list: () => [...featureFlagKeys.all, 'list'] as const,
  detail: (id: string) => [...featureFlagKeys.all, 'detail', id] as const,
  codigo: (codigo: string) => [...featureFlagKeys.all, 'codigo', codigo] as const,
  categoria: (categoria: string) => [...featureFlagKeys.all, 'categoria', categoria] as const,
  categorias: () => [...featureFlagKeys.all, 'categorias'] as const,
  oficina: (oficinaId: string) => [...featureFlagKeys.all, 'oficina', oficinaId] as const,
  stats: (id: string) => [...featureFlagKeys.all, 'stats', id] as const,
};

export const useFeatureFlags = () => {
  return useQuery({
    queryKey: featureFlagKeys.list(),
    queryFn: () => featureFlagService.findAll(),
    staleTime: 60 * 1000,
  });
};

export const useFeatureFlagDetail = (id?: string) => {
  return useQuery({
    queryKey: featureFlagKeys.detail(id || ''),
    queryFn: () => featureFlagService.findById(id!),
    enabled: !!id,
    staleTime: 60 * 1000,
  });
};

export const useFeatureFlagByCodigo = (codigo?: string) => {
  return useQuery({
    queryKey: featureFlagKeys.codigo(codigo || ''),
    queryFn: () => featureFlagService.findByCodigo(codigo!),
    enabled: !!codigo,
    staleTime: 60 * 1000,
  });
};

export const useFeatureFlagsByCategoria = (categoria?: string) => {
  return useQuery({
    queryKey: featureFlagKeys.categoria(categoria || ''),
    queryFn: () => featureFlagService.findByCategoria(categoria!),
    enabled: !!categoria,
    staleTime: 60 * 1000,
  });
};

export const useFeatureFlagCategorias = () => {
  return useQuery({
    queryKey: featureFlagKeys.categorias(),
    queryFn: () => featureFlagService.getCategorias(),
    staleTime: 5 * 60 * 1000,
  });
};

export const useOficinaFeatureFlags = (oficinaId?: string) => {
  return useQuery({
    queryKey: featureFlagKeys.oficina(oficinaId || ''),
    queryFn: () => featureFlagService.getOficinaFeatures(oficinaId!),
    enabled: !!oficinaId,
    staleTime: 60 * 1000,
  });
};

export const useFeatureFlagStats = (id?: string) => {
  return useQuery({
    queryKey: featureFlagKeys.stats(id || ''),
    queryFn: () => featureFlagService.getStats(id!),
    enabled: !!id,
    staleTime: 60 * 1000,
  });
};

export const useCreateFeatureFlag = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateFeatureFlagRequest) => featureFlagService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: featureFlagKeys.all });
    },
  });
};

export const useUpdateFeatureFlag = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateFeatureFlagRequest }) =>
      featureFlagService.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: featureFlagKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: featureFlagKeys.list() });
    },
  });
};

export const useDeleteFeatureFlag = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => featureFlagService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: featureFlagKeys.all });
    },
  });
};

export const useToggleFeatureFlag = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ToggleFeatureFlagRequest }) =>
      featureFlagService.toggle(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: featureFlagKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: featureFlagKeys.list() });
    },
  });
};

export const useToggleFeatureFlagGlobal = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, habilitado }: { id: string; habilitado: boolean }) =>
      featureFlagService.toggleGlobal(id, habilitado),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: featureFlagKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: featureFlagKeys.list() });
    },
  });
};
