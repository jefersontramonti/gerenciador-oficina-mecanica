/**
 * Página principal do Dashboard
 * Exibe estatísticas, gráficos e ordens de serviço recentes
 */

import { Users, Car, ClipboardList, DollarSign } from 'lucide-react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { useDashboardStats } from '../hooks/useDashboardStats';
import { StatCard } from '../components/StatCard';
import { OSStatusPieChart } from '../components/OSStatusPieChart';
import { FaturamentoBarChart } from '../components/FaturamentoBarChart';
import { RecentOSTable } from '../components/RecentOSTable';

export const DashboardPage = () => {
  const { user } = useAuth();
  const { data: stats, isLoading } = useDashboardStats();

  // TODO: Integrar com WebSocket para invalidar cache quando receber notificações
  // useEffect(() => {
  //   const handleDashboardUpdate = () => {
  //     queryClient.invalidateQueries({ queryKey: ['dashboard'] });
  //   };
  //   websocketService.subscribeToBroadcast('dashboard-updates', handleDashboardUpdate);
  //   return () => { /* cleanup */ };
  // }, []);

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
    }).format(value);
  };

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <p className="mt-2 text-gray-600">
          Bem-vindo(a), <span className="font-medium">{user?.nome}</span>!
        </p>
      </div>

      {/* Stat Cards */}
      <div className="mb-8 grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total de Clientes"
          value={stats?.totalClientes ?? 0}
          icon={Users}
          iconColor="text-blue-600"
          iconBgColor="bg-blue-100"
          isLoading={isLoading}
        />
        <StatCard
          title="Total de Veículos"
          value={stats?.totalVeiculos ?? 0}
          icon={Car}
          iconColor="text-green-600"
          iconBgColor="bg-green-100"
          isLoading={isLoading}
        />
        <StatCard
          title="OS Ativas"
          value={stats?.osAtivas ?? 0}
          icon={ClipboardList}
          iconColor="text-yellow-600"
          iconBgColor="bg-yellow-100"
          isLoading={isLoading}
        />
        <StatCard
          title="Faturamento do Mês"
          value={stats ? formatCurrency(stats.faturamentoMes) : 'R$ 0,00'}
          icon={DollarSign}
          iconColor="text-emerald-600"
          iconBgColor="bg-emerald-100"
          isLoading={isLoading}
        />
      </div>

      {/* Charts */}
      <div className="mb-8 grid gap-6 lg:grid-cols-2">
        <OSStatusPieChart />
        <FaturamentoBarChart />
      </div>

      {/* Recent Service Orders */}
      <div className="mb-8">
        <RecentOSTable />
      </div>
    </div>
  );
};
