/**
 * Serviço para chamadas à API do Dashboard
 */

import { api } from '@/shared/services/api';
import type {
  DashboardStats,
  DashboardStatsComTrend,
  DashboardExtras,
  DashboardAlertas,
  OSStatusCount,
  FaturamentoMensal,
  RecentOS,
  PagamentosResumo,
  PagamentoPorTipo,
  ManutencaoResumo,
  ProximaManutencao,
  NotasFiscaisResumo,
} from '../types';
import { StatusOS } from '@/features/ordens-servico/types';

// Flag para usar mock data (quando endpoints não estiverem prontos)
const USE_MOCK_DATA = false;

/**
 * Gera mock data para estatísticas
 */
const mockStats: DashboardStats = {
  totalClientes: 45,
  totalVeiculos: 67,
  osAtivas: 12,
  faturamentoMes: 28750.5,
};

/**
 * Gera mock data para contagem de OS por status
 */
const mockOSByStatus: OSStatusCount[] = [
  {
    status: StatusOS.ORCAMENTO,
    count: 5,
    label: 'Orçamento',
    color: '#3b82f6', // Azul
  },
  {
    status: StatusOS.APROVADO,
    count: 2,
    label: 'Aprovado',
    color: '#8b5cf6', // Roxo
  },
  {
    status: StatusOS.EM_ANDAMENTO,
    count: 7,
    label: 'Em Andamento',
    color: '#f59e0b', // Laranja/Amarelo
  },
  {
    status: StatusOS.AGUARDANDO_PECA,
    count: 3,
    label: 'Aguardando Peça',
    color: '#ec4899', // Rosa/Pink
  },
  {
    status: StatusOS.FINALIZADO,
    count: 15,
    label: 'Finalizado',
    color: '#10b981', // Verde
  },
  {
    status: StatusOS.ENTREGUE,
    count: 23,
    label: 'Entregue',
    color: '#06b6d4', // Ciano/Azul claro
  },
  {
    status: StatusOS.CANCELADO,
    count: 4,
    label: 'Cancelado',
    color: '#ef4444', // Vermelho
  },
];

/**
 * Gera mock data para faturamento mensal
 */
const mockFaturamentoMensal: FaturamentoMensal[] = [
  { mes: 'Mai/2024', valor: 12500.0 },
  { mes: 'Jun/2024', valor: 15000.0 },
  { mes: 'Jul/2024', valor: 18750.0 },
  { mes: 'Ago/2024', valor: 22300.0 },
  { mes: 'Set/2024', valor: 19500.0 },
  { mes: 'Out/2024', valor: 28750.5 },
];

/**
 * Gera mock data para OS recentes
 */
const mockRecentOS: RecentOS[] = [
  {
    id: '1',
    numero: 1245,
    status: StatusOS.EM_ANDAMENTO,
    clienteNome: 'João da Silva',
    veiculoPlaca: 'ABC-1234',
    dataAbertura: '2024-10-28T10:30:00',
    valorFinal: 1250.0,
  },
  {
    id: '2',
    numero: 1244,
    status: StatusOS.APROVADO,
    clienteNome: 'Maria Santos',
    veiculoPlaca: 'XYZ-5678',
    dataAbertura: '2024-10-28T09:15:00',
    valorFinal: 850.0,
  },
  {
    id: '3',
    numero: 1243,
    status: StatusOS.AGUARDANDO_PECA,
    clienteNome: 'Carlos Oliveira',
    veiculoPlaca: 'DEF-9012',
    dataAbertura: '2024-10-27T14:20:00',
    valorFinal: 2100.0,
  },
  {
    id: '4',
    numero: 1242,
    status: StatusOS.ORCAMENTO,
    clienteNome: 'Ana Paula Costa',
    veiculoPlaca: 'GHI-3456',
    dataAbertura: '2024-10-27T11:00:00',
    valorFinal: 450.0,
  },
  {
    id: '5',
    numero: 1241,
    status: StatusOS.FINALIZADO,
    clienteNome: 'Roberto Almeida',
    veiculoPlaca: 'JKL-7890',
    dataAbertura: '2024-10-26T16:45:00',
    valorFinal: 3200.0,
  },
  {
    id: '6',
    numero: 1240,
    status: StatusOS.EM_ANDAMENTO,
    clienteNome: 'Patricia Lima',
    veiculoPlaca: 'MNO-2468',
    dataAbertura: '2024-10-26T13:30:00',
    valorFinal: 1800.0,
  },
  {
    id: '7',
    numero: 1239,
    status: StatusOS.APROVADO,
    clienteNome: 'Fernando Souza',
    veiculoPlaca: 'PQR-1357',
    dataAbertura: '2024-10-25T10:00:00',
    valorFinal: 950.0,
  },
  {
    id: '8',
    numero: 1238,
    status: StatusOS.ENTREGUE,
    clienteNome: 'Juliana Rocha',
    veiculoPlaca: 'STU-9753',
    dataAbertura: '2024-10-24T15:20:00',
    valorFinal: 1500.0,
  },
  {
    id: '9',
    numero: 1237,
    status: StatusOS.FINALIZADO,
    clienteNome: 'Ricardo Pereira',
    veiculoPlaca: 'VWX-8642',
    dataAbertura: '2024-10-24T09:00:00',
    valorFinal: 2750.0,
  },
  {
    id: '10',
    numero: 1236,
    status: StatusOS.CANCELADO,
    clienteNome: 'Beatriz Martins',
    veiculoPlaca: 'YZA-4321',
    dataAbertura: '2024-10-23T11:30:00',
    valorFinal: 0,
  },
];

