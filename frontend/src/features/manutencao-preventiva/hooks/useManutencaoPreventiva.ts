import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { planoService, templateService, agendamentoService, dashboardService } from '../services/manutencaoService';
import type {
  PlanoFilters,
  PlanoManutencaoRequest,
  ExecutarPlanoRequest,
  AplicarTemplateRequest,
  TemplateFilters,
  TemplateManutencaoRequest,
  AgendamentoFilters,
  AgendamentoManutencaoRequest,
  RemarcarAgendamentoRequest,
} from '../types';

// ==================== QUERY KEYS ====================

export const manutencaoKeys = {
  all: ['manutencao-preventiva'] as const,
  planos: () => [...manutencaoKeys.all, 'planos'] as const,
  planosList: (filters: PlanoFilters) => [...manutencaoKeys.planos(), 'list', filters] as const,
  planoDetail: (id: string) => [...manutencaoKeys.planos(), 'detail', id] as const,
  planosVeiculo: (veiculoId: string) => [...manutencaoKeys.planos(), 'veiculo', veiculoId] as const,
  planosVencidos: () => [...manutencaoKeys.planos(), 'vencidos'] as const,
  planosProximos: (dias: number) => [...manutencaoKeys.planos(), 'proximos', dias] as const,

  templates: () => [...manutencaoKeys.all, 'templates'] as const,
  templatesList: (filters: TemplateFilters) => [...manutencaoKeys.templates(), 'list', filters] as const,
  templatesDisponiveis: () => [...manutencaoKeys.templates(), 'disponiveis'] as const,
  tiposManutencao: () => [...manutencaoKeys.templates(), 'tipos'] as const,
  templateDetail: (id: string) => [...manutencaoKeys.templates(), 'detail', id] as const,

  agendamentos: () => [...manutencaoKeys.all, 'agendamentos'] as const,
  agendamentosList: (filters: AgendamentoFilters) => [...manutencaoKeys.agendamentos(), 'list', filters] as const,
  agendamentoDetail: (id: string) => [...manutencaoKeys.agendamentos(), 'detail', id] as const,
  agendamentosHoje: () => [...manutencaoKeys.agendamentos(), 'hoje'] as const,
  agendamentosProximos: (limite: number) => [...manutencaoKeys.agendamentos(), 'proximos', limite] as const,
  calendario: (mes: number, ano: number) => [...manutencaoKeys.agendamentos(), 'calendario', mes, ano] as const,

  dashboard: () => [...manutencaoKeys.all, 'dashboard'] as const,
  estatisticas: () => [...manutencaoKeys.all, 'estatisticas'] as const,
};

// ==================== PLANOS HOOKS ====================

export function usePlanos(filters: PlanoFilters = {}) {
  return useQuery({
    queryKey: manutencaoKeys.planosList(filters),
    queryFn: () => planoService.listar(filters),
    staleTime: 2 * 60 * 1000,
  });
}

export function usePlano(id?: string) {
  return useQuery({
    queryKey: manutencaoKeys.planoDetail(id || ''),
    queryFn: () => planoService.buscarPorId(id!),
    enabled: !!id,
    staleTime: 2 * 60 * 1000,
  });
}

export function usePlanosPorVeiculo(veiculoId?: string) {
  return useQuery({
    queryKey: manutencaoKeys.planosVeiculo(veiculoId || ''),
    queryFn: () => planoService.listarPorVeiculo(veiculoId!),
    enabled: !!veiculoId,
    staleTime: 2 * 60 * 1000,
  });
}

export function usePlanosVencidos() {
  return useQuery({
    queryKey: manutencaoKeys.planosVencidos(),
    queryFn: () => planoService.listarVencidos(),
    staleTime: 2 * 60 * 1000,
  });
}

export function usePlanosProximosAVencer(dias = 30) {
  return useQuery({
    queryKey: manutencaoKeys.planosProximos(dias),
    queryFn: () => planoService.listarProximosAVencer(dias),
    staleTime: 2 * 60 * 1000,
  });
}

export function useCriarPlano() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: PlanoManutencaoRequest) => planoService.criar(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.planos() });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.dashboard() });
    },
  });
}

export function useAtualizarPlano() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: PlanoManutencaoRequest }) =>
      planoService.atualizar(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.planoDetail(id) });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.planos() });
    },
  });
}

export function useAtivarPlano() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => planoService.ativar(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.planoDetail(id) });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.planos() });
    },
  });
}

export function usePausarPlano() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, motivo }: { id: string; motivo?: string }) =>
      planoService.pausar(id, motivo),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.planoDetail(id) });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.planos() });
    },
  });
}

export function useConcluirPlano() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => planoService.concluir(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.planoDetail(id) });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.planos() });
    },
  });
}

