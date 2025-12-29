import { useState } from 'react';
import {
  FileText,
  DollarSign,
  Activity,
  TrendingUp,
  Download,
  Calendar,
  RefreshCw,
  FileSpreadsheet,
  FileDown,
} from 'lucide-react';
import {
  useRelatoriosSummary,
  useRelatorioFinanceiro,
  useRelatorioOperacional,
  useRelatorioCrescimento,
  useExportarRelatorio,
} from '../hooks/useSaas';
import type { TipoRelatorio, FormatoExport } from '../types';

type TabType = 'overview' | 'financeiro' | 'operacional' | 'crescimento';
type ReportType = 'financeiro' | 'operacional' | 'crescimento';

export function RelatoriosPage() {
  const [activeTab, setActiveTab] = useState<TabType>('overview');
  const [dataInicio, setDataInicio] = useState(() => {
    const date = new Date();
    date.setMonth(date.getMonth() - 1);
    return date.toISOString().split('T')[0];
  });
  const [dataFim, setDataFim] = useState(() => new Date().toISOString().split('T')[0]);

  useRelatoriosSummary(); // Fetch summary for future use
  const { data: relatorioFinanceiro, isLoading: loadingFinanceiro, refetch: refetchFinanceiro } =
    useRelatorioFinanceiro(dataInicio, dataFim, activeTab === 'financeiro');
  const { data: relatorioOperacional, isLoading: loadingOperacional, refetch: refetchOperacional } =
    useRelatorioOperacional(dataInicio, dataFim, activeTab === 'operacional');
  const { data: relatorioCrescimento, isLoading: loadingCrescimento, refetch: refetchCrescimento } =
    useRelatorioCrescimento(dataInicio, dataFim, activeTab === 'crescimento');

  const exportarRelatorio = useExportarRelatorio();

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  };

  const formatPercent = (value: number) => {
    return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`;
  };

  const handleExport = (tipo: 'financeiro' | 'operacional' | 'crescimento', formato: FormatoExport) => {
    exportarRelatorio.mutate({ tipo, dataInicio, dataFim, formato });
  };

  const reportCards: Array<{
    tipo: TipoRelatorio;
    tab: ReportType;
    nome: string;
    descricao: string;
    icon: typeof DollarSign;
    color: string;
  }> = [
    {
      tipo: 'FINANCEIRO',
      tab: 'financeiro',
      nome: 'Financeiro',
      descricao: 'Receitas, faturas, inadimplência e evolução financeira',
      icon: DollarSign,
      color: 'green',
    },
    {
      tipo: 'OPERACIONAL',
      tab: 'operacional',
      nome: 'Operacional',
      descricao: 'Oficinas, usuários, uso do sistema e métricas',
      icon: Activity,
      color: 'blue',
    },
    {
      tipo: 'CRESCIMENTO',
      tab: 'crescimento',
      nome: 'Crescimento',
      descricao: 'Aquisição, churn, retenção e evolução do MRR',
      icon: TrendingUp,
      color: 'purple',
    },
  ];

  const tabs = [
    { id: 'overview' as TabType, label: 'Visão Geral', icon: FileText },
    { id: 'financeiro' as TabType, label: 'Financeiro', icon: DollarSign },
    { id: 'operacional' as TabType, label: 'Operacional', icon: Activity },
    { id: 'crescimento' as TabType, label: 'Crescimento', icon: TrendingUp },
  ];

  return (
    <div className="min-h-screen bg-gray-50 p-6 dark:bg-gray-900">
      <div className="mx-auto max-w-7xl">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Central de Relatórios</h1>
          <p className="mt-1 text-gray-600 dark:text-gray-400">
            Gere e exporte relatórios detalhados do seu SaaS
          </p>
        </div>

        {/* Period Selector */}
        <div className="mb-6 flex flex-wrap items-center gap-4 rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex items-center gap-2">
            <Calendar className="h-5 w-5 text-gray-500 dark:text-gray-400" />
            <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Período:</span>
          </div>
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={dataInicio}
              onChange={(e) => setDataInicio(e.target.value)}
              className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            />
            <span className="text-gray-500">até</span>
            <input
              type="date"
              value={dataFim}
              onChange={(e) => setDataFim(e.target.value)}
              className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            />
          </div>
        </div>

        {/* Tabs */}
        <div className="mb-6 border-b border-gray-200 dark:border-gray-700">
          <nav className="flex space-x-8">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 border-b-2 px-1 py-4 text-sm font-medium transition-colors ${
                  activeTab === tab.id
                    ? 'border-blue-500 text-blue-600 dark:border-blue-400 dark:text-blue-400'
                    : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300'
                }`}
              >
                <tab.icon className="h-4 w-4" />
                {tab.label}
              </button>
            ))}
          </nav>
        </div>

        {/* Content */}
        {activeTab === 'overview' && (
          <div className="grid gap-6 md:grid-cols-3">
            {reportCards.map((card) => (
              <div
                key={card.tipo}
                className="rounded-lg bg-white p-6 shadow transition-shadow hover:shadow-lg dark:bg-gray-800"
              >
                <div className="mb-4 flex items-center gap-3">
                  <div
                    className={`rounded-lg p-3 ${
                      card.color === 'green'
                        ? 'bg-green-100 dark:bg-green-900/30'
                        : card.color === 'blue'
                        ? 'bg-blue-100 dark:bg-blue-900/30'
                        : 'bg-purple-100 dark:bg-purple-900/30'
                    }`}
                  >
                    <card.icon
                      className={`h-6 w-6 ${
                        card.color === 'green'
                          ? 'text-green-600 dark:text-green-400'
                          : card.color === 'blue'
                          ? 'text-blue-600 dark:text-blue-400'
                          : 'text-purple-600 dark:text-purple-400'
                      }`}
                    />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 dark:text-white">{card.nome}</h3>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{card.descricao}</p>
                  </div>
                </div>

                <div className="flex gap-2">
                  <button
                    onClick={() => setActiveTab(card.tab)}
                    className="flex-1 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
                  >
                    Visualizar
                  </button>
                  <button
                    onClick={() => handleExport(card.tab, 'PDF')}
                    disabled={exportarRelatorio.isPending}
                    className="rounded-lg border border-gray-300 p-2 text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                    title="Exportar PDF"
                  >
                    <FileDown className="h-4 w-4" />
                  </button>
                  <button
                    onClick={() => handleExport(card.tab, 'EXCEL')}
                    disabled={exportarRelatorio.isPending}
                    className="rounded-lg border border-gray-300 p-2 text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                    title="Exportar Excel"
                  >
                    <FileSpreadsheet className="h-4 w-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {activeTab === 'financeiro' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Relatório Financeiro</h2>
              <div className="flex gap-2">
                <button
                  onClick={() => refetchFinanceiro()}
                  className="flex items-center gap-2 rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                >
                  <RefreshCw className={`h-4 w-4 ${loadingFinanceiro ? 'animate-spin' : ''}`} />
                  Atualizar
                </button>
                <button
                  onClick={() => handleExport('financeiro', 'PDF')}
                  disabled={exportarRelatorio.isPending}
                  className="flex items-center gap-2 rounded-lg bg-blue-600 px-3 py-2 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  <Download className="h-4 w-4" />
                  Exportar PDF
                </button>
              </div>
            </div>

            {loadingFinanceiro ? (
              <div className="flex justify-center py-12">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
              </div>
            ) : relatorioFinanceiro ? (
              <div className="space-y-6">
                {/* Summary Cards */}
                <div className="grid gap-4 md:grid-cols-4">
                  <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Receita Total</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">
                      {formatCurrency(relatorioFinanceiro.receitaTotal)}
                    </p>
                    <p className={`text-sm ${relatorioFinanceiro.variacaoPercentual >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                      {formatPercent(relatorioFinanceiro.variacaoPercentual)} vs anterior
                    </p>
                  </div>
                  <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">MRR Atual</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">
                      {formatCurrency(relatorioFinanceiro.mrrAtual)}
                    </p>
                    <p className="text-sm text-gray-500">ARR: {formatCurrency(relatorioFinanceiro.arrAtual)}</p>
                  </div>
                  <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Ticket Médio</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">
                      {formatCurrency(relatorioFinanceiro.ticketMedio)}
                    </p>
                  </div>
                  <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Inadimplência</p>
                    <p className="text-2xl font-bold text-red-600 dark:text-red-400">
                      {formatCurrency(relatorioFinanceiro.valorInadimplente)}
                    </p>
                    <p className="text-sm text-gray-500">{relatorioFinanceiro.taxaInadimplencia.toFixed(1)}% do total</p>
                  </div>
                </div>

                {/* Faturas Summary */}
                <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
                  <h3 className="mb-4 font-semibold text-gray-900 dark:text-white">Resumo de Faturas</h3>
                  <div className="grid gap-4 md:grid-cols-5">
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Total</p>
                      <p className="text-xl font-bold text-gray-900 dark:text-white">{relatorioFinanceiro.totalFaturas}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Pagas</p>
                      <p className="text-xl font-bold text-green-600 dark:text-green-400">{relatorioFinanceiro.faturasPagas}</p>
                      <p className="text-xs text-gray-500">{formatCurrency(relatorioFinanceiro.valorFaturasPagas)}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Pendentes</p>
                      <p className="text-xl font-bold text-yellow-600 dark:text-yellow-400">{relatorioFinanceiro.faturasPendentes}</p>
                      <p className="text-xs text-gray-500">{formatCurrency(relatorioFinanceiro.valorFaturasPendentes)}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Vencidas</p>
                      <p className="text-xl font-bold text-red-600 dark:text-red-400">{relatorioFinanceiro.faturasVencidas}</p>
                      <p className="text-xs text-gray-500">{formatCurrency(relatorioFinanceiro.valorFaturasVencidas)}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Canceladas</p>
                      <p className="text-xl font-bold text-gray-600 dark:text-gray-400">{relatorioFinanceiro.faturasCanceladas}</p>
                    </div>
                  </div>
                </div>

                {/* Receita por Plano */}
                {relatorioFinanceiro.receitaPorPlano && relatorioFinanceiro.receitaPorPlano.length > 0 && (
                  <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
                    <h3 className="mb-4 font-semibold text-gray-900 dark:text-white">Receita por Plano</h3>
                    <div className="space-y-3">
                      {relatorioFinanceiro.receitaPorPlano.map((plano) => (
                        <div key={plano.planoCodigo} className="flex items-center justify-between">
                          <div className="flex items-center gap-3">
                            <div className="w-32 font-medium text-gray-900 dark:text-white">{plano.planoNome}</div>
                            <div className="text-sm text-gray-500">{plano.quantidadeOficinas} oficinas</div>
                          </div>
                          <div className="flex items-center gap-4">
                            <div className="w-32 text-right font-medium text-gray-900 dark:text-white">
                              {formatCurrency(plano.receitaTotal)}
                            </div>
                            <div className="w-24">
                              <div className="h-2 rounded-full bg-gray-200 dark:bg-gray-700">
                                <div
                                  className="h-2 rounded-full bg-blue-600"
                                  style={{ width: `${plano.percentualReceita}%` }}
                                />
                              </div>
                            </div>
                            <div className="w-16 text-right text-sm text-gray-500">
                              {plano.percentualReceita.toFixed(1)}%
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="text-center text-gray-500 py-12">Nenhum dado disponível</div>
            )}
          </div>
        )}

        {activeTab === 'operacional' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Relatório Operacional</h2>
              <div className="flex gap-2">
                <button
                  onClick={() => refetchOperacional()}
                  className="flex items-center gap-2 rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                >
                  <RefreshCw className={`h-4 w-4 ${loadingOperacional ? 'animate-spin' : ''}`} />
                  Atualizar
                </button>
                <button
                  onClick={() => handleExport('operacional', 'PDF')}
                  disabled={exportarRelatorio.isPending}
                  className="flex items-center gap-2 rounded-lg bg-blue-600 px-3 py-2 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  <Download className="h-4 w-4" />
                  Exportar PDF
                </button>
              </div>
            </div>

            {loadingOperacional ? (
              <div className="flex justify-center py-12">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
              </div>
            ) : relatorioOperacional ? (
              <div className="space-y-6">
                {/* Oficinas Summary */}
                <div className="grid gap-4 md:grid-cols-5">
                  <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Total Oficinas</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">{relatorioOperacional.totalOficinas}</p>
                  </div>
                  <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Ativas</p>
                    <p className="text-2xl font-bold text-green-600 dark:text-green-400">{relatorioOperacional.oficinasAtivas}</p>
                  </div>
                  <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Trial</p>
                    <p className="text-2xl font-bold text-blue-600 dark:text-blue-400">{relatorioOperacional.oficinasEmTrial}</p>
                  </div>
                  <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Suspensas</p>
                    <p className="text-2xl font-bold text-yellow-600 dark:text-yellow-400">{relatorioOperacional.oficinasSuspensas}</p>
                  </div>
                  <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Canceladas</p>
                    <p className="text-2xl font-bold text-red-600 dark:text-red-400">{relatorioOperacional.oficinasCanceladas}</p>
                  </div>
                </div>

                {/* Users & Metrics */}
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
                    <h3 className="mb-4 font-semibold text-gray-900 dark:text-white">Usuários</h3>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-sm text-gray-500 dark:text-gray-400">Total</p>
                        <p className="text-xl font-bold text-gray-900 dark:text-white">{relatorioOperacional.totalUsuarios}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-500 dark:text-gray-400">Ativos</p>
                        <p className="text-xl font-bold text-green-600 dark:text-green-400">{relatorioOperacional.usuariosAtivos}</p>
                      </div>
                    </div>
                  </div>
                  <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
                    <h3 className="mb-4 font-semibold text-gray-900 dark:text-white">Médias por Oficina</h3>
                    <div className="grid grid-cols-3 gap-4">
                      <div>
                        <p className="text-sm text-gray-500 dark:text-gray-400">Usuários</p>
                        <p className="text-xl font-bold text-gray-900 dark:text-white">
                          {relatorioOperacional.mediaUsuariosPorOficina.toFixed(1)}
                        </p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-500 dark:text-gray-400">Clientes</p>
                        <p className="text-xl font-bold text-gray-900 dark:text-white">
                          {relatorioOperacional.mediaClientesPorOficina.toFixed(1)}
                        </p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-500 dark:text-gray-400">OS/Mês</p>
                        <p className="text-xl font-bold text-gray-900 dark:text-white">
                          {relatorioOperacional.mediaOSPorOficina.toFixed(1)}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Distribution */}
                {relatorioOperacional.distribuicaoStatus && (
                  <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
                    <h3 className="mb-4 font-semibold text-gray-900 dark:text-white">Distribuição por Status</h3>
                    <div className="flex gap-4">
                      {relatorioOperacional.distribuicaoStatus.map((status) => (
                        <div key={status.status} className="flex-1">
                          <div className="mb-2 flex justify-between text-sm">
                            <span className="text-gray-600 dark:text-gray-400">{status.status}</span>
                            <span className="font-medium text-gray-900 dark:text-white">{status.quantidade}</span>
                          </div>
                          <div className="h-2 rounded-full bg-gray-200 dark:bg-gray-700">
                            <div
                              className={`h-2 rounded-full ${
                                status.status === 'ATIVA'
                                  ? 'bg-green-500'
                                  : status.status === 'TRIAL'
                                  ? 'bg-blue-500'
                                  : status.status === 'SUSPENSA'
                                  ? 'bg-yellow-500'
                                  : 'bg-red-500'
                              }`}
                              style={{ width: `${status.percentual}%` }}
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="text-center text-gray-500 py-12">Nenhum dado disponível</div>
            )}
          </div>
        )}

        {activeTab === 'crescimento' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Relatório de Crescimento</h2>
              <div className="flex gap-2">
                <button
                  onClick={() => refetchCrescimento()}
                  className="flex items-center gap-2 rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                >
                  <RefreshCw className={`h-4 w-4 ${loadingCrescimento ? 'animate-spin' : ''}`} />
                  Atualizar
                </button>
                <button
                  onClick={() => handleExport('crescimento', 'PDF')}
                  disabled={exportarRelatorio.isPending}
                  className="flex items-center gap-2 rounded-lg bg-blue-600 px-3 py-2 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  <Download className="h-4 w-4" />
                  Exportar PDF
                </button>
              </div>
            </div>

            {loadingCrescimento ? (
              <div className="flex justify-center py-12">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
              </div>
            ) : relatorioCrescimento ? (
              <div className="space-y-6">
                {/* Growth Summary */}
                <div className="grid gap-4 md:grid-cols-4">
                  <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Novas Oficinas</p>
                    <p className="text-2xl font-bold text-green-600 dark:text-green-400">+{relatorioCrescimento.novasOficinas}</p>
                  </div>
                  <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Cancelamentos</p>
                    <p className="text-2xl font-bold text-red-600 dark:text-red-400">-{relatorioCrescimento.cancelamentos}</p>
                  </div>
                  <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Crescimento Líquido</p>
                    <p className={`text-2xl font-bold ${relatorioCrescimento.crescimentoLiquido >= 0 ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}`}>
                      {relatorioCrescimento.crescimentoLiquido >= 0 ? '+' : ''}{relatorioCrescimento.crescimentoLiquido}
                    </p>
                  </div>
                  <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">Churn Rate</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">{relatorioCrescimento.churnRate.toFixed(2)}%</p>
                  </div>
                </div>

                {/* LTV & CAC */}
                <div className="grid gap-4 md:grid-cols-3">
                  <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">LTV (Lifetime Value)</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">{formatCurrency(relatorioCrescimento.ltv)}</p>
                  </div>
                  <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">CAC (Custo Aquisição)</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">{formatCurrency(relatorioCrescimento.cac)}</p>
                  </div>
                  <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
                    <p className="text-sm text-gray-500 dark:text-gray-400">LTV/CAC Ratio</p>
                    <p className={`text-2xl font-bold ${relatorioCrescimento.ltvCacRatio >= 3 ? 'text-green-600 dark:text-green-400' : 'text-yellow-600 dark:text-yellow-400'}`}>
                      {relatorioCrescimento.ltvCacRatio.toFixed(1)}x
                    </p>
                    <p className="text-xs text-gray-500">Ideal: &gt; 3x</p>
                  </div>
                </div>

                {/* Trial Conversion */}
                <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
                  <h3 className="mb-4 font-semibold text-gray-900 dark:text-white">Conversão de Trial</h3>
                  <div className="grid gap-4 md:grid-cols-4">
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Trials Iniciados</p>
                      <p className="text-xl font-bold text-gray-900 dark:text-white">{relatorioCrescimento.trialsIniciados}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Trials Convertidos</p>
                      <p className="text-xl font-bold text-green-600 dark:text-green-400">{relatorioCrescimento.trialsConvertidos}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Taxa Conversão</p>
                      <p className="text-xl font-bold text-gray-900 dark:text-white">{relatorioCrescimento.taxaConversaoTrial.toFixed(1)}%</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">Média Dias Trial</p>
                      <p className="text-xl font-bold text-gray-900 dark:text-white">{relatorioCrescimento.mediaDiasTrial.toFixed(0)} dias</p>
                    </div>
                  </div>
                </div>

                {/* Retention */}
                <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
                  <h3 className="mb-4 font-semibold text-gray-900 dark:text-white">Taxas de Retenção</h3>
                  <div className="grid gap-4 md:grid-cols-3">
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">30 dias</p>
                      <p className="text-xl font-bold text-gray-900 dark:text-white">{relatorioCrescimento.taxaRetencao30d.toFixed(1)}%</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">90 dias</p>
                      <p className="text-xl font-bold text-gray-900 dark:text-white">{relatorioCrescimento.taxaRetencao90d.toFixed(1)}%</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">12 meses</p>
                      <p className="text-xl font-bold text-gray-900 dark:text-white">{relatorioCrescimento.taxaRetencao12m.toFixed(1)}%</p>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="text-center text-gray-500 py-12">Nenhum dado disponível</div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
