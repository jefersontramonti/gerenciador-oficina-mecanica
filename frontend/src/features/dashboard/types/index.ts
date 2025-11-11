/**
 * Tipos e interfaces para o módulo de Dashboard
 */

import { StatusOS } from '@/features/ordens-servico/types';

/**
 * Estatísticas gerais do dashboard
 */
export interface DashboardStats {
  totalClientes: number;
  totalVeiculos: number;
  osAtivas: number;
  faturamentoMes: number;
}

/**
 * Contagem de OS por status
 */
export interface OSStatusCount {
  status: StatusOS;
  count: number;
  label: string;
  color: string;
}

/**
 * Faturamento mensal (para gráfico)
 */
export interface FaturamentoMensal {
  mes: string;      // "2024-10" ou "Out/2024"
  valor: number;
}

/**
 * OS recente para tabela de resumo
 */
export interface RecentOS {
  id: string;
  numero: number;
  status: StatusOS;
  clienteNome: string;
  veiculoPlaca: string;
  dataAbertura: string;
  valorFinal?: number;
}
