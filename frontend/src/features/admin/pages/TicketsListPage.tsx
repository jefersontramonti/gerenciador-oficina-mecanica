import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  MessageSquare,
  Search,
  Filter,
  Clock,
  AlertTriangle,
  CheckCircle,
  User,
  Building2,
  ChevronRight,
  RefreshCw,
  Plus,
} from 'lucide-react';
import { useTickets, useTicketMetricas } from '../hooks/useSaas';
import type {
  TicketFilters,
  StatusTicket,
  TipoTicket,
  PrioridadeTicket,
} from '../types';
import {
  statusTicketLabels,
  statusTicketCores,
  tipoTicketLabels,
  prioridadeTicketLabels,
  prioridadeTicketCores,
} from '../types';

export function TicketsListPage() {
  const [filters, setFilters] = useState<TicketFilters>({
    page: 0,
    size: 20,
  });
  const [showFilters, setShowFilters] = useState(false);

  const { data: ticketsData, isLoading, refetch } = useTickets(filters);
  const { data: metricas } = useTicketMetricas();

  const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const busca = formData.get('busca') as string;
    setFilters((prev) => ({ ...prev, busca, page: 0 }));
  };

  const handleFilterChange = (key: keyof TicketFilters, value: string | undefined) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value || undefined,
      page: 0,
    }));
  };

  const handlePageChange = (newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage }));
  };

  const getStatusColor = (status: StatusTicket) => {
    const colors: Record<string, string> = {
      blue: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
      yellow: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
      purple: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400',
      orange: 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400',
      green: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
      gray: 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400',
    };
    return colors[statusTicketCores[status]] || colors.gray;
  };

  const getPrioridadeColor = (prioridade: PrioridadeTicket) => {
    const colors: Record<string, string> = {
      gray: 'bg-gray-100 text-gray-600 dark:bg-gray-900/30 dark:text-gray-400',
      blue: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400',
      orange: 'bg-orange-100 text-orange-600 dark:bg-orange-900/30 dark:text-orange-400',
      red: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400',
    };
    return colors[prioridadeTicketCores[prioridade]] || colors.gray;
  };

  const formatTimeAgo = (date: string) => {
    const now = new Date();
    const past = new Date(date);
    const diffMs = now.getTime() - past.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffDays > 0) return `${diffDays}d atrás`;
    if (diffHours > 0) return `${diffHours}h atrás`;
    if (diffMins > 0) return `${diffMins}min atrás`;
    return 'Agora';
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6 dark:bg-gray-900">
      <div className="mx-auto max-w-7xl">
        {/* Header */}
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Tickets de Suporte</h1>
            <p className="mt-1 text-gray-600 dark:text-gray-400">
              Gerencie os tickets de suporte das oficinas
            </p>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={() => refetch()}
              className="flex items-center gap-2 rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
              Atualizar
            </button>
            <Link
              to="/admin/tickets/novo"
              className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              <Plus className="h-4 w-4" />
              Novo Ticket
            </Link>
          </div>
        </div>

        {/* Metrics Cards */}
        {metricas && (
          <div className="mb-6 grid gap-4 md:grid-cols-5">
            <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-blue-100 p-2 dark:bg-blue-900/30">
                  <MessageSquare className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Abertos</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">
                    {metricas.ticketsAbertos}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-yellow-100 p-2 dark:bg-yellow-900/30">
                  <Clock className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Em Andamento</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">
                    {metricas.ticketsEmAndamento}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-red-100 p-2 dark:bg-red-900/30">
                  <AlertTriangle className="h-5 w-5 text-red-600 dark:text-red-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">SLA Vencido</p>
                  <p className="text-xl font-bold text-red-600 dark:text-red-400">
                    {metricas.comSlaVencido}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-green-100 p-2 dark:bg-green-900/30">
                  <CheckCircle className="h-5 w-5 text-green-600 dark:text-green-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Resolvidos (30d)</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">
                    {metricas.resolvidosUltimos30d}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-purple-100 p-2 dark:bg-purple-900/30">
                  <User className="h-5 w-5 text-purple-600 dark:text-purple-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Não Atribuídos</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">
                    {metricas.naoAtribuidos}
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Search and Filters */}
        <div className="mb-6 rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex flex-wrap items-center gap-4">
            {/* Search */}
            <form onSubmit={handleSearch} className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <input
                  type="text"
                  name="busca"
                  placeholder="Buscar por número, assunto ou solicitante..."
                  defaultValue={filters.busca || ''}
                  className="w-full rounded-lg border border-gray-300 bg-white py-2 pl-10 pr-4 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
              </div>
            </form>

            {/* Filter Toggle */}
            <button
              onClick={() => setShowFilters(!showFilters)}
              className={`flex items-center gap-2 rounded-lg border px-3 py-2 text-sm ${
                showFilters
                  ? 'border-blue-500 bg-blue-50 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'
                  : 'border-gray-300 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700'
              }`}
            >
              <Filter className="h-4 w-4" />
              Filtros
            </button>
          </div>

          {/* Filters Panel */}
          {showFilters && (
            <div className="mt-4 grid gap-4 border-t border-gray-200 pt-4 dark:border-gray-700 md:grid-cols-4">
              <div>
                <label className="mb-1 block text-xs font-medium text-gray-700 dark:text-gray-300">
                  Status
                </label>
                <select
                  value={filters.status || ''}
                  onChange={(e) => handleFilterChange('status', e.target.value as StatusTicket)}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                >
                  <option value="">Todos</option>
                  {Object.entries(statusTicketLabels).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-gray-700 dark:text-gray-300">
                  Tipo
                </label>
                <select
                  value={filters.tipo || ''}
                  onChange={(e) => handleFilterChange('tipo', e.target.value as TipoTicket)}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                >
                  <option value="">Todos</option>
                  {Object.entries(tipoTicketLabels).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-xs font-medium text-gray-700 dark:text-gray-300">
                  Prioridade
                </label>
                <select
                  value={filters.prioridade || ''}
                  onChange={(e) => handleFilterChange('prioridade', e.target.value as PrioridadeTicket)}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                >
                  <option value="">Todas</option>
                  {Object.entries(prioridadeTicketLabels).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="flex items-end">
                <button
                  onClick={() =>
                    setFilters({
                      page: 0,
                      size: 20,
                    })
                  }
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                >
                  Limpar Filtros
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Tickets List */}
        <div className="rounded-lg bg-white shadow dark:bg-gray-800">
          {isLoading ? (
            <div className="flex justify-center py-12">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
            </div>
          ) : !ticketsData?.content.length ? (
            <div className="py-12 text-center text-gray-500 dark:text-gray-400">
              <MessageSquare className="mx-auto mb-3 h-12 w-12 text-gray-300 dark:text-gray-600" />
              <p>Nenhum ticket encontrado</p>
            </div>
          ) : (
            <div className="divide-y divide-gray-200 dark:divide-gray-700">
              {ticketsData.content.map((ticket) => (
                <Link
                  key={ticket.id}
                  to={`/admin/tickets/${ticket.id}`}
                  className="flex items-center gap-4 p-4 transition-colors hover:bg-gray-50 dark:hover:bg-gray-700/50"
                >
                  {/* Priority Indicator */}
                  <div
                    className={`h-full w-1 self-stretch rounded-full ${
                      ticket.prioridade === 'URGENTE'
                        ? 'bg-red-500'
                        : ticket.prioridade === 'ALTA'
                        ? 'bg-orange-500'
                        : ticket.prioridade === 'MEDIA'
                        ? 'bg-blue-500'
                        : 'bg-gray-300'
                    }`}
                  />

                  {/* Main Content */}
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-sm text-gray-500 dark:text-gray-400">
                        {ticket.numero}
                      </span>
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${getStatusColor(ticket.status)}`}>
                        {statusTicketLabels[ticket.status]}
                      </span>
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${getPrioridadeColor(ticket.prioridade)}`}>
                        {prioridadeTicketLabels[ticket.prioridade]}
                      </span>
                      {ticket.slaVencido && (
                        <span className="flex items-center gap-1 rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800 dark:bg-red-900/30 dark:text-red-400">
                          <AlertTriangle className="h-3 w-3" />
                          SLA Vencido
                        </span>
                      )}
                    </div>
                    <h3 className="mt-1 truncate font-medium text-gray-900 dark:text-white">
                      {ticket.assunto}
                    </h3>
                    <div className="mt-1 flex items-center gap-4 text-sm text-gray-500 dark:text-gray-400">
                      <span className="flex items-center gap-1">
                        <User className="h-3 w-3" />
                        {ticket.usuarioNome}
                      </span>
                      {ticket.oficinaNome && (
                        <span className="flex items-center gap-1">
                          <Building2 className="h-3 w-3" />
                          {ticket.oficinaNome}
                        </span>
                      )}
                      <span className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {formatTimeAgo(ticket.aberturaEm)}
                      </span>
                    </div>
                  </div>

                  {/* Right Side */}
                  <div className="text-right">
                    {ticket.atribuidoANome ? (
                      <div className="text-sm">
                        <p className="text-gray-500 dark:text-gray-400">Atribuído a</p>
                        <p className="font-medium text-gray-900 dark:text-white">
                          {ticket.atribuidoANome}
                        </p>
                      </div>
                    ) : (
                      <span className="text-sm text-orange-600 dark:text-orange-400">
                        Não atribuído
                      </span>
                    )}
                  </div>

                  <ChevronRight className="h-5 w-5 text-gray-400" />
                </Link>
              ))}
            </div>
          )}

          {/* Pagination */}
          {ticketsData && ticketsData.totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-gray-200 px-4 py-3 dark:border-gray-700">
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Mostrando {ticketsData.numberOfElements} de {ticketsData.totalElements} tickets
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => handlePageChange(filters.page! - 1)}
                  disabled={ticketsData.first}
                  className="rounded-lg border border-gray-300 px-3 py-1 text-sm disabled:opacity-50 dark:border-gray-600"
                >
                  Anterior
                </button>
                <span className="px-3 py-1 text-sm text-gray-600 dark:text-gray-400">
                  Página {ticketsData.number + 1} de {ticketsData.totalPages}
                </span>
                <button
                  onClick={() => handlePageChange(filters.page! + 1)}
                  disabled={ticketsData.last}
                  className="rounded-lg border border-gray-300 px-3 py-1 text-sm disabled:opacity-50 dark:border-gray-600"
                >
                  Próxima
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
