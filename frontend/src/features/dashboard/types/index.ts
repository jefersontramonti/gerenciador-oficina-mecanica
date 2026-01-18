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
 * Estatísticas do dashboard com variação percentual vs mês anterior
 */
export interface DashboardStatsComTrend {
  totalClientes: number;
  totalVeiculos: number;
  osAtivas: number;
  faturamentoMes: number;
  faturamentoMesAnterior: number;
  variacaoFaturamento: number; // +15.5 ou -10.2
  ticketMedio: number;
  ticketMedioAnterior: number;
  variacaoTicketMedio: number;
}

/**
 * Indicadores extras do dashboard
 */
export interface DashboardExtras {
  ticketMedio: number;
  valorTotalEstoque: number;
  estoqueBaixoCount: number;
}

/**
 * Alertas dinâmicos do dashboard
 */
export interface DashboardAlertas {
  pagamentosVencidos: number;
  manutencoesAtrasadas: number;
  pecasCriticas: number;
  planosManutencaoAtivos: number;
}

/**
 * Tipo de alerta para exibição
 */
export type AlertaType = 'warning' | 'danger' | 'info';

/**
 * Alerta individual para exibição na AlertsBar
 */
export interface AlertaItem {
  id: string;
  type: AlertaType;
  icon: string;
  emoji: string;
  message: string;
  subtitle?: string;
  count: number;
  link: string;
  buttonText: string;
}

/**
 * Resumo de pagamentos para widget expansível
 * Nomes devem corresponder ao PagamentosResumoDTO do backend
 */
export interface PagamentosResumo {
  recebidoMes: number;
  pendentesCount: number;
  pendentesValor: number;
  vencidosCount: number;
  vencidosValor: number;
  porTipo?: PagamentoPorTipo[];
  vencidosLista?: PagamentoVencido[];
}

/**
 * Pagamento agrupado por tipo (para gráfico)
 */
export interface PagamentoPorTipo {
  tipo: string;
  label: string;
  valor: number;
  quantidade: number;
}

/**
 * Pagamento vencido individual
 */
export interface PagamentoVencido {
  id: string;
  clienteNome: string;
  valor: number;
  dataVencimento: string;
  diasAtraso: number;
}

/**
 * Resumo de manutenção preventiva para widget
 * Nomes devem corresponder ao ManutencaoResumoDTO do backend
 */
export interface ManutencaoResumo {
  planosAtivos: number;
  alertasPendentes: number;
  planosVencidos: number;
  proximasManutencoes?: ProximaManutencao[];
}

/**
 * Próxima manutenção agendada
 */
export interface ProximaManutencao {
  id: string;
  veiculoPlaca: string;
  veiculoModelo: string;
  clienteNome: string;
  tipoManutencao: string;
  dataPrevisao: string;
  diasRestantes: number;
}

/**
 * Resumo de notas fiscais para widget
 */
export interface NotasFiscaisResumo {
  emitidasMes: number;
  rascunhos: number;
  canceladasMes: number;
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
