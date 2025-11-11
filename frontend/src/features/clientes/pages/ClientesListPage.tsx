import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useClientes, useDeleteCliente, useReativarCliente } from '../hooks/useClientes';
import { Plus, Search, Edit, Eye, CheckCircle, XCircle, FilterX, Trash2 } from 'lucide-react';
import { showError } from '@/shared/utils/notifications';
import type { ClienteFilters, TipoCliente } from '../types';

const ITEMS_PER_PAGE = 20;

export const ClientesListPage = () => {
  const [filters, setFilters] = useState<ClienteFilters>({
    page: 0,
    size: ITEMS_PER_PAGE,
    sort: 'nome,asc',
  });

  const { data, isLoading, error } = useClientes(filters);
  const deleteMutation = useDeleteCliente();
  const reativarMutation = useReativarCliente();

  const handleSearch = (nome: string) => {
    setFilters((prev) => ({ ...prev, nome: nome || undefined, page: 0 }));
  };

  const handleFilterChange = (key: keyof ClienteFilters, value: any) => {
    setFilters((prev) => ({ ...prev, [key]: value || undefined, page: 0 }));
  };

  const handlePageChange = (newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage }));
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Tem certeza que deseja desativar este cliente?')) {
      try {
        await deleteMutation.mutateAsync(id);
      } catch (error) {
        showError('Erro ao desativar cliente');
      }
    }
  };

  const handleReativar = async (id: string) => {
    try {
      await reativarMutation.mutateAsync(id);
    } catch (error) {
      showError('Erro ao reativar cliente');
    }
  };

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Erro ao carregar clientes. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Clientes</h1>
          <p className="mt-1 text-sm text-gray-600">
            {data?.totalElements || 0} cliente(s) cadastrado(s)
          </p>
        </div>
        <Link
          to="/clientes/novo"
          className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
        >
          <Plus className="h-5 w-5" />
          Novo Cliente
        </Link>
      </div>

      {/* Filters */}
      <div className="mb-6 rounded-lg bg-white p-4 shadow">
        <div className="grid gap-4 md:grid-cols-4">
          {/* Search by name */}
          <div className="md:col-span-2">
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Buscar por nome
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Digite o nome..."
                className="w-full rounded-lg border border-gray-300 py-2 pl-10 pr-4 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                onChange={(e) => handleSearch(e.target.value)}
                defaultValue={filters.nome}
              />
            </div>
          </div>

          {/* Filter by type */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Tipo</label>
            <select
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={filters.tipo || ''}
              onChange={(e) =>
                handleFilterChange('tipo', e.target.value as TipoCliente)
              }
            >
              <option value="">Todos</option>
              <option value="PESSOA_FISICA">Pessoa Física</option>
              <option value="PESSOA_JURIDICA">Pessoa Jurídica</option>
            </select>
          </div>

          {/* Clear filters */}
          <div className="flex items-end">
            <button
              onClick={() =>
                setFilters({ page: 0, size: ITEMS_PER_PAGE, sort: 'nome,asc' })
              }
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-orange-300 bg-orange-50 px-4 py-2 text-orange-700 hover:bg-orange-100"
            >
              <FilterX className="h-4 w-4" />
              Limpar Filtros
            </button>
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
                  Nome
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  CPF/CNPJ
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Tipo
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Contato
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700">
                  Status
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                    Carregando...
                  </td>
                </tr>
              ) : data?.content.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                    Nenhum cliente encontrado
                  </td>
                </tr>
              ) : (
                data?.content.map((cliente) => (
                  <tr key={cliente.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 text-sm font-medium text-gray-900">
                      {cliente.nome}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">
                      {cliente.cpfCnpj}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">
                      {cliente.tipo === 'PESSOA_FISICA' ? 'PF' : 'PJ'}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">
                      {cliente.celular || cliente.telefone || '-'}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      {cliente.ativo ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2 py-1 text-xs font-semibold text-green-800">
                          <CheckCircle className="h-3 w-3" />
                          Ativo
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2 py-1 text-xs font-semibold text-gray-800">
                          <XCircle className="h-3 w-3" />
                          Inativo
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right text-sm">
                      <div className="flex items-center justify-end gap-2">
                        <Link
                          to={`/clientes/${cliente.id}`}
                          className="text-blue-600 hover:text-blue-800"
                          title="Visualizar"
                        >
                          <Eye className="h-5 w-5" />
                        </Link>
                        <Link
                          to={`/clientes/${cliente.id}/editar`}
                          className="text-gray-600 hover:text-gray-800"
                          title="Editar"
                        >
                          <Edit className="h-5 w-5" />
                        </Link>
                        {cliente.ativo ? (
                          <>
                            <button
                              onClick={() => handleDelete(cliente.id)}
                              className="text-green-600 hover:text-green-800"
                              title="Desativar Cliente"
                              disabled={deleteMutation.isPending}
                            >
                              <CheckCircle className="h-5 w-5" />
                            </button>
                            <button
                              onClick={() => handleDelete(cliente.id)}
                              className="text-red-600 hover:text-red-800"
                              title="Desativar Cliente"
                              disabled={deleteMutation.isPending}
                            >
                              <Trash2 className="h-5 w-5" />
                            </button>
                          </>
                        ) : (
                          <button
                            onClick={() => handleReativar(cliente.id)}
                            className="text-gray-500 hover:text-gray-700"
                            title="Reativar Cliente"
                            disabled={reativarMutation.isPending}
                          >
                            <XCircle className="h-5 w-5" />
                          </button>
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
          <div className="flex items-center justify-between border-t border-gray-200 bg-white px-6 py-3">
            <div className="text-sm text-gray-700">
              Página {data.number + 1} de {data.totalPages} ({data.totalElements}{' '}
              total)
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
