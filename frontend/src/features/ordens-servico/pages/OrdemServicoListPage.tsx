/**
 * Página de listagem de Ordens de Serviço
 * Com filtros por status, veículo, mecânico e datas
 */

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useOrdensServico } from '../hooks/useOrdensServico';
import { Eye, Edit, Plus, FilterX } from 'lucide-react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import type { OrdemServicoFilters } from '../types';
import { StatusOS } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { canEdit } from '../utils/statusTransitions';

const ITEMS_PER_PAGE = 20;

/**
 * Converte array de números ou string ISO para objeto Date
 */
const parseDate = (date?: string | number[]): Date | null => {
  if (!date) return null;

  if (Array.isArray(date)) {
    const [year, month, day] = date;
    return new Date(year, month - 1, day);
  }

  return new Date(date);
};

/**
 * Formata data para exibição
 */
const formatDate = (date?: string | number[]): string => {
  const parsed = parseDate(date);
  if (!parsed) return '-';
  return format(parsed, 'dd/MM/yyyy', { locale: ptBR });
};

/**
 * Formata valor monetário
 */
const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

export const OrdemServicoListPage = () => {
  const [filters, setFilters] = useState<OrdemServicoFilters>({
    page: 0,
    size: ITEMS_PER_PAGE,
    sort: 'numero,desc',
  });

  const { data, isLoading, error } = useOrdensServico(filters);

  const handleFilterChange = (key: keyof OrdemServicoFilters, value: any) => {
    setFilters((prev) => ({ ...prev, [key]: value || undefined, page: 0 }));
  };

  const handleClearFilters = () => {
    setFilters({
      page: 0,
      size: ITEMS_PER_PAGE,
      sort: 'numero,desc',
    });
  };

  const handlePageChange = (newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage }));
  };

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Erro ao carregar ordens de serviço. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Ordens de Serviço</h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            Gerenciamento de ordens de serviço da oficina
          </p>
        </div>
        <Link
          to="/ordens-servico/novo"
          className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
        >
          <Plus className="h-5 w-5" />
          Nova OS
        </Link>
      </div>

      {/* Filters */}
      <div className="mb-6 rounded-lg bg-white p-4 shadow dark:bg-gray-800">
        <div className="grid gap-4 md:grid-cols-4">
          {/* Filter by Status */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Status</label>
            <select
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white dark:focus:border-blue-400"
              onChange={(e) => handleFilterChange('status', e.target.value as StatusOS)}
              defaultValue={filters.status || ''}
            >
              <option value="">Todos</option>
              <option value={StatusOS.ORCAMENTO}>Orçamento</option>
              <option value={StatusOS.APROVADO}>Aprovado</option>
              <option value={StatusOS.EM_ANDAMENTO}>Em Andamento</option>
              <option value={StatusOS.AGUARDANDO_PECA}>Aguardando Peça</option>
              <option value={StatusOS.FINALIZADO}>Finalizado</option>
              <option value={StatusOS.ENTREGUE}>Entregue</option>
              <option value={StatusOS.CANCELADO}>Cancelado</option>
            </select>
          </div>

          {/* Filter by Data Início */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Data Inicial
            </label>
            <input
              type="date"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white dark:focus:border-blue-400"
              onChange={(e) => handleFilterChange('dataInicio', e.target.value)}
              defaultValue={filters.dataInicio}
            />
          </div>

          {/* Filter by Data Fim */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Data Final</label>
            <input
              type="date"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white dark:focus:border-blue-400"
              onChange={(e) => handleFilterChange('dataFim', e.target.value)}
              defaultValue={filters.dataFim}
            />
          </div>

          {/* Clear Filters Button */}
          <div className="flex items-end">
            <button
              type="button"
              onClick={handleClearFilters}
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              <FilterX className="h-5 w-5" />
              Limpar Filtros
            </button>
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-lg bg-white shadow dark:bg-gray-800">
        <div className="overflow-x-auto">
          <table className="w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead className="bg-gray-50 dark:bg-gray-900">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Número
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Veículo / Cliente
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Mecânico
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Data Abertura
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Status
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Valor Final
                </th>
                <th className="px-6 py-3 text-center text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white dark:divide-gray-700 dark:bg-gray-800">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-8 text-center text-sm text-gray-500 dark:text-gray-400">
                    Carregando...
                  </td>
                </tr>
              ) : !data?.content || data.content.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-8 text-center text-sm text-gray-500 dark:text-gray-400">
                    Nenhuma ordem de serviço encontrada
                  </td>
                </tr>
              ) : (
                data.content.map((os) => (
                  <tr key={os.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                    {/* Número */}
                    <td className="whitespace-nowrap px-6 py-4">
                      <div className="text-sm font-medium text-gray-900 dark:text-white">#{os.numero}</div>
                    </td>

                    {/* Veículo / Cliente */}
                    <td className="px-6 py-4">
                      <div className="text-sm text-gray-900 dark:text-white">
                        {os.veiculo?.placa || 'N/A'}
                      </div>
                      <div className="text-sm text-gray-500 dark:text-gray-400">
                        {os.cliente?.nome || 'Cliente não informado'}
                      </div>
                    </td>

                    {/* Mecânico */}
                    <td className="px-6 py-4">
                      <div className="text-sm text-gray-900 dark:text-white">
                        {os.mecanico?.nome || 'Não atribuído'}
                      </div>
                    </td>

                    {/* Data Abertura */}
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-900 dark:text-white">
                      {formatDate(os.dataAbertura)}
                    </td>

                    {/* Status */}
                    <td className="whitespace-nowrap px-6 py-4">
                      <StatusBadge status={os.status} />
                    </td>

                    {/* Valor Final */}
                    <td className="whitespace-nowrap px-6 py-4 text-right text-sm font-medium text-gray-900 dark:text-white">
                      {formatCurrency(os.valorFinal)}
                    </td>

                    {/* Ações */}
                    <td className="whitespace-nowrap px-6 py-4 text-center">
                      <div className="flex items-center justify-center gap-2">
                        <Link
                          to={`/ordens-servico/${os.id}`}
                          className="rounded p-1 text-blue-600 hover:bg-blue-50 dark:text-blue-400 dark:hover:bg-blue-900/30"
                          title="Visualizar"
                        >
                          <Eye className="h-5 w-5" />
                        </Link>
                        {canEdit(os.status) && (
                          <Link
                            to={`/ordens-servico/${os.id}/editar`}
                            className="rounded p-1 text-green-600 hover:bg-green-50 dark:text-green-400 dark:hover:bg-green-900/30"
                            title="Editar"
                          >
                            <Edit className="h-5 w-5" />
                          </Link>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-gray-200 bg-white px-6 py-4 dark:border-gray-700 dark:bg-gray-800">
            <div className="text-sm text-gray-700 dark:text-gray-300">
              Mostrando <span className="font-medium">{data.content.length}</span> de{' '}
              <span className="font-medium">{data.totalElements}</span> resultado(s)
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => handlePageChange(filters.page! - 1)}
                disabled={data.first}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                Anterior
              </button>
              <button
                onClick={() => handlePageChange(filters.page! + 1)}
                disabled={data.last}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                Próxima
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
