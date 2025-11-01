import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useVeiculos, useDeleteVeiculo } from '../hooks/useVeiculos';
import { Eye, Edit, Trash2, Plus, Search, Car } from 'lucide-react';
import type { VeiculoFilters } from '../types';

export const VeiculosListPage = () => {
  const [filters, setFilters] = useState<VeiculoFilters>({
    page: 0,
    size: 20,
    sort: 'placa,asc',
  });

  const { data, isLoading, error } = useVeiculos(filters);
  const deleteMutation = useDeleteVeiculo();

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

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="text-gray-500">Carregando...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Erro ao carregar veículos
        </div>
      </div>
    );
  }

  const veiculos = data?.content || [];
  const totalPages = data?.totalPages || 0;
  const currentPage = data?.number || 0;

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
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Placa</label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="ABC-1234"
                value={filters.placa || ''}
                onChange={(e) =>
                  setFilters((prev) => ({ ...prev, placa: e.target.value, page: 0 }))
                }
                className="w-full rounded-lg border border-gray-300 py-2 pl-10 pr-3 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
            </div>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Marca</label>
            <input
              type="text"
              placeholder="Filtrar por marca"
              value={filters.marca || ''}
              onChange={(e) =>
                setFilters((prev) => ({ ...prev, marca: e.target.value, page: 0 }))
              }
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Modelo</label>
            <input
              type="text"
              placeholder="Filtrar por modelo"
              value={filters.modelo || ''}
              onChange={(e) =>
                setFilters((prev) => ({ ...prev, modelo: e.target.value, page: 0 }))
              }
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Ano</label>
            <input
              type="number"
              placeholder="Ex: 2020"
              value={filters.ano || ''}
              onChange={(e) =>
                setFilters((prev) => ({
                  ...prev,
                  ano: e.target.value ? parseInt(e.target.value) : undefined,
                  page: 0,
                }))
              }
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>
        </div>
      </div>

      {/* Table */}
      {veiculos.length === 0 ? (
        <div className="rounded-lg bg-white p-12 text-center shadow">
          <Car className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-4 text-lg font-medium text-gray-900">Nenhum veículo encontrado</h3>
          <p className="mt-2 text-sm text-gray-600">
            {Object.keys(filters).some((k) => filters[k as keyof VeiculoFilters])
              ? 'Tente ajustar os filtros ou'
              : 'Comece'}
            {' cadastrando um novo veículo.'}
          </p>
          <Link
            to="/veiculos/novo"
            className="mt-6 inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
          >
            <Plus className="h-5 w-5" />
            Novo Veículo
          </Link>
        </div>
      ) : (
        <>
          <div className="overflow-hidden rounded-lg bg-white shadow">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Placa
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Marca/Modelo
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Ano
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Cor
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Proprietário
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    KM
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">
                    Ações
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 bg-white">
                {veiculos.map((veiculo) => (
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
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-4 flex items-center justify-between rounded-lg bg-white px-4 py-3 shadow">
              <div className="text-sm text-gray-700">
                Página {currentPage + 1} de {totalPages}
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 0}
                  className="rounded-lg border border-gray-300 px-3 py-1 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Anterior
                </button>
                <button
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage >= totalPages - 1}
                  className="rounded-lg border border-gray-300 px-3 py-1 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Próxima
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};
