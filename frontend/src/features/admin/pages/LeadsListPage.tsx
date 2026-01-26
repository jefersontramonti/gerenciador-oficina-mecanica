import { useState } from 'react';
import {
  Users,
  Search,
  Filter,
  RefreshCw,
  Mail,
  Phone,
  Copy,
  CheckCircle,
  Clock,
  Star,
  TrendingUp,
  XCircle,
} from 'lucide-react';
import { useLeads, useLeadStats } from '../hooks/useLeads';
import type { LeadFilters, LeadResumo } from '../types/lead';
import { StatusLead } from '../types/lead';
import toast from 'react-hot-toast';

const statusLabels: Record<string, string> = {
  [StatusLead.NOVO]: 'Novo',
  [StatusLead.CONTATADO]: 'Contatado',
  [StatusLead.QUALIFICADO]: 'Qualificado',
  [StatusLead.CONVERTIDO]: 'Convertido',
  [StatusLead.PERDIDO]: 'Perdido',
};

const statusColors: Record<string, string> = {
  [StatusLead.NOVO]: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
  [StatusLead.CONTATADO]:
    'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
  [StatusLead.QUALIFICADO]:
    'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400',
  [StatusLead.CONVERTIDO]:
    'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
  [StatusLead.PERDIDO]: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
};

const statusIcons: Record<string, React.ElementType> = {
  [StatusLead.NOVO]: Star,
  [StatusLead.CONTATADO]: Clock,
  [StatusLead.QUALIFICADO]: TrendingUp,
  [StatusLead.CONVERTIDO]: CheckCircle,
  [StatusLead.PERDIDO]: XCircle,
};

