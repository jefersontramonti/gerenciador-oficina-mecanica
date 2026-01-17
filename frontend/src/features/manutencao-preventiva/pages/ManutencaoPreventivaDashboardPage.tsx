import { Link } from 'react-router-dom';
import { Calendar, Wrench, AlertTriangle, Clock, Plus, ChevronRight } from 'lucide-react';
import { useDashboardManutencao } from '../hooks/useManutencaoPreventiva';
import type { PlanoManutencao, AgendamentoManutencao } from '../types';

export default function ManutencaoPreventivaDashboardPage() {
  const { data: dashboard, isLoading, error } = useDashboardManutencao();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
        Erro ao carregar dashboard. Tente novamente.
      </div>
    );
  }

  const stats = dashboard?.estatisticas;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Manutenção Preventiva
          </h1>
          <p className="text-gray-600 dark:text-gray-400">
            Gerencie planos de manutenção e agendamentos
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            to="/manutencao-preventiva/novo"
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <Plus className="h-4 w-4" />
            <span>Novo Plano</span>
          </Link>
          <Link
            to="/manutencao-preventiva/agendamentos/novo"
            className="flex items-center gap-2 px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            <Calendar className="h-4 w-4" />
            <span>Agendar</span>
          </Link>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard
          title="Planos Ativos"
          value={stats?.totalPlanosAtivos ?? 0}
          icon={<Wrench className="h-5 w-5" />}
          color="blue"
        />
        <StatCard
          title="Vencidos"
          value={stats?.planosVencidos ?? 0}
          icon={<AlertTriangle className="h-5 w-5" />}
          color="red"
        />
        <StatCard
          title="Próximos 30 dias"
          value={stats?.planosProximos30Dias ?? 0}
          icon={<Clock className="h-5 w-5" />}
          color="yellow"
        />
        <StatCard
          title="Agendamentos Hoje"
          value={stats?.agendamentosHoje ?? 0}
          icon={<Calendar className="h-5 w-5" />}
          color="green"
        />
      </div>

      {/* Main Content */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Próximas Manutenções */}
        <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Próximas Manutenções
            </h2>
            <Link
              to="/manutencao-preventiva/planos"
              className="text-sm text-blue-600 dark:text-blue-400 hover:underline flex items-center gap-1"
            >
              Ver todos <ChevronRight className="h-4 w-4" />
            </Link>
          </div>

          {dashboard?.proximasManutencoes && dashboard.proximasManutencoes.length > 0 ? (
            <div className="space-y-3">
              {dashboard.proximasManutencoes.slice(0, 5).map((plano) => (
                <PlanoCard key={plano.id} plano={plano} />
              ))}
            </div>
          ) : (
            <p className="text-gray-500 dark:text-gray-400 text-center py-8">
              Nenhuma manutenção próxima
            </p>
          )}
        </div>

        {/* Agendamentos de Hoje */}
        <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Agendamentos de Hoje
            </h2>
            <Link
              to="/manutencao-preventiva/agendamentos"
              className="text-sm text-blue-600 dark:text-blue-400 hover:underline flex items-center gap-1"
            >
              Ver todos <ChevronRight className="h-4 w-4" />
            </Link>
          </div>

          {dashboard?.agendamentosHoje && dashboard.agendamentosHoje.length > 0 ? (
            <div className="space-y-3">
              {dashboard.agendamentosHoje.map((agendamento) => (
                <AgendamentoCard key={agendamento.id} agendamento={agendamento} />
              ))}
            </div>
          ) : (
            <p className="text-gray-500 dark:text-gray-400 text-center py-8">
              Nenhum agendamento para hoje
            </p>
          )}
        </div>
      </div>

      {/* Quick Links */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <QuickLinkCard
          to="/manutencao-preventiva/planos"
          icon={<Wrench className="h-6 w-6" />}
          title="Planos"
          description="Gerenciar planos"
        />
        <QuickLinkCard
          to="/manutencao-preventiva/templates"
          icon={<Wrench className="h-6 w-6" />}
          title="Templates"
          description="Modelos de manutenção"
        />
        <QuickLinkCard
          to="/manutencao-preventiva/agendamentos"
          icon={<Calendar className="h-6 w-6" />}
          title="Agendamentos"
          description="Calendário de manutenções"
        />
        <QuickLinkCard
          to="/manutencao-preventiva/vencidos"
          icon={<AlertTriangle className="h-6 w-6" />}
          title="Vencidos"
          description="Manutenções atrasadas"
        />
      </div>
    </div>
  );
}

