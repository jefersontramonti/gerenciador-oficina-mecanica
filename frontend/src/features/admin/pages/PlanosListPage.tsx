/**
 * Planos List Page - View and manage subscription plans
 */

import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  CreditCard,
  Plus,
  Edit,
  Eye,
  EyeOff,
  Trash2,
  Star,
  Users,
  HardDrive,
  FileText,
  MessageSquare,
  Mail,
  Check,
  X,
  RefreshCw,
  TrendingUp,
} from 'lucide-react';
import { usePlanos, useDeletePlano, useTogglePlanoVisibility, usePlanosStatistics } from '../hooks/useSaas';
import { formatCurrency } from '@/shared/utils/formatters';
import { showSuccess, showError } from '@/shared/utils/notifications';
import { Modal } from '@/shared/components/ui/Modal';
import type { Plano } from '../types';

export const PlanosListPage = () => {
  const { data: planos, isLoading, error } = usePlanos();
  const { data: statistics } = usePlanosStatistics();
  const deleteMutation = useDeletePlano();
  const toggleVisibilityMutation = useTogglePlanoVisibility();

  const [confirmModal, setConfirmModal] = useState<{
    open: boolean;
    plano?: Plano;
  }>({ open: false });

  const handleToggleVisibility = async (plano: Plano) => {
    try {
      await toggleVisibilityMutation.mutateAsync(plano.id);
      showSuccess(
        plano.visivel
          ? 'Plano ocultado da página de preços'
          : 'Plano visível na página de preços'
      );
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao alterar visibilidade');
    }
  };

  const handleDelete = async () => {
    if (!confirmModal.plano) return;

    try {
      await deleteMutation.mutateAsync(confirmModal.plano.id);
      showSuccess('Plano desativado com sucesso!');
      setConfirmModal({ open: false });
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao desativar plano');
    }
  };

  const formatLimit = (limit: number): string => {
    if (limit === -1) return 'Ilimitado';
    return String(limit);
  };

  const formatStorage = (mb: number): string => {
    if (mb === -1) return 'Ilimitado';
    if (mb >= 1024) return `${(mb / 1024).toFixed(0)} GB`;
    return `${mb} MB`;
  };

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-300 bg-red-50 p-4 text-red-800 dark:border-red-800 dark:bg-red-900/20 dark:text-red-400">
          Erro ao carregar planos. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6 dark:bg-gray-900">
      {/* Header */}
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Planos de Assinatura
          </h1>
          <p className="mt-1 text-gray-600 dark:text-gray-400">
            Gerencie os planos disponíveis para as oficinas
          </p>
        </div>

        <Link
          to="/admin/planos/novo"
          className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
        >
          <Plus className="h-4 w-4" />
          Novo Plano
        </Link>
      </div>

      {/* Statistics */}
      {statistics && (
        <div className="mb-8 grid gap-4 md:grid-cols-4">
          <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-blue-100 p-2 dark:bg-blue-900/30">
                <CreditCard className="h-5 w-5 text-blue-600 dark:text-blue-400" />
              </div>
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400">Total de Planos</p>
                <p className="text-xl font-bold text-gray-900 dark:text-white">
                  {statistics.totalPlanos}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-green-100 p-2 dark:bg-green-900/30">
                <Check className="h-5 w-5 text-green-600 dark:text-green-400" />
              </div>
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400">Planos Ativos</p>
                <p className="text-xl font-bold text-gray-900 dark:text-white">
                  {statistics.planosAtivos}
                </p>
              </div>
            </div>
          </div>

          {statistics.oficinasPerPlano && (
            <>
              <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-purple-100 p-2 dark:bg-purple-900/30">
                    <Users className="h-5 w-5 text-purple-600 dark:text-purple-400" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-600 dark:text-gray-400">Oficinas Ativas</p>
                    <p className="text-xl font-bold text-gray-900 dark:text-white">
                      {Object.values(statistics.oficinasPerPlano).reduce((a, b) => a + b, 0)}
                    </p>
                  </div>
                </div>
              </div>

              <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-amber-100 p-2 dark:bg-amber-900/30">
                    <TrendingUp className="h-5 w-5 text-amber-600 dark:text-amber-400" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-600 dark:text-gray-400">MRR Total</p>
                    <p className="text-xl font-bold text-gray-900 dark:text-white">
                      {formatCurrency(
                        Object.values(statistics.mrrPerPlano || {}).reduce((a, b) => a + b, 0)
                      )}
                    </p>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      )}

      {/* Plans Grid */}
      {isLoading ? (
        <div className="flex h-64 items-center justify-center">
          <RefreshCw className="h-8 w-8 animate-spin text-gray-400" />
        </div>
      ) : (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {planos?.map((plano) => (
            <div
              key={plano.id}
              className={`relative rounded-xl bg-white p-6 shadow-lg transition-all hover:shadow-xl dark:bg-gray-800 ${
                plano.recomendado ? 'ring-2 ring-purple-500' : ''
              } ${!plano.ativo ? 'opacity-60' : ''}`}
              style={
                plano.corDestaque
                  ? { borderTop: `4px solid ${plano.corDestaque}` }
                  : undefined
              }
            >
              {/* Badges */}
              <div className="absolute -top-3 right-4 flex gap-2">
                {plano.recomendado && (
                  <span className="flex items-center gap-1 rounded-full bg-purple-600 px-3 py-1 text-xs font-semibold text-white">
                    <Star className="h-3 w-3" /> Recomendado
                  </span>
                )}
                {plano.tagPromocao && (
                  <span className="rounded-full bg-amber-500 px-3 py-1 text-xs font-semibold text-white">
                    {plano.tagPromocao}
                  </span>
                )}
              </div>

              {/* Status */}
              <div className="mb-4 flex items-center gap-2">
                <span
                  className={`rounded-full px-2 py-1 text-xs font-medium ${
                    plano.ativo
                      ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                      : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
                  }`}
                >
                  {plano.ativo ? 'Ativo' : 'Inativo'}
                </span>
                <span
                  className={`rounded-full px-2 py-1 text-xs font-medium ${
                    plano.visivel
                      ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'
                      : 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-400'
                  }`}
                >
                  {plano.visivel ? 'Visível' : 'Oculto'}
                </span>
              </div>

              {/* Header */}
              <div className="mb-4">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  {plano.codigo}
                </p>
                <h3 className="text-xl font-bold text-gray-900 dark:text-white">
                  {plano.nome}
                </h3>
              </div>

              {/* Price */}
              <div className="mb-4">
                {plano.precoSobConsulta ? (
                  <p className="text-2xl font-bold text-gray-900 dark:text-white">
                    Sob Consulta
                  </p>
                ) : (
                  <>
                    <p className="text-3xl font-bold text-gray-900 dark:text-white">
                      {formatCurrency(plano.valorMensal)}
                      <span className="text-base font-normal text-gray-500">/mês</span>
                    </p>
                    {plano.valorAnual && plano.descontoAnual && plano.descontoAnual > 0 && (
                      <p className="text-sm text-green-600 dark:text-green-400">
                        {formatCurrency(plano.valorAnual)}/ano ({plano.descontoAnual.toFixed(0)}% off)
                      </p>
                    )}
                  </>
                )}
              </div>

              {/* Description */}
              {plano.descricao && (
                <p className="mb-4 text-sm text-gray-600 dark:text-gray-400 line-clamp-2">
                  {plano.descricao}
                </p>
              )}

              {/* Limits */}
              <div className="mb-4 space-y-2">
                <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                  <Users className="h-4 w-4" />
                  <span>{formatLimit(plano.limiteUsuarios)} usuário(s)</span>
                </div>
                <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                  <HardDrive className="h-4 w-4" />
                  <span>{formatStorage(plano.limiteEspacoMb)} de armazenamento</span>
                </div>
                <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                  <FileText className="h-4 w-4" />
                  <span>{formatLimit(plano.limiteOsMes)} OS/mês</span>
                </div>
                <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                  <MessageSquare className="h-4 w-4" />
                  <span>{formatLimit(plano.limiteWhatsappMensagens)} WhatsApp/mês</span>
                </div>
                <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                  <Mail className="h-4 w-4" />
                  <span>{formatLimit(plano.limiteEmailsMes)} emails/mês</span>
                </div>
              </div>

              {/* Features */}
              <div className="mb-4 border-t border-gray-200 pt-4 dark:border-gray-700">
                <div className="grid grid-cols-2 gap-2 text-sm">
                  {Object.entries(plano.features || {}).map(([key, value]) => (
                    <div
                      key={key}
                      className={`flex items-center gap-1 ${
                        value
                          ? 'text-green-600 dark:text-green-400'
                          : 'text-gray-400 dark:text-gray-500'
                      }`}
                    >
                      {value ? (
                        <Check className="h-3 w-3" />
                      ) : (
                        <X className="h-3 w-3" />
                      )}
                      <span className="truncate text-xs">
                        {key.replace(/([A-Z])/g, ' $1').trim()}
                      </span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Statistics */}
              {statistics?.oficinasPerPlano && (
                <div className="mb-4 rounded-lg bg-gray-50 p-3 dark:bg-gray-700/50">
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600 dark:text-gray-400">Oficinas:</span>
                    <span className="font-semibold text-gray-900 dark:text-white">
                      {statistics.oficinasPerPlano[plano.codigo] || 0}
                    </span>
                  </div>
                  {statistics.mrrPerPlano && (
                    <div className="flex justify-between text-sm">
                      <span className="text-gray-600 dark:text-gray-400">MRR:</span>
                      <span className="font-semibold text-gray-900 dark:text-white">
                        {formatCurrency(statistics.mrrPerPlano[plano.codigo] || 0)}
                      </span>
                    </div>
                  )}
                </div>
              )}

              {/* Actions */}
              <div className="flex items-center gap-2 border-t border-gray-200 pt-4 dark:border-gray-700">
                <Link
                  to={`/admin/planos/${plano.id}/editar`}
                  className="flex flex-1 items-center justify-center gap-2 rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                >
                  <Edit className="h-4 w-4" />
                  Editar
                </Link>

                <button
                  onClick={() => handleToggleVisibility(plano)}
                  disabled={toggleVisibilityMutation.isPending}
                  className="rounded-lg border border-gray-300 p-2 text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                  title={plano.visivel ? 'Ocultar' : 'Mostrar'}
                >
                  {plano.visivel ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>

                <button
                  onClick={() => setConfirmModal({ open: true, plano })}
                  disabled={deleteMutation.isPending}
                  className="rounded-lg border border-red-300 p-2 text-red-600 hover:bg-red-50 disabled:opacity-50 dark:border-red-700 dark:text-red-400 dark:hover:bg-red-900/20"
                  title="Desativar"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={confirmModal.open}
        onClose={() => setConfirmModal({ open: false })}
        title="Desativar Plano"
      >
        <div className="space-y-4">
          <p className="text-gray-600 dark:text-gray-400">
            Tem certeza que deseja desativar o plano{' '}
            <strong>{confirmModal.plano?.nome}</strong>?
          </p>
          <p className="text-sm text-amber-600 dark:text-amber-400">
            Nota: Planos em uso por oficinas não podem ser excluídos, apenas desativados.
          </p>
          <div className="flex justify-end gap-3">
            <button
              onClick={() => setConfirmModal({ open: false })}
              className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Cancelar
            </button>
            <button
              onClick={handleDelete}
              disabled={deleteMutation.isPending}
              className="rounded-lg bg-red-600 px-4 py-2 text-white hover:bg-red-700 disabled:opacity-50"
            >
              {deleteMutation.isPending ? 'Desativando...' : 'Desativar'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
