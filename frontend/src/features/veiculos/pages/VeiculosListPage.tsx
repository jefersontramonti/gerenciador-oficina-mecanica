import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useVeiculos, useDeleteVeiculo } from '../hooks/useVeiculos';
import { Eye, Edit, Trash2, Plus, Search, FilterX, FileText } from 'lucide-react';
import { showError } from '@/shared/utils/notifications';
import type { VeiculoFilters } from '../types';

const ITEMS_PER_PAGE = 20;

export const VeiculosListPage = () => {
  const [filters, setFilters] = useState<VeiculoFilters>({
    page: 0,
    size: ITEMS_PER_PAGE,
    sort: 'placa,asc',
  });

  const { data, isLoading, error } = useVeiculos(filters);
  const deleteMutation = useDeleteVeiculo();

  const handleSearch = (placa: string) => {
    setFilters((prev) => ({ ...prev, placa: placa || undefined, page: 0 }));
  };

  const handleFilterChange = (key: keyof VeiculoFilters, value: any) => {
    setFilters((prev) => ({ ...prev, [key]: value || undefined, page: 0 }));
  };

  const handlePageChange = (newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage }));
  };

  const handleDelete = async (id: string, placa: string) => {
    if (window.confirm(`Tem certeza que deseja remover o veículo ${placa}?`)) {
      try {
        await deleteMutation.mutateAsync(id);
      } catch (error: any) {
        if (error.response?.status === 409) {
          showError('Não é possível remover este veículo pois há ordens de serviço vinculadas.');
        } else {
          showError('Erro ao remover veículo');
        }
      }
    }
  };

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400 dark:text-red-300">
          Erro ao carregar veículos. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">Veículos</h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            Gerenciamento de veículos cadastrados
          </p>
        </div>
        <Link
          to="/veiculos/novo"
          className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 w-full sm:w-auto"
        >
          <Plus className="h-5 w-5" />
          Novo Veículo
        </Link>
      </div>

      {/* Filters */}
      <div className="mb-6 rounded-lg bg-white dark:bg-gray-800 p-4 shadow">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {/* Search by placa */}
          <div className="sm:col-span-2">
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Buscar por placa
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Digite a placa..."
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white py-2 pl-10 pr-4 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                onChange={(e) => handleSearch(e.target.value)}
                defaultValue={filters.placa}
              />
            </div>
          </div>

          {/* Filter by marca */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Marca</label>
            <input
              type="text"
              placeholder="Filtrar por marca"
              defaultValue={filters.marca}
              onChange={(e) => handleFilterChange('marca', e.target.value)}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          {/* Clear filters */}
          <div className="flex items-end">
            <button
              onClick={() =>
                setFilters({ page: 0, size: ITEMS_PER_PAGE, sort: 'placa,asc' })
              }
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-orange-300 dark:border-orange-700 bg-orange-50 dark:bg-orange-900/20 px-4 py-2 text-orange-700 dark:text-orange-400 hover:bg-orange-100 dark:hover:bg-orange-900/30"
            >
              <FilterX className="h-4 w-4" />
              <span className="sm:hidden lg:inline">Limpar Filtros</span>
              <span className="hidden sm:inline lg:hidden">Limpar</span>
            </button>
          </div>

          {/* Filter by modelo */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Modelo</label>
            <input
              type="text"
              placeholder="Filtrar por modelo"
              defaultValue={filters.modelo}
              onChange={(e) => handleFilterChange('modelo', e.target.value)}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          {/* Filter by ano */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Ano</label>
            <input
              type="number"
              placeholder="Ex: 2020"
              defaultValue={filters.ano}
              onChange={(e) =>
                handleFilterChange('ano', e.target.value ? parseInt(e.target.value) : undefined)
              }
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>
        </div>
      </div>

      {/* Mobile: Card Layout */}
      <div className="space-y-3 lg:hidden">
        {isLoading ? (
          <div className="rounded-lg bg-white dark:bg-gray-800 p-8 shadow text-center text-gray-500 dark:text-gray-400">
            Carregando...
          </div>
        ) : data?.content.length === 0 ? (
          <div className="rounded-lg bg-white dark:bg-gray-800 p-8 shadow text-center text-gray-500 dark:text-gray-400">
            Nenhum veículo encontrado
          </div>
        ) : (
          data?.content.map((veiculo) => (
            <div
              key={veiculo.id}
              className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-bold text-gray-900 dark:text-white">
                      {veiculo.placa}
                    </span>
                    <span className="text-sm text-gray-500 dark:text-gray-400">
                      {veiculo.ano}
                    </span>
                  </div>
                  <p className="text-sm text-gray-900 dark:text-white mt-1">
                    {veiculo.marca} {veiculo.modelo}
                  </p>
                  {veiculo.cor && (
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      Cor: {veiculo.cor}
                    </p>
                  )}
                </div>
                <div className="flex gap-1 shrink-0">
                  <Link
                    to={`/veiculos/${veiculo.id}`}
                    className="rounded p-2 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20"
                  >
                    <Eye className="h-5 w-5" />
                  </Link>
                  <Link
                    to={`/veiculos/${veiculo.id}/editar`}
                    className="rounded p-2 text-yellow-600 dark:text-yellow-400 hover:bg-yellow-50 dark:hover:bg-yellow-900/20"
                  >
                    <Edit className="h-5 w-5" />
                  </Link>
                </div>
              </div>

              {veiculo.cliente && (
                <div className="mt-3 pt-3 border-t border-gray-200 dark:border-gray-700">
                  <p className="text-sm font-medium text-gray-900 dark:text-white">
                    {veiculo.cliente.nome}
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {veiculo.cliente.telefone}
                  </p>
                </div>
              )}

              <div className="mt-3 flex items-center justify-between">
                <span className="text-xs text-gray-500 dark:text-gray-400">
                  {veiculo.quilometragem?.toLocaleString('pt-BR') || '-'} km
                </span>
                <div className="flex gap-2">
                  <Link
                    to={`/ordens-servico/novo?veiculoId=${veiculo.id}`}
                    className="text-xs text-green-600 dark:text-green-400 hover:underline"
                  >
                    + Nova OS
                  </Link>
                  <button
                    onClick={() => handleDelete(veiculo.id, veiculo.placa)}
                    disabled={deleteMutation.isPending}
                    className="text-xs text-red-600 dark:text-red-400 hover:underline disabled:opacity-50"
                  >
                    Remover
                  </button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Desktop: Table Layout */}
      <div className="hidden lg:block overflow-hidden rounded-lg bg-white dark:bg-gray-800 shadow">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 dark:bg-gray-900">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Placa
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Marca/Modelo
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Ano
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Cor
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Proprietário
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  KM
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                    Carregando...
                  </td>
                </tr>
              ) : data?.content.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                    Nenhum veículo encontrado
                  </td>
                </tr>
              ) : (
                data?.content.map((veiculo) => (
                  <tr key={veiculo.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                    <td className="whitespace-nowrap px-6 py-4">
                      <div className="font-medium text-gray-900 dark:text-white">{veiculo.placa}</div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <div className="text-sm text-gray-900 dark:text-white">{veiculo.marca}</div>
                      <div className="text-sm text-gray-500 dark:text-gray-400">{veiculo.modelo}</div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-900 dark:text-white">
                      {veiculo.ano}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
                      {veiculo.cor || '-'}
                    </td>
                    <td className="px-6 py-4">
                      {veiculo.cliente ? (
                        <div>
                          <div className="text-sm font-medium text-gray-900 dark:text-white">
                            {veiculo.cliente.nome}
                          </div>
                          <div className="text-sm text-gray-500 dark:text-gray-400">{veiculo.cliente.telefone}</div>
                        </div>
                      ) : (
                        <span className="text-sm text-gray-500 dark:text-gray-400">-</span>
                      )}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
                      {veiculo.quilometragem?.toLocaleString('pt-BR') || '-'} km
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-right text-sm font-medium">
                      <div className="flex justify-end gap-2">
                        <Link
                          to={`/veiculos/${veiculo.id}`}
                          className="rounded p-1 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20"
                          title="Visualizar"
                        >
                          <Eye className="h-5 w-5" />
                        </Link>
                        <Link
                          to={`/veiculos/${veiculo.id}/editar`}
                          className="rounded p-1 text-yellow-600 dark:text-yellow-400 hover:bg-yellow-50 dark:hover:bg-yellow-900/20"
                          title="Editar"
                        >
                          <Edit className="h-5 w-5" />
                        </Link>
                        <Link
                          to={`/ordens-servico/novo?veiculoId=${veiculo.id}`}
                          className="rounded p-1 text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/20"
                          title="Criar OS"
                        >
                          <FileText className="h-5 w-5" />
                        </Link>
                        <button
                          onClick={() => handleDelete(veiculo.id, veiculo.placa)}
                          disabled={deleteMutation.isPending}
                          className="rounded p-1 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 disabled:cursor-not-allowed disabled:opacity-50"
                          title="Remover"
                        >
                          <Trash2 className="h-5 w-5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination - Both mobile and desktop */}
      {data && data.totalPages > 1 && (
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between rounded-lg bg-white dark:bg-gray-800 px-4 sm:px-6 py-3 shadow">
          <div className="text-sm text-gray-700 dark:text-gray-300 text-center sm:text-left">
            Página {data.number + 1} de {data.totalPages} ({data.totalElements} total)
          </div>
          <div className="flex gap-2 justify-center sm:justify-end">
            <button
              onClick={() => handlePageChange(filters.page! - 1)}
              disabled={data.first}
              className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Anterior
            </button>
            <button
              onClick={() => handlePageChange(filters.page! + 1)}
              disabled={data.last}
              className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Próxima
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
