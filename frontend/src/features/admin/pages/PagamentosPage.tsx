/**
 * Pagamentos Management Page - Track subscription payments
 */

import { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import {
  RefreshCw,
  Calendar,
  CheckCircle,
  Clock,
  AlertTriangle,
} from 'lucide-react';
import { usePagamentos, usePagamentosPendentes, usePagamentosInadimplentes } from '../hooks/useSaas';
import { formatCurrency, formatDate } from '@/shared/utils/formatters';
import type { PagamentoFilters, StatusPagamento } from '../types';

export const PagamentosPage = () => {
  const [searchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState<'all' | 'pendentes' | 'inadimplentes'>(
    searchParams.get('status') === 'PENDENTE' ? 'pendentes' :
    searchParams.get('status') === 'ATRASADO' ? 'inadimplentes' : 'all'
  );

  const [filters, setFilters] = useState<PagamentoFilters>({
    oficinaId: searchParams.get('oficinaId') || undefined,
    page: 0,
    size: 20,
  });

  const { data: pagamentos, isLoading: loadingPagamentos } = usePagamentos(filters);
  const { data: pendentes, isLoading: loadingPendentes } = usePagamentosPendentes();
  const { data: inadimplentes, isLoading: loadingInadimplentes } = usePagamentosInadimplentes();

  const getStatusBadge = (status: StatusPagamento) => {
    const config: Record<StatusPagamento, { color: string; icon: any; label: string }> = {
      PAGO: {
        color: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
        icon: CheckCircle,
        label: 'Pago',
      },
      PENDENTE: {
        color: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
        icon: Clock,
        label: 'Pendente',
      },
      ATRASADO: {
        color: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
        icon: AlertTriangle,
        label: 'Atrasado',
      },
      CANCELADO: {
        color: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
        icon: Clock,
        label: 'Cancelado',
      },
    };

    const { color, icon: Icon, label } = config[status];

    return (
      <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${color}`}>
        <Icon className="h-3 w-3" />
        {label}
      </span>
    );
  };

  const tabs = [
    { id: 'all' as const, label: 'Todos', count: pagamentos?.totalElements || 0 },
    { id: 'pendentes' as const, label: 'Pendentes', count: pendentes?.totalElements || 0 },
    { id: 'inadimplentes' as const, label: 'Inadimplentes', count: inadimplentes?.totalElements || 0 },
  ];

  const isLoading = loadingPagamentos || loadingPendentes || loadingInadimplentes;

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          Pagamentos
        </h1>
        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
          Acompanhe os pagamentos das assinaturas
        </p>
      </div>

      {/* Stats Cards */}
      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-green-100 p-2 dark:bg-green-900/30">
              <CheckCircle className="h-5 w-5 text-green-600 dark:text-green-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Pagos</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {pagamentos?.content?.filter(p => p.status === 'PAGO').length || 0}
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-yellow-100 p-2 dark:bg-yellow-900/30">
              <Clock className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Pendentes</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {pendentes?.totalElements || 0}
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-red-100 p-2 dark:bg-red-900/30">
              <AlertTriangle className="h-5 w-5 text-red-600 dark:text-red-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Inadimplentes</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {inadimplentes?.totalElements || 0}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="mb-6 border-b border-gray-200 dark:border-gray-700">
        <nav className="-mb-px flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`whitespace-nowrap border-b-2 px-1 py-4 text-sm font-medium ${
                activeTab === tab.id
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              {tab.label}
              <span className={`ml-2 rounded-full px-2.5 py-0.5 text-xs ${
                activeTab === tab.id
                  ? 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400'
                  : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
              }`}>
                {tab.count}
              </span>
            </button>
          ))}
        </nav>
      </div>

      {/* Content based on active tab */}
      <div className="rounded-lg bg-white shadow dark:bg-gray-800">
        {activeTab === 'all' && (
          <>
            {/* Filters */}
            <div className="border-b border-gray-200 p-4 dark:border-gray-700">
              <div className="grid gap-4 md:grid-cols-4">
                <div className="relative">
                  <Calendar className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                  <input
                    type="date"
                    onChange={(e) => setFilters(prev => ({ ...prev, dataInicio: e.target.value, page: 0 }))}
                    className="w-full rounded-lg border border-gray-300 bg-white py-2 pl-10 pr-4 text-gray-900 focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    placeholder="Data início"
                  />
                </div>
                <div className="relative">
                  <Calendar className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                  <input
                    type="date"
                    onChange={(e) => setFilters(prev => ({ ...prev, dataFim: e.target.value, page: 0 }))}
                    className="w-full rounded-lg border border-gray-300 bg-white py-2 pl-10 pr-4 text-gray-900 focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    placeholder="Data fim"
                  />
                </div>
              </div>
            </div>

            {/* Table */}
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                      Oficina
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                      Valor
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                      Vencimento
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                      Pagamento
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                      Status
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                  {isLoading ? (
                    <tr>
                      <td colSpan={5} className="px-6 py-12 text-center">
                        <RefreshCw className="mx-auto h-8 w-8 animate-spin text-gray-400" />
                      </td>
                    </tr>
                  ) : pagamentos?.content && pagamentos.content.length > 0 ? (
                    pagamentos.content.map((pagamento) => (
                      <tr key={pagamento.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                        <td className="whitespace-nowrap px-6 py-4">
                          <Link
                            to={`/admin/oficinas/${pagamento.oficinaId}`}
                            className="font-medium text-blue-600 hover:underline dark:text-blue-400"
                          >
                            {pagamento.oficinaNome}
                          </Link>
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 font-medium text-gray-900 dark:text-white">
                          {formatCurrency(pagamento.valor)}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 text-gray-500 dark:text-gray-400">
                          {formatDate(pagamento.dataVencimento)}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 text-gray-500 dark:text-gray-400">
                          {pagamento.dataPagamento ? formatDate(pagamento.dataPagamento) : '-'}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4">
                          {getStatusBadge(pagamento.status)}
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={5} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                        Nenhum pagamento encontrado
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </>
        )}

        {activeTab === 'pendentes' && (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                    Oficina
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                    Mensalidade
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                    Vencimento
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                    Status
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {loadingPendentes ? (
                  <tr>
                    <td colSpan={4} className="px-6 py-12 text-center">
                      <RefreshCw className="mx-auto h-8 w-8 animate-spin text-gray-400" />
                    </td>
                  </tr>
                ) : pendentes?.content && pendentes.content.length > 0 ? (
                  pendentes.content.map((oficina) => (
                    <tr key={oficina.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                      <td className="whitespace-nowrap px-6 py-4">
                        <Link
                          to={`/admin/oficinas/${oficina.id}`}
                          className="font-medium text-blue-600 hover:underline dark:text-blue-400"
                        >
                          {oficina.nomeFantasia}
                        </Link>
                        <p className="text-sm text-gray-500 dark:text-gray-400">{oficina.email}</p>
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 font-medium text-gray-900 dark:text-white">
                        {formatCurrency(oficina.valorMensalidade)}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-gray-500 dark:text-gray-400">
                        {formatDate(oficina.dataVencimentoPlano)}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4">
                        <span className="inline-flex items-center gap-1 rounded-full bg-yellow-100 px-2.5 py-0.5 text-xs font-medium text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400">
                          <Clock className="h-3 w-3" />
                          Pendente
                        </span>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={4} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                      Nenhum pagamento pendente
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {activeTab === 'inadimplentes' && (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                    Oficina
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                    Mensalidade
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                    Vencido em
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                    Status
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {loadingInadimplentes ? (
                  <tr>
                    <td colSpan={4} className="px-6 py-12 text-center">
                      <RefreshCw className="mx-auto h-8 w-8 animate-spin text-gray-400" />
                    </td>
                  </tr>
                ) : inadimplentes?.content && inadimplentes.content.length > 0 ? (
                  inadimplentes.content.map((oficina) => (
                    <tr key={oficina.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                      <td className="whitespace-nowrap px-6 py-4">
                        <Link
                          to={`/admin/oficinas/${oficina.id}`}
                          className="font-medium text-blue-600 hover:underline dark:text-blue-400"
                        >
                          {oficina.nomeFantasia}
                        </Link>
                        <p className="text-sm text-gray-500 dark:text-gray-400">{oficina.email}</p>
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 font-medium text-gray-900 dark:text-white">
                        {formatCurrency(oficina.valorMensalidade)}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-red-600 dark:text-red-400">
                        {formatDate(oficina.dataVencimentoPlano)}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4">
                        <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-800 dark:bg-red-900/30 dark:text-red-400">
                          <AlertTriangle className="h-3 w-3" />
                          Inadimplente
                        </span>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={4} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                      Nenhuma oficina inadimplente
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
