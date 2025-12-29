/**
 * SaaS Dashboard - SUPER_ADMIN main dashboard with advanced metrics
 */

import { Link } from 'react-router-dom';
import {
  Building2,
  DollarSign,
  Users,
  Car,
  FileText,
  AlertTriangle,
  TrendingUp,
  TrendingDown,
  Clock,
  CheckCircle,
  XCircle,
  PlayCircle,
  RefreshCw,
  BarChart3,
  Target,
  Percent,
} from 'lucide-react';
import {
  useDashboardStats,
  useDashboardMetrics,
  useMRRBreakdown,
  useMRREvolution,
  useChurnEvolution,
  useSignupsVsCancellations,
  useTrialsExpiring,
  useRunJob,
} from '../hooks/useSaas';
import {
  MRREvolutionChart,
  ChurnEvolutionChart,
  SignupsVsCancellationsChart,
  OficinasStatusChart,
} from '../components/charts';
import { formatCurrency, formatNumber } from '@/shared/utils/formatters';
import { planoLabels, StatusOficina, PlanoAssinatura } from '../types';
import { showSuccess, showError } from '@/shared/utils/notifications';

export const SaasDashboardPage = () => {
  const { data: stats, isLoading: loadingStats } = useDashboardStats();
  const { data: metrics, isLoading: loadingMetrics } = useDashboardMetrics();
  const { data: mrrBreakdown, isLoading: loadingMRR } = useMRRBreakdown();
  const { data: mrrEvolution, isLoading: loadingMRREvolution } = useMRREvolution(12);
  const { data: churnEvolution, isLoading: loadingChurn } = useChurnEvolution(12);
  const { data: signupsData, isLoading: loadingSignups } = useSignupsVsCancellations(12);
  const { data: trialsExpiring } = useTrialsExpiring();
  const runJobMutation = useRunJob();

  const handleRunJob = async (job: 'suspend-overdue' | 'alert-trials' | 'refresh-stats' | 'run-all') => {
    try {
      await runJobMutation.mutateAsync(job);
      showSuccess('Job executado com sucesso!');
    } catch {
      showError('Erro ao executar job');
    }
  };

  const getStatusColor = (status: StatusOficina) => {
    switch (status) {
      case 'ATIVA':
        return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400';
      case 'TRIAL':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400';
      case 'SUSPENSA':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400';
      case 'CANCELADA':
        return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300';
    }
  };

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Painel SaaS
          </h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            Visão geral da plataforma PitStop
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => handleRunJob('refresh-stats')}
            disabled={runJobMutation.isPending}
            className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700"
          >
            <RefreshCw className={`h-4 w-4 ${runJobMutation.isPending ? 'animate-spin' : ''}`} />
            Atualizar
          </button>
        </div>
      </div>

      {/* Financial KPIs Row */}
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {/* MRR Total */}
        <div className="rounded-lg bg-gradient-to-br from-green-500 to-green-600 p-5 text-white shadow-lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-green-100">MRR</p>
              <p className="mt-1 text-2xl font-bold">
                {loadingMetrics ? '...' : formatCurrency(metrics?.mrrTotal || 0)}
              </p>
            </div>
            <DollarSign className="h-8 w-8 text-green-200" />
          </div>
          <div className="mt-3 flex items-center text-sm">
            {metrics && metrics.mrrGrowth !== 0 && (
              <>
                {metrics.mrrGrowth >= 0 ? (
                  <TrendingUp className="mr-1 h-4 w-4 text-green-200" />
                ) : (
                  <TrendingDown className="mr-1 h-4 w-4 text-red-200" />
                )}
                <span className={metrics.mrrGrowth >= 0 ? 'text-green-200' : 'text-red-200'}>
                  {metrics.mrrGrowth >= 0 ? '+' : ''}{metrics.mrrGrowth.toFixed(1)}%
                </span>
              </>
            )}
          </div>
        </div>

        {/* ARR */}
        <div className="rounded-lg bg-white p-5 shadow dark:bg-gray-800">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 dark:text-gray-400">ARR</p>
              <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-white">
                {loadingMetrics ? '...' : formatCurrency(metrics?.arrTotal || 0)}
              </p>
            </div>
            <div className="rounded-full bg-blue-100 p-2 dark:bg-blue-900/30">
              <BarChart3 className="h-6 w-6 text-blue-600 dark:text-blue-400" />
            </div>
          </div>
          <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
            Receita Anual Recorrente
          </p>
        </div>

        {/* LTV */}
        <div className="rounded-lg bg-white p-5 shadow dark:bg-gray-800">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 dark:text-gray-400">LTV Médio</p>
              <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-white">
                {loadingMetrics ? '...' : formatCurrency(metrics?.ltv || 0)}
              </p>
            </div>
            <div className="rounded-full bg-purple-100 p-2 dark:bg-purple-900/30">
              <Target className="h-6 w-6 text-purple-600 dark:text-purple-400" />
            </div>
          </div>
          <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
            Lifetime Value por cliente
          </p>
        </div>

        {/* Churn Rate */}
        <div className="rounded-lg bg-white p-5 shadow dark:bg-gray-800">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 dark:text-gray-400">Churn Rate</p>
              <p className={`mt-1 text-2xl font-bold ${
                (metrics?.churnRate || 0) <= 2 ? 'text-green-600 dark:text-green-400' :
                (metrics?.churnRate || 0) <= 5 ? 'text-yellow-600 dark:text-yellow-400' :
                'text-red-600 dark:text-red-400'
              }`}>
                {loadingMetrics ? '...' : `${(metrics?.churnRate || 0).toFixed(1)}%`}
              </p>
            </div>
            <div className={`rounded-full p-2 ${
              (metrics?.churnRate || 0) <= 2 ? 'bg-green-100 dark:bg-green-900/30' :
              (metrics?.churnRate || 0) <= 5 ? 'bg-yellow-100 dark:bg-yellow-900/30' :
              'bg-red-100 dark:bg-red-900/30'
            }`}>
              <Percent className={`h-6 w-6 ${
                (metrics?.churnRate || 0) <= 2 ? 'text-green-600 dark:text-green-400' :
                (metrics?.churnRate || 0) <= 5 ? 'text-yellow-600 dark:text-yellow-400' :
                'text-red-600 dark:text-red-400'
              }`} />
            </div>
          </div>
          <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
            Taxa de cancelamento mensal
          </p>
        </div>

        {/* Faturamento do Mês */}
        <div className="rounded-lg bg-white p-5 shadow dark:bg-gray-800">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 dark:text-gray-400">Faturamento Mês</p>
              <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-white">
                {loadingMetrics ? '...' : formatCurrency(metrics?.faturamentoMes || 0)}
              </p>
            </div>
            <div className="rounded-full bg-emerald-100 p-2 dark:bg-emerald-900/30">
              <TrendingUp className="h-6 w-6 text-emerald-600 dark:text-emerald-400" />
            </div>
          </div>
          <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
            Total OS entregues no mês
          </p>
        </div>
      </div>

      {/* Workshop Stats Row */}
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Oficinas Ativas */}
        <div className="rounded-lg bg-white p-5 shadow dark:bg-gray-800">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 dark:text-gray-400">Oficinas Ativas</p>
              <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-white">
                {loadingMetrics ? '...' : formatNumber(metrics?.oficinasAtivas || 0)}
              </p>
            </div>
            <div className="rounded-full bg-green-100 p-2 dark:bg-green-900/30">
              <CheckCircle className="h-6 w-6 text-green-600 dark:text-green-400" />
            </div>
          </div>
          <Link
            to="/admin/oficinas?status=ATIVA"
            className="mt-3 inline-block text-sm text-blue-600 hover:underline dark:text-blue-400"
          >
            Ver todas
          </Link>
        </div>

        {/* Oficinas em Trial */}
        <div className="rounded-lg bg-white p-5 shadow dark:bg-gray-800">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 dark:text-gray-400">Em Trial</p>
              <p className="mt-1 text-2xl font-bold text-gray-900 dark:text-white">
                {loadingMetrics ? '...' : formatNumber(metrics?.oficinasTrial || 0)}
              </p>
            </div>
            <div className="rounded-full bg-blue-100 p-2 dark:bg-blue-900/30">
              <Clock className="h-6 w-6 text-blue-600 dark:text-blue-400" />
            </div>
          </div>
          <Link
            to="/admin/oficinas?status=TRIAL"
            className="mt-3 inline-block text-sm text-blue-600 hover:underline dark:text-blue-400"
          >
            Ver todas
          </Link>
        </div>

        {/* Novas no Mês */}
        <div className="rounded-lg bg-white p-5 shadow dark:bg-gray-800">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 dark:text-gray-400">Novas (30d)</p>
              <p className="mt-1 text-2xl font-bold text-green-600 dark:text-green-400">
                {loadingMetrics ? '...' : `+${formatNumber(metrics?.novasOficinas30d || 0)}`}
              </p>
            </div>
            <div className="rounded-full bg-green-100 p-2 dark:bg-green-900/30">
              <TrendingUp className="h-6 w-6 text-green-600 dark:text-green-400" />
            </div>
          </div>
          <p className="mt-3 text-xs text-gray-500 dark:text-gray-400">
            Novos cadastros nos últimos 30 dias
          </p>
        </div>

        {/* Cancelamentos no Mês */}
        <div className="rounded-lg bg-white p-5 shadow dark:bg-gray-800">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600 dark:text-gray-400">Cancelamentos (30d)</p>
              <p className="mt-1 text-2xl font-bold text-red-600 dark:text-red-400">
                {loadingMetrics ? '...' : `-${formatNumber(metrics?.cancelamentos30d || 0)}`}
              </p>
            </div>
            <div className="rounded-full bg-red-100 p-2 dark:bg-red-900/30">
              <XCircle className="h-6 w-6 text-red-600 dark:text-red-400" />
            </div>
          </div>
          <p className="mt-3 text-xs text-gray-500 dark:text-gray-400">
            Cancelamentos nos últimos 30 dias
          </p>
        </div>
      </div>

      {/* Charts Row 1 - MRR Evolution */}
      <div className="mb-6">
        {mrrEvolution && (
          <MRREvolutionChart data={mrrEvolution} isLoading={loadingMRREvolution} />
        )}
        {!mrrEvolution && loadingMRREvolution && (
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <div className="flex h-80 items-center justify-center">
              <RefreshCw className="h-8 w-8 animate-spin text-gray-400" />
            </div>
          </div>
        )}
      </div>

      {/* Charts Row 2 - Churn & Signups */}
      <div className="mb-6 grid gap-6 lg:grid-cols-2">
        {churnEvolution && (
          <ChurnEvolutionChart data={churnEvolution} isLoading={loadingChurn} />
        )}
        {!churnEvolution && loadingChurn && (
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <div className="flex h-80 items-center justify-center">
              <RefreshCw className="h-8 w-8 animate-spin text-gray-400" />
            </div>
          </div>
        )}
        {signupsData && (
          <SignupsVsCancellationsChart data={signupsData} isLoading={loadingSignups} />
        )}
        {!signupsData && loadingSignups && (
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <div className="flex h-80 items-center justify-center">
              <RefreshCw className="h-8 w-8 animate-spin text-gray-400" />
            </div>
          </div>
        )}
      </div>

      {/* Bottom Row - MRR by Plan, Status Chart, Trials Expiring */}
      <div className="mb-6 grid gap-6 lg:grid-cols-3">
        {/* MRR by Plan */}
        <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
          <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
            MRR por Plano
          </h2>
          {loadingMRR ? (
            <div className="flex h-48 items-center justify-center">
              <RefreshCw className="h-8 w-8 animate-spin text-gray-400" />
            </div>
          ) : mrrBreakdown && mrrBreakdown.length > 0 ? (
            <div className="space-y-4">
              {mrrBreakdown.map((item) => (
                <div key={item.plano}>
                  <div className="mb-1 flex items-center justify-between">
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                      {planoLabels[item.plano as PlanoAssinatura]}
                    </span>
                    <span className="text-sm text-gray-600 dark:text-gray-400">
                      {item.quantidadeOficinas} oficinas
                    </span>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="h-3 flex-1 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
                      <div
                        className={`h-full rounded-full ${
                          item.plano === 'TURBINADO'
                            ? 'bg-purple-500'
                            : item.plano === 'PROFISSIONAL'
                            ? 'bg-blue-500'
                            : 'bg-green-500'
                        }`}
                        style={{ width: `${item.percentualTotal}%` }}
                      />
                    </div>
                    <span className="w-24 text-right text-sm font-semibold text-gray-900 dark:text-white">
                      {formatCurrency(item.mrrPlano)}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-center text-gray-500 dark:text-gray-400">
              Nenhum dado disponível
            </p>
          )}
        </div>

        {/* Oficinas Status Chart */}
        {metrics && (
          <OficinasStatusChart data={metrics} isLoading={loadingMetrics} />
        )}

        {/* Trials Expiring */}
        <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Trials Expirando (7 dias)
            </h2>
            <span className="rounded-full bg-yellow-100 px-2.5 py-0.5 text-xs font-medium text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400">
              {trialsExpiring?.totalElements || 0}
            </span>
          </div>
          {trialsExpiring?.content && trialsExpiring.content.length > 0 ? (
            <div className="space-y-3">
              {trialsExpiring.content.slice(0, 5).map((oficina) => (
                <Link
                  key={oficina.id}
                  to={`/admin/oficinas/${oficina.id}`}
                  className="flex items-center justify-between rounded-lg border border-gray-200 p-3 transition-colors hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-700"
                >
                  <div>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {oficina.nomeFantasia}
                    </p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {oficina.email}
                    </p>
                  </div>
                  <div className="text-right">
                    <span className={`inline-block rounded-full px-2 py-1 text-xs font-medium ${getStatusColor(oficina.status)}`}>
                      {oficina.diasRestantesTrial} dias
                    </span>
                  </div>
                </Link>
              ))}
              {trialsExpiring.totalElements > 5 && (
                <Link
                  to="/admin/oficinas?status=TRIAL"
                  className="block text-center text-sm text-blue-600 hover:underline dark:text-blue-400"
                >
                  Ver todas ({trialsExpiring.totalElements})
                </Link>
              )}
            </div>
          ) : (
            <p className="text-center text-gray-500 dark:text-gray-400">
              Nenhum trial expirando
            </p>
          )}
        </div>
      </div>

      {/* General Stats Row */}
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {/* Total Oficinas */}
        <div className="rounded-lg bg-white p-5 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-purple-100 p-2 dark:bg-purple-900/30">
              <Building2 className="h-5 w-5 text-purple-600 dark:text-purple-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Total Oficinas</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {loadingStats ? '...' : formatNumber(stats?.totalOficinas || 0)}
              </p>
            </div>
          </div>
        </div>

        {/* Total Clientes */}
        <div className="rounded-lg bg-white p-5 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-indigo-100 p-2 dark:bg-indigo-900/30">
              <Users className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Total Clientes</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {loadingMetrics ? '...' : formatNumber(metrics?.totalClientes || 0)}
              </p>
            </div>
          </div>
        </div>

        {/* Total Veículos */}
        <div className="rounded-lg bg-white p-5 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-cyan-100 p-2 dark:bg-cyan-900/30">
              <Car className="h-5 w-5 text-cyan-600 dark:text-cyan-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Total Veículos</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {loadingMetrics ? '...' : formatNumber(metrics?.totalVeiculos || 0)}
              </p>
            </div>
          </div>
        </div>

        {/* Total OS */}
        <div className="rounded-lg bg-white p-5 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-orange-100 p-2 dark:bg-orange-900/30">
              <FileText className="h-5 w-5 text-orange-600 dark:text-orange-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Total OS</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {loadingMetrics ? '...' : formatNumber(metrics?.totalOS || 0)}
              </p>
            </div>
          </div>
        </div>

        {/* OS no Mês */}
        <div className="rounded-lg bg-white p-5 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-teal-100 p-2 dark:bg-teal-900/30">
              <FileText className="h-5 w-5 text-teal-600 dark:text-teal-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">OS no Mês</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {loadingMetrics ? '...' : formatNumber(metrics?.totalOSMes || 0)}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Alerts Section */}
      <div className="mb-6 grid gap-6 lg:grid-cols-2">
        {/* Pagamentos Pendentes */}
        <div className="rounded-lg border-l-4 border-yellow-500 bg-yellow-50 p-4 dark:bg-yellow-900/20">
          <div className="flex items-center gap-3">
            <AlertTriangle className="h-6 w-6 text-yellow-600 dark:text-yellow-400" />
            <div>
              <h3 className="font-medium text-yellow-800 dark:text-yellow-300">
                Pagamentos Pendentes
              </h3>
              <p className="text-sm text-yellow-700 dark:text-yellow-400">
                {stats?.pagamentosPendentes || 0} oficina(s) com pagamento pendente
              </p>
            </div>
            <Link
              to="/admin/pagamentos?status=PENDENTE"
              className="ml-auto text-sm font-medium text-yellow-800 hover:underline dark:text-yellow-300"
            >
              Ver
            </Link>
          </div>
        </div>

        {/* Inadimplentes */}
        <div className="rounded-lg border-l-4 border-red-500 bg-red-50 p-4 dark:bg-red-900/20">
          <div className="flex items-center gap-3">
            <XCircle className="h-6 w-6 text-red-600 dark:text-red-400" />
            <div>
              <h3 className="font-medium text-red-800 dark:text-red-300">
                Inadimplentes
              </h3>
              <p className="text-sm text-red-700 dark:text-red-400">
                {metrics?.oficinasInadimplentes || 0} oficina(s) inadimplente(s)
              </p>
            </div>
            <Link
              to="/admin/pagamentos?status=ATRASADO"
              className="ml-auto text-sm font-medium text-red-800 hover:underline dark:text-red-300"
            >
              Ver
            </Link>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
        <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
          Ações Rápidas
        </h2>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={() => handleRunJob('suspend-overdue')}
            disabled={runJobMutation.isPending}
            className="flex items-center gap-2 rounded-lg bg-red-100 px-4 py-2 text-red-700 transition-colors hover:bg-red-200 dark:bg-red-900/30 dark:text-red-400 dark:hover:bg-red-900/50"
          >
            <XCircle className="h-4 w-4" />
            Suspender Inadimplentes
          </button>
          <button
            onClick={() => handleRunJob('alert-trials')}
            disabled={runJobMutation.isPending}
            className="flex items-center gap-2 rounded-lg bg-yellow-100 px-4 py-2 text-yellow-700 transition-colors hover:bg-yellow-200 dark:bg-yellow-900/30 dark:text-yellow-400 dark:hover:bg-yellow-900/50"
          >
            <Clock className="h-4 w-4" />
            Alertar Trials
          </button>
          <button
            onClick={() => handleRunJob('run-all')}
            disabled={runJobMutation.isPending}
            className="flex items-center gap-2 rounded-lg bg-blue-100 px-4 py-2 text-blue-700 transition-colors hover:bg-blue-200 dark:bg-blue-900/30 dark:text-blue-400 dark:hover:bg-blue-900/50"
          >
            <PlayCircle className="h-4 w-4" />
            Executar Todos os Jobs
          </button>
        </div>
      </div>
    </div>
  );
};
