import { useState } from 'react';
import { History, Mail, MessageSquare, Smartphone, Send, Clock, CheckCircle, XCircle, Ban, Eye, Bell, Search } from 'lucide-react';
import { useNotificacoes } from '../hooks/useNotificacoes';
import { NotificacaoDetailModal } from '../components/NotificacaoDetailModal';
import type { NotificacaoFilters, TipoNotificacao, StatusNotificacao } from '../types';

export function HistoricoNotificacoesPage() {
  const [filters, setFilters] = useState<NotificacaoFilters>({
    page: 0,
    size: 20,
    sort: 'createdAt,desc',
  });

  const [searchTerm, setSearchTerm] = useState('');
  const [selectedNotificacaoId, setSelectedNotificacaoId] = useState<string | null>(null);

  const { data, isLoading, error } = useNotificacoes(filters);

  const handleFilterChange = (key: keyof NotificacaoFilters, value: any) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value || undefined,
      page: 0,
    }));
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleFilterChange('destinatario', searchTerm || undefined);
  };

  const handlePageChange = (newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage }));
  };

  const getTipoIcon = (tipo: TipoNotificacao) => {
    switch (tipo) {
      case 'EMAIL':
        return Mail;
      case 'WHATSAPP':
        return MessageSquare;
      case 'SMS':
        return Smartphone;
      case 'TELEGRAM':
        return Bell;
      default:
        return Send;
    }
  };

  const getTipoLabel = (tipo: TipoNotificacao) => {
    switch (tipo) {
      case 'EMAIL': return 'Email';
      case 'WHATSAPP': return 'WhatsApp';
      case 'SMS': return 'SMS';
      case 'TELEGRAM': return 'Telegram';
      default: return tipo;
    }
  };

  const getStatusConfig = (status: StatusNotificacao) => {
    switch (status) {
      case 'ENVIADO':
        return {
          icon: CheckCircle,
          color: 'text-green-600 dark:text-green-400',
          bgColor: 'bg-green-100 dark:bg-green-900/30',
          label: 'Enviado',
        };
      case 'ENTREGUE':
        return {
          icon: CheckCircle,
          color: 'text-green-700 dark:text-green-400',
          bgColor: 'bg-green-100 dark:bg-green-900/30',
          label: 'Entregue',
        };
      case 'LIDO':
        return {
          icon: CheckCircle,
          color: 'text-blue-600 dark:text-blue-400',
          bgColor: 'bg-blue-100 dark:bg-blue-900/30',
          label: 'Lido',
        };
      case 'PENDENTE':
        return {
          icon: Clock,
          color: 'text-yellow-600 dark:text-yellow-400',
          bgColor: 'bg-yellow-100 dark:bg-yellow-900/30',
          label: 'Pendente',
        };
      case 'AGENDADO':
        return {
          icon: Clock,
          color: 'text-purple-600 dark:text-purple-400',
          bgColor: 'bg-purple-100 dark:bg-purple-900/30',
          label: 'Agendado',
        };
      case 'FALHA':
        return {
          icon: XCircle,
          color: 'text-red-600 dark:text-red-400',
          bgColor: 'bg-red-100 dark:bg-red-900/30',
          label: 'Falha',
        };
      case 'CANCELADO':
        return {
          icon: Ban,
          color: 'text-gray-600 dark:text-gray-400',
          bgColor: 'bg-gray-100 dark:bg-gray-700',
          label: 'Cancelado',
        };
      default:
        return {
          icon: Clock,
          color: 'text-gray-600 dark:text-gray-400',
          bgColor: 'bg-gray-100 dark:bg-gray-700',
          label: status,
        };
    }
  };

  const formatDate = (date: string | number[] | undefined) => {
    if (!date) return '-';

    const dateObj = Array.isArray(date)
      ? new Date(date[0], date[1] - 1, date[2], date[3] || 0, date[4] || 0)
      : new Date(date);

    return dateObj.toLocaleString('pt-BR');
  };

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Header */}
      <div className="mb-6 flex items-center gap-3">
        <History className="h-6 w-6 sm:h-8 sm:w-8 text-blue-600 dark:text-blue-400" />
        <div>
          <h1 className="text-xl sm:text-2xl lg:text-3xl font-bold text-gray-900 dark:text-white">Histórico de Notificações</h1>
          <p className="text-xs sm:text-sm text-gray-500 dark:text-gray-400">Visualize todas as notificações enviadas</p>
        </div>
      </div>

      {/* Filters */}
      <div className="mb-6 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 sm:p-6 shadow-sm">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {/* Search */}
          <div className="lg:col-span-2">
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Buscar destinatário
            </label>
            <form onSubmit={handleSearchSubmit} className="flex gap-2">
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Email, telefone..."
                className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 px-3 py-2 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
              <button
                type="submit"
                className="rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 transition-colors"
              >
                <Search className="h-4 w-4" />
              </button>
            </form>
          </div>

          {/* Tipo */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Canal
            </label>
            <select
              value={filters.tipo || ''}
              onChange={(e) => handleFilterChange('tipo', e.target.value)}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            >
              <option value="">Todos</option>
              <option value="WHATSAPP">WhatsApp</option>
              <option value="TELEGRAM">Telegram</option>
              <option value="EMAIL">Email</option>
              <option value="SMS">SMS</option>
            </select>
          </div>

          {/* Status */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Status
            </label>
            <select
              value={filters.status || ''}
              onChange={(e) => handleFilterChange('status', e.target.value)}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            >
              <option value="">Todos</option>
              <option value="ENVIADO">Enviado</option>
              <option value="ENTREGUE">Entregue</option>
              <option value="LIDO">Lido</option>
              <option value="PENDENTE">Pendente</option>
              <option value="FALHA">Falha</option>
              <option value="AGENDADO">Agendado</option>
              <option value="CANCELADO">Cancelado</option>
            </select>
          </div>

          {/* Evento */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Evento
            </label>
            <select
              value={filters.evento || ''}
              onChange={(e) => handleFilterChange('evento', e.target.value)}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            >
              <option value="">Todos</option>
              <option value="TESTE">Teste</option>
              <option value="OS_CRIADA">OS Criada</option>
              <option value="OS_AGUARDANDO_APROVACAO">Aguardando Aprovação</option>
              <option value="OS_APROVADA">OS Aprovada</option>
              <option value="OS_EM_ANDAMENTO">Em Andamento</option>
              <option value="OS_AGUARDANDO_PECA">Aguardando Peça</option>
              <option value="OS_FINALIZADA">OS Finalizada</option>
              <option value="OS_ENTREGUE">OS Entregue</option>
              <option value="PAGAMENTO_PENDENTE">Pagamento Pendente</option>
              <option value="PAGAMENTO_CONFIRMADO">Pagamento Confirmado</option>
              <option value="LEMBRETE_RETIRADA">Lembrete Retirada</option>
              <option value="LEMBRETE_REVISAO">Lembrete Revisão</option>
            </select>
          </div>
        </div>

        {/* Active filters */}
        {(filters.tipo || filters.status || filters.evento || filters.destinatario) && (
          <div className="mt-4 flex flex-wrap items-center gap-2">
            <span className="text-sm text-gray-500 dark:text-gray-400">Filtros ativos:</span>
            {filters.tipo && (
              <span className="inline-flex items-center gap-1 rounded-full bg-blue-100 dark:bg-blue-900/30 px-2 py-1 text-xs font-medium text-blue-700 dark:text-blue-400">
                {getTipoLabel(filters.tipo as TipoNotificacao)}
                <button onClick={() => handleFilterChange('tipo', undefined)} className="hover:text-blue-900 dark:hover:text-blue-200">×</button>
              </span>
            )}
            {filters.status && (
              <span className="inline-flex items-center gap-1 rounded-full bg-green-100 dark:bg-green-900/30 px-2 py-1 text-xs font-medium text-green-700 dark:text-green-400">
                {filters.status}
                <button onClick={() => handleFilterChange('status', undefined)} className="hover:text-green-900 dark:hover:text-green-200">×</button>
              </span>
            )}
            {filters.evento && (
              <span className="inline-flex items-center gap-1 rounded-full bg-purple-100 dark:bg-purple-900/30 px-2 py-1 text-xs font-medium text-purple-700 dark:text-purple-400">
                {filters.evento.replace(/_/g, ' ')}
                <button onClick={() => handleFilterChange('evento', undefined)} className="hover:text-purple-900 dark:hover:text-purple-200">×</button>
              </span>
            )}
            {filters.destinatario && (
              <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 dark:bg-gray-700 px-2 py-1 text-xs font-medium text-gray-700 dark:text-gray-300">
                "{filters.destinatario}"
                <button onClick={() => { handleFilterChange('destinatario', undefined); setSearchTerm(''); }} className="hover:text-gray-900 dark:hover:text-gray-100">×</button>
              </span>
            )}
            <button
              onClick={() => {
                setFilters({ page: 0, size: 20, sort: 'createdAt,desc' });
                setSearchTerm('');
              }}
              className="text-xs text-red-600 dark:text-red-400 hover:underline"
            >
              Limpar todos
            </button>
          </div>
        )}
      </div>

      {/* Loading State */}
      {isLoading && (
        <div className="flex items-center justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 dark:border-gray-700 border-t-blue-600 dark:border-t-blue-400" />
        </div>
      )}

      {/* Error State */}
      {!isLoading && error && (
        <div className="rounded-xl border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20 p-4 text-center text-red-600 dark:text-red-400">
          Erro ao carregar notificações. Tente novamente.
        </div>
      )}

      {/* Empty State */}
      {!isLoading && !error && (!data?.content || data.content.length === 0) && (
        <div className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-8 text-center text-gray-500 dark:text-gray-400">
          Nenhuma notificação encontrada
        </div>
      )}

      {/* Mobile: Card Layout */}
      {!isLoading && !error && data?.content && data.content.length > 0 && (
        <div className="space-y-3 lg:hidden">
          {data.content.map((notificacao) => {
            const TipoIcon = getTipoIcon(notificacao.tipo);
            const statusConfig = getStatusConfig(notificacao.status);
            const StatusIcon = statusConfig.icon;

            return (
              <div
                key={notificacao.id}
                className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4"
              >
                {/* Header: Canal e Status */}
                <div className="flex items-start justify-between gap-2 mb-3">
                  <div className="flex items-center gap-2">
                    <TipoIcon className="h-5 w-5 text-gray-500 dark:text-gray-400" />
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {getTipoLabel(notificacao.tipo)}
                    </span>
                  </div>
                  <div
                    className={`inline-flex items-center gap-1 rounded-full ${statusConfig.bgColor} px-2.5 py-1`}
                  >
                    <StatusIcon className={`h-3 w-3 ${statusConfig.color}`} />
                    <span className={`text-xs font-medium ${statusConfig.color}`}>
                      {statusConfig.label}
                    </span>
                  </div>
                </div>

                {/* Destinatário */}
                <div className="mb-2">
                  {notificacao.nomeDestinatario || notificacao.nomeCliente ? (
                    <>
                      <div className="text-sm font-medium text-gray-900 dark:text-white">
                        {notificacao.nomeDestinatario || notificacao.nomeCliente}
                      </div>
                      <div className="font-mono text-xs text-gray-500 dark:text-gray-400">
                        {notificacao.destinatario}
                      </div>
                    </>
                  ) : (
                    <div className="font-mono text-sm text-gray-900 dark:text-white">
                      {notificacao.destinatario}
                    </div>
                  )}
                </div>

                {/* Evento e Data */}
                <div className="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400 mb-3 pb-3 border-b border-gray-200 dark:border-gray-700">
                  <span>{notificacao.evento?.replace(/_/g, ' ')}</span>
                  <span>{formatDate(notificacao.dataEnvio || notificacao.createdAt)}</span>
                </div>

                {/* Ação */}
                <button
                  onClick={() => setSelectedNotificacaoId(notificacao.id)}
                  className="w-full flex items-center justify-center gap-2 rounded-lg border border-blue-600 dark:border-blue-500 px-3 py-2 text-sm font-medium text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/30 transition-colors"
                >
                  <Eye className="h-4 w-4" />
                  Ver Detalhes
                </button>
              </div>
            );
          })}
        </div>
      )}

      {/* Desktop: Table Layout */}
      {!isLoading && !error && data?.content && data.content.length > 0 && (
        <div className="hidden lg:block overflow-hidden rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full divide-y divide-gray-200 dark:divide-gray-700">
              <thead className="bg-gray-50 dark:bg-gray-900">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Canal
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Destinatário
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Evento
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Data
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Ações
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800">
                {data.content.map((notificacao) => {
                  const TipoIcon = getTipoIcon(notificacao.tipo);
                  const statusConfig = getStatusConfig(notificacao.status);
                  const StatusIcon = statusConfig.icon;

                  return (
                    <tr key={notificacao.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                      <td className="whitespace-nowrap px-6 py-4">
                        <div className="flex items-center gap-2">
                          <TipoIcon className="h-4 w-4 text-gray-500 dark:text-gray-400" />
                          <span className="text-sm text-gray-900 dark:text-white">
                            {getTipoLabel(notificacao.tipo)}
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex flex-col">
                          {notificacao.nomeDestinatario || notificacao.nomeCliente ? (
                            <>
                              <span className="text-sm font-medium text-gray-900 dark:text-white">
                                {notificacao.nomeDestinatario || notificacao.nomeCliente}
                              </span>
                              <span className="font-mono text-xs text-gray-500 dark:text-gray-400">
                                {notificacao.destinatario}
                              </span>
                            </>
                          ) : (
                            <span className="font-mono text-sm text-gray-900 dark:text-white">
                              {notificacao.destinatario}
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <span className="text-sm text-gray-600 dark:text-gray-400">
                          {notificacao.evento?.replace(/_/g, ' ')}
                        </span>
                      </td>
                      <td className="whitespace-nowrap px-6 py-4">
                        <div
                          className={`inline-flex items-center gap-1 rounded-full ${statusConfig.bgColor} px-2.5 py-1`}
                        >
                          <StatusIcon className={`h-3 w-3 ${statusConfig.color}`} />
                          <span className={`text-xs font-medium ${statusConfig.color}`}>
                            {statusConfig.label}
                          </span>
                        </div>
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-900 dark:text-white">
                        {formatDate(notificacao.dataEnvio || notificacao.createdAt)}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4">
                        <button
                          onClick={() => setSelectedNotificacaoId(notificacao.id)}
                          className="flex items-center gap-1 text-sm text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 transition-colors"
                        >
                          <Eye className="h-4 w-4" />
                          Detalhes
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 px-4 sm:px-6 py-4">
          <div className="text-sm text-gray-700 dark:text-gray-300 text-center sm:text-left">
            Mostrando <span className="font-medium">{data.content.length}</span> de{' '}
            <span className="font-medium">{data.totalElements}</span> notificações
          </div>
          <div className="flex gap-2 justify-center sm:justify-end">
            <button
              onClick={() => handlePageChange((filters.page || 0) - 1)}
              disabled={filters.page === 0}
              className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 transition-colors hover:bg-gray-50 dark:hover:bg-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Anterior
            </button>
            <span className="hidden sm:flex items-center px-4 text-sm text-gray-700 dark:text-gray-300">
              Página {(filters.page || 0) + 1} de {data.totalPages}
            </span>
            <button
              onClick={() => handlePageChange((filters.page || 0) + 1)}
              disabled={(filters.page || 0) + 1 >= data.totalPages}
              className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 transition-colors hover:bg-gray-50 dark:hover:bg-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Próxima
            </button>
          </div>
        </div>
      )}

      {/* Detail Modal */}
      {selectedNotificacaoId && (
        <NotificacaoDetailModal
          isOpen={!!selectedNotificacaoId}
          onClose={() => setSelectedNotificacaoId(null)}
          notificacaoId={selectedNotificacaoId}
        />
      )}
    </div>
  );
}
