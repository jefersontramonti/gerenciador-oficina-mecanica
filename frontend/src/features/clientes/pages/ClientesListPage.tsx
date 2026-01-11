import { useState, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useClientes, useDeleteCliente, useReativarCliente } from '../hooks/useClientes';
import { Plus, Search, Edit, Eye, CheckCircle, XCircle, FilterX, Ban, RotateCcw } from 'lucide-react';
import { showError } from '@/shared/utils/notifications';
import { DataTable, type ColumnDef, type RowAction } from '@/shared/components/table';
import type { Cliente, ClienteFilters, TipoCliente } from '../types';

const ITEMS_PER_PAGE = 20;

export const ClientesListPage = () => {
  const navigate = useNavigate();
  const [filters, setFilters] = useState<ClienteFilters>({
    page: 0,
    size: ITEMS_PER_PAGE,
    sort: 'nome,asc',
    ativo: true, // Mostrar apenas ativos por padrão
  });

  const { data, isLoading, error } = useClientes(filters);
  const deleteMutation = useDeleteCliente();
  const reativarMutation = useReativarCliente();

  const handleSearch = (nome: string) => {
    setFilters((prev) => ({ ...prev, nome: nome || undefined, page: 0 }));
  };

  const handleFilterChange = (key: keyof ClienteFilters, value: any) => {
    // Para boolean, não usar || undefined porque false seria convertido para undefined
    const filterValue = value === '' || value === null ? undefined : value;
    setFilters((prev) => ({ ...prev, [key]: filterValue, page: 0 }));
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

  // Column definitions
  const columns = useMemo<ColumnDef<Cliente>[]>(() => [
    {
      id: 'nome',
      header: 'Nome',
      cell: (cliente) => cliente.nome,
      cellClassName: 'font-medium text-gray-900 dark:text-white',
    },
    {
      id: 'cpfCnpj',
      header: 'CPF/CNPJ',
      cell: (cliente) => cliente.cpfCnpj,
    },
    {
      id: 'tipo',
      header: 'Tipo',
      cell: (cliente) => (cliente.tipo === 'PESSOA_FISICA' ? 'PF' : 'PJ'),
    },
    {
      id: 'contato',
      header: 'Contato',
      cell: (cliente) => cliente.celular || cliente.telefone || '-',
    },
    {
      id: 'status',
      header: 'Status',
      cell: (cliente) =>
        cliente.ativo ? (
          <span className="inline-flex items-center gap-1 rounded-full bg-green-100 dark:bg-green-900/30 px-2 py-1 text-xs font-semibold text-green-800 dark:text-green-400">
            <CheckCircle className="h-3 w-3" />
            Ativo
          </span>
        ) : (
          <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 dark:bg-gray-700 px-2 py-1 text-xs font-semibold text-gray-800 dark:text-gray-300">
            <XCircle className="h-3 w-3" />
            Inativo
          </span>
        ),
    },
  ], []);

  // Row actions
  const actions = useMemo<RowAction<Cliente>[]>(() => [
    {
      icon: Eye,
      title: 'Visualizar',
      variant: 'primary',
      onClick: (cliente) => navigate(`/clientes/${cliente.id}`),
    },
    {
      icon: Edit,
      title: 'Editar',
      variant: 'secondary',
      onClick: (cliente) => navigate(`/clientes/${cliente.id}/editar`),
    },
    {
      icon: Ban,
      title: 'Desativar Cliente',
      variant: 'warning',
      onClick: (cliente) => handleDelete(cliente.id),
      show: (cliente) => cliente.ativo,
      disabled: () => deleteMutation.isPending,
    },
    {
      icon: RotateCcw,
      title: 'Reativar Cliente',
      variant: 'success',
      onClick: (cliente) => handleReativar(cliente.id),
      show: (cliente) => !cliente.ativo,
      disabled: () => reativarMutation.isPending,
    },
  ], [navigate, deleteMutation.isPending, reativarMutation.isPending]);

  if (error) {
    return (
      <div className="p-4 sm:p-6">
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400 dark:text-red-300">
          Erro ao carregar clientes. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-4 sm:mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">Clientes</h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            {data?.totalElements || 0} cliente(s) cadastrado(s)
          </p>
        </div>
        <Link
          to="/clientes/novo"
          className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 w-full sm:w-auto"
        >
          <Plus className="h-4 w-4 sm:h-5 sm:w-5" />
          Novo Cliente
        </Link>
      </div>

      {/* Filters */}
      <div className="mb-4 sm:mb-6 rounded-lg bg-white dark:bg-gray-800 p-3 sm:p-4 shadow">
        <div className="grid gap-3 sm:gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-5">
          {/* Search by name */}
          <div className="sm:col-span-2 lg:col-span-2">
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Buscar por nome
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Digite o nome..."
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white py-2 pl-10 pr-4 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                onChange={(e) => handleSearch(e.target.value)}
                defaultValue={filters.nome}
              />
            </div>
          </div>

          {/* Filter by type */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Tipo</label>
            <select
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
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

          {/* Filter by status */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Status</label>
            <select
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              value={filters.ativo === undefined ? '' : filters.ativo ? 'true' : 'false'}
              onChange={(e) => {
                const value = e.target.value;
                handleFilterChange('ativo', value === '' ? undefined : value === 'true');
              }}
            >
              <option value="">Todos</option>
              <option value="true">Ativos</option>
              <option value="false">Inativos</option>
            </select>
          </div>

          {/* Clear filters */}
          <div className="flex items-end">
            <button
              onClick={() =>
                setFilters({ page: 0, size: ITEMS_PER_PAGE, sort: 'nome,asc', ativo: true })
              }
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-orange-300 dark:border-orange-700 bg-orange-50 dark:bg-orange-900/20 px-4 py-2 text-orange-700 dark:text-orange-400 hover:bg-orange-100 dark:hover:bg-orange-900/30"
            >
              <FilterX className="h-4 w-4" />
              Limpar Filtros
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Cards */}
      <div className="lg:hidden space-y-3">
        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 border-t-transparent"></div>
          </div>
        ) : data?.content && data.content.length > 0 ? (
          <>
            {data.content.map((cliente) => (
              <div
                key={cliente.id}
                className="rounded-lg bg-white dark:bg-gray-800 p-4 shadow border border-gray-200 dark:border-gray-700"
              >
                <div className="flex items-start justify-between mb-3">
                  <div className="flex-1 min-w-0">
                    <h3 className="font-medium text-gray-900 dark:text-white truncate">
                      {cliente.nome}
                    </h3>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {cliente.cpfCnpj}
                    </p>
                  </div>
                  <div className="flex items-center gap-1 ml-2">
                    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-semibold ${
                      cliente.ativo
                        ? 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-400'
                        : 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300'
                    }`}>
                      {cliente.ativo ? (
                        <>
                          <CheckCircle className="h-3 w-3" />
                          Ativo
                        </>
                      ) : (
                        <>
                          <XCircle className="h-3 w-3" />
                          Inativo
                        </>
                      )}
                    </span>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 text-sm mb-3">
                  <div>
                    <span className="text-gray-500 dark:text-gray-400">Tipo:</span>
                    <span className="ml-1 text-gray-900 dark:text-white">
                      {cliente.tipo === 'PESSOA_FISICA' ? 'PF' : 'PJ'}
                    </span>
                  </div>
                  <div>
                    <span className="text-gray-500 dark:text-gray-400">Contato:</span>
                    <span className="ml-1 text-gray-900 dark:text-white">
                      {cliente.celular || cliente.telefone || '-'}
                    </span>
                  </div>
                </div>

                <div className="flex gap-2 pt-3 border-t border-gray-200 dark:border-gray-700">
                  <button
                    onClick={() => navigate(`/clientes/${cliente.id}`)}
                    className="flex-1 flex items-center justify-center gap-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 px-3 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                  >
                    <Eye className="h-4 w-4" />
                    Ver
                  </button>
                  <button
                    onClick={() => navigate(`/clientes/${cliente.id}/editar`)}
                    className="flex-1 flex items-center justify-center gap-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 px-3 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                  >
                    <Edit className="h-4 w-4" />
                    Editar
                  </button>
                  {cliente.ativo ? (
                    <button
                      onClick={() => handleDelete(cliente.id)}
                      disabled={deleteMutation.isPending}
                      className="flex items-center justify-center gap-1 rounded-lg border border-orange-300 dark:border-orange-700 bg-orange-50 dark:bg-orange-900/20 px-3 py-2 text-sm font-medium text-orange-700 dark:text-orange-400 hover:bg-orange-100 dark:hover:bg-orange-900/30 disabled:opacity-50"
                    >
                      <Ban className="h-4 w-4" />
                    </button>
                  ) : (
                    <button
                      onClick={() => handleReativar(cliente.id)}
                      disabled={reativarMutation.isPending}
                      className="flex items-center justify-center gap-1 rounded-lg border border-green-300 dark:border-green-700 bg-green-50 dark:bg-green-900/20 px-3 py-2 text-sm font-medium text-green-700 dark:text-green-400 hover:bg-green-100 dark:hover:bg-green-900/30 disabled:opacity-50"
                    >
                      <RotateCcw className="h-4 w-4" />
                    </button>
                  )}
                </div>
              </div>
            ))}
            {/* Mobile Pagination */}
            {data && data.totalPages > 1 && (
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between bg-white dark:bg-gray-800 rounded-lg p-4 shadow">
                <div className="text-sm text-gray-700 dark:text-gray-300 text-center sm:text-left">
                  Página {data.number + 1} de {data.totalPages} ({data.totalElements} total)
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => handlePageChange(data.number - 1)}
                    disabled={data.first}
                    className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Anterior
                  </button>
                  <button
                    onClick={() => handlePageChange(data.number + 1)}
                    disabled={data.last}
                    className="flex-1 sm:flex-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Próxima
                  </button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="rounded-lg bg-white dark:bg-gray-800 p-8 text-center shadow">
            <p className="text-gray-500 dark:text-gray-400">Nenhum cliente encontrado</p>
          </div>
        )}
      </div>

      {/* Desktop Table */}
      <div className="hidden lg:block">
        <DataTable
          data={data?.content || []}
          columns={columns}
          isLoading={isLoading}
          emptyMessage="Nenhum cliente encontrado"
          pagination={data}
          onPageChange={handlePageChange}
          actions={actions}
          getRowKey={(cliente) => cliente.id}
        />
      </div>
    </div>
  );
};
