/**
 * Página de listagem de usuários
 * Exibe tabela com filtros, paginação e ações (visualizar, editar, desativar/reativar)
 */

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Edit, Eye, UserX, UserCheck } from 'lucide-react';
import { formatDateTime } from '@/shared/utils/dateFormatter';
import { showError } from '@/shared/utils/notifications';
import {
  useUsuarios,
  useDeleteUsuario,
  useReactivateUsuario,
} from '../hooks/useUsuarios';
import type { UsuarioFilters, PerfilUsuario } from '../types';

/**
 * Badge de perfil do usuário
 */
const PerfilBadge = ({ perfil }: { perfil: PerfilUsuario }) => {
  const colors: Record<PerfilUsuario, string> = {
    ADMIN: 'bg-purple-100 text-purple-800 border-purple-200 dark:bg-purple-900/30 dark:text-purple-400 dark:border-purple-800',
    GERENTE: 'bg-blue-100 text-blue-800 border-blue-200 dark:bg-blue-900/30 dark:text-blue-400 dark:border-blue-800',
    ATENDENTE: 'bg-green-100 text-green-800 border-green-200 dark:bg-green-900/30 dark:text-green-400 dark:border-green-800',
    MECANICO: 'bg-orange-100 text-orange-800 border-orange-200 dark:bg-orange-900/30 dark:text-orange-400 dark:border-orange-800',
  };

  const labels: Record<PerfilUsuario, string> = {
    ADMIN: 'Administrador',
    GERENTE: 'Gerente',
    ATENDENTE: 'Atendente',
    MECANICO: 'Mecânico',
  };

  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${colors[perfil]}`}
    >
      {labels[perfil]}
    </span>
  );
};

export const UsuariosListPage = () => {
  const [filters, setFilters] = useState<UsuarioFilters>({
    page: 0,
    size: 20,
    sort: 'nome,asc',
  });

  const { data, isLoading, error, refetch } = useUsuarios(filters);
  const deleteMutation = useDeleteUsuario();
  const reactivateMutation = useReactivateUsuario();

  const handleFilterChange = (key: keyof UsuarioFilters, value: any) => {
    setFilters((prev) => ({ ...prev, [key]: value, page: 0 }));
  };

  const handlePageChange = (newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage }));
  };

  const handleDesativar = async (id: string, nome: string) => {
    if (!window.confirm(`Deseja realmente desativar o usuário "${nome}"?`)) {
      return;
    }

    try {
      await deleteMutation.mutateAsync(id);
      refetch();
    } catch (error: any) {
      showError(`Erro ao desativar usuário: ${error.message}`);
    }
  };

  const handleReativar = async (id: string, nome: string) => {
    if (!window.confirm(`Deseja reativar o usuário "${nome}"?`)) {
      return;
    }

    try {
      await reactivateMutation.mutateAsync(id);
      refetch();
    } catch (error: any) {
      showError(`Erro ao reativar usuário: ${error.message}`);
    }
  };

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <p className="text-gray-600 dark:text-gray-400">Carregando usuários...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400 dark:text-red-300">
          Erro ao carregar usuários. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Usuários</h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            Gerencie os usuários do sistema (mecânicos, atendentes, gerentes, administradores)
          </p>
        </div>
        <Link
          to="/usuarios/novo"
          className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
        >
          <Plus className="h-5 w-5" />
          Novo Usuário
        </Link>
      </div>

      {/* Filters */}
      <div className="mb-6 rounded-lg bg-white dark:bg-gray-800 p-4 shadow">
        <div className="grid gap-4 md:grid-cols-3">
          {/* Filtro: Perfil */}
          <div>
            <label htmlFor="perfil" className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Perfil
            </label>
            <select
              id="perfil"
              value={filters.perfil || ''}
              onChange={(e) =>
                handleFilterChange('perfil', e.target.value || undefined)
              }
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            >
              <option value="">Todos os perfis</option>
              <option value="ADMIN">Administrador</option>
              <option value="GERENTE">Gerente</option>
              <option value="ATENDENTE">Atendente</option>
              <option value="MECANICO">Mecânico</option>
            </select>
          </div>

          {/* Filtro: Status Ativo */}
          <div>
            <label htmlFor="ativo" className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Status
            </label>
            <select
              id="ativo"
              value={
                filters.ativo === undefined ? '' : filters.ativo ? 'true' : 'false'
              }
              onChange={(e) => {
                const value = e.target.value;
                handleFilterChange(
                  'ativo',
                  value === '' ? undefined : value === 'true'
                );
              }}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            >
              <option value="">Todos</option>
              <option value="true">Ativos</option>
              <option value="false">Inativos</option>
            </select>
          </div>

          {/* Filtro: Ordenação */}
          <div>
            <label htmlFor="sort" className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Ordenar por
            </label>
            <select
              id="sort"
              value={filters.sort || 'nome,asc'}
              onChange={(e) => handleFilterChange('sort', e.target.value)}
              className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            >
              <option value="nome,asc">Nome (A-Z)</option>
              <option value="nome,desc">Nome (Z-A)</option>
              <option value="email,asc">Email (A-Z)</option>
              <option value="createdAt,desc">Mais recentes</option>
              <option value="createdAt,asc">Mais antigos</option>
              <option value="ultimoAcesso,desc">Último acesso (recente)</option>
            </select>
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-lg bg-white dark:bg-gray-800 shadow">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead className="bg-gray-50 dark:bg-gray-700">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-300">
                  Nome
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-300">
                  Email
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-300">
                  Perfil
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-300">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-300">
                  Último Acesso
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-300">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800">
              {data && data.content.length > 0 ? (
                data.content.map((usuario) => (
                  <tr key={usuario.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                    <td className="whitespace-nowrap px-6 py-4">
                      <div className="text-sm font-medium text-gray-900 dark:text-gray-100">
                        {usuario.nome}
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <div className="text-sm text-gray-600 dark:text-gray-400">{usuario.email}</div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <PerfilBadge perfil={usuario.perfil} />
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <span
                        className={`inline-flex rounded-full px-2 text-xs font-semibold leading-5 ${
                          usuario.ativo
                            ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                            : 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400'
                        }`}
                      >
                        {usuario.ativo ? 'Ativo' : 'Inativo'}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-600 dark:text-gray-400">
                      {formatDateTime(usuario.ultimoAcesso)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-right text-sm font-medium">
                      <div className="flex items-center justify-end gap-2">
                        <Link
                          to={`/usuarios/${usuario.id}`}
                          className="rounded p-1 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/30"
                          title="Visualizar"
                        >
                          <Eye className="h-5 w-5" />
                        </Link>
                        <Link
                          to={`/usuarios/${usuario.id}/editar`}
                          className="rounded p-1 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700"
                          title="Editar"
                        >
                          <Edit className="h-5 w-5" />
                        </Link>
                        {usuario.ativo ? (
                          <button
                            onClick={() => handleDesativar(usuario.id, usuario.nome)}
                            className="rounded p-1 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30"
                            title="Desativar"
                            disabled={deleteMutation.isPending}
                          >
                            <UserX className="h-5 w-5" />
                          </button>
                        ) : (
                          <button
                            onClick={() => handleReativar(usuario.id, usuario.nome)}
                            className="rounded p-1 text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30"
                            title="Reativar"
                            disabled={reactivateMutation.isPending}
                          >
                            <UserCheck className="h-5 w-5" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center">
                    <p className="text-gray-500 dark:text-gray-400">Nenhum usuário encontrado</p>
                    <Link
                      to="/usuarios/novo"
                      className="mt-2 inline-block text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300"
                    >
                      Criar primeiro usuário
                    </Link>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && data.content.length > 0 && (
          <div className="flex items-center justify-between border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 px-4 py-3 sm:px-6">
            <div className="flex flex-1 justify-between sm:hidden">
              <button
                onClick={() => handlePageChange(filters.page! - 1)}
                disabled={filters.page === 0}
                className="relative inline-flex items-center rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 disabled:opacity-50"
              >
                Anterior
              </button>
              <button
                onClick={() => handlePageChange(filters.page! + 1)}
                disabled={data.last}
                className="relative ml-3 inline-flex items-center rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 disabled:opacity-50"
              >
                Próxima
              </button>
            </div>
            <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
              <div>
                <p className="text-sm text-gray-700 dark:text-gray-300">
                  Mostrando{' '}
                  <span className="font-medium">{data.number * data.size + 1}</span>{' '}
                  a{' '}
                  <span className="font-medium">
                    {Math.min((data.number + 1) * data.size, data.totalElements)}
                  </span>{' '}
                  de <span className="font-medium">{data.totalElements}</span>{' '}
                  resultados
                </p>
              </div>
              <div>
                <nav
                  className="isolate inline-flex -space-x-px rounded-md shadow-sm"
                  aria-label="Pagination"
                >
                  <button
                    onClick={() => handlePageChange(filters.page! - 1)}
                    disabled={data.first}
                    className="relative inline-flex items-center rounded-l-md px-2 py-2 text-gray-400 dark:text-gray-500 ring-1 ring-inset ring-gray-300 dark:ring-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 focus:z-20 disabled:opacity-50"
                  >
                    <span className="sr-only">Anterior</span>
                    <svg
                      className="h-5 w-5"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                      aria-hidden="true"
                    >
                      <path
                        fillRule="evenodd"
                        d="M12.79 5.23a.75.75 0 01-.02 1.06L8.832 10l3.938 3.71a.75.75 0 11-1.04 1.08l-4.5-4.25a.75.75 0 010-1.08l4.5-4.25a.75.75 0 011.06.02z"
                        clipRule="evenodd"
                      />
                    </svg>
                  </button>
                  <span className="relative inline-flex items-center px-4 py-2 text-sm font-semibold text-gray-900 dark:text-gray-100 ring-1 ring-inset ring-gray-300 dark:ring-gray-600">
                    Página {data.number + 1} de {data.totalPages}
                  </span>
                  <button
                    onClick={() => handlePageChange(filters.page! + 1)}
                    disabled={data.last}
                    className="relative inline-flex items-center rounded-r-md px-2 py-2 text-gray-400 dark:text-gray-500 ring-1 ring-inset ring-gray-300 dark:ring-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 focus:z-20 disabled:opacity-50"
                  >
                    <span className="sr-only">Próxima</span>
                    <svg
                      className="h-5 w-5"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                      aria-hidden="true"
                    >
                      <path
                        fillRule="evenodd"
                        d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z"
                        clipRule="evenodd"
                      />
                    </svg>
                  </button>
                </nav>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