export function useExecutarPlano() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ExecutarPlanoRequest }) =>
      planoService.executar(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.planoDetail(id) });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.planos() });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.dashboard() });
    },
  });
}

export function useDeletarPlano() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => planoService.deletar(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.planos() });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.dashboard() });
    },
  });
}

// ==================== TEMPLATES HOOKS ====================

export function useTemplates(filters: TemplateFilters = {}) {
  return useQuery({
    queryKey: manutencaoKeys.templatesList(filters),
    queryFn: () => templateService.listar(filters),
    staleTime: 5 * 60 * 1000,
  });
}

export function useTemplatesDisponiveis() {
  return useQuery({
    queryKey: manutencaoKeys.templatesDisponiveis(),
    queryFn: () => templateService.listarDisponiveis(),
    staleTime: 5 * 60 * 1000,
  });
}

export function useTiposManutencao() {
  return useQuery({
    queryKey: manutencaoKeys.tiposManutencao(),
    queryFn: () => templateService.listarTiposManutencao(),
    staleTime: 10 * 60 * 1000,
  });
}

export function useTemplate(id?: string) {
  return useQuery({
    queryKey: manutencaoKeys.templateDetail(id || ''),
    queryFn: () => templateService.buscarPorId(id!),
    enabled: !!id,
    staleTime: 5 * 60 * 1000,
  });
}

export function useCriarTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: TemplateManutencaoRequest) => templateService.criar(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.templates() });
    },
  });
}

export function useAtualizarTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: TemplateManutencaoRequest }) =>
      templateService.atualizar(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.templateDetail(id) });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.templates() });
    },
  });
}

export function useDeletarTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => templateService.deletar(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.templates() });
    },
  });
}

export function useAplicarTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ templateId, data }: { templateId: string; data: AplicarTemplateRequest }) =>
      templateService.aplicar(templateId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.planos() });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.dashboard() });
    },
  });
}

// ==================== AGENDAMENTOS HOOKS ====================

export function useAgendamentos(filters: AgendamentoFilters = {}) {
  return useQuery({
    queryKey: manutencaoKeys.agendamentosList(filters),
    queryFn: () => agendamentoService.listar(filters),
    staleTime: 1 * 60 * 1000,
  });
}

export function useAgendamento(id?: string) {
  return useQuery({
    queryKey: manutencaoKeys.agendamentoDetail(id || ''),
    queryFn: () => agendamentoService.buscarPorId(id!),
    enabled: !!id,
    staleTime: 1 * 60 * 1000,
  });
}

export function useAgendamentosHoje() {
  return useQuery({
    queryKey: manutencaoKeys.agendamentosHoje(),
    queryFn: () => agendamentoService.listarHoje(),
    staleTime: 1 * 60 * 1000,
  });
}

export function useAgendamentosProximos(limite = 10) {
  return useQuery({
    queryKey: manutencaoKeys.agendamentosProximos(limite),
    queryFn: () => agendamentoService.listarProximos(limite),
    staleTime: 1 * 60 * 1000,
  });
}

export function useCalendario(mes: number, ano: number) {
  return useQuery({
    queryKey: manutencaoKeys.calendario(mes, ano),
    queryFn: () => agendamentoService.listarCalendario(mes, ano),
    staleTime: 1 * 60 * 1000,
  });
}

export function useCriarAgendamento() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: AgendamentoManutencaoRequest) => agendamentoService.criar(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.agendamentos() });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.dashboard() });
    },
  });
}

export function useAtualizarAgendamento() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AgendamentoManutencaoRequest }) =>
      agendamentoService.atualizar(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.agendamentoDetail(id) });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.agendamentos() });
    },
  });
}

export function useConfirmarAgendamento() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => agendamentoService.confirmar(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.agendamentoDetail(id) });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.agendamentos() });
    },
  });
}

export function useRemarcarAgendamento() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RemarcarAgendamentoRequest }) =>
      agendamentoService.remarcar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.agendamentos() });
    },
  });
}

export function useCancelarAgendamento() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, motivo }: { id: string; motivo?: string }) =>
      agendamentoService.cancelar(id, motivo),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.agendamentoDetail(id) });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.agendamentos() });
    },
  });
}

export function useDeletarAgendamento() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => agendamentoService.deletar(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.agendamentos() });
      queryClient.invalidateQueries({ queryKey: manutencaoKeys.dashboard() });
    },
  });
}

// ==================== DASHBOARD HOOKS ====================

export function useDashboardManutencao() {
  return useQuery({
    queryKey: manutencaoKeys.dashboard(),
    queryFn: () => dashboardService.getDashboard(),
    staleTime: 1 * 60 * 1000,
  });
}

export function useEstatisticasManutencao() {
  return useQuery({
    queryKey: manutencaoKeys.estatisticas(),
    queryFn: () => dashboardService.getEstatisticas(),
    staleTime: 1 * 60 * 1000,
  });
}
