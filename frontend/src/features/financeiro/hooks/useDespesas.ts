import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import despesaService from '../services/despesaService';
import type {
  DespesaCreateRequest,
  DespesaUpdateRequest,
  DespesaPagamentoRequest,
  DespesaFiltros,
} from '../types/despesa';

// Query keys
export const despesaKeys = {
  all: ['despesas'] as const,
  lists: () => [...despesaKeys.all, 'list'] as const,
  list: (filtros: DespesaFiltros) => [...despesaKeys.lists(), filtros] as const,
  details: () => [...despesaKeys.all, 'detail'] as const,
  detail: (id: string) => [...despesaKeys.details(), id] as const,
  vencidas: () => [...despesaKeys.all, 'vencidas'] as const,
  aVencer: (dias: number) => [...despesaKeys.all, 'a-vencer', dias] as const,
  resumo: () => [...despesaKeys.all, 'resumo'] as const,
  categorias: () => [...despesaKeys.all, 'categorias'] as const,
};

/**
 * Hook para listar despesas com filtros
 */
export function useDespesas(filtros: DespesaFiltros = {}) {
  return useQuery({
    queryKey: despesaKeys.list(filtros),
    queryFn: () => despesaService.listar(filtros),
  });
}

/**
 * Hook para buscar despesa por ID
 */
export function useDespesa(id: string) {
  return useQuery({
    queryKey: despesaKeys.detail(id),
    queryFn: () => despesaService.buscarPorId(id),
    enabled: !!id,
  });
}

/**
 * Hook para listar despesas vencidas
 */
export function useDespesasVencidas() {
  return useQuery({
    queryKey: despesaKeys.vencidas(),
    queryFn: () => despesaService.listarVencidas(),
  });
}

/**
 * Hook para listar despesas a vencer
 */
export function useDespesasAVencer(dias: number = 7) {
  return useQuery({
    queryKey: despesaKeys.aVencer(dias),
    queryFn: () => despesaService.listarAVencer(dias),
  });
}

/**
 * Hook para buscar resumo das despesas
 * @param enabled - Se false, não faz a chamada à API
 */
export function useDespesasResumo(enabled = true) {
  return useQuery({
    queryKey: despesaKeys.resumo(),
    queryFn: () => despesaService.getResumo(),
    enabled,
  });
}

/**
 * Hook para listar categorias
 */
export function useCategoriasDespesa() {
  return useQuery({
    queryKey: despesaKeys.categorias(),
    queryFn: () => despesaService.listarCategorias(),
    staleTime: 1000 * 60 * 60, // 1 hora (categorias raramente mudam)
  });
}

/**
 * Hook para criar despesa
 */
export function useCreateDespesa() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: DespesaCreateRequest) => despesaService.criar(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: despesaKeys.all });
      toast.success('Despesa criada com sucesso!');
    },
    onError: (error: Error) => {
      toast.error(`Erro ao criar despesa: ${error.message}`);
    },
  });
}

/**
 * Hook para atualizar despesa
 */
export function useUpdateDespesa() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: DespesaUpdateRequest }) =>
      despesaService.atualizar(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: despesaKeys.all });
      queryClient.invalidateQueries({ queryKey: despesaKeys.detail(id) });
      toast.success('Despesa atualizada com sucesso!');
    },
    onError: (error: Error) => {
      toast.error(`Erro ao atualizar despesa: ${error.message}`);
    },
  });
}

/**
 * Hook para excluir despesa
 */
export function useDeleteDespesa() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => despesaService.excluir(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: despesaKeys.all });
      toast.success('Despesa excluída com sucesso!');
    },
    onError: (error: Error) => {
      toast.error(`Erro ao excluir despesa: ${error.message}`);
    },
  });
}

/**
 * Hook para pagar despesa
 */
export function usePagarDespesa() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: DespesaPagamentoRequest }) =>
      despesaService.pagar(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: despesaKeys.all });
      queryClient.invalidateQueries({ queryKey: despesaKeys.detail(id) });
      toast.success('Pagamento registrado com sucesso!');
    },
    onError: (error: Error) => {
      toast.error(`Erro ao registrar pagamento: ${error.message}`);
    },
  });
}

/**
 * Hook para cancelar despesa
 */
export function useCancelarDespesa() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => despesaService.cancelar(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: despesaKeys.all });
      queryClient.invalidateQueries({ queryKey: despesaKeys.detail(id) });
      toast.success('Despesa cancelada com sucesso!');
    },
    onError: (error: Error) => {
      toast.error(`Erro ao cancelar despesa: ${error.message}`);
    },
  });
}

/**
 * Hook para contar alertas de despesas.
 * Usado para exibir badge no menu.
 * @param enabled - Se false, não faz a chamada à API (útil para SUPER_ADMIN)
 */
export function useContadorAlertasDespesas(enabled = true) {
  const { data: resumo } = useDespesasResumo(enabled);

  // Conta alertas baseados no resumo
  let criticos = 0;
  let warnings = 0;

  if (resumo) {
    // Crítico: despesas vencidas
    if (resumo.quantidadeVencida > 0) {
      criticos++;
    }
    // Atenção: muitas despesas a vencer ou total pendente alto
    if (resumo.quantidadeAVencer7Dias >= 5) {
      warnings++;
    }
    if (resumo.totalPendente > 10000 && resumo.quantidadePendente > 10) {
      warnings++;
    }
  }

  return {
    total: criticos + warnings,
    criticos,
    warnings,
  };
}