export function LeadsListPage() {
  const [filters, setFilters] = useState<LeadFilters & { page: number }>({
    page: 0,
  });
  const [showFilters, setShowFilters] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const { data: leadsData, isLoading, refetch } = useLeads(filters, filters.page, 20);
  const { data: stats, refetch: refetchStats } = useLeadStats();

  const handleRefresh = async () => {
    setIsRefreshing(true);
    toast.loading('Atualizando leads...', { id: 'refresh-leads' });
    try {
      await Promise.all([refetch(), refetchStats()]);
      toast.success('Leads atualizados com sucesso!', { id: 'refresh-leads' });
    } catch {
      toast.error('Erro ao atualizar leads', { id: 'refresh-leads' });
    } finally {
      setIsRefreshing(false);
    }
  };

  const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const nome = formData.get('nome') as string;
    const email = formData.get('email') as string;
    setFilters((prev) => ({ ...prev, nome, email, page: 0 }));
  };

  const handleFilterChange = (key: keyof LeadFilters, value: string | undefined) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value || undefined,
      page: 0,
    }));
  };

  const handlePageChange = (newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage }));
  };

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    toast.success(`${label} copiado!`);
  };

  const openWhatsApp = (phone: string) => {
    const cleanPhone = phone.replace(/\D/g, '');
    window.open(`https://wa.me/${cleanPhone}`, '_blank');
  };

  const formatDate = (date: string | null | undefined) => {
    if (!date) return '-';
    try {
      const parsed = new Date(date);
      if (isNaN(parsed.getTime())) return '-';
      return new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      }).format(parsed);
    } catch {
      return '-';
    }
  };

  const renderLeadRow = (lead: LeadResumo) => {
    const StatusIcon = statusIcons[lead.status] || Star;
    return (
      <tr key={lead.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
        <td className="px-4 sm:px-6 py-4">
          <div>
            <div className="text-sm font-medium text-gray-900 dark:text-white">
              {lead.nome}
            </div>
            <div className="text-xs text-gray-500 dark:text-gray-400 lg:hidden">
              {lead.email}
            </div>
          </div>
        </td>
        <td className="hidden lg:table-cell px-6 py-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2 text-sm text-gray-900 dark:text-gray-100">
              <Mail className="h-3 w-3 text-gray-400" />
              {lead.email}
            </div>
            <div className="flex items-center gap-2 text-sm text-gray-900 dark:text-gray-100">
              <Phone className="h-3 w-3 text-gray-400" />
              {lead.whatsapp}
            </div>
          </div>
        </td>
        <td className="px-4 sm:px-6 py-4">
          <span
            className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium ${statusColors[lead.status] || ''}`}
          >
            <StatusIcon className="h-3 w-3" />
            {statusLabels[lead.status] || lead.status}
          </span>
        </td>
        <td className="hidden md:table-cell px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
          {lead.origem}
        </td>
        <td className="hidden xl:table-cell px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
          {formatDate(lead.createdAt)}
        </td>
        <td className="px-4 sm:px-6 py-4 text-right">
          <div className="flex items-center justify-end gap-1 sm:gap-2">
            <button
              onClick={() => openWhatsApp(lead.whatsapp)}
              title="Abrir WhatsApp"
              className="rounded p-1 text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30"
            >
              <Phone className="h-4 w-4" />
            </button>
            <button
              onClick={() => copyToClipboard(lead.email, 'Email')}
              title="Copiar email"
              className="rounded p-1 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700"
            >
              <Mail className="h-4 w-4" />
            </button>
            <button
              onClick={() => copyToClipboard(lead.whatsapp, 'WhatsApp')}
              title="Copiar telefone"
              className="rounded p-1 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700"
            >
              <Copy className="h-4 w-4" />
            </button>
          </div>
        </td>
      </tr>
    );
  };

  return (
    <div className="min-h-screen bg-gray-50 p-4 sm:p-6 dark:bg-gray-900">
      <div className="mx-auto max-w-7xl">
        {/* Header */}
        <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
              Gestão de Leads
            </h1>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              Leads capturados de landing pages e formulários
            </p>
          </div>
          <button
            onClick={handleRefresh}
            disabled={isRefreshing || isLoading}
            className="flex items-center justify-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200"
          >
            <RefreshCw className={`h-4 w-4 transition-transform duration-500 ${isRefreshing || isLoading ? 'animate-spin' : ''}`} />
            {isRefreshing ? 'Atualizando...' : 'Atualizar'}
          </button>
        </div>

        {/* Stats Cards */}
        {stats && (
          <div className="mb-6 grid gap-3 grid-cols-2 sm:grid-cols-3 lg:grid-cols-6">
            <div className="rounded-lg bg-white dark:bg-gray-800 p-3 sm:p-4 shadow border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-2 sm:gap-3">
                <div className="rounded-lg bg-blue-100 dark:bg-blue-900/30 p-2">
                  <Star className="h-4 w-4 sm:h-5 sm:w-5 text-blue-600 dark:text-blue-400" />
                </div>
                <div>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Novos</p>
                  <p className="text-lg sm:text-xl font-bold text-gray-900 dark:text-white">
                    {stats.totalNovos}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white dark:bg-gray-800 p-3 sm:p-4 shadow border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-2 sm:gap-3">
                <div className="rounded-lg bg-yellow-100 dark:bg-yellow-900/30 p-2">
                  <Clock className="h-4 w-4 sm:h-5 sm:w-5 text-yellow-600 dark:text-yellow-400" />
                </div>
                <div>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Contatados</p>
                  <p className="text-lg sm:text-xl font-bold text-gray-900 dark:text-white">
                    {stats.totalContatados}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white dark:bg-gray-800 p-3 sm:p-4 shadow border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-2 sm:gap-3">
                <div className="rounded-lg bg-purple-100 dark:bg-purple-900/30 p-2">
                  <TrendingUp className="h-4 w-4 sm:h-5 sm:w-5 text-purple-600 dark:text-purple-400" />
                </div>
                <div>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Qualificados</p>
                  <p className="text-lg sm:text-xl font-bold text-gray-900 dark:text-white">
                    {stats.totalQualificados}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white dark:bg-gray-800 p-3 sm:p-4 shadow border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-2 sm:gap-3">
                <div className="rounded-lg bg-green-100 dark:bg-green-900/30 p-2">
                  <CheckCircle className="h-4 w-4 sm:h-5 sm:w-5 text-green-600 dark:text-green-400" />
                </div>
                <div>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Convertidos</p>
                  <p className="text-lg sm:text-xl font-bold text-gray-900 dark:text-white">
                    {stats.totalConvertidos}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white dark:bg-gray-800 p-3 sm:p-4 shadow border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-2 sm:gap-3">
                <div className="rounded-lg bg-red-100 dark:bg-red-900/30 p-2">
                  <XCircle className="h-4 w-4 sm:h-5 sm:w-5 text-red-600 dark:text-red-400" />
                </div>
                <div>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Perdidos</p>
                  <p className="text-lg sm:text-xl font-bold text-gray-900 dark:text-white">
                    {stats.totalPerdidos}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white dark:bg-gray-800 p-3 sm:p-4 shadow border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-2 sm:gap-3">
                <div className="rounded-lg bg-gray-100 dark:bg-gray-700 p-2">
                  <Users className="h-4 w-4 sm:h-5 sm:w-5 text-gray-600 dark:text-gray-400" />
                </div>
                <div>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Total</p>
                  <p className="text-lg sm:text-xl font-bold text-gray-900 dark:text-white">
                    {stats.totalGeral}
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Search and Filters */}
        <div className="mb-6 rounded-lg bg-white dark:bg-gray-800 p-4 shadow border border-gray-200 dark:border-gray-700">
          <div className="flex flex-col lg:flex-row lg:items-center gap-4">
            {/* Search */}
            <form onSubmit={handleSearch} className="flex-1">
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                  <input
                    type="text"
                    name="nome"
                    placeholder="Buscar por nome..."
                    defaultValue={filters.nome}
                    className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 pl-9 pr-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  />
                </div>
                <button
                  type="submit"
                  className="rounded-lg bg-blue-600 dark:bg-blue-700 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 dark:hover:bg-blue-600"
                >
                  Buscar
                </button>
              </div>
            </form>

            {/* Filters Toggle */}
            <button
              onClick={() => setShowFilters(!showFilters)}
              className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              <Filter className="h-4 w-4" />
              Filtros
            </button>
          </div>

          {/* Expanded Filters */}
          {showFilters && (
            <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-4 border-t border-gray-200 dark:border-gray-700 pt-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Status
                </label>
                <select
                  value={filters.status || ''}
                  onChange={(e) =>
                    handleFilterChange('status', e.target.value || undefined)
                  }
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-white focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                >
                  <option value="">Todos os status</option>
                  {Object.entries(statusLabels).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Origem
                </label>
                <input
                  type="text"
                  placeholder="Ex: landing-page, google-ads"
                  defaultValue={filters.origem}
                  onChange={(e) => handleFilterChange('origem', e.target.value)}
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
              </div>
            </div>
          )}
        </div>

        {/* Table */}
        <div className="overflow-hidden rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700">
          <div className="overflow-x-auto">
            <table className="w-full divide-y divide-gray-200 dark:divide-gray-700">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-4 sm:px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Lead
                  </th>
                  <th className="hidden lg:table-cell px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Contato
                  </th>
                  <th className="px-4 sm:px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Status
                  </th>
                  <th className="hidden md:table-cell px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Origem
                  </th>
                  <th className="hidden xl:table-cell px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Data
                  </th>
                  <th className="px-4 sm:px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Ações
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800">
                {isLoading ? (
                  <tr>
                    <td colSpan={6} className="px-6 py-8 text-center">
                      <div className="flex justify-center">
                        <RefreshCw className="h-6 w-6 animate-spin text-blue-600 dark:text-blue-400" />
                      </div>
                    </td>
                  </tr>
                ) : leadsData?.content.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-6 py-8 text-center text-sm text-gray-500 dark:text-gray-400">
                      Nenhum lead encontrado
                    </td>
                  </tr>
                ) : (
                  leadsData?.content.map(renderLeadRow)
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {leadsData && leadsData.totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 px-4 py-3 sm:px-6">
              <div className="flex flex-1 justify-between sm:hidden">
                <button
                  onClick={() => handlePageChange(filters.page - 1)}
                  disabled={filters.page === 0}
                  className="relative inline-flex items-center rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
                >
                  Anterior
                </button>
                <button
                  onClick={() => handlePageChange(filters.page + 1)}
                  disabled={filters.page === leadsData.totalPages - 1}
                  className="relative ml-3 inline-flex items-center rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
                >
                  Próxima
                </button>
              </div>
              <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm text-gray-700 dark:text-gray-300">
                    Mostrando{' '}
                    <span className="font-medium">
                      {filters.page * 20 + 1}
                    </span>{' '}
                    a{' '}
                    <span className="font-medium">
                      {Math.min((filters.page + 1) * 20, leadsData.totalElements)}
                    </span>{' '}
                    de{' '}
                    <span className="font-medium">{leadsData.totalElements}</span>{' '}
                    resultados
                  </p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => handlePageChange(filters.page - 1)}
                    disabled={filters.page === 0}
                    className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-1 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
                  >
                    Anterior
                  </button>
                  <span className="inline-flex items-center px-3 text-sm text-gray-700 dark:text-gray-300">
                    Página {filters.page + 1} de {leadsData.totalPages}
                  </span>
                  <button
                    onClick={() => handlePageChange(filters.page + 1)}
                    disabled={filters.page === leadsData.totalPages - 1}
                    className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-1 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
                  >
                    Próxima
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
