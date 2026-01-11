/**
 * Oficinas Management Page - List and manage workshops
 */

import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  Building2,
  Plus,
  Search,
  Eye,
  Play,
  Pause,
  Ban,
  RefreshCw,
  Filter,
} from 'lucide-react';
import { useOficinas, useActivateOficina, useSuspendOficina, useCancelOficina } from '../hooks/useSaas';
import { formatCurrency, formatDate } from '@/shared/utils/formatters';
import { showSuccess, showError } from '@/shared/utils/notifications';
import {
  StatusOficina,
  PlanoAssinatura,
  statusLabels,
  planoLabels,
  type OficinaFilters,
} from '../types';
import { Modal } from '@/shared/components/ui/Modal';

export const OficinasPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [filters, setFilters] = useState<OficinaFilters>({
    status: (searchParams.get('status') as StatusOficina) || undefined,
    plano: undefined,
    searchTerm: '',
    page: 0,
    size: 20,
    sort: 'createdAt,desc',
  });

  const [confirmModal, setConfirmModal] = useState<{
    open: boolean;
    type: 'activate' | 'suspend' | 'cancel';
    oficina?: { id: string; nome: string };
  }>({ open: false, type: 'activate' });

  const { data, isLoading, error } = useOficinas(filters);
  const activateMutation = useActivateOficina();
  const suspendMutation = useSuspendOficina();
  const cancelMutation = useCancelOficina();

  const handleSearch = (value: string) => {
    setFilters((prev) => ({ ...prev, searchTerm: value, page: 0 }));
  };

  const handleFilterChange = (key: keyof OficinaFilters, value: string | undefined) => {
    const filterValue = value === '' ? undefined : value;
    setFilters((prev) => ({ ...prev, [key]: filterValue, page: 0 }));

    // Update URL params for status filter
    if (key === 'status') {
      if (filterValue) {
        searchParams.set('status', filterValue);
      } else {
        searchParams.delete('status');
      }
      setSearchParams(searchParams);
    }
  };

  const handleAction = async (type: 'activate' | 'suspend' | 'cancel') => {
    if (!confirmModal.oficina) return;

    try {
      switch (type) {
        case 'activate':
          await activateMutation.mutateAsync(confirmModal.oficina.id);
          showSuccess('Oficina ativada com sucesso!');
          break;
        case 'suspend':
          await suspendMutation.mutateAsync(confirmModal.oficina.id);
          showSuccess('Oficina suspensa com sucesso!');
          break;
        case 'cancel':
          await cancelMutation.mutateAsync(confirmModal.oficina.id);
          showSuccess('Oficina cancelada com sucesso!');
          break;
      }
      setConfirmModal({ open: false, type: 'activate' });
    } catch {
      showError('Erro ao executar ação');
    }
  };

  const getStatusBadge = (status: StatusOficina) => {
    const colors: Record<StatusOficina, string> = {
      ATIVA: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
      TRIAL: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
      SUSPENSA: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
      CANCELADA: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
      INATIVA: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
    };

    return (
      <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${colors[status]}`}>
        {statusLabels[status]}
      </span>
    );
  };

  const getPlanoBadge = (plano: PlanoAssinatura) => {
    const colors: Record<PlanoAssinatura, string> = {
      ECONOMICO: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
      PROFISSIONAL: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
      TURBINADO: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400',
    };

    return (
      <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${colors[plano]}`}>
        {planoLabels[plano]}
      </span>
    );
  };

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-300 bg-red-50 p-4 text-red-800 dark:border-red-800 dark:bg-red-900/20 dark:text-red-400">
          Erro ao carregar oficinas. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
            Oficinas
          </h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            {data?.totalElements || 0} oficina(s) cadastrada(s)
          </p>
        </div>
        <Link
          to="/admin/oficinas/nova"
          className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white transition-colors hover:bg-blue-700 w-full sm:w-auto"
        >
          <Plus className="h-5 w-5" />
          Nova Oficina
        </Link>
      </div>

      {/* Filters */}
      <div className="mb-6 rounded-lg bg-white p-4 shadow dark:bg-gray-800">
        <div className="grid gap-4 md:grid-cols-4">
          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="Buscar por nome..."
              defaultValue={filters.searchTerm}
              onChange={(e) => handleSearch(e.target.value)}
              className="w-full rounded-lg border border-gray-300 bg-white py-2 pl-10 pr-4 text-gray-900 placeholder-gray-500 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white dark:placeholder-gray-400"
            />
          </div>

          {/* Status Filter */}
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <select
              value={filters.status || ''}
              onChange={(e) => handleFilterChange('status', e.target.value)}
              className="w-full appearance-none rounded-lg border border-gray-300 bg-white py-2 pl-10 pr-8 text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            >
              <option value="">Todos os status</option>
              {Object.entries(statusLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          {/* Plano Filter */}
          <div className="relative">
            <Building2 className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <select
              value={filters.plano || ''}
              onChange={(e) => handleFilterChange('plano', e.target.value)}
              className="w-full appearance-none rounded-lg border border-gray-300 bg-white py-2 pl-10 pr-8 text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            >
              <option value="">Todos os planos</option>
              {Object.entries(planoLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          {/* Clear Filters */}
          <button
            onClick={() => {
              setFilters({
                page: 0,
                size: 20,
                sort: 'createdAt,desc',
              });
              setSearchParams({});
            }}
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
                  Oficina
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Contato
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Plano
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Vencimento
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Ações
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
                data.content.map((oficina) => (
                  <tr key={oficina.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                    <td className="whitespace-nowrap px-6 py-4">
                      <div>
                        <p className="font-medium text-gray-900 dark:text-white">
                          {oficina.nomeFantasia}
                        </p>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          {oficina.cnpjCpf}
                        </p>
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <div>
                        <p className="text-gray-900 dark:text-white">{oficina.email}</p>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          {oficina.telefone}
                        </p>
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <div>
                        {getPlanoBadge(oficina.plano)}
                        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                          {formatCurrency(oficina.valorMensalidade)}/mês
                        </p>
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      {getStatusBadge(oficina.status)}
                      {oficina.diasRestantesTrial !== undefined && oficina.status === 'TRIAL' && (
                        <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                          {oficina.diasRestantesTrial} dias restantes
                        </p>
                      )}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
                      {formatDate(oficina.dataVencimentoPlano)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Link
                          to={`/admin/oficinas/${oficina.id}`}
                          className="rounded p-1 text-gray-500 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-300"
                          title="Ver detalhes"
                        >
                          <Eye className="h-5 w-5" />
                        </Link>
                        {oficina.status === 'SUSPENSA' && (
                          <button
                            onClick={() =>
                              setConfirmModal({
                                open: true,
                                type: 'activate',
                                oficina: { id: oficina.id, nome: oficina.nomeFantasia },
                              })
                            }
                            className="rounded p-1 text-green-500 hover:bg-green-100 hover:text-green-700 dark:hover:bg-green-900/30"
                            title="Ativar"
                          >
                            <Play className="h-5 w-5" />
                          </button>
                        )}
                        {(oficina.status === 'ATIVA' || oficina.status === 'TRIAL') && (
                          <button
                            onClick={() =>
                              setConfirmModal({
                                open: true,
                                type: 'suspend',
                                oficina: { id: oficina.id, nome: oficina.nomeFantasia },
                              })
                            }
                            className="rounded p-1 text-yellow-500 hover:bg-yellow-100 hover:text-yellow-700 dark:hover:bg-yellow-900/30"
                            title="Suspender"
                          >
                            <Pause className="h-5 w-5" />
                          </button>
                        )}
                        {oficina.status !== 'CANCELADA' && (
                          <button
                            onClick={() =>
                              setConfirmModal({
                                open: true,
                                type: 'cancel',
                                oficina: { id: oficina.id, nome: oficina.nomeFantasia },
                              })
                            }
                            className="rounded p-1 text-red-500 hover:bg-red-100 hover:text-red-700 dark:hover:bg-red-900/30"
                            title="Cancelar"
                          >
                            <Ban className="h-5 w-5" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                    Nenhuma oficina encontrada
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
                className="rounded-lg border border-gray-300 px-3 py-1 text-sm disabled:opacity-50 dark:border-gray-600"
              >
                Anterior
              </button>
              <button
                onClick={() => setFilters((prev) => ({ ...prev, page: prev.page! + 1 }))}
                disabled={data.last}
                className="rounded-lg border border-gray-300 px-3 py-1 text-sm disabled:opacity-50 dark:border-gray-600"
              >
                Próximo
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Confirmation Modal */}
      <Modal
        isOpen={confirmModal.open}
        onClose={() => setConfirmModal({ open: false, type: 'activate' })}
        title={
          confirmModal.type === 'activate'
            ? 'Ativar Oficina'
            : confirmModal.type === 'suspend'
            ? 'Suspender Oficina'
            : 'Cancelar Oficina'
        }
      >
        <div className="space-y-4">
          <p className="text-gray-700 dark:text-gray-300">
            {confirmModal.type === 'activate' && (
              <>
                Deseja ativar a oficina <strong>{confirmModal.oficina?.nome}</strong>?
                Isso permitirá que os usuários acessem o sistema novamente.
              </>
            )}
            {confirmModal.type === 'suspend' && (
              <>
                Deseja suspender a oficina <strong>{confirmModal.oficina?.nome}</strong>?
                Os usuários não poderão acessar o sistema.
              </>
            )}
            {confirmModal.type === 'cancel' && (
              <>
                Deseja cancelar a oficina <strong>{confirmModal.oficina?.nome}</strong>?
                <span className="block mt-2 text-red-600 dark:text-red-400 font-medium">
                  Esta ação é irreversível!
                </span>
              </>
            )}
          </p>
          <div className="flex justify-end gap-3">
            <button
              onClick={() => setConfirmModal({ open: false, type: 'activate' })}
              className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Cancelar
            </button>
            <button
              onClick={() => handleAction(confirmModal.type)}
              disabled={activateMutation.isPending || suspendMutation.isPending || cancelMutation.isPending}
              className={`rounded-lg px-4 py-2 text-white ${
                confirmModal.type === 'activate'
                  ? 'bg-green-600 hover:bg-green-700'
                  : confirmModal.type === 'suspend'
                  ? 'bg-yellow-600 hover:bg-yellow-700'
                  : 'bg-red-600 hover:bg-red-700'
              }`}
            >
              Confirmar
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
