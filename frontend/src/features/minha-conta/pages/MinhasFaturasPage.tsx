/**
 * Página de listagem de faturas da oficina
 */

import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import {
  FileText,
  Download,
  CreditCard,
  AlertCircle,
  Clock,
  CheckCircle,
  XCircle,
  FilterX,
  ArrowLeft,
  Eye,
} from 'lucide-react';
import { useMinhasFaturas, useDownloadFaturaPdf } from '../hooks/useMinhaContaFaturas';
import {
  StatusFatura,
  StatusFaturaLabels,
  StatusFaturaCores,
  type FiltrosFatura,
} from '../types/fatura';

const ITEMS_PER_PAGE = 12;

const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatDate = (dateString?: string): string => {
  if (!dateString) return '-';
  try {
    return format(new Date(dateString), 'dd/MM/yyyy', { locale: ptBR });
  } catch {
    return '-';
  }
};

const getStatusIcon = (status: StatusFatura) => {
  switch (status) {
    case StatusFatura.PENDENTE:
      return <Clock className="h-4 w-4" />;
    case StatusFatura.VENCIDO:
      return <AlertCircle className="h-4 w-4" />;
    case StatusFatura.PAGO:
      return <CheckCircle className="h-4 w-4" />;
    case StatusFatura.CANCELADO:
      return <XCircle className="h-4 w-4" />;
    default:
      return null;
  }
};

