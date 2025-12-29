import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Megaphone,
  Search,
  Filter,
  Clock,
  Send,
  FileText,
  Eye,
  CheckCircle2,
  ChevronRight,
  RefreshCw,
  Plus,
  Calendar,
  Users,
  BarChart3,
} from 'lucide-react';
import { useComunicados, useComunicadoMetricas } from '../hooks/useSaas';
import type {
  ComunicadoFilters,
  StatusComunicado,
  TipoComunicado,
  PrioridadeComunicado,
} from '../types';
import {
  statusComunicadoLabels,
  statusComunicadoCores,
  tipoComunicadoLabels,
  prioridadeComunicadoLabels,
  prioridadeComunicadoCores,
} from '../types';

export function ComunicadosListPage() {
  const [filters, setFilters] = useState<ComunicadoFilters>({
    page: 0,
    size: 20,
  });
  const [showFilters, setShowFilters] = useState(false);

  const { data: comunicadosData, isLoading, refetch } = useComunicados(filters);
  const { data: metricas } = useComunicadoMetricas();

  const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const busca = formData.get('busca') as string;
    setFilters((prev) => ({ ...prev, busca, page: 0 }));
  };

  const handleFilterChange = (key: keyof ComunicadoFilters, value: string | undefined) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value || undefined,
      page: 0,
    }));
  };

  const handlePageChange = (newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage }));
  };

  const getStatusColor = (status: StatusComunicado) => {
    const colors: Record<string, string> = {
      gray: 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400',
      blue: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
      green: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
      red: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
    };
    return colors[statusComunicadoCores[status]] || colors.gray;
  };

  const getPrioridadeColor = (prioridade: PrioridadeComunicado) => {
    const colors: Record<string, string> = {
      gray: 'bg-gray-100 text-gray-600 dark:bg-gray-900/30 dark:text-gray-400',
      blue: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400',
      orange: 'bg-orange-100 text-orange-600 dark:bg-orange-900/30 dark:text-orange-400',
      red: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400',
    };
    return colors[prioridadeComunicadoCores[prioridade]] || colors.gray;
  };

  const formatDate = (dateValue: string | number) => {
    if (!dateValue) return '-';
    const date = typeof dateValue === 'number'
      ? new Date(dateValue * 1000)
      : new Date(dateValue);
    if (date.getFullYear() < 2020 && typeof dateValue === 'string' && !isNaN(Number(dateValue))) {
      return new Date(Number(dateValue) * 1000).toLocaleDateString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    }
    return date.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const formatDateShort = (dateValue: string | number) => {
    if (!dateValue) return '-';
    const date = typeof dateValue === 'number'
      ? new Date(dateValue * 1000)
      : new Date(dateValue);
    if (date.getFullYear() < 2020 && typeof dateValue === 'string' && !isNaN(Number(dateValue))) {
      return new Date(Number(dateValue) * 1000).toLocaleDateString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
      });
    }
    return date.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
    });
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6 dark:bg-gray-900">
      <div className="mx-auto max-w-7xl">
        {/* Header */}
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Comunicados</h1>
            <p className="mt-1 text-gray-600 dark:text-gray-400">
              Gerencie comunicados e avisos para as oficinas
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
              to="/admin/comunicados/novo"
              className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              <Plus className="h-4 w-4" />
              Novo Comunicado
            </Link>
          </div>
        </div>

        {/* Metrics Cards */}
        {metricas && (
          <div className="mb-6 grid gap-4 md:grid-cols-5">
            <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-gray-100 p-2 dark:bg-gray-900/30">
                  <FileText className="h-5 w-5 text-gray-600 dark:text-gray-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Rascunhos</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">
                    {metricas.totalRascunhos}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-blue-100 p-2 dark:bg-blue-900/30">
                  <Clock className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Agendados</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">
                    {metricas.totalAgendados}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-green-100 p-2 dark:bg-green-900/30">
                  <Send className="h-5 w-5 text-green-600 dark:text-green-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Enviados</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">
                    {metricas.totalEnviados}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-purple-100 p-2 dark:bg-purple-900/30">
                  <Users className="h-5 w-5 text-purple-600 dark:text-purple-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Dest. (30d)</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">
                    {metricas.destinatariosNoMes}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-orange-100 p-2 dark:bg-orange-900/30">
                  <BarChart3 className="h-5 w-5 text-orange-600 dark:text-orange-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Taxa Visual.</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">
                    {metricas.taxaVisualizacaoMedia.toFixed(1)}%
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
                  placeholder="Buscar por titulo ou conteudo..."
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
                  onChange={(e) => handleFilterChange('status', e.target.value as StatusComunicado)}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                >
                  <option value="">Todos</option>
                  {Object.entries(statusComunicadoLabels).map(([value, label]) => (
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
                  onChange={(e) => handleFilterChange('tipo', e.target.value as TipoComunicado)}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                >
                  <option value="">Todos</option>
                  {Object.entries(tipoComunicadoLabels).map(([value, label]) => (
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
                  onChange={(e) => handleFilterChange('prioridade', e.target.value as PrioridadeComunicado)}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                >
                  <option value="">Todas</option>
                  {Object.entries(prioridadeComunicadoLabels).map(([value, label]) => (
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

        {/* Comunicados List */}
        <div className="rounded-lg bg-white shadow dark:bg-gray-800">
          {isLoading ? (
            <div className="flex justify-center py-12">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
            </div>
          ) : !comunicadosData?.content.length ? (
            <div className="py-12 text-center text-gray-500 dark:text-gray-400">
              <Megaphone className="mx-auto mb-3 h-12 w-12 text-gray-300 dark:text-gray-600" />
              <p>Nenhum comunicado encontrado</p>
            </div>
          ) : (
            <div className="divide-y divide-gray-200 dark:divide-gray-700">
              {comunicadosData.content.map((comunicado) => (
                <Link
                  key={comunicado.id}
                  to={`/admin/comunicados/${comunicado.id}`}
                  className="flex items-center gap-4 p-4 transition-colors hover:bg-gray-50 dark:hover:bg-gray-700/50"
                >
                  {/* Priority Indicator */}
                  <div
                    className={`h-full w-1 self-stretch rounded-full ${
                      comunicado.prioridade === 'URGENTE'
                        ? 'bg-red-500'
                        : comunicado.prioridade === 'ALTA'
                        ? 'bg-orange-500'
                        : comunicado.prioridade === 'NORMAL'
                        ? 'bg-blue-500'
                        : 'bg-gray-300'
                    }`}
                  />

                  {/* Main Content */}
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${getStatusColor(comunicado.status)}`}>
                        {statusComunicadoLabels[comunicado.status]}
                      </span>
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${getPrioridadeColor(comunicado.prioridade)}`}>
                        {prioridadeComunicadoLabels[comunicado.prioridade]}
                      </span>
                      <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600 dark:bg-gray-900/30 dark:text-gray-400">
                        {tipoComunicadoLabels[comunicado.tipo]}
                      </span>
                      {comunicado.requerConfirmacao && (
                        <span className="flex items-center gap-1 rounded-full bg-purple-100 px-2 py-0.5 text-xs font-medium text-purple-800 dark:bg-purple-900/30 dark:text-purple-400">
                          <CheckCircle2 className="h-3 w-3" />
                          Requer Confirmacao
                        </span>
                      )}
                    </div>
                    <h3 className="mt-1 truncate font-medium text-gray-900 dark:text-white">
                      {comunicado.titulo}
                    </h3>
                    {comunicado.resumo && (
                      <p className="mt-0.5 truncate text-sm text-gray-500 dark:text-gray-400">
                        {comunicado.resumo}
                      </p>
                    )}
                    <div className="mt-2 flex items-center gap-4 text-sm text-gray-500 dark:text-gray-400">
                      <span className="flex items-center gap-1">
                        <Calendar className="h-3 w-3" />
                        {comunicado.status === 'AGENDADO' && comunicado.dataAgendamento
                          ? `Agendado: ${formatDate(comunicado.dataAgendamento)}`
                          : comunicado.status === 'ENVIADO' && comunicado.dataEnvio
                          ? `Enviado: ${formatDateShort(comunicado.dataEnvio)}`
                          : `Criado: ${formatDateShort(comunicado.createdAt)}`}
                      </span>
                      {comunicado.status === 'ENVIADO' && (
                        <>
                          <span className="flex items-center gap-1">
                            <Users className="h-3 w-3" />
                            {comunicado.totalDestinatarios} dest.
                          </span>
                          <span className="flex items-center gap-1">
                            <Eye className="h-3 w-3" />
                            {comunicado.taxaVisualizacao.toFixed(0)}% visual.
                          </span>
                        </>
                      )}
                    </div>
                  </div>

                  {/* Right Side - Stats */}
                  {comunicado.status === 'ENVIADO' && (
                    <div className="text-right">
                      <div className="text-sm">
                        <p className="text-gray-500 dark:text-gray-400">Visualizacoes</p>
                        <p className="font-medium text-gray-900 dark:text-white">
                          {comunicado.totalVisualizacoes}/{comunicado.totalDestinatarios}
                        </p>
                      </div>
                      {comunicado.requerConfirmacao && (
                        <div className="mt-1 text-sm">
                          <p className="text-gray-500 dark:text-gray-400">Confirmacoes</p>
                          <p className="font-medium text-gray-900 dark:text-white">
                            {comunicado.totalConfirmacoes}/{comunicado.totalDestinatarios}
                          </p>
                        </div>
                      )}
                    </div>
                  )}

                  <ChevronRight className="h-5 w-5 text-gray-400" />
                </Link>
              ))}
            </div>
          )}

          {/* Pagination */}
          {comunicadosData && comunicadosData.totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-gray-200 px-4 py-3 dark:border-gray-700">
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Mostrando {comunicadosData.numberOfElements} de {comunicadosData.totalElements} comunicados
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => handlePageChange(filters.page! - 1)}
                  disabled={comunicadosData.first}
                  className="rounded-lg border border-gray-300 px-3 py-1 text-sm disabled:opacity-50 dark:border-gray-600"
                >
                  Anterior
                </button>
                <span className="px-3 py-1 text-sm text-gray-600 dark:text-gray-400">
                  Pagina {comunicadosData.number + 1} de {comunicadosData.totalPages}
                </span>
                <button
                  onClick={() => handlePageChange(filters.page! + 1)}
                  disabled={comunicadosData.last}
                  className="rounded-lg border border-gray-300 px-3 py-1 text-sm disabled:opacity-50 dark:border-gray-600"
                >
                  Proxima
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
