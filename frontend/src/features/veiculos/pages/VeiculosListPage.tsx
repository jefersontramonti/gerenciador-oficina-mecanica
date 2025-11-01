import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useVeiculos, useDeleteVeiculo } from '../hooks/useVeiculos';
import { Eye, Edit, Trash2, Plus, Search, FilterX } from 'lucide-react';
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
          alert('Não é possível remover este veículo pois há ordens de serviço vinculadas.');
        } else {
          alert('Erro ao remover veículo');
        }
      }
    }
  };

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Erro ao carregar veículos. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Veículos</h1>
          <p className="mt-1 text-sm text-gray-600">
            Gerenciamento de veículos cadastrados
          </p>
        </div>
        <Link
          to="/veiculos/novo"
          className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
        >
          <Plus className="h-5 w-5" />
          Novo Veículo
        </Link>
      </div>

      {/* Filters */}
      <div className="mb-6 rounded-lg bg-white p-4 shadow">
        <div className="grid gap-4 md:grid-cols-4">
          {/* Search by placa */}
          <div className="md:col-span-2">
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Buscar por placa
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Digite a placa..."
                className="w-full rounded-lg border border-gray-300 py-2 pl-10 pr-4 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                onChange={(e) => handleSearch(e.target.value)}
                defaultValue={filters.placa}
              />
            </div>
          </div>

          {/* Filter by marca */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Marca</label>
            <input
              type="text"
              placeholder="Filtrar por marca"
              defaultValue={filters.marca}
              onChange={(e) => handleFilterChange('marca', e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          {/* Clear filters */}
          <div className="flex items-end">
            <button
              onClick={() =>
                setFilters({ page: 0, size: ITEMS_PER_PAGE, sort: 'placa,asc' })
              }
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-orange-300 bg-orange-50 px-4 py-2 text-orange-700 hover:bg-orange-100"
            >
              <FilterX className="h-4 w-4" />
              Limpar Filtros
            </button>
          </div>

          {/* Filter by modelo */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Modelo</label>
            <input
              type="text"
              placeholder="Filtrar por modelo"
              defaultValue={filters.modelo}
              onChange={(e) => handleFilterChange('modelo', e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          {/* Filter by ano */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Ano</label>
            <input
              type="number"
              placeholder="Ex: 2020"
              defaultValue={filters.ano}
              onChange={(e) =>
                handleFilterChange('ano', e.target.value ? parseInt(e.target.value) : undefined)
              }
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-lg bg-white shadow">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Placa
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Marca/Modelo
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Ano
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Cor
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Proprietário
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  KM
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-gray-500">
                    Carregando...
                  </td>
                </tr>
              ) : data?.content.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-gray-500">
                    Nenhum veículo encontrado
                  </td>
                </tr>
              ) : (
                data?.content.map((veiculo) => (
                  <tr key={veiculo.id} className="hover:bg-gray-50">
                    <td className="whitespace-nowrap px-6 py-4">
                      <div className="font-medium text-gray-900">{veiculo.placa}</div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <div className="text-sm text-gray-900">{veiculo.marca}</div>
                      <div className="text-sm text-gray-500">{veiculo.modelo}</div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-900">
                      {veiculo.ano}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500">
                      {veiculo.cor || '-'}
                    </td>
                    <td className="px-6 py-4">
                      {veiculo.cliente ? (
                        <div>
                          <div className="text-sm font-medium text-gray-900">
                            {veiculo.cliente.nome}
                          </div>
                          <div className="text-sm text-gray-500">{veiculo.cliente.telefone}</div>
                        </div>
                      ) : (
                        <span className="text-sm text-gray-500">-</span>
                      )}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500">
                      {veiculo.quilometragem?.toLocaleString('pt-BR') || '-'} km
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-right text-sm font-medium">
                      <div className="flex justify-end gap-2">
                        <Link
                          to={`/veiculos/${veiculo.id}`}
                          className="rounded p-1 text-blue-600 hover:bg-blue-50"
                          title="Visualizar"
                        >
                          <Eye className="h-5 w-5" />
                        </Link>
                        <Link
                          to={`/veiculos/${veiculo.id}/editar`}
                          className="rounded p-1 text-yellow-600 hover:bg-yellow-50"
                          title="Editar"
                        >
                          <Edit className="h-5 w-5" />
                        </Link>
                        <button
                          onClick={() => handleDelete(veiculo.id, veiculo.placa)}
                          disabled={deleteMutation.isPending}
                          className="rounded p-1 text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
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

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-gray-200 bg-white px-6 py-3">
            <div className="text-sm text-gray-700">
              Página {data.number + 1} de {data.totalPages} ({data.totalElements} total)
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => handlePageChange(filters.page! - 1)}
                disabled={data.first}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Anterior
              </button>
              <button
                onClick={() => handlePageChange(filters.page! + 1)}
                disabled={data.last}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
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
