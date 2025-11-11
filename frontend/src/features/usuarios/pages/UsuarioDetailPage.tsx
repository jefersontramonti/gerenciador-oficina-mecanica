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
    ADMIN: 'bg-purple-100 text-purple-800 border-purple-200',
    GERENTE: 'bg-blue-100 text-blue-800 border-blue-200',
    ATENDENTE: 'bg-green-100 text-green-800 border-green-200',
    MECANICO: 'bg-orange-100 text-orange-800 border-orange-200',
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
        <p className="text-gray-600">Carregando usuário...</p>
      </div>
    );
  }

  if (error || !usuario) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Usuário não encontrado
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/usuarios')}
            className="rounded-lg p-2 hover:bg-gray-100"
            title="Voltar"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{usuario.nome}</h1>
            <div className="mt-1 flex items-center gap-2">
              <PerfilBadge perfil={usuario.perfil} />
              <span
                className={`inline-flex rounded-full px-2 text-xs font-semibold leading-5 ${
                  usuario.ativo
                    ? 'bg-green-100 text-green-800'
                    : 'bg-red-100 text-red-800'
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
            className="flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50"
          >
            <Edit className="h-5 w-5" />
            Editar
          </Link>
          {usuario.ativo ? (
            <button
              onClick={handleDesativar}
              disabled={deleteMutation.isPending}
              className="flex items-center gap-2 rounded-lg border border-red-600 px-4 py-2 text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <UserX className="h-5 w-5" />
              Desativar
            </button>
          ) : (
            <button
              onClick={handleReativar}
              disabled={reactivateMutation.isPending}
              className="flex items-center gap-2 rounded-lg border border-green-600 px-4 py-2 text-green-600 hover:bg-green-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <UserCheck className="h-5 w-5" />
              Reativar
            </button>
          )}
        </div>
      </div>

      {/* Main Content */}
      <div className="grid gap-6 md:grid-cols-2">
        {/* Informações Básicas */}
        <div className="rounded-lg bg-white p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900">
            Informações Básicas
          </h2>
          <div className="space-y-4">
            <div>
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <Mail className="h-4 w-4" />
                <span>Email</span>
              </div>
              <p className="mt-1 text-gray-900">{usuario.email}</p>
            </div>

            <div>
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <Shield className="h-4 w-4" />
                <span>Perfil de Acesso</span>
              </div>
              <p className="mt-1 text-gray-900">{usuario.perfilNome || usuario.perfil}</p>
            </div>
          </div>
        </div>

        {/* Informações de Sistema */}
        <div className="rounded-lg bg-white p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900">
            Informações do Sistema
          </h2>
          <div className="space-y-4">
            <div>
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <Clock className="h-4 w-4" />
                <span>Último Acesso</span>
              </div>
              <p className="mt-1 text-gray-900">
                {formatDateTime(usuario.ultimoAcesso)}
              </p>
            </div>

            <div>
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <Calendar className="h-4 w-4" />
                <span>Cadastrado em</span>
              </div>
              <p className="mt-1 text-gray-900">{formatDateTime(usuario.createdAt)}</p>
            </div>

            <div>
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <Calendar className="h-4 w-4" />
                <span>Última atualização</span>
              </div>
              <p className="mt-1 text-gray-900">{formatDateTime(usuario.updatedAt)}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