// Componentes auxiliares

interface StatCardProps {
  title: string;
  value: number;
  icon: React.ReactNode;
  color: 'blue' | 'red' | 'yellow' | 'green';
}

function StatCard({ title, value, icon, color }: StatCardProps) {
  const colors = {
    blue: 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400',
    red: 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
    yellow: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400',
    green: 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400',
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
      <div className="flex items-center gap-3">
        <div className={`p-2 rounded-lg ${colors[color]}`}>
          {icon}
        </div>
        <div>
          <p className="text-sm text-gray-500 dark:text-gray-400">{title}</p>
          <p className="text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
        </div>
      </div>
    </div>
  );
}

function PlanoCard({ plano }: { plano: PlanoManutencao }) {
  return (
    <Link
      to={`/manutencao-preventiva/${plano.id}`}
      className="block p-3 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700/50"
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="font-medium text-gray-900 dark:text-white">{plano.nome}</p>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {plano.veiculo ? (
              <>{plano.veiculo.placaFormatada || plano.veiculo.placa} - {plano.veiculo.marca} {plano.veiculo.modelo}</>
            ) : (
              <span className="italic">Veículo não informado</span>
            )}
          </p>
        </div>
        <div className="text-right">
          {plano.vencido ? (
            <span className="inline-block px-2 py-1 text-xs font-medium rounded-full bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400">
              Vencido
            </span>
          ) : plano.proximoAVencer ? (
            <span className="inline-block px-2 py-1 text-xs font-medium rounded-full bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400">
              {plano.diasParaVencer}d
            </span>
          ) : (
            <span className="text-sm text-gray-500 dark:text-gray-400">
              {plano.proximaPrevisaoData}
            </span>
          )}
        </div>
      </div>
    </Link>
  );
}

function AgendamentoCard({ agendamento }: { agendamento: AgendamentoManutencao }) {
  const statusColors: Record<string, string> = {
    AGENDADO: 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400',
    CONFIRMADO: 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400',
    REMARCADO: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400',
    CANCELADO: 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
    REALIZADO: 'bg-gray-100 dark:bg-gray-900/30 text-gray-600 dark:text-gray-400',
  };

  return (
    <Link
      to={`/manutencao-preventiva/agendamentos/${agendamento.id}`}
      className="block p-3 rounded-lg border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700/50"
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="font-medium text-gray-900 dark:text-white">
            {agendamento.horaAgendamento} - {agendamento.tipoManutencao}
          </p>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {agendamento.veiculo?.placaFormatada || agendamento.veiculo?.placa || '-'} - {agendamento.cliente?.nome || 'Cliente não informado'}
          </p>
        </div>
        <span className={`inline-block px-2 py-1 text-xs font-medium rounded-full ${statusColors[agendamento.status] || statusColors.AGENDADO}`}>
          {agendamento.status}
        </span>
      </div>
    </Link>
  );
}

interface QuickLinkCardProps {
  to: string;
  icon: React.ReactNode;
  title: string;
  description: string;
}

function QuickLinkCard({ to, icon, title, description }: QuickLinkCardProps) {
  return (
    <Link
      to={to}
      className="block p-4 bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 hover:border-blue-500 dark:hover:border-blue-500 transition-colors"
    >
      <div className="text-blue-600 dark:text-blue-400 mb-2">{icon}</div>
      <p className="font-medium text-gray-900 dark:text-white">{title}</p>
      <p className="text-sm text-gray-500 dark:text-gray-400">{description}</p>
    </Link>
  );
}
