/**
 * Feature Flags Management Page - SUPER_ADMIN
 */

import { useState } from 'react';
import {
  Flag,
  Plus,
  ToggleLeft,
  ToggleRight,
  Trash2,
  Edit,
  ChevronDown,
  ChevronUp,
  Globe,
  Building2,
  Layers,
  Percent,
  Calendar,
  RefreshCw,
  Check,
  X,
  Info,
  UserPlus,
} from 'lucide-react';
import { OficinaSelectorModal } from '../components/OficinaSelectorModal';
import {
  useFeatureFlags,
  useToggleFeatureFlagGlobal,
  useDeleteFeatureFlag,
  useCreateFeatureFlag,
  useUpdateFeatureFlag,
} from '../hooks/useSaas';
import {
  type FeatureFlag,
  type CreateFeatureFlagRequest,
  type UpdateFeatureFlagRequest,
  planoLabels,
  PlanoAssinatura,
  categoriaFeatureFlagLabels,
} from '../types';
import { showSuccess, showError } from '@/shared/utils/notifications';

export const FeatureFlagsPage = () => {
  const { data: flags, isLoading, refetch } = useFeatureFlags();
  const toggleGlobalMutation = useToggleFeatureFlagGlobal();
  const deleteMutation = useDeleteFeatureFlag();
  const createMutation = useCreateFeatureFlag();
  const updateMutation = useUpdateFeatureFlag();

  const [expandedFlag, setExpandedFlag] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingFlag, setEditingFlag] = useState<FeatureFlag | null>(null);
  const [filterCategoria, setFilterCategoria] = useState<string>('');
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState('');
  const [oficinaSelectorFlag, setOficinaSelectorFlag] = useState<FeatureFlag | null>(null);
  const [confirmDisableFlag, setConfirmDisableFlag] = useState<FeatureFlag | null>(null);

  // Filter flags
  const filteredFlags = flags?.filter((flag) => {
    const matchesCategoria = !filterCategoria || flag.categoria === filterCategoria;
    const matchesSearch = !searchTerm ||
      flag.nome.toLowerCase().includes(searchTerm.toLowerCase()) ||
      flag.codigo.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = !filterStatus ||
      (filterStatus === 'ativo' && flag.habilitadoGlobal) ||
      (filterStatus === 'inativo' && !flag.habilitadoGlobal) ||
      (filterStatus === 'por_plano' && Object.values(flag.habilitadoPorPlano || {}).some(v => v)) ||
      (filterStatus === 'rollout' && flag.percentualRollout > 0 && flag.percentualRollout < 100);
    return matchesCategoria && matchesSearch && matchesStatus;
  }) || [];

  const handleToggleGlobal = async (flag: FeatureFlag, confirmed = false) => {
    // Se está habilitada e vai desabilitar, pedir confirmação
    if (flag.habilitadoGlobal && !confirmed) {
      setConfirmDisableFlag(flag);
      return;
    }

    try {
      await toggleGlobalMutation.mutateAsync({
        id: flag.id,
        habilitado: !flag.habilitadoGlobal,
      });
      showSuccess(`Feature "${flag.nome}" ${!flag.habilitadoGlobal ? 'habilitada' : 'desabilitada'} globalmente`);
      setConfirmDisableFlag(null);
    } catch (error) {
      showError('Erro ao alterar feature flag');
    }
  };

  const handleDelete = async (flag: FeatureFlag) => {
    if (!confirm(`Tem certeza que deseja excluir a feature "${flag.nome}"?`)) {
      return;
    }
    try {
      await deleteMutation.mutateAsync(flag.id);
      showSuccess('Feature flag excluída com sucesso');
    } catch (error) {
      showError('Erro ao excluir feature flag');
    }
  };

  const handleUpdateOficinas = async (selectedIds: string[]) => {
    if (!oficinaSelectorFlag) return;
    try {
      await updateMutation.mutateAsync({
        id: oficinaSelectorFlag.id,
        data: {
          habilitadoPorOficina: selectedIds,
        },
      });
      showSuccess(`Oficinas atualizadas para "${oficinaSelectorFlag.nome}"`);
      setOficinaSelectorFlag(null);
    } catch (error) {
      showError('Erro ao atualizar oficinas');
    }
  };

  const getCategoriaBadgeColor = (categoria: string) => {
    switch (categoria) {
      case 'PREMIUM':
        return 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400';
      case 'COMUNICACAO':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400';
      case 'RELATORIOS':
        return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400';
      case 'FINANCEIRO':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400';
      case 'INTEGRACAO':
        return 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400';
      case 'OPERACIONAL':
        return 'bg-cyan-100 text-cyan-800 dark:bg-cyan-900/30 dark:text-cyan-400';
      case 'FISCAL':
        return 'bg-indigo-100 text-indigo-800 dark:bg-indigo-900/30 dark:text-indigo-400';
      case 'MOBILE':
        return 'bg-pink-100 text-pink-800 dark:bg-pink-900/30 dark:text-pink-400';
      case 'BRANDING':
        return 'bg-rose-100 text-rose-800 dark:bg-rose-900/30 dark:text-rose-400';
      case 'MARKETING':
        return 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400';
      case 'SEGURANCA':
        return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300';
    }
  };

  const formatDate = (dateValue?: string) => {
    if (!dateValue) return '-';
    return new Date(dateValue).toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const planosHabilitados = (flag: FeatureFlag) => {
    if (!flag.habilitadoPorPlano) return [];
    return Object.entries(flag.habilitadoPorPlano)
      .filter(([_, enabled]) => enabled)
      .map(([plano, _]) => plano);
  };

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Feature Flags
          </h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            Gerenciar funcionalidades e rollout gradual
          </p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={() => refetch()}
            className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700"
          >
            <RefreshCw className="h-4 w-4" />
            Atualizar
          </button>
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
          >
            <Plus className="h-4 w-4" />
            Nova Feature
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="mb-6 flex flex-wrap gap-4">
        <div className="flex-1 min-w-[200px]">
          <input
            type="text"
            placeholder="Buscar por nome ou código..."
            className="w-full rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-800 dark:text-white"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <select
          className="rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-800 dark:text-white"
          value={filterStatus}
          onChange={(e) => setFilterStatus(e.target.value)}
        >
          <option value="">Todos Status</option>
          <option value="ativo">Ativo Globalmente</option>
          <option value="inativo">Inativo Globalmente</option>
          <option value="por_plano">Habilitado por Plano</option>
          <option value="rollout">Em Rollout</option>
        </select>
        <select
          className="rounded-lg border border-gray-300 px-4 py-2 focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-800 dark:text-white"
          value={filterCategoria}
          onChange={(e) => setFilterCategoria(e.target.value)}
        >
          <option value="">Todas Categorias</option>
          {Object.entries(categoriaFeatureFlagLabels).map(([key, label]) => (
            <option key={key} value={key}>
              {label}
            </option>
          ))}
        </select>
        {(filterStatus || filterCategoria || searchTerm) && (
          <button
            onClick={() => {
              setFilterStatus('');
              setFilterCategoria('');
              setSearchTerm('');
            }}
            className="rounded-lg border border-gray-300 px-4 py-2 text-gray-600 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-400 dark:hover:bg-gray-700"
          >
            Limpar filtros
          </button>
        )}
      </div>

      {/* Stats Cards */}
      <div className="mb-6 grid gap-4 sm:grid-cols-4">
        <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-blue-100 p-2 dark:bg-blue-900/30">
              <Flag className="h-5 w-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Total Features</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {flags?.length || 0}
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-green-100 p-2 dark:bg-green-900/30">
              <Globe className="h-5 w-5 text-green-600 dark:text-green-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Habilitadas Globalmente</p>
              <p className="text-xl font-bold text-green-600 dark:text-green-400">
                {flags?.filter((f) => f.habilitadoGlobal).length || 0}
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-purple-100 p-2 dark:bg-purple-900/30">
              <Layers className="h-5 w-5 text-purple-600 dark:text-purple-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Por Plano</p>
              <p className="text-xl font-bold text-purple-600 dark:text-purple-400">
                {flags?.filter((f) => planosHabilitados(f).length > 0).length || 0}
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-orange-100 p-2 dark:bg-orange-900/30">
              <Percent className="h-5 w-5 text-orange-600 dark:text-orange-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Em Rollout</p>
              <p className="text-xl font-bold text-orange-600 dark:text-orange-400">
                {flags?.filter((f) => f.percentualRollout > 0 && f.percentualRollout < 100).length || 0}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Flags List */}
      <div className="rounded-lg bg-white shadow dark:bg-gray-800">
        {isLoading ? (
          <div className="flex h-64 items-center justify-center">
            <RefreshCw className="h-8 w-8 animate-spin text-gray-400" />
          </div>
        ) : filteredFlags.length === 0 ? (
          <div className="flex h-64 flex-col items-center justify-center text-gray-500 dark:text-gray-400">
            <Flag className="mb-4 h-12 w-12" />
            <p>Nenhuma feature flag encontrada</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-200 dark:divide-gray-700">
            {filteredFlags.map((flag) => (
              <div key={flag.id} className="p-4">
                {/* Flag Header */}
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    {/* Toggle Button */}
                    <button
                      onClick={() => handleToggleGlobal(flag)}
                      disabled={toggleGlobalMutation.isPending}
                      className="focus:outline-none"
                      title={flag.habilitadoGlobal ? 'Desabilitar globalmente' : 'Habilitar globalmente'}
                    >
                      {flag.habilitadoGlobal ? (
                        <ToggleRight className="h-8 w-8 text-green-500" />
                      ) : (
                        <ToggleLeft className="h-8 w-8 text-gray-400" />
                      )}
                    </button>

                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="font-medium text-gray-900 dark:text-white">
                          {flag.nome}
                        </h3>
                        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${getCategoriaBadgeColor(flag.categoria)}`}>
                          {categoriaFeatureFlagLabels[flag.categoria] || flag.categoria}
                        </span>
                        {!flag.ativo && (
                          <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800 dark:bg-red-900/30 dark:text-red-400">
                            Inativo
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        <code className="rounded bg-gray-100 px-1 dark:bg-gray-700">
                          {flag.codigo}
                        </code>
                        {flag.descricao && (
                          <span className="ml-2">{flag.descricao}</span>
                        )}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    {/* Status indicators */}
                    <div className="flex items-center gap-3 text-sm">
                      {flag.habilitadoGlobal && (
                        <span className="flex items-center gap-1 text-green-600 dark:text-green-400">
                          <Globe className="h-4 w-4" />
                          Global
                        </span>
                      )}
                      {planosHabilitados(flag).length > 0 && (
                        <span className="flex items-center gap-1 text-purple-600 dark:text-purple-400">
                          <Layers className="h-4 w-4" />
                          {planosHabilitados(flag).length} plano(s)
                        </span>
                      )}
                      {flag.habilitadoPorOficina?.length > 0 && (
                        <span className="flex items-center gap-1 text-blue-600 dark:text-blue-400">
                          <Building2 className="h-4 w-4" />
                          {flag.habilitadoPorOficina.length} oficina(s)
                        </span>
                      )}
                      {flag.percentualRollout > 0 && (
                        <span className="flex items-center gap-1 text-orange-600 dark:text-orange-400">
                          <Percent className="h-4 w-4" />
                          {flag.percentualRollout}%
                        </span>
                      )}
                    </div>

                    {/* Actions */}
                    <button
                      onClick={() => setEditingFlag(flag)}
                      className="rounded p-1 text-gray-500 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-300"
                      title="Editar"
                    >
                      <Edit className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(flag)}
                      disabled={deleteMutation.isPending}
                      className="rounded p-1 text-gray-500 hover:bg-red-100 hover:text-red-700 dark:text-gray-400 dark:hover:bg-red-900/30 dark:hover:text-red-400"
                      title="Excluir"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => setExpandedFlag(expandedFlag === flag.id ? null : flag.id)}
                      className="rounded p-1 text-gray-500 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-300"
                    >
                      {expandedFlag === flag.id ? (
                        <ChevronUp className="h-4 w-4" />
                      ) : (
                        <ChevronDown className="h-4 w-4" />
                      )}
                    </button>
                  </div>
                </div>

                {/* Expanded Details */}
                {expandedFlag === flag.id && (
                  <div className="mt-4 rounded-lg bg-gray-50 p-4 dark:bg-gray-700/50">
                    <div className="grid gap-4 md:grid-cols-3">
                      {/* Planos habilitados */}
                      <div>
                        <h4 className="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                          Planos Habilitados
                        </h4>
                        <div className="flex flex-wrap gap-2">
                          {Object.entries(PlanoAssinatura).map(([key, value]) => {
                            const enabled = flag.habilitadoPorPlano?.[value];
                            return (
                              <span
                                key={key}
                                className={`flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium ${
                                  enabled
                                    ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                                    : 'bg-gray-100 text-gray-500 dark:bg-gray-600 dark:text-gray-400'
                                }`}
                              >
                                {enabled ? <Check className="h-3 w-3" /> : <X className="h-3 w-3" />}
                                {planoLabels[value]}
                              </span>
                            );
                          })}
                        </div>
                      </div>

                      {/* Oficinas específicas */}
                      <div>
                        <h4 className="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                          Oficinas Específicas (Beta Testers)
                        </h4>
                        <div className="flex items-center gap-3">
                          <p className="text-sm text-gray-600 dark:text-gray-400">
                            {flag.habilitadoPorOficina?.length || 0} oficina(s) habilitada(s)
                          </p>
                          <button
                            onClick={() => setOficinaSelectorFlag(flag)}
                            className="flex items-center gap-1 rounded-lg border border-blue-500 px-2 py-1 text-xs font-medium text-blue-600 hover:bg-blue-50 dark:border-blue-400 dark:text-blue-400 dark:hover:bg-blue-900/20"
                          >
                            <UserPlus className="h-3 w-3" />
                            Gerenciar
                          </button>
                        </div>
                      </div>

                      {/* Rollout e Período */}
                      <div>
                        <h4 className="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                          Rollout & Período
                        </h4>
                        <div className="space-y-1 text-sm text-gray-600 dark:text-gray-400">
                          <p className="flex items-center gap-2">
                            <Percent className="h-4 w-4" />
                            Percentual: {flag.percentualRollout}%
                          </p>
                          {(flag.dataInicio || flag.dataFim) && (
                            <p className="flex items-center gap-2">
                              <Calendar className="h-4 w-4" />
                              {flag.dataInicio ? formatDate(flag.dataInicio) : 'Sem início'} -{' '}
                              {flag.dataFim ? formatDate(flag.dataFim) : 'Sem fim'}
                            </p>
                          )}
                          {flag.requerAutorizacao && (
                            <p className="flex items-center gap-2 text-yellow-600 dark:text-yellow-400">
                              <Info className="h-4 w-4" />
                              Requer autorização
                            </p>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="mt-4 border-t border-gray-200 pt-4 dark:border-gray-600">
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        Criado em {formatDate(flag.createdAt)} | Atualizado em {formatDate(flag.updatedAt)}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Create/Edit Modal */}
      {(showCreateModal || editingFlag) && (
        <FeatureFlagModal
          flag={editingFlag}
          onClose={() => {
            setShowCreateModal(false);
            setEditingFlag(null);
          }}
          onSave={async (data) => {
            try {
              if (editingFlag) {
                await updateMutation.mutateAsync({ id: editingFlag.id, data });
                showSuccess('Feature flag atualizada com sucesso');
              } else {
                await createMutation.mutateAsync(data as CreateFeatureFlagRequest);
                showSuccess('Feature flag criada com sucesso');
              }
              setShowCreateModal(false);
              setEditingFlag(null);
            } catch (error: any) {
              showError(error.response?.data?.message || 'Erro ao salvar feature flag');
            }
          }}
          isLoading={createMutation.isPending || updateMutation.isPending}
        />
      )}

      {/* Oficina Selector Modal */}
      <OficinaSelectorModal
        isOpen={!!oficinaSelectorFlag}
        onClose={() => setOficinaSelectorFlag(null)}
        onConfirm={handleUpdateOficinas}
        initialSelectedIds={oficinaSelectorFlag?.habilitadoPorOficina || []}
        title={`Oficinas para "${oficinaSelectorFlag?.nome || ''}"`}
        description="Selecione as oficinas que terão acesso a esta feature como beta testers"
      />

      {/* Confirm Disable Modal */}
      {confirmDisableFlag && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl dark:bg-gray-800">
            <div className="mb-4 flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-yellow-100 dark:bg-yellow-900/30">
                <Info className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                Desativar Feature
              </h3>
            </div>

            <p className="mb-4 text-gray-600 dark:text-gray-400">
              Tem certeza que deseja desativar globalmente a feature{' '}
              <strong className="text-gray-900 dark:text-white">
                {confirmDisableFlag.nome}
              </strong>
              ?
            </p>

            <div className="mb-4 rounded-lg bg-yellow-50 p-3 dark:bg-yellow-900/20">
              <p className="text-sm text-yellow-800 dark:text-yellow-300">
                <strong>Atenção:</strong> Esta feature será desabilitada para todas as
                oficinas que dependem da flag global. Oficinas com acesso específico
                continuarão com acesso.
              </p>
            </div>

            <div className="flex justify-end gap-3">
              <button
                onClick={() => setConfirmDisableFlag(null)}
                className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                Cancelar
              </button>
              <button
                onClick={() => handleToggleGlobal(confirmDisableFlag, true)}
                disabled={toggleGlobalMutation.isPending}
                className="rounded-lg bg-yellow-600 px-4 py-2 text-white hover:bg-yellow-700 disabled:opacity-50"
              >
                {toggleGlobalMutation.isPending ? 'Desativando...' : 'Sim, desativar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// Modal Component
interface FeatureFlagModalProps {
  flag: FeatureFlag | null;
  onClose: () => void;
  onSave: (data: CreateFeatureFlagRequest | UpdateFeatureFlagRequest) => void;
  isLoading: boolean;
}

const FeatureFlagModal = ({ flag, onClose, onSave, isLoading }: FeatureFlagModalProps) => {
  // Helper para formatar data para input datetime-local
  const formatDateForInput = (dateStr?: string) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toISOString().slice(0, 16); // Format: YYYY-MM-DDTHH:mm
  };

  const [formData, setFormData] = useState({
    codigo: flag?.codigo || '',
    nome: flag?.nome || '',
    descricao: flag?.descricao || '',
    categoria: flag?.categoria || 'GERAL',
    habilitadoGlobal: flag?.habilitadoGlobal || false,
    percentualRollout: flag?.percentualRollout || 0,
    requerAutorizacao: flag?.requerAutorizacao || false,
    habilitadoPorPlano: flag?.habilitadoPorPlano || {},
    dataInicio: formatDateForInput(flag?.dataInicio),
    dataFim: formatDateForInput(flag?.dataFim),
  });

  // Validação: dataFim deve ser maior que dataInicio
  const isDateValid = () => {
    if (formData.dataInicio && formData.dataFim) {
      return new Date(formData.dataFim) > new Date(formData.dataInicio);
    }
    return true;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!isDateValid()) {
      return;
    }
    // Converter strings vazias para undefined
    const dataToSave = {
      ...formData,
      dataInicio: formData.dataInicio || undefined,
      dataFim: formData.dataFim || undefined,
    };
    onSave(dataToSave);
  };

  const togglePlano = (plano: string) => {
    setFormData((prev) => ({
      ...prev,
      habilitadoPorPlano: {
        ...prev.habilitadoPorPlano,
        [plano]: !prev.habilitadoPorPlano[plano],
      },
    }));
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-xl max-h-[90vh] overflow-y-auto rounded-lg bg-white p-6 shadow-xl dark:bg-gray-800">
        <h2 className="mb-4 text-xl font-bold text-gray-900 dark:text-white">
          {flag ? 'Editar Feature Flag' : 'Nova Feature Flag'}
        </h2>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Código */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Código *
            </label>
            <input
              type="text"
              required
              disabled={!!flag}
              pattern="^[A-Z][A-Z0-9_]*$"
              placeholder="MINHA_FEATURE"
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none disabled:bg-gray-100 dark:border-gray-600 dark:bg-gray-700 dark:text-white dark:disabled:bg-gray-600"
              value={formData.codigo}
              onChange={(e) => setFormData({ ...formData, codigo: e.target.value.toUpperCase() })}
            />
            <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
              Maiúsculas, números e underscores
            </p>
          </div>

          {/* Nome */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Nome *
            </label>
            <input
              type="text"
              required
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              value={formData.nome}
              onChange={(e) => setFormData({ ...formData, nome: e.target.value })}
            />
          </div>

          {/* Descrição */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Descrição
            </label>
            <textarea
              rows={2}
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              value={formData.descricao}
              onChange={(e) => setFormData({ ...formData, descricao: e.target.value })}
            />
          </div>

          {/* Categoria */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Categoria
            </label>
            <select
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              value={formData.categoria}
              onChange={(e) => setFormData({ ...formData, categoria: e.target.value })}
            >
              {Object.entries(categoriaFeatureFlagLabels).map(([key, label]) => (
                <option key={key} value={key}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          {/* Habilitado Global */}
          <div className="flex items-center gap-3">
            <input
              type="checkbox"
              id="habilitadoGlobal"
              className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              checked={formData.habilitadoGlobal}
              onChange={(e) => setFormData({ ...formData, habilitadoGlobal: e.target.checked })}
            />
            <label htmlFor="habilitadoGlobal" className="text-sm text-gray-700 dark:text-gray-300">
              Habilitado globalmente para todas as oficinas
            </label>
          </div>

          {/* Planos */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Habilitar por Plano
            </label>
            <div className="mt-2 flex flex-wrap gap-2">
              {Object.entries(PlanoAssinatura).map(([key, value]) => (
                <button
                  key={key}
                  type="button"
                  onClick={() => togglePlano(value)}
                  className={`rounded-full px-3 py-1 text-sm font-medium transition-colors ${
                    formData.habilitadoPorPlano[value]
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600'
                  }`}
                >
                  {planoLabels[value]}
                </button>
              ))}
            </div>
          </div>

          {/* Percentual Rollout */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Percentual de Rollout: {formData.percentualRollout}%
            </label>
            <input
              type="range"
              min="0"
              max="100"
              className="mt-2 w-full"
              value={formData.percentualRollout}
              onChange={(e) => setFormData({ ...formData, percentualRollout: Number(e.target.value) })}
            />
            <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
              0% = Desabilitado, 100% = Habilitado para todas oficinas não cobertas por outras regras
            </p>
          </div>

          {/* Período de Validade */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Período de Validade
            </label>
            <div className="mt-2 grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">
                  Data Início
                </label>
                <input
                  type="datetime-local"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  value={formData.dataInicio}
                  onChange={(e) => setFormData({ ...formData, dataInicio: e.target.value })}
                />
              </div>
              <div>
                <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">
                  Data Fim
                </label>
                <input
                  type="datetime-local"
                  className={`w-full rounded-lg border px-3 py-2 text-sm focus:outline-none dark:bg-gray-700 dark:text-white ${
                    !isDateValid()
                      ? 'border-red-500 focus:border-red-500'
                      : 'border-gray-300 focus:border-blue-500 dark:border-gray-600'
                  }`}
                  value={formData.dataFim}
                  onChange={(e) => setFormData({ ...formData, dataFim: e.target.value })}
                />
              </div>
            </div>
            {!isDateValid() && (
              <p className="mt-1 text-xs text-red-500">
                Data fim deve ser posterior à data início
              </p>
            )}
            <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
              💡 Deixe em branco para feature permanente
            </p>
          </div>

          {/* Requer Autorização */}
          <div className="flex items-center gap-3">
            <input
              type="checkbox"
              id="requerAutorizacao"
              className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              checked={formData.requerAutorizacao}
              onChange={(e) => setFormData({ ...formData, requerAutorizacao: e.target.checked })}
            />
            <label htmlFor="requerAutorizacao" className="text-sm text-gray-700 dark:text-gray-300">
              Requer autorização especial
            </label>
          </div>

          {/* Preview de Impacto */}
          <div className="rounded-lg border border-blue-200 bg-blue-50 p-4 dark:border-blue-900 dark:bg-blue-900/20">
            <h4 className="mb-2 flex items-center gap-2 text-sm font-medium text-blue-800 dark:text-blue-300">
              <Layers className="h-4 w-4" />
              Preview de Impacto
            </h4>
            <div className="space-y-1 text-sm text-blue-700 dark:text-blue-400">
              {formData.habilitadoGlobal ? (
                <p className="flex items-center gap-2">
                  <Globe className="h-4 w-4" />
                  <strong>Todas as oficinas</strong> terão acesso
                </p>
              ) : (
                <>
                  {Object.entries(formData.habilitadoPorPlano).filter(([_, v]) => v).length > 0 && (
                    <p className="flex items-center gap-2">
                      <Layers className="h-4 w-4" />
                      Oficinas com plano:{' '}
                      <strong>
                        {Object.entries(formData.habilitadoPorPlano)
                          .filter(([_, v]) => v)
                          .map(([k]) => planoLabels[k as keyof typeof planoLabels])
                          .join(', ')}
                      </strong>
                    </p>
                  )}
                  {formData.percentualRollout > 0 && formData.percentualRollout < 100 && (
                    <p className="flex items-center gap-2">
                      <Percent className="h-4 w-4" />
                      <strong>{formData.percentualRollout}%</strong> das oficinas restantes (rollout gradual)
                    </p>
                  )}
                  {formData.percentualRollout === 100 && (
                    <p className="flex items-center gap-2">
                      <Percent className="h-4 w-4" />
                      <strong>100%</strong> das oficinas restantes
                    </p>
                  )}
                  {!formData.habilitadoGlobal &&
                   Object.entries(formData.habilitadoPorPlano).filter(([_, v]) => v).length === 0 &&
                   formData.percentualRollout === 0 && (
                    <p className="text-gray-500 dark:text-gray-400">
                      Nenhuma oficina terá acesso automático.
                      Use "Oficinas Específicas" após criar para adicionar beta testers.
                    </p>
                  )}
                </>
              )}
              {(formData.dataInicio || formData.dataFim) && (
                <p className="mt-2 flex items-center gap-2 text-xs">
                  <Calendar className="h-3 w-3" />
                  Válido: {formData.dataInicio ? new Date(formData.dataInicio).toLocaleDateString('pt-BR') : 'Imediato'}
                  {' → '}
                  {formData.dataFim ? new Date(formData.dataFim).toLocaleDateString('pt-BR') : 'Permanente'}
                </p>
              )}
            </div>
          </div>

          {/* Buttons */}
          <div className="flex justify-end gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {isLoading ? 'Salvando...' : flag ? 'Atualizar' : 'Criar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default FeatureFlagsPage;
