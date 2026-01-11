/**
 * Audit Logs Page - View system audit trail
 */

import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  RefreshCw,
  Calendar,
  Download,
  User,
  Building2,
  Activity,
} from 'lucide-react';
import { useAuditLogs, useExportAudit } from '../hooks/useSaas';
import { formatDateTime } from '@/shared/utils/formatters';
import { showSuccess, showError } from '@/shared/utils/notifications';
import type { AuditFilters } from '../types';

export const AuditPage = () => {
  const [searchParams] = useSearchParams();
  const [filters, setFilters] = useState<AuditFilters>({
    acao: searchParams.get('acao') || undefined,
    entidade: searchParams.get('entidade') || undefined,
    page: 0,
    size: 50,
  });

  const { data, isLoading, error } = useAuditLogs(filters);
  const exportMutation = useExportAudit();

  const handleExport = async () => {
    try {
      const blob = await exportMutation.mutateAsync(filters);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `audit-logs-${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      showSuccess('Logs exportados com sucesso!');
    } catch {
      showError('Erro ao exportar logs');
    }
  };

  const getActionBadge = (acao: string) => {
    const actionColors: Record<string, string> = {
      CRIAR: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
      EDITAR: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
      ATIVAR: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400',
      SUSPENDER: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
      CANCELAR: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
      LOGIN: 'bg-indigo-100 text-indigo-800 dark:bg-indigo-900/30 dark:text-indigo-400',
      LOGOUT: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
      ALTERAR_PLANO: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400',
      REGISTRAR_PAGAMENTO: 'bg-cyan-100 text-cyan-800 dark:bg-cyan-900/30 dark:text-cyan-400',
    };

    const color = Object.entries(actionColors).find(([key]) => acao.includes(key))?.[1]
      || 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300';

    return (
      <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${color}`}>
        {acao.replace(/_/g, ' ')}
      </span>
    );
  };

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-300 bg-red-50 p-4 text-red-800 dark:border-red-800 dark:bg-red-900/20 dark:text-red-400">
          Erro ao carregar logs de auditoria. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Logs de Auditoria
          </h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            {data?.totalElements || 0} registro(s) encontrado(s)
          </p>
        </div>
        <button
          onClick={handleExport}
          disabled={exportMutation.isPending}
          className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white transition-colors hover:bg-blue-700 disabled:opacity-50"
        >
          <Download className={`h-4 w-4 ${exportMutation.isPending ? 'animate-pulse' : ''}`} />
          Exportar CSV
        </button>
      </div>

      {/* Filters */}
      <div className="mb-6 rounded-lg bg-white p-4 shadow dark:bg-gray-800">
        <div className="grid gap-4 md:grid-cols-5">
          {/* Action Filter */}
          <div className="relative">
            <Activity className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <select
              value={filters.acao || ''}
              onChange={(e) => setFilters(prev => ({ ...prev, acao: e.target.value || undefined, page: 0 }))}
              className="w-full appearance-none rounded-lg border border-gray-300 bg-white py-2 pl-10 pr-8 text-gray-900 focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            >
              <option value="">Todas as ações</option>
              <option value="CRIAR_OFICINA">Criar Oficina</option>
              <option value="EDITAR_OFICINA">Editar Oficina</option>
              <option value="ATIVAR_OFICINA">Ativar Oficina</option>
              <option value="SUSPENDER_OFICINA">Suspender Oficina</option>
              <option value="CANCELAR_OFICINA">Cancelar Oficina</option>
              <option value="ALTERAR_PLANO">Alterar Plano</option>
              <option value="REGISTRAR_PAGAMENTO">Registrar Pagamento</option>
              <option value="LOGIN_SUPER_ADMIN">Login Super Admin</option>
              <option value="LOGOUT_SUPER_ADMIN">Logout Super Admin</option>
            </select>
          </div>

          {/* Entity Filter */}
          <div className="relative">
            <Building2 className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <select
              value={filters.entidade || ''}
              onChange={(e) => setFilters(prev => ({ ...prev, entidade: e.target.value || undefined, page: 0 }))}
              className="w-full appearance-none rounded-lg border border-gray-300 bg-white py-2 pl-10 pr-8 text-gray-900 focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            >
              <option value="">Todas as entidades</option>
              <option value="Oficina">Oficina</option>
              <option value="Usuario">Usuário</option>
              <option value="Pagamento">Pagamento</option>
              <option value="Plano">Plano</option>
              <option value="Fatura">Fatura</option>
            </select>
          </div>

          {/* Date Start */}
          <div className="relative">
            <Calendar className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              type="date"
              value={filters.dataInicio || ''}
              onChange={(e) => setFilters(prev => ({ ...prev, dataInicio: e.target.value || undefined, page: 0 }))}
              className="w-full rounded-lg border border-gray-300 bg-white py-2 pl-10 pr-4 text-gray-900 focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              placeholder="Data início"
            />
          </div>

          {/* Date End */}
          <div className="relative">
            <Calendar className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              type="date"
              value={filters.dataFim || ''}
              onChange={(e) => setFilters(prev => ({ ...prev, dataFim: e.target.value || undefined, page: 0 }))}
              className="w-full rounded-lg border border-gray-300 bg-white py-2 pl-10 pr-4 text-gray-900 focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              placeholder="Data fim"
            />
          </div>

          {/* Clear Filters */}
          <button
            onClick={() => setFilters({ page: 0, size: 50 })}
            className="flex items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-gray-700 transition-colors hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600"
          >
            <RefreshCw className="h-4 w-4" />
            Limpar
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-lg bg-white shadow dark:bg-gray-800">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 dark:bg-gray-700">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Data/Hora
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Ação
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Entidade
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Usuário
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Detalhes
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  IP
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center">
                    <RefreshCw className="mx-auto h-8 w-8 animate-spin text-gray-400" />
                  </td>
                </tr>
              ) : data?.content && data.content.length > 0 ? (
                data.content.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
                      {formatDateTime(log.timestamp)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      {getActionBadge(log.acao)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <div>
                        <p className="font-medium text-gray-900 dark:text-white">{log.entidade}</p>
                        {log.entidadeId && (
                          <p className="text-xs text-gray-500 dark:text-gray-400">
                            ID: {log.entidadeId.substring(0, 8)}...
                          </p>
                        )}
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <div className="flex items-center gap-2">
                        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-700">
                          <User className="h-4 w-4 text-gray-600 dark:text-gray-400" />
                        </div>
                        <span className="text-sm text-gray-900 dark:text-white">
                          {log.usuarioEmail || 'Sistema'}
                        </span>
                      </div>
                    </td>
                    <td className="max-w-xs truncate px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
                      {log.detalhes || '-'}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
                      {log.ipAddress || '-'}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                    Nenhum log encontrado
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-gray-200 px-6 py-4 dark:border-gray-700">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Mostrando {data.number * data.size + 1} a{' '}
              {Math.min((data.number + 1) * data.size, data.totalElements)} de{' '}
              {data.totalElements}
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setFilters((prev) => ({ ...prev, page: prev.page! - 1 }))}
                disabled={data.first}
                className="rounded-lg border border-gray-300 px-3 py-1 text-sm disabled:opacity-50 dark:border-gray-600 dark:text-gray-300"
              >
                Anterior
              </button>
              <button
                onClick={() => setFilters((prev) => ({ ...prev, page: prev.page! + 1 }))}
                disabled={data.last}
                className="rounded-lg border border-gray-300 px-3 py-1 text-sm disabled:opacity-50 dark:border-gray-600 dark:text-gray-300"
              >
                Próximo
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