export function MinhasFaturasPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialStatus = searchParams.get('status') as StatusFatura | null;

  const [page, setPage] = useState(0);
  const [filtros, setFiltros] = useState<FiltrosFatura>({
    status: initialStatus || undefined,
  });

  const { data, isLoading, error } = useMinhasFaturas(filtros, page, ITEMS_PER_PAGE);
  const downloadPdfMutation = useDownloadFaturaPdf();

  const handleFiltroChange = (status: StatusFatura | '') => {
    const newFiltros = { ...filtros, status: status || undefined };
    setFiltros(newFiltros);
    setPage(0);
    if (status) {
      searchParams.set('status', status);
    } else {
      searchParams.delete('status');
    }
    setSearchParams(searchParams);
  };

  const limparFiltros = () => {
    setFiltros({});
    setPage(0);
    setSearchParams({});
  };

  const handleDownloadPdf = (faturaId: string) => {
    downloadPdfMutation.mutate(faturaId);
  };

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            to="/minha-conta"
            className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-600 dark:text-gray-400"
          >
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
              Minhas Faturas
            </h1>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              {data?.totalElements || 0} fatura(s) encontrada(s)
            </p>
          </div>
        </div>
      </div>

      {/* Error State */}
      {error && (
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400">
          <p className="font-semibold">Erro ao carregar faturas</p>
          <p className="mt-1 text-sm">{error?.message || 'Tente novamente mais tarde.'}</p>
        </div>
      )}

      {/* Filtros */}
      <div className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow border border-gray-200 dark:border-gray-700">
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => handleFiltroChange('')}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
              !filtros.status
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
            }`}
          >
            Todas
          </button>
          {Object.values(StatusFatura).map((status) => (
            <button
              key={status}
              onClick={() => handleFiltroChange(status)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                filtros.status === status
                  ? `${StatusFaturaCores[status].bg} ${StatusFaturaCores[status].text}`
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
              }`}
            >
              {getStatusIcon(status)}
              {StatusFaturaLabels[status]}
            </button>
          ))}

          {filtros.status && (
            <button
              onClick={limparFiltros}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium bg-orange-100 dark:bg-orange-900/30 text-orange-700 dark:text-orange-400 hover:bg-orange-200 dark:hover:bg-orange-900/50"
            >
              <FilterX className="h-4 w-4" />
              Limpar
            </button>
          )}
        </div>
      </div>

      {/* Loading State */}
      {isLoading && (
        <div className="flex justify-center items-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 dark:border-blue-400 border-t-transparent"></div>
        </div>
      )}

      {/* Empty State */}
      {!isLoading && (!data || data.content.length === 0) && (
        <div className="rounded-lg bg-white dark:bg-gray-800 p-8 shadow border border-gray-200 dark:border-gray-700 text-center">
          <FileText className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-500 dark:text-gray-400">Nenhuma fatura encontrada</p>
        </div>
      )}

      {/* Mobile: Card Layout */}
      {!isLoading && data && data.content.length > 0 && (
        <div className="space-y-3 lg:hidden">
          {data.content.map((fatura) => (
            <div
              key={fatura.id}
              className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow border border-gray-200 dark:border-gray-700"
            >
              {/* Header */}
              <div className="flex items-start justify-between gap-2 mb-3">
                <div>
                  <div className="text-lg font-bold text-gray-900 dark:text-white">
                    {fatura.numero}
                  </div>
                  <div className="text-sm text-gray-500 dark:text-gray-400">
                    {fatura.mesReferenciaFormatado}
                  </div>
                </div>
                <span
                  className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                    StatusFaturaCores[fatura.status as StatusFatura].bg
                  } ${StatusFaturaCores[fatura.status as StatusFatura].text}`}
                >
                  {getStatusIcon(fatura.status as StatusFatura)}
                  {StatusFaturaLabels[fatura.status as StatusFatura]}
                </span>
              </div>

              {/* Info */}
              <div className="grid grid-cols-2 gap-2 text-sm mb-3 pb-3 border-b border-gray-200 dark:border-gray-700">
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Valor: </span>
                  <span className="font-semibold text-gray-900 dark:text-white">
                    {formatCurrency(fatura.valorTotal)}
                  </span>
                </div>
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Venc: </span>
                  <span className="text-gray-900 dark:text-gray-100">
                    {formatDate(fatura.dataVencimento)}
                  </span>
                </div>
                {fatura.dataPagamento && (
                  <div className="col-span-2">
                    <span className="text-gray-500 dark:text-gray-400">Pago em: </span>
                    <span className="text-gray-900 dark:text-gray-100">
                      {formatDate(fatura.dataPagamento)}
                    </span>
                  </div>
                )}
              </div>

              {/* Dias até vencimento */}
              {fatura.diasAteVencimento !== null && fatura.pagavel && (
                <p
                  className={`text-sm font-medium mb-3 ${
                    fatura.diasAteVencimento < 0
                      ? 'text-red-600 dark:text-red-400'
                      : fatura.diasAteVencimento <= 3
                      ? 'text-yellow-600 dark:text-yellow-400'
                      : 'text-gray-600 dark:text-gray-400'
                  }`}
                >
                  {fatura.diasAteVencimento < 0
                    ? `Vencida há ${Math.abs(fatura.diasAteVencimento)} dia(s)`
                    : fatura.diasAteVencimento === 0
                    ? 'Vence hoje'
                    : `Vence em ${fatura.diasAteVencimento} dia(s)`}
                </p>
              )}

              {/* Ações */}
              <div className="flex items-center gap-2">
                <Link
                  to={`/minha-conta/faturas/${fatura.id}`}
                  className="flex-1 flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600 text-sm font-medium"
                >
                  <Eye className="h-4 w-4" />
                  Ver Detalhes
                </Link>
                {fatura.pagavel && (
                  <Link
                    to={`/minha-conta/faturas/${fatura.id}`}
                    className="flex-1 flex items-center justify-center gap-1.5 px-3 py-2 rounded-lg bg-green-600 dark:bg-green-700 text-white hover:bg-green-700 dark:hover:bg-green-600 text-sm font-medium"
                  >
                    <CreditCard className="h-4 w-4" />
                    Pagar
                  </Link>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Desktop: Table Layout */}
      {!isLoading && data && data.content.length > 0 && (
        <div className="hidden lg:block rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Fatura
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Referência
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                    Valor
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
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {data.content.map((fatura) => (
                  <tr
                    key={fatura.id}
                    className="hover:bg-gray-50 dark:hover:bg-gray-700/50"
                  >
                    <td className="px-6 py-4 text-sm font-medium text-gray-900 dark:text-white">
                      {fatura.numero}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600 dark:text-gray-400">
                      {fatura.mesReferenciaFormatado}
                    </td>
                    <td className="px-6 py-4 text-sm font-semibold text-gray-900 dark:text-white">
                      {formatCurrency(fatura.valorTotal)}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      <div className="text-gray-900 dark:text-gray-100">
                        {formatDate(fatura.dataVencimento)}
                      </div>
                      {fatura.diasAteVencimento !== null && fatura.pagavel && (
                        <div
                          className={`text-xs ${
                            fatura.diasAteVencimento < 0
                              ? 'text-red-600 dark:text-red-400'
                              : fatura.diasAteVencimento <= 3
                              ? 'text-yellow-600 dark:text-yellow-400'
                              : 'text-gray-500 dark:text-gray-400'
                          }`}
                        >
                          {fatura.diasAteVencimento < 0
                            ? `${Math.abs(fatura.diasAteVencimento)}d atraso`
                            : fatura.diasAteVencimento === 0
                            ? 'Vence hoje'
                            : `${fatura.diasAteVencimento}d restantes`}
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      <span
                        className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          StatusFaturaCores[fatura.status as StatusFatura].bg
                        } ${StatusFaturaCores[fatura.status as StatusFatura].text}`}
                      >
                        {getStatusIcon(fatura.status as StatusFatura)}
                        {StatusFaturaLabels[fatura.status as StatusFatura]}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Link
                          to={`/minha-conta/faturas/${fatura.id}`}
                          className="p-1.5 rounded text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700"
                          title="Ver detalhes"
                        >
                          <Eye className="h-4 w-4" />
                        </Link>
                        <button
                          onClick={() => handleDownloadPdf(fatura.id)}
                          disabled={downloadPdfMutation.isPending}
                          className="p-1.5 rounded text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 disabled:opacity-50"
                          title="Download PDF"
                        >
                          <Download className="h-4 w-4" />
                        </button>
                        {fatura.pagavel && (
                          <Link
                            to={`/minha-conta/faturas/${fatura.id}`}
                            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-green-600 dark:bg-green-700 text-white hover:bg-green-700 dark:hover:bg-green-600 text-xs font-medium"
                          >
                            <CreditCard className="h-3.5 w-3.5" />
                            Pagar
                          </Link>
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
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between rounded-lg bg-white dark:bg-gray-800 px-4 sm:px-6 py-4 shadow border border-gray-200 dark:border-gray-700">
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
