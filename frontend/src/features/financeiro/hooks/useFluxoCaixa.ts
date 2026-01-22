import { useQuery } from '@tanstack/react-query';
import { fluxoCaixaService } from '../services/fluxoCaixaService';
import type { FluxoCaixa, DRESimplificado, ProjecaoFinanceira } from '../types/fluxoCaixa';

// Query Keys
export const fluxoCaixaKeys = {
  all: ['fluxoCaixa'] as const,
  periodo: (inicio: string, fim: string) => [...fluxoCaixaKeys.all, 'periodo', inicio, fim] as const,
  mesAtual: () => [...fluxoCaixaKeys.all, 'mesAtual'] as const,
  ultimosDias: (dias: number) => [...fluxoCaixaKeys.all, 'ultimosDias', dias] as const,
};

export const dreKeys = {
  all: ['dre'] as const,
  mensal: (mes: number, ano: number) => [...dreKeys.all, 'mensal', mes, ano] as const,
  mesAtual: () => [...dreKeys.all, 'mesAtual'] as const,
  mesAnterior: () => [...dreKeys.all, 'mesAnterior'] as const,
};

export const projecaoKeys = {
  all: ['projecao'] as const,
  dias: (dias: number) => [...projecaoKeys.all, 'dias', dias] as const,
  semanal: () => [...projecaoKeys.all, 'semanal'] as const,
  mensal: () => [...projecaoKeys.all, 'mensal'] as const,
  trimestral: () => [...projecaoKeys.all, 'trimestral'] as const,
};

// ========== Fluxo de Caixa Hooks ==========

/**
 * Hook para buscar fluxo de caixa de um período.
 */
export function useFluxoCaixa(inicio: string, fim: string, enabled = true) {
  return useQuery<FluxoCaixa, Error>({
    queryKey: fluxoCaixaKeys.periodo(inicio, fim),
    queryFn: () => fluxoCaixaService.getFluxoCaixa(inicio, fim),
    enabled: enabled && !!inicio && !!fim,
    staleTime: 5 * 60 * 1000, // 5 minutos
  });
}

/**
 * Hook para buscar fluxo de caixa do mês atual.
 */
export function useFluxoCaixaMesAtual() {
  return useQuery<FluxoCaixa, Error>({
    queryKey: fluxoCaixaKeys.mesAtual(),
    queryFn: () => fluxoCaixaService.getFluxoCaixaMesAtual(),
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Hook para buscar fluxo de caixa dos últimos N dias.
 */
export function useFluxoCaixaUltimosDias(dias: number = 30, enabled = true) {
  return useQuery<FluxoCaixa, Error>({
    queryKey: fluxoCaixaKeys.ultimosDias(dias),
    queryFn: () => fluxoCaixaService.getFluxoCaixaUltimosDias(dias),
    enabled,
    staleTime: 5 * 60 * 1000,
  });
}

// ========== DRE Hooks ==========

/**
 * Hook para buscar DRE de um mês específico.
 */
export function useDRE(mes: number, ano: number, enabled = true) {
  return useQuery<DRESimplificado, Error>({
    queryKey: dreKeys.mensal(mes, ano),
    queryFn: () => fluxoCaixaService.getDRE(mes, ano),
    enabled,
    staleTime: 10 * 60 * 1000, // 10 minutos
  });
}

/**
 * Hook para buscar DRE do mês atual.
 */
export function useDREMesAtual() {
  return useQuery<DRESimplificado, Error>({
    queryKey: dreKeys.mesAtual(),
    queryFn: () => fluxoCaixaService.getDREMesAtual(),
    staleTime: 10 * 60 * 1000,
  });
}

/**
 * Hook para buscar DRE do mês anterior.
 */
export function useDREMesAnterior() {
  return useQuery<DRESimplificado, Error>({
    queryKey: dreKeys.mesAnterior(),
    queryFn: () => fluxoCaixaService.getDREMesAnterior(),
    staleTime: 10 * 60 * 1000,
  });
}

// ========== Projeção Hooks ==========

/**
 * Hook para buscar projeção financeira.
 */
export function useProjecao(dias: number = 30) {
  return useQuery<ProjecaoFinanceira, Error>({
    queryKey: projecaoKeys.dias(dias),
    queryFn: () => fluxoCaixaService.getProjecao(dias),
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Hook para buscar projeção semanal (7 dias).
 */
export function useProjecaoSemanal() {
  return useQuery<ProjecaoFinanceira, Error>({
    queryKey: projecaoKeys.semanal(),
    queryFn: () => fluxoCaixaService.getProjecaoSemanal(),
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Hook para buscar projeção mensal (30 dias).
 */
export function useProjecaoMensal() {
  return useQuery<ProjecaoFinanceira, Error>({
    queryKey: projecaoKeys.mensal(),
    queryFn: () => fluxoCaixaService.getProjecaoMensal(),
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Hook para buscar projeção trimestral (90 dias).
 */
export function useProjecaoTrimestral() {
  return useQuery<ProjecaoFinanceira, Error>({
    queryKey: projecaoKeys.trimestral(),
    queryFn: () => fluxoCaixaService.getProjecaoTrimestral(),
    staleTime: 5 * 60 * 1000,
  });
}

// ========== Alertas Hooks ==========

/**
 * Hook para contar alertas do DRE do mês atual.
 * Usado para exibir badge no menu.
 * @param enabled - Se false, não faz a chamada à API (útil para SUPER_ADMIN)
 */
export function useContadorAlertasDRE(enabled = true) {
  const hoje = new Date();
  const mes = hoje.getMonth() + 1;
  const ano = hoje.getFullYear();

  const { data: dre } = useDRE(mes, ano, enabled);

  // Retorna a contagem de alertas críticos e de atenção (não conta INFO)
  const contadorCriticos = dre?.alertas?.filter(a => a.nivel === 'CRITICAL').length || 0;
  const contadorWarnings = dre?.alertas?.filter(a => a.nivel === 'WARNING').length || 0;

  return {
    total: contadorCriticos + contadorWarnings,
    criticos: contadorCriticos,
    warnings: contadorWarnings,
    alertas: dre?.alertas || [],
  };
}

/**
 * Hook para contar alertas do Fluxo de Caixa dos últimos 30 dias.
 * Usado para exibir badge no menu.
 * @param enabled - Se false, não faz a chamada à API (útil para SUPER_ADMIN)
 */
export function useContadorAlertasFluxo(enabled = true) {
  const { data: fluxo } = useFluxoCaixaUltimosDias(30, enabled);

  // Conta alertas baseados no fluxo de caixa
  let criticos = 0;
  let warnings = 0;

  if (fluxo) {
    // Crítico: saldo final negativo
    if (fluxo.saldoFinal < 0) {
      criticos++;
    }
    // Atenção: queda significativa de receitas
    if (fluxo.variacaoReceitas < -20) {
      warnings++;
    }
    // Atenção: despesas muito altas
    if (fluxo.totalReceitas > 0) {
      const percentualDespesas = (fluxo.totalDespesas / fluxo.totalReceitas) * 100;
      if (percentualDespesas > 90) {
        warnings++;
      }
    }
  }

  return {
    total: criticos + warnings,
    criticos,
    warnings,
  };
}