/**
 * Simula delay de rede (para testes de loading state)
 */
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export const dashboardService = {
  /**
   * Busca estatísticas gerais do dashboard
   */
  async getStats(): Promise<DashboardStats> {
    if (USE_MOCK_DATA) {
      await delay(500);
      return mockStats;
    }

    const { data } = await api.get<DashboardStats>('/dashboard/stats');
    return data;
  },

  /**
   * Busca contagem de OS por status
   */
  async getOSByStatus(): Promise<OSStatusCount[]> {
    if (USE_MOCK_DATA) {
      await delay(600);
      return mockOSByStatus;
    }

    const { data } = await api.get<OSStatusCount[]>('/dashboard/os-por-status');
    return data;
  },

  /**
   * Busca faturamento mensal (últimos 6 meses)
   */
  async getFaturamentoMensal(): Promise<FaturamentoMensal[]> {
    if (USE_MOCK_DATA) {
      await delay(700);
      return mockFaturamentoMensal;
    }

    const { data } = await api.get<FaturamentoMensal[]>(
      '/dashboard/faturamento-mensal',
      {
        params: { meses: 6 },
      }
    );
    return data;
  },

  /**
   * Busca OS recentes (últimas 10)
   */
  async getRecentOS(): Promise<RecentOS[]> {
    if (USE_MOCK_DATA) {
      await delay(550);
      return mockRecentOS;
    }

    const { data } = await api.get<RecentOS[]>('/dashboard/os-recentes', {
      params: { limit: 10 },
    });
    return data;
  },

  /**
   * Busca indicadores extras do dashboard (ticket médio, valor estoque, estoque baixo)
   */
  async getExtras(): Promise<DashboardExtras> {
    if (USE_MOCK_DATA) {
      await delay(600);
      return {
        ticketMedio: 1580.50,
        valorTotalEstoque: 45750.00,
        estoqueBaixoCount: 8,
      };
    }

    // Buscar em paralelo para melhor performance
    const now = new Date();
    const firstDayOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDayOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);

    const [ticketMedioRes, valorEstoqueRes, estoqueBaixoRes] = await Promise.all([
      api.get<{ ticketMedio: number }>('/ordens-servico/dashboard/ticket-medio', {
        params: {
          dataInicio: firstDayOfMonth.toISOString(),
          dataFim: lastDayOfMonth.toISOString(),
        },
      }).catch(() => ({ data: { ticketMedio: 0 } })),
      api.get<number>('/estoque/relatorios/valor-total').catch(() => ({ data: 0 })),
      api.get<number>('/estoque/dashboard/estoque-baixo').catch(() => ({ data: 0 })),
    ]);

    return {
      ticketMedio: ticketMedioRes.data.ticketMedio || 0,
      valorTotalEstoque: valorEstoqueRes.data || 0,
      estoqueBaixoCount: estoqueBaixoRes.data || 0,
    };
  },

  /**
   * Busca estatísticas com variação percentual vs mês anterior
   */
  async getStatsComTrend(): Promise<DashboardStatsComTrend> {
    const { data } = await api.get<DashboardStatsComTrend>('/dashboard/stats-trend');
    return data;
  },

  /**
   * Busca alertas dinâmicos do dashboard
   */
  async getAlertas(): Promise<DashboardAlertas> {
    const { data } = await api.get<DashboardAlertas>('/dashboard/alertas');
    return data;
  },

  /**
   * Busca resumo de pagamentos para widget expansível
   */
  async getPagamentosResumo(): Promise<PagamentosResumo> {
    const { data } = await api.get<PagamentosResumo>('/dashboard/pagamentos-resumo');
    return data;
  },

  /**
   * Busca pagamentos agrupados por tipo para gráfico
   */
  async getPagamentosPorTipo(): Promise<PagamentoPorTipo[]> {
    const { data } = await api.get<PagamentoPorTipo[]>('/dashboard/pagamentos-por-tipo');
    return data;
  },

  /**
   * Busca resumo de manutenção preventiva para widget
   */
  async getManutencaoResumo(): Promise<ManutencaoResumo> {
    const { data } = await api.get<ManutencaoResumo>('/dashboard/manutencao-resumo');
    return data;
  },

  /**
   * Busca lista das próximas manutenções
   * @param dias Dias à frente para buscar (padrão 7)
   * @param limite Quantidade máxima de resultados (padrão 5)
   */
  async getProximasManutencoes(dias = 7, limite = 5): Promise<ProximaManutencao[]> {
    const { data } = await api.get<ProximaManutencao[]>('/dashboard/proximas-manutencoes', {
      params: { dias, limite },
    });
    return data;
  },

  /**
   * Busca resumo de notas fiscais para widget
   */
  async getNotasFiscaisResumo(): Promise<NotasFiscaisResumo> {
    const { data } = await api.get<NotasFiscaisResumo>('/dashboard/notas-fiscais-resumo');
    return data;
  },
};
