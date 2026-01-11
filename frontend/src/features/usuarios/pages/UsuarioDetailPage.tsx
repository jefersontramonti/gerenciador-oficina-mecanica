/**
 * Página de detalhes de um usuário
 * Exibe informações completas, com opções para editar, desativar/reativar
 */

import { useNavigate, useParams, Link } from 'react-router-dom';
import { ArrowLeft, Edit, UserX, UserCheck, Mail, Shield, Calendar, Clock } from 'lucide-react';
import { formatDateTime } from '@/shared/utils/dateFormatter';
import { showError } from '@/shared/utils/notifications';
import { useUsuario, useDeleteUsuario, useReactivateUsuario } from '../hooks/useUsuarios';
import type { PerfilUsuario } from '../types';

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
      className={`inline-flex items-center rounded-full border px-3 py-1 text-sm font-medium ${colors[perfil]}`}
    >
      {labels[perfil]}
    </span>
  );
};

export const UsuarioDetailPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();

  const { data: usuario, isLoading, error } = useUsuario(id);
  const deleteMutation = useDeleteUsuario();
  const reactivateMutation = useReactivateUsuario();

  const handleDesativar = async () => {
    if (window.confirm(`Tem certeza que deseja desativar o usuário "${usuario?.nome}"?`)) {
      try {
        await deleteMutation.mutateAsync(id!);
        navigate('/usuarios');
      } catch (error: any) {
        showError(`Erro ao desativar usuário: ${error.message}`);
      }
    }
  };

  const handleReativar = async () => {
    if (window.confirm(`Tem certeza que deseja reativar o usuário "${usuario?.nome}"?`)) {
      try {
        await reactivateMutation.mutateAsync(id!);
      } catch (error: any) {
        showError(`Erro ao reativar usuário: ${error.message}`);
      }
    }
  };

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <p className="text-gray-600 dark:text-gray-400">Carregando usuário...</p>
      </div>
    );
  }

  if (error || !usuario) {
    return (
      <div className="p-4 sm:p-6">
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400 dark:text-red-300">
          Usuário não encontrado
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-4 sm:mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3 sm:gap-4">
          <button
            onClick={() => navigate('/usuarios')}
            className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
            title="Voltar"
          >
            <ArrowLeft className="h-5 w-5 text-gray-900 dark:text-gray-100" />
          </button>
          <div className="min-w-0 flex-1">
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100 truncate">{usuario.nome}</h1>
            <div className="mt-1 flex flex-wrap items-center gap-2">
              <PerfilBadge perfil={usuario.perfil} />
              <span
                className={`inline-flex rounded-full px-2 text-xs font-semibold leading-5 ${
                  usuario.ativo
                    ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                    : 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400'
                }`}
              >
                {usuario.ativo ? 'Ativo' : 'Inativo'}
              </span>
            </div>
          </div>
        </div>

        <div className="flex gap-2">
          <Link
            to={`/usuarios/${id}/editar`}
            className="flex flex-1 sm:flex-none items-center justify-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 sm:px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
          >
            <Edit className="h-4 w-4 sm:h-5 sm:w-5" />
            <span className="hidden sm:inline">Editar</span>
          </Link>
          {usuario.ativo ? (
            <button
              onClick={handleDesativar}
              disabled={deleteMutation.isPending}
              className="flex flex-1 sm:flex-none items-center justify-center gap-2 rounded-lg border border-red-600 dark:border-red-700 bg-white dark:bg-gray-700 px-3 sm:px-4 py-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <UserX className="h-4 w-4 sm:h-5 sm:w-5" />
              <span className="hidden sm:inline">Desativar</span>
            </button>
          ) : (
            <button
              onClick={handleReativar}
              disabled={reactivateMutation.isPending}
              className="flex flex-1 sm:flex-none items-center justify-center gap-2 rounded-lg border border-green-600 dark:border-green-700 bg-white dark:bg-gray-700 px-3 sm:px-4 py-2 text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <UserCheck className="h-4 w-4 sm:h-5 sm:w-5" />
              <span className="hidden sm:inline">Reativar</span>
            </button>
          )}
        </div>
      </div>

      {/* Main Content */}
      <div className="grid gap-4 sm:gap-6 grid-cols-1 md:grid-cols-2">
        {/* Informações Básicas */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
            Informações Básicas
          </h2>
          <div className="space-y-4">
            <div>
              <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
                <Mail className="h-4 w-4" />
                <span>Email</span>
              </div>
              <p className="mt-1 text-gray-900 dark:text-gray-100">{usuario.email}</p>
            </div>

            <div>
              <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
                <Shield className="h-4 w-4" />
                <span>Perfil de Acesso</span>
              </div>
              <p className="mt-1 text-gray-900 dark:text-gray-100">{usuario.perfilNome || usuario.perfil}</p>
            </div>
          </div>
        </div>

        {/* Informações de Sistema */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
            Informações do Sistema
          </h2>
          <div className="space-y-4">
            <div>
              <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
                <Clock className="h-4 w-4" />
                <span>Último Acesso</span>
              </div>
              <p className="mt-1 text-gray-900 dark:text-gray-100">
                {formatDateTime(usuario.ultimoAcesso)}
              </p>
            </div>

            <div>
              <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
                <Calendar className="h-4 w-4" />
                <span>Cadastrado em</span>
              </div>
              <p className="mt-1 text-gray-900 dark:text-gray-100">{formatDateTime(usuario.createdAt)}</p>
            </div>

            <div>
              <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
                <Calendar className="h-4 w-4" />
                <span>Última atualização</span>
              </div>
              <p className="mt-1 text-gray-900 dark:text-gray-100">{formatDateTime(usuario.updatedAt)}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
