import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-hot-toast';
import type {
  PlanoAssinaturaDTO,
  CreateAssinaturaDTO,
  CancelarAssinaturaDTO,
  RegistrarPagamentoDTO,
  StatusAssinatura,
  StatusFaturaAssinatura,
} from '../types/assinatura';
import * as assinaturaService from '../services/assinaturaService';

// Query Keys
export const assinaturaKeys = {
  all: ['assinaturas'] as const,
  lists: () => [...assinaturaKeys.all, 'list'] as const,
  list: (filters: object) => [...assinaturaKeys.lists(), filters] as const,
  details: () => [...assinaturaKeys.all, 'detail'] as const,
  detail: (id: string) => [...assinaturaKeys.details(), id] as const,

  planos: ['planos-assinatura'] as const,
  planosLists: () => [...assinaturaKeys.planos, 'list'] as const,
  planosAtivos: () => [...assinaturaKeys.planos, 'ativos'] as const,
  planoDetail: (id: string) => [...assinaturaKeys.planos, 'detail', id] as const,

  faturas: ['faturas-assinatura'] as const,
  faturasLists: () => [...assinaturaKeys.faturas, 'list'] as const,
  faturasList: (filters: object) => [...assinaturaKeys.faturasLists(), filters] as const,
  faturaDetail: (id: string) => [...assinaturaKeys.faturas, 'detail', id] as const,
  faturasAssinatura: (assinaturaId: string) => [...assinaturaKeys.faturas, 'assinatura', assinaturaId] as const,
};

// ========== PLANOS HOOKS ==========

export function usePlanos() {
  return useQuery({
    queryKey: assinaturaKeys.planosLists(),
    queryFn: assinaturaService.listarPlanos,
  });
}

export function usePlanosAtivos() {
  return useQuery({
    queryKey: assinaturaKeys.planosAtivos(),
    queryFn: assinaturaService.listarPlanosAtivos,
  });
}

export function usePlano(id: string) {
  return useQuery({
    queryKey: assinaturaKeys.planoDetail(id),
    queryFn: () => assinaturaService.buscarPlano(id),
    enabled: !!id,
  });
}

export function useCriarPlano() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (plano: PlanoAssinaturaDTO) => assinaturaService.criarPlano(plano),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assinaturaKeys.planos });
      toast.success('Plano criado com sucesso!');
    },
    onError: () => {
      toast.error('Erro ao criar plano');
    },
  });
}

export function useAtualizarPlano() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, plano }: { id: string; plano: PlanoAssinaturaDTO }) =>
      assinaturaService.atualizarPlano(id, plano),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assinaturaKeys.planos });
      toast.success('Plano atualizado com sucesso!');
    },
    onError: () => {
      toast.error('Erro ao atualizar plano');
    },
  });
}

export function useDesativarPlano() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => assinaturaService.desativarPlano(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assinaturaKeys.planos });
      toast.success('Plano desativado com sucesso!');
    },
    onError: () => {
      toast.error('Erro ao desativar plano');
    },
  });
}

// ========== ASSINATURAS HOOKS ==========

export function useAssinaturas(params: {
  status?: StatusAssinatura;
  planoId?: string;
  busca?: string;
  page?: number;
  size?: number;
} = {}, enabled = true) {
  return useQuery({
    queryKey: assinaturaKeys.list(params),
    queryFn: () => assinaturaService.listarAssinaturas(params),
    enabled,
  });
}

export function useAssinatura(id: string) {
  return useQuery({
    queryKey: assinaturaKeys.detail(id),
    queryFn: () => assinaturaService.buscarAssinatura(id),
    enabled: !!id,
  });
}

export function useCriarAssinatura() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (dto: CreateAssinaturaDTO) => assinaturaService.criarAssinatura(dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assinaturaKeys.all });
      toast.success('Assinatura criada com sucesso!');
    },
    onError: () => {
      toast.error('Erro ao criar assinatura');
    },
  });
}

export function usePausarAssinatura() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => assinaturaService.pausarAssinatura(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assinaturaKeys.all });
      toast.success('Assinatura pausada com sucesso!');
    },
    onError: () => {
      toast.error('Erro ao pausar assinatura');
    },
  });
}

export function useReativarAssinatura() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => assinaturaService.reativarAssinatura(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assinaturaKeys.all });
      toast.success('Assinatura reativada com sucesso!');
    },
    onError: () => {
      toast.error('Erro ao reativar assinatura');
    },
  });
}

export function useCancelarAssinatura() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, dto }: { id: string; dto: CancelarAssinaturaDTO }) =>
      assinaturaService.cancelarAssinatura(id, dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assinaturaKeys.all });
      toast.success('Assinatura cancelada com sucesso!');
    },
    onError: () => {
      toast.error('Erro ao cancelar assinatura');
    },
  });
}

// ========== FATURAS HOOKS ==========

export function useFaturas(params: {
  status?: StatusFaturaAssinatura;
  assinaturaId?: string;
  page?: number;
  size?: number;
} = {}) {
  return useQuery({
    queryKey: assinaturaKeys.faturasList(params),
    queryFn: () => assinaturaService.listarFaturas(params),
  });
}

export function useFatura(id: string) {
  return useQuery({
    queryKey: assinaturaKeys.faturaDetail(id),
    queryFn: () => assinaturaService.buscarFatura(id),
    enabled: !!id,
  });
}

export function useFaturasAssinatura(assinaturaId: string) {
  return useQuery({
    queryKey: assinaturaKeys.faturasAssinatura(assinaturaId),
    queryFn: () => assinaturaService.listarFaturasAssinatura(assinaturaId),
    enabled: !!assinaturaId,
  });
}

export function useRegistrarPagamento() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ faturaId, dto }: { faturaId: string; dto?: RegistrarPagamentoDTO }) =>
      assinaturaService.registrarPagamento(faturaId, dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assinaturaKeys.faturas });
      queryClient.invalidateQueries({ queryKey: assinaturaKeys.all });
      toast.success('Pagamento registrado com sucesso!');
    },
    onError: () => {
      toast.error('Erro ao registrar pagamento');
    },
  });
}

export function useCancelarFatura() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ faturaId, observacao }: { faturaId: string; observacao?: string }) =>
      assinaturaService.cancelarFatura(faturaId, observacao),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assinaturaKeys.faturas });
      toast.success('Fatura cancelada com sucesso!');
    },
    onError: () => {
      toast.error('Erro ao cancelar fatura');
    },
  });
}

/**
 * Hook para contar alertas de assinaturas.
 * Usado para exibir badge no menu.
 * @param enabled - Se false, não faz a chamada à API (útil para SUPER_ADMIN)
 */
export function useContadorAlertasAssinaturas(enabled = true) {
  // Buscar todas as assinaturas sem filtro de status
  const { data: assinaturasPage } = useAssinaturas({ page: 0, size: 100 }, enabled);

  const assinaturas = assinaturasPage?.content || [];

  // Conta alertas baseados nas assinaturas
  let criticos = 0;
  let warnings = 0;

  if (assinaturas.length > 0) {
    // Crítico: assinaturas inadimplentes
    const inadimplentes = assinaturas.filter((a) => a.status === 'INADIMPLENTE');
    if (inadimplentes.length > 0) {
      criticos++;
    }

    // Atenção: faturas vencidas
    const totalFaturasVencidas = assinaturas.reduce((acc, a) => acc + (a.faturasVencidas || 0), 0);
    if (totalFaturasVencidas > 0) {
      warnings++;
    }
  }

  return {
    total: criticos + warnings,
    criticos,
    warnings,
  };
}
