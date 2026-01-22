/**
 * Página de formulário para criar/editar despesas
 */

import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ArrowLeft, Save, Loader2 } from 'lucide-react';
import { useDespesa, useCreateDespesa, useUpdateDespesa } from '../hooks/useDespesas';
import type { CategoriaDespesa } from '../types/despesa';
import { CATEGORIAS_AGRUPADAS, GRUPO_LABELS } from '../types/despesa';

// Schema de validação
const despesaSchema = z.object({
  categoria: z.string().min(1, 'Categoria é obrigatória'),
  descricao: z.string().min(3, 'Descrição deve ter no mínimo 3 caracteres').max(500),
  valor: z.number().positive('Valor deve ser maior que zero'),
  dataVencimento: z.string().min(1, 'Data de vencimento é obrigatória'),
  numeroDocumento: z.string().optional(),
  fornecedor: z.string().max(200).optional(),
  observacoes: z.string().optional(),
  recorrente: z.boolean().optional(),
});

type DespesaFormData = z.infer<typeof despesaSchema>;

export function DespesaFormPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEditing = !!id;

  const { data: despesa, isLoading: isLoadingDespesa } = useDespesa(id || '');
  const createMutation = useCreateDespesa();
  const updateMutation = useUpdateDespesa();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<DespesaFormData>({
    resolver: zodResolver(despesaSchema),
    defaultValues: {
      valor: 0,
      recorrente: false,
    },
  });

  // Preenche o formulário quando editar
  useEffect(() => {
    if (despesa && isEditing) {
      reset({
        categoria: despesa.categoria,
        descricao: despesa.descricao,
        valor: despesa.valor,
        dataVencimento: despesa.dataVencimento,
        numeroDocumento: despesa.numeroDocumento || '',
        fornecedor: despesa.fornecedor || '',
        observacoes: despesa.observacoes || '',
        recorrente: despesa.recorrente,
      });
    }
  }, [despesa, isEditing, reset]);

  const onSubmit = async (data: DespesaFormData) => {
    try {
      if (isEditing) {
        await updateMutation.mutateAsync({
          id: id!,
          data: {
            ...data,
            categoria: data.categoria as CategoriaDespesa,
          },
        });
      } else {
        await createMutation.mutateAsync({
          ...data,
          categoria: data.categoria as CategoriaDespesa,
        });
      }
      navigate('/financeiro/despesas');
    } catch (error) {
      // Erros já são tratados pelo hook com toast
    }
  };

  if (isEditing && isLoadingDespesa) {
    return (
      <div className="p-4 sm:p-6">
        <div className="flex justify-center items-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 dark:border-blue-400 border-t-transparent"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-6">
        <button
          onClick={() => navigate('/financeiro/despesas')}
          className="flex items-center gap-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-100 mb-4"
        >
          <ArrowLeft className="h-4 w-4" />
          Voltar
        </button>
        <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100">
          {isEditing ? 'Editar Despesa' : 'Nova Despesa'}
        </h1>
        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
          {isEditing
            ? 'Atualize as informações da despesa'
            : 'Cadastre uma nova despesa operacional'}
        </p>
      </div>

      {/* Formulário */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
          <div className="grid gap-4 sm:gap-6 sm:grid-cols-2">
            {/* Categoria */}
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Categoria <span className="text-red-500">*</span>
              </label>
              <select
                {...register('categoria')}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              >
                <option value="">Selecione uma categoria</option>
                {Object.entries(CATEGORIAS_AGRUPADAS).map(([grupo, categorias]) => (
                  <optgroup key={grupo} label={GRUPO_LABELS[grupo]}>
                    {categorias.map((cat) => (
                      <option key={cat.value} value={cat.value}>
                        {cat.label}
                      </option>
                    ))}
                  </optgroup>
                ))}
              </select>
              {errors.categoria && (
                <p className="mt-1 text-sm text-red-500">{errors.categoria.message}</p>
              )}
            </div>

            {/* Descrição */}
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Descrição <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                {...register('descricao')}
                placeholder="Ex: Conta de energia elétrica - Janeiro/2026"
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
              {errors.descricao && (
                <p className="mt-1 text-sm text-red-500">{errors.descricao.message}</p>
              )}
            </div>

            {/* Valor */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Valor <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 dark:text-gray-400">
                  R$
                </span>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  {...register('valor', { valueAsNumber: true })}
                  placeholder="0,00"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 pl-10 pr-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
              </div>
              {errors.valor && (
                <p className="mt-1 text-sm text-red-500">{errors.valor.message}</p>
              )}
            </div>

            {/* Data Vencimento */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Data de Vencimento <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                {...register('dataVencimento')}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
              {errors.dataVencimento && (
                <p className="mt-1 text-sm text-red-500">{errors.dataVencimento.message}</p>
              )}
            </div>

            {/* Fornecedor */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Fornecedor/Beneficiário
              </label>
              <input
                type="text"
                {...register('fornecedor')}
                placeholder="Ex: CEMIG, Contador João, etc."
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
            </div>

            {/* Número Documento */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Número do Documento
              </label>
              <input
                type="text"
                {...register('numeroDocumento')}
                placeholder="Nota fiscal, boleto, etc."
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
            </div>

            {/* Observações */}
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Observações
              </label>
              <textarea
                {...register('observacoes')}
                rows={3}
                placeholder="Informações adicionais sobre a despesa..."
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
            </div>

            {/* Recorrente */}
            <div className="sm:col-span-2">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  {...register('recorrente')}
                  className="h-4 w-4 rounded border-gray-300 dark:border-gray-600 text-blue-600 focus:ring-blue-500"
                />
                <span className="text-sm text-gray-700 dark:text-gray-300">
                  Despesa recorrente (mensal)
                </span>
              </label>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Marque se esta despesa se repete todos os meses (ex: aluguel, salários)
              </p>
            </div>
          </div>
        </div>

        {/* Botões */}
        <div className="flex flex-col sm:flex-row gap-3 sm:justify-end">
          <button
            type="button"
            onClick={() => navigate('/financeiro/despesas')}
            className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            {isSubmitting ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}
            {isEditing ? 'Salvar Alterações' : 'Cadastrar Despesa'}
          </button>
        </div>
      </form>
    </div>
  );
}
