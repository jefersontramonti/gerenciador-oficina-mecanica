import { useState } from 'react';
import { Plus, Edit2, Trash2, Package, Check, X, Loader2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  usePlanos,
  useCriarPlano,
  useAtualizarPlano,
  useDesativarPlano,
} from '../hooks/useAssinaturas';
import type { PlanoAssinaturaDTO } from '../types/assinatura';
import { periodicidadeLabels } from '../types/assinatura';
import { FeatureGate } from '@/shared/components/FeatureGate';

const planoSchema = z.object({
  nome: z.string().min(1, 'Nome é obrigatório'),
  descricao: z.string().optional(),
  valor: z.number().min(0.01, 'Valor deve ser maior que zero'),
  periodicidade: z.enum(['SEMANAL', 'QUINZENAL', 'MENSAL', 'TRIMESTRAL', 'SEMESTRAL', 'ANUAL']),
  ativo: z.boolean(),
});

type PlanoFormData = z.infer<typeof planoSchema>;

export default function PlanosAssinaturaPage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [editingPlano, setEditingPlano] = useState<PlanoAssinaturaDTO | null>(null);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [planoToDelete, setPlanoToDelete] = useState<PlanoAssinaturaDTO | null>(null);

  const { data: planos, isLoading } = usePlanos();
  const criarPlano = useCriarPlano();
  const atualizarPlano = useAtualizarPlano();
  const desativarPlano = useDesativarPlano();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PlanoFormData>({
    resolver: zodResolver(planoSchema),
    defaultValues: {
      ativo: true,
      periodicidade: 'MENSAL',
    },
  });

  const openCreate = () => {
    setEditingPlano(null);
    reset({
      nome: '',
      descricao: '',
      valor: 0,
      periodicidade: 'MENSAL',
      ativo: true,
    });
    setModalOpen(true);
  };

  const openEdit = (plano: PlanoAssinaturaDTO) => {
    setEditingPlano(plano);
    reset({
      nome: plano.nome,
      descricao: plano.descricao || '',
      valor: plano.valor,
      periodicidade: plano.periodicidade,
      ativo: plano.ativo,
    });
    setModalOpen(true);
  };

  const openDeleteConfirm = (plano: PlanoAssinaturaDTO) => {
    setPlanoToDelete(plano);
    setDeleteConfirmOpen(true);
  };

  const onSubmit = async (data: PlanoFormData) => {
    const planoData: PlanoAssinaturaDTO = {
      ...data,
      id: editingPlano?.id,
    };

    try {
      if (editingPlano) {
        await atualizarPlano.mutateAsync({ id: editingPlano.id!, plano: planoData });
      } else {
        await criarPlano.mutateAsync(planoData);
      }
      setModalOpen(false);
      reset();
    } catch {
      // Error handled by mutation
    }
  };

  const handleDelete = async () => {
    if (planoToDelete) {
      try {
        await desativarPlano.mutateAsync(planoToDelete.id!);
        setDeleteConfirmOpen(false);
        setPlanoToDelete(null);
      } catch {
        // Error handled by mutation
      }
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  return (
    <FeatureGate feature="COBRANCA_RECORRENTE" fallback={<div>Feature não disponível no seu plano</div>}>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
              Planos de Assinatura
            </h1>
            <p className="text-gray-600 dark:text-gray-400 mt-1">
              Gerencie os planos de cobrança recorrente
            </p>
          </div>
          <button
            onClick={openCreate}
            className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus className="w-4 h-4 mr-2" />
            Novo Plano
          </button>
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center h-64">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
          </div>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {planos?.map((plano) => (
              <div
                key={plano.id}
                className={`bg-white dark:bg-gray-800 rounded-lg shadow-sm border p-6 ${
                  plano.ativo
                    ? 'border-gray-200 dark:border-gray-700'
                    : 'border-gray-300 dark:border-gray-600 opacity-60'
                }`}
              >
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                      <Package className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900 dark:text-white">
                        {plano.nome}
                      </h3>
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                          plano.ativo
                            ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                            : 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400'
                        }`}
                      >
                        {plano.ativo ? (
                          <>
                            <Check className="w-3 h-3 mr-1" />
                            Ativo
                          </>
                        ) : (
                          <>
                            <X className="w-3 h-3 mr-1" />
                            Inativo
                          </>
                        )}
                      </span>
                    </div>
                  </div>
                  <div className="flex gap-1">
                    <button
                      onClick={() => openEdit(plano)}
                      className="p-1.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                    {plano.ativo && (
                      <button
                        onClick={() => openDeleteConfirm(plano)}
                        className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>

                <p className="text-sm text-gray-600 dark:text-gray-400 mb-4 line-clamp-2">
                  {plano.descricao || 'Sem descrição'}
                </p>

                <div className="flex items-baseline justify-between pt-4 border-t border-gray-200 dark:border-gray-700">
                  <div>
                    <span className="text-2xl font-bold text-gray-900 dark:text-white">
                      {formatCurrency(plano.valor)}
                    </span>
                    <span className="text-sm text-gray-500 dark:text-gray-400">
                      /{periodicidadeLabels[plano.periodicidade].toLowerCase()}
                    </span>
                  </div>
                </div>
              </div>
            ))}

            {planos?.length === 0 && (
              <div className="col-span-full text-center py-12 bg-white dark:bg-gray-800 rounded-lg shadow-sm">
                <Package className="w-12 h-12 mx-auto text-gray-400 mb-4" />
                <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
                  Nenhum plano cadastrado
                </h3>
                <p className="text-gray-500 dark:text-gray-400 mb-4">
                  Crie seu primeiro plano de assinatura
                </p>
                <button
                  onClick={openCreate}
                  className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  Criar Plano
                </button>
              </div>
            )}
          </div>
        )}

        {/* Modal de Criação/Edição */}
        {modalOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-md p-6">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">
                {editingPlano ? 'Editar Plano' : 'Novo Plano'}
              </h2>
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Nome do Plano
                  </label>
                  <input
                    {...register('nome')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                    placeholder="Ex: Plano Mensal Básico"
                  />
                  {errors.nome && (
                    <p className="text-red-500 text-sm mt-1">{errors.nome.message}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Descrição
                  </label>
                  <input
                    {...register('descricao')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                    placeholder="Descrição do plano (opcional)"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Valor (R$)
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      {...register('valor', { valueAsNumber: true })}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                    />
                    {errors.valor && (
                      <p className="text-red-500 text-sm mt-1">{errors.valor.message}</p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Periodicidade
                    </label>
                    <select
                      {...register('periodicidade')}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                    >
                      {Object.entries(periodicidadeLabels).map(([value, label]) => (
                        <option key={value} value={value}>
                          {label}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="ativo"
                    {...register('ativo')}
                    className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                  />
                  <label htmlFor="ativo" className="text-sm text-gray-700 dark:text-gray-300">
                    Plano ativo (disponível para novas assinaturas)
                  </label>
                </div>

                <div className="flex justify-end gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setModalOpen(false)}
                    className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    disabled={criarPlano.isPending || atualizarPlano.isPending}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 flex items-center gap-2"
                  >
                    {(criarPlano.isPending || atualizarPlano.isPending) && (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    )}
                    {editingPlano ? 'Salvar' : 'Criar Plano'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Confirmação de Exclusão */}
        {deleteConfirmOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-sm p-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                Desativar Plano
              </h2>
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                Tem certeza que deseja desativar o plano "{planoToDelete?.nome}"? Assinaturas existentes não serão afetadas.
              </p>
              <div className="flex justify-end gap-3">
                <button
                  onClick={() => setDeleteConfirmOpen(false)}
                  className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  Cancelar
                </button>
                <button
                  onClick={handleDelete}
                  disabled={desativarPlano.isPending}
                  className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 flex items-center gap-2"
                >
                  {desativarPlano.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  Desativar
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </FeatureGate>
  );
}
