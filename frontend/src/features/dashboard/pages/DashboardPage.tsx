/**
 * Página principal do Dashboard
 * Exibe alertas, estatísticas com trend, widgets expansíveis, gráficos e OS recentes
 */

import { Users, Car, ClipboardList, DollarSign, TrendingUp, Wrench, Package, AlertTriangle } from 'lucide-react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { usePermissions } from '@/features/auth/hooks/usePermissions';
import { PerfilUsuario } from '@/features/auth/types';
import { useDashboardStats } from '../hooks/useDashboardStats';
import { useDashboardExtras } from '../hooks/useDashboardExtras';
import { useDashboardStatsComTrend, useManutencaoResumo } from '../hooks/useDashboardWidgets';
import { StatCard } from '../components/StatCard';
import { StatCardComTrend } from '../components/StatCardComTrend';
import { AlertsBar } from '../components/AlertsBar';
import { PagamentosWidget } from '../components/PagamentosWidget';
import { ManutencaoWidget } from '../components/ManutencaoWidget';
import { NotasFiscaisWidget } from '../components/NotasFiscaisWidget';
import { OSStatusPieChart } from '../components/OSStatusPieChart';
import { FaturamentoBarChart } from '../components/FaturamentoBarChart';
import { PagamentosPorTipoChart } from '../components/PagamentosPorTipoChart';
import { RecentOSTable } from '../components/RecentOSTable';
import { ComunicadoAlert } from '@/features/comunicados';
import { PlanoLimitesCard } from '@/features/configuracoes/components/PlanoLimitesCard';

export const DashboardPage = () => {
  const { user } = useAuth();
  const { canManageFinancial, hasRole } = usePermissions();
  const { data: stats, isLoading } = useDashboardStats();
  const { data: extras, isLoading: isLoadingExtras } = useDashboardExtras();
  const { data: statsTrend, isLoading: isLoadingTrend } = useDashboardStatsComTrend();
  const { data: manutencaoResumo } = useManutencaoResumo();

  const showFinancial = canManageFinancial();
  const isGerente = hasRole([PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]);

  // SUPER_ADMIN deve ir para o Dashboard SaaS
  if (user?.perfil === PerfilUsuario.SUPER_ADMIN) {
    return <Navigate to="/admin" replace />;
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-4 sm:mb-6">
        <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white">Dashboard</h1>
        <p className="mt-1 text-sm sm:text-base text-gray-600 dark:text-gray-300">
          Bem-vindo(a), <span className="font-medium">{user?.nome}</span>!
        </p>
      </div>

      {/* Alerta de Comunicados */}
      <ComunicadoAlert />

      {/* Barra de Alertas Dinâmicos */}
      <AlertsBar />

      {/* Stat Cards - Linha Principal */}
      <div className="mb-4 sm:mb-6 grid gap-3 sm:gap-4 grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total de Clientes"
          value={stats?.totalClientes ?? 0}
          icon={Users}
          iconColor="text-blue-600 dark:text-blue-400"
          iconBgColor="bg-blue-100 dark:bg-blue-900/30"
          isLoading={isLoading}
        />
        <StatCard
          title="Total de Veículos"
          value={stats?.totalVeiculos ?? 0}
          icon={Car}
          iconColor="text-green-600 dark:text-green-400"
          iconBgColor="bg-green-100 dark:bg-green-900/30"
          isLoading={isLoading}
        />
        <StatCard
          title="OS Ativas"
          value={stats?.osAtivas ?? 0}
          icon={ClipboardList}
          iconColor="text-amber-600 dark:text-amber-400"
          iconBgColor="bg-amber-100 dark:bg-amber-900/30"
          isLoading={isLoading}
        />
        {/* Card de Manutenção com badge de alertas */}
        <StatCardComTrend
          title="Planos Manutenção"
          value={manutencaoResumo?.planosAtivos ?? 0}
          icon={Wrench}
          iconColor="text-orange-600 dark:text-orange-400"
          iconBgColor="bg-orange-100 dark:bg-orange-900/30"
          badge={manutencaoResumo?.alertasPendentes}
          badgeColor="bg-red-500"
          isLoading={isLoading}
        />
      </div>

      {/* Cards financeiros com trend (apenas ADMIN/GERENTE) */}
      {isGerente && (
        <div className="mb-4 sm:mb-6 grid gap-3 sm:gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
          <StatCardComTrend
            title="Faturamento do Mês"
            value={statsTrend ? formatCurrency(statsTrend.faturamentoMes) : 'R$ 0'}
            icon={DollarSign}
            iconColor="text-emerald-600 dark:text-emerald-400"
            iconBgColor="bg-emerald-100 dark:bg-emerald-900/30"
            trend={statsTrend?.variacaoFaturamento}
            trendLabel="vs mês anterior"
            isLoading={isLoadingTrend}
          />
          <StatCardComTrend
            title="Ticket Médio"
            value={statsTrend ? formatCurrency(statsTrend.ticketMedio) : 'R$ 0'}
            icon={TrendingUp}
            iconColor="text-purple-600 dark:text-purple-400"
            iconBgColor="bg-purple-100 dark:bg-purple-900/30"
            trend={statsTrend?.variacaoTicketMedio}
            trendLabel="vs mês anterior"
            isLoading={isLoadingTrend}
          />
          <StatCard
            title="Valor Total Estoque"
            value={extras ? formatCurrency(extras.valorTotalEstoque) : 'R$ 0'}
            icon={Package}
            iconColor="text-indigo-600 dark:text-indigo-400"
            iconBgColor="bg-indigo-100 dark:bg-indigo-900/30"
            isLoading={isLoadingExtras}
          />
          {extras && extras.estoqueBaixoCount > 0 && (
            <Link to="/estoque/alertas" className="block">
              <StatCard
                title="Peças Estoque Baixo"
                value={extras.estoqueBaixoCount}
                icon={AlertTriangle}
                iconColor="text-red-600 dark:text-red-400"
                iconBgColor="bg-red-100 dark:bg-red-900/30"
                isLoading={isLoadingExtras}
              />
            </Link>
          )}
        </div>
      )}

      {/* Mini-Widgets Expansíveis */}
      {showFinancial && (
        <div className="mb-4 sm:mb-6 grid gap-3 sm:gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
          <PagamentosWidget />
          <ManutencaoWidget />
          {isGerente && <NotasFiscaisWidget />}
          {isGerente && <PlanoLimitesCard />}
        </div>
      )}

      {/* Gráficos */}
      <div className="mb-4 sm:mb-6 grid gap-4 sm:gap-6 grid-cols-1 lg:grid-cols-2">
        <OSStatusPieChart />
        <FaturamentoBarChart />
      </div>

      {/* Gráfico de Pagamentos por Tipo (apenas ADMIN/GERENTE) */}
      {isGerente && (
        <div className="mb-4 sm:mb-6 grid gap-4 sm:gap-6 grid-cols-1 lg:grid-cols-2">
          <PagamentosPorTipoChart />
        </div>
      )}

      {/* Tabela de OS Recentes */}
      <div className="mb-4 sm:mb-8">
        <RecentOSTable />
      </div>
    </div>
  );
};
