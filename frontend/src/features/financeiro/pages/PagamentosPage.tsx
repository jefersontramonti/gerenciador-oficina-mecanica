/**
 * Página principal de gerenciamento de pagamentos
 */

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { DollarSign, FilterX, ExternalLink, CheckCircle, XCircle, RotateCcw } from 'lucide-react';
import { usePagamentos, useConfirmarPagamento, useCancelarPagamento, useEstornarPagamento } from '../hooks/usePagamentos';
import { PaymentStatusBadge } from '../components/PaymentStatusBadge';
import {
  TipoPagamento,
  StatusPagamento,
  TipoPagamentoLabels,
  StatusPagamentoLabels,
  type FiltrosPagamento
} from '../types/pagamento';

const ITEMS_PER_PAGE = 20;

const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(value);
};

const formatDate = (dateString?: string | null): string => {
  if (!dateString) return '-';

  try {
    const date = new Date(dateString);
    // Verifica se a data é válida
    if (isNaN(date.getTime())) return '-';

    return format(date, 'dd/MM/yyyy', { locale: ptBR });
  } catch {
    return '-';
  }
};

export function PagamentosPage() {
  const [page, setPage] = useState(0);
  const [filtros, setFiltros] = useState<FiltrosPagamento>({});

  const { data, isLoading, error, isError } = usePagamentos(filtros, page, ITEMS_PER_PAGE);
  const confirmarMutation = useConfirmarPagamento();
  const cancelarMutation = useCancelarPagamento();
  const estornarMutation = useEstornarPagamento();

  const handleConfirmar = async (id: string) => {
    if (!window.confirm('Confirmar este pagamento como pago?')) return;

    const dataPagamento = new Date().toISOString().split('T')[0];
    await confirmarMutation.mutateAsync({ id, data: { dataPagamento } });
  };

  const handleCancelar = async (id: string) => {
    if (!window.confirm('Cancelar este pagamento?')) return;
    await cancelarMutation.mutateAsync(id);
  };

  const handleEstornar = async (id: string) => {
    if (!window.confirm('Estornar este pagamento? Esta ação é irreversível.')) return;
    await estornarMutation.mutateAsync(id);
  };

  const handleFiltroChange = (key: keyof FiltrosPagamento, value: any) => {
    setFiltros((prev) => ({ ...prev, [key]: value || undefined }));
    setPage(0);
  };

  const limparFiltros = () => {
    setFiltros({});
    setPage(0);
  };

  return (
    <div className="p-4 sm:p-6">
      {/* Error State */}
      {isError && (
        <div className="mb-6 rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400 dark:text-red-300">
          <p className="font-semibold">Erro ao carregar pagamentos</p>
          <p className="mt-1 text-sm">{error?.message || 'Tente novamente mais tarde.'}</p>
        </div>
      )}

      {/* Header */}
      <div className="mb-6">
        <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100">Pagamentos</h1>
        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
          {data?.totalElements || 0} pagamento(s) registrado(s)
        </p>
      </div>

      {/* Resumo cards */}
      {data && data.content && Array.isArray(data.content) && (
        <div className="mb-6 grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">
                <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400 truncate">
                  Total
                </p>
                <p className="mt-1 sm:mt-2 text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100">
                  {data.totalElements || 0}
                </p>
              </div>
              <div className="rounded-full bg-blue-100 dark:bg-blue-900/30 p-2 sm:p-3 shrink-0">
                <DollarSign className="h-5 w-5 sm:h-6 sm:w-6 text-blue-600 dark:text-blue-400" />
              </div>
            </div>
          </div>

          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">
                <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400">Pagos</p>
                <p className="mt-1 sm:mt-2 text-xl sm:text-2xl font-bold text-green-600 dark:text-green-400">
                  {data.content.filter((p) => p.status === 'PAGO').length}
                </p>
              </div>
              <div className="rounded-full bg-green-100 dark:bg-green-900/30 p-2 sm:p-3 shrink-0">
                <CheckCircle className="h-5 w-5 sm:h-6 sm:w-6 text-green-600 dark:text-green-400" />
              </div>
            </div>
          </div>

          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">
                <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400">Pendentes</p>
                <p className="mt-1 sm:mt-2 text-xl sm:text-2xl font-bold text-yellow-600 dark:text-yellow-400">
                  {data.content.filter((p) => p.status === 'PENDENTE').length}
                </p>
              </div>
              <div className="rounded-full bg-yellow-100 dark:bg-yellow-900/30 p-2 sm:p-3 shrink-0">
                <XCircle className="h-5 w-5 sm:h-6 sm:w-6 text-yellow-600 dark:text-yellow-400" />
              </div>
            </div>
          </div>

          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">
                <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400 truncate">Valor Total</p>
                <p className="mt-1 sm:mt-2 text-lg sm:text-2xl font-bold text-gray-900 dark:text-gray-100 truncate">
                  {formatCurrency(
                    data.content.reduce((sum, p) => sum + (p.valor || 0), 0)
                  )}
                </p>
              </div>
              <div className="rounded-full bg-purple-100 dark:bg-purple-900/30 p-2 sm:p-3 shrink-0">
                <DollarSign className="h-5 w-5 sm:h-6 sm:w-6 text-purple-600 dark:text-purple-400" />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Filtros */}
      <div className="mb-6 rounded-lg bg-white dark:bg-gray-800 p-4 shadow">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {/* Tipo */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Tipo
            </label>
            <select
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={filtros.tipo || ''}
              onChange={(e) =>
                handleFiltroChange(
                  'tipo',
                  e.target.value === '' ? undefined : (e.target.value as TipoPagamento)
                )
              }
            >
              <option value="">Todos</option>
              {Object.values(TipoPagamento).map((tipo) => (
                <option key={tipo} value={tipo}>
                  {TipoPagamentoLabels[tipo]}
                </option>
              ))}
            </select>
          </div>

          {/* Status */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Status
            </label>
            <select
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={filtros.status || ''}
              onChange={(e) =>
                handleFiltroChange(
                  'status',
                  e.target.value === '' ? undefined : (e.target.value as StatusPagamento)
                )
              }
            >
              <option value="">Todos</option>
              {Object.values(StatusPagamento).map((status) => (
                <option key={status} value={status}>
                  {StatusPagamentoLabels[status]}
                </option>
              ))}
            </select>
          </div>

          {/* Data Início */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Data Início
            </label>
            <input
              type="date"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={filtros.dataInicio || ''}
              onChange={(e) => handleFiltroChange('dataInicio', e.target.value || undefined)}
            />
          </div>

          {/* Data Fim */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Data Fim
            </label>
            <input
              type="date"
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={filtros.dataFim || ''}
              onChange={(e) => handleFiltroChange('dataFim', e.target.value || undefined)}
            />
          </div>
        </div>

        {/* Limpar Filtros */}
        {Object.keys(filtros).length > 0 && (
          <div className="mt-4 flex justify-end">
            <button
              onClick={limparFiltros}
              className="flex items-center gap-2 rounded-lg border border-orange-300 dark:border-orange-700 bg-orange-50 dark:bg-orange-900/30 px-4 py-2 text-orange-700 dark:text-orange-400 hover:bg-orange-100 dark:hover:bg-orange-900/50"
            >
              <FilterX className="h-4 w-4" />
              Limpar Filtros
            </button>
          </div>
        )}
      </div>

      {/* Loading State */}
      {isLoading && (
        <div className="flex justify-center items-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 dark:border-blue-400 border-t-transparent"></div>
        </div>
      )}

      {/* Empty State */}
      {!isLoading && (!data || data.content.length === 0) && (
        <div className="rounded-lg bg-white dark:bg-gray-800 p-8 shadow text-center">
          <p className="text-gray-500 dark:text-gray-400">Nenhum pagamento encontrado</p>
        </div>
      )}

      {/* Mobile: Card Layout */}
      {!isLoading && data && data.content.length > 0 && (
        <div className="space-y-3 lg:hidden">
          {data.content.map((pagamento) => (
            <div
              key={pagamento.id}
              className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow"
            >
              {/* Header: Valor e Status */}
              <div className="flex items-start justify-between gap-2 mb-3">
                <div>
                  <div className="text-lg font-bold text-gray-900 dark:text-white">
                    {formatCurrency(pagamento.valor)}
                  </div>
                  <div className="text-sm text-gray-500 dark:text-gray-400">
                    {TipoPagamentoLabels[pagamento.tipo]}
                  </div>
                </div>
                <PaymentStatusBadge status={pagamento.status} />
              </div>

              {/* Info */}
              <div className="grid grid-cols-2 gap-2 text-sm mb-3 pb-3 border-b border-gray-200 dark:border-gray-700">
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Data: </span>
                  <span className="text-gray-900 dark:text-gray-100">{formatDate(pagamento.createdAt)}</span>
                </div>
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Venc: </span>
                  <span className="text-gray-900 dark:text-gray-100">{formatDate(pagamento.dataVencimento)}</span>
                </div>
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Parcela: </span>
                  <span className="text-gray-900 dark:text-gray-100">{pagamento.parcelaAtual}/{pagamento.parcelas}</span>
                </div>
              </div>

              {/* Ações */}
              <div className="flex items-center justify-between gap-2">
                <Link
                  to={`/ordens-servico/${pagamento.ordemServicoId}`}
                  className="flex items-center gap-1 text-sm text-blue-600 dark:text-blue-400 hover:text-blue-800"
                >
                  Ver OS
                  <ExternalLink className="h-3 w-3" />
                </Link>
                <div className="flex gap-2">
                  {pagamento.status === 'PENDENTE' && (
                    <>
                      <button
                        onClick={() => handleConfirmar(pagamento.id)}
                        disabled={confirmarMutation.isPending}
                        className="flex items-center gap-1 rounded-lg bg-green-600 px-3 py-1.5 text-sm text-white hover:bg-green-700 disabled:opacity-50"
                      >
                        <CheckCircle className="h-4 w-4" />
                        Confirmar
                      </button>
                      <button
                        onClick={() => handleCancelar(pagamento.id)}
                        disabled={cancelarMutation.isPending}
                        className="rounded-lg border border-red-600 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/30 disabled:opacity-50"
                      >
                        <XCircle className="h-4 w-4" />
                      </button>
                    </>
                  )}
                  {pagamento.status === 'PAGO' && (
                    <button
                      onClick={() => handleEstornar(pagamento.id)}
                      disabled={estornarMutation.isPending}
                      className="flex items-center gap-1 rounded-lg border border-orange-600 px-3 py-1.5 text-sm text-orange-600 hover:bg-orange-50 dark:hover:bg-orange-900/30 disabled:opacity-50"
                    >
                      <RotateCcw className="h-4 w-4" />
                      Estornar
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Desktop: Table Layout */}
      {!isLoading && data && data.content.length > 0 && (
        <div className="hidden lg:block overflow-hidden rounded-lg bg-white dark:bg-gray-800 shadow">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    OS
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Data
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Tipo
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Valor
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Parcelas
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Vencimento
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Status
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Ações
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800">
                {data.content.map((pagamento) => (
                  <tr key={pagamento.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                    <td className="px-6 py-4 text-sm">
                      <Link
                        to={`/ordens-servico/${pagamento.ordemServicoId}`}
                        className="flex items-center gap-1 text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300"
                      >
                        Ver OS
                        <ExternalLink className="h-3 w-3" />
                      </Link>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900 dark:text-gray-100">
                      {formatDate(pagamento.createdAt)}
                    </td>
                    <td className="px-6 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">
                      {TipoPagamentoLabels[pagamento.tipo]}
                    </td>
                    <td className="px-6 py-4 text-sm font-semibold text-gray-900 dark:text-gray-100">
                      {formatCurrency(pagamento.valor)}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900 dark:text-gray-100">
                      {pagamento.parcelaAtual}/{pagamento.parcelas}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900 dark:text-gray-100">
                      {formatDate(pagamento.dataVencimento)}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      <PaymentStatusBadge status={pagamento.status} />
                    </td>
                    <td className="px-6 py-4 text-right text-sm">
                      <div className="flex justify-end gap-2">
                        {pagamento.status === 'PENDENTE' && (
                          <button
                            onClick={() => handleConfirmar(pagamento.id)}
                            disabled={confirmarMutation.isPending}
                            className="rounded p-1 text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30 disabled:opacity-50"
                            title="Confirmar pagamento"
                          >
                            <CheckCircle className="h-4 w-4" />
                          </button>
                        )}
                        {pagamento.status === 'PENDENTE' && (
                          <button
                            onClick={() => handleCancelar(pagamento.id)}
                            disabled={cancelarMutation.isPending}
                            className="rounded p-1 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30 disabled:opacity-50"
                            title="Cancelar pagamento"
                          >
                            <XCircle className="h-4 w-4" />
                          </button>
                        )}
                        {pagamento.status === 'PAGO' && (
                          <button
                            onClick={() => handleEstornar(pagamento.id)}
                            disabled={estornarMutation.isPending}
                            className="rounded p-1 text-orange-600 dark:text-orange-400 hover:bg-orange-50 dark:hover:bg-orange-900/30 disabled:opacity-50"
                            title="Estornar pagamento"
                          >
                            <RotateCcw className="h-4 w-4" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Paginação */}
      {data && data.totalPages > 1 && (
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between rounded-lg bg-white dark:bg-gray-800 px-4 sm:px-6 py-4 shadow">
          <div className="text-sm text-gray-700 dark:text-gray-300 text-center sm:text-left">
            Mostrando {page * ITEMS_PER_PAGE + 1} a{' '}
            {Math.min((page + 1) * ITEMS_PER_PAGE, data.totalElements)} de{' '}
            {data.totalElements} resultados
          </div>
          <div className="flex gap-2 justify-center sm:justify-end">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Anterior
            </button>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={page >= data.totalPages - 1}
              className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Próxima
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
