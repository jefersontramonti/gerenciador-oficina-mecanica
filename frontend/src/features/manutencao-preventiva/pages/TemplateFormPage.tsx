import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useForm, Controller, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ArrowLeft, Save, Plus, Trash2 } from 'lucide-react';
import { useTemplate, useCriarTemplate, useAtualizarTemplate, useTiposManutencao } from '../hooks/useManutencaoPreventiva';
import { showSuccess, showError } from '@/shared/utils/notifications';
import type { TemplateManutencaoRequest } from '../types';

const templateSchema = z.object({
  nome: z.string().min(3, 'Nome deve ter pelo menos 3 caracteres'),
  descricao: z.string().optional(),
  tipoManutencao: z.string().min(1, 'Selecione o tipo de manutenção'),
  criterio: z.enum(['TEMPO', 'KM', 'AMBOS']),
  intervaloDias: z.number().min(1).optional().nullable(),
  intervaloKm: z.number().min(1).optional().nullable(),
  antecedenciaDias: z.number().min(0),
  antecedenciaKm: z.number().min(0),
  checklist: z.array(z.object({
    item: z.string().min(1, 'Item é obrigatório'),
    obrigatorio: z.boolean(),
  })).optional(),
  valorEstimado: z.number().optional().nullable(),
  tempoEstimadoMinutos: z.number().optional().nullable(),
});

type TemplateFormData = z.infer<typeof templateSchema>;

export default function TemplateFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditing = !!id;

  const [tipoCustom, setTipoCustom] = useState('');
  const [useTipoCustom, setUseTipoCustom] = useState(false);

  const { data: template, isLoading: loadingTemplate } = useTemplate(id);
  const { data: tiposManutencao } = useTiposManutencao();
  const criarMutation = useCriarTemplate();
  const atualizarMutation = useAtualizarTemplate();

  const {
    register,
    handleSubmit,
    control,
    watch,
    reset,
    formState: { errors },
  } = useForm<TemplateFormData>({
    resolver: zodResolver(templateSchema),
    defaultValues: {
      criterio: 'TEMPO',
      antecedenciaDias: 7,
      antecedenciaKm: 500,
      checklist: [],
    },
  });

  const { fields: checklistFields, append: appendChecklist, remove: removeChecklist } = useFieldArray({
    control,
    name: 'checklist',
  });

  const criterio = watch('criterio');

  // Carregar dados do template quando editando
  useEffect(() => {
    if (template && isEditing) {
      reset({
        nome: template.nome,
        descricao: template.descricao || '',
        tipoManutencao: template.tipoManutencao,
        criterio: template.criterio,
        intervaloDias: template.intervaloDias || null,
        intervaloKm: template.intervaloKm || null,
        antecedenciaDias: template.antecedenciaDias,
        antecedenciaKm: template.antecedenciaKm,
        checklist: template.checklist || [],
        valorEstimado: template.valorEstimado || null,
        tempoEstimadoMinutos: template.tempoEstimadoMinutos || null,
      });
    }
  }, [template, isEditing, reset]);

  const onSubmit = async (data: TemplateFormData) => {
    try {
      const tipoFinal = useTipoCustom ? tipoCustom : data.tipoManutencao;

      const request: TemplateManutencaoRequest = {
        nome: data.nome,
        descricao: data.descricao || undefined,
        tipoManutencao: tipoFinal,
        criterio: data.criterio,
        intervaloDias: data.intervaloDias || undefined,
        intervaloKm: data.intervaloKm || undefined,
        antecedenciaDias: data.antecedenciaDias,
        antecedenciaKm: data.antecedenciaKm,
        checklist: data.checklist || undefined,
        valorEstimado: data.valorEstimado || undefined,
        tempoEstimadoMinutos: data.tempoEstimadoMinutos || undefined,
      };

      if (isEditing && id) {
        await atualizarMutation.mutateAsync({ id, data: request });
        showSuccess('Template atualizado com sucesso');
      } else {
        await criarMutation.mutateAsync(request);
        showSuccess('Template criado com sucesso');
      }
      navigate('/manutencao-preventiva/templates');
    } catch (error) {
      console.error('Erro ao salvar template:', error);
      showError('Erro ao salvar template');
    }
  };

  if (isEditing && loadingTemplate) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3 sm:gap-4">
        <Link
          to="/manutencao-preventiva/templates"
          className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
        >
          <ArrowLeft className="h-5 w-5 text-gray-500 dark:text-gray-400" />
        </Link>
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
            {isEditing ? 'Editar Template' : 'Novo Template'}
          </h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 hidden sm:block">
            {isEditing ? 'Atualize as informações do template' : 'Crie um modelo de manutenção reutilizável'}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 sm:space-y-6">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
          {/* Left Column - Basic Info */}
          <div className="space-y-4 sm:space-y-6">
            {/* Informações Básicas */}
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-3 sm:p-4">
              <h2 className="text-base sm:text-lg font-semibold text-gray-900 dark:text-white mb-3 sm:mb-4">Informações Básicas</h2>

              <div className="space-y-3 sm:space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Nome do Template *
                  </label>
                  <input
                    type="text"
                    {...register('nome')}
                    placeholder="Ex: Troca de Óleo - Padrão"
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm sm:text-base"
                  />
                  {errors.nome && (
                    <p className="mt-1 text-sm text-red-500">{errors.nome.message}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Tipo de Manutenção *
                  </label>
                  {!useTipoCustom ? (
                    <div className="flex flex-col sm:flex-row gap-2">
                      <select
                        {...register('tipoManutencao')}
                        className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm sm:text-base"
                      >
                        <option value="">Selecione...</option>
                        <option value="TROCA_OLEO">Troca de Óleo</option>
                        <option value="REVISAO">Revisão</option>
                        <option value="ALINHAMENTO">Alinhamento</option>
                        <option value="BALANCEAMENTO">Balanceamento</option>
                        <option value="FREIOS">Freios</option>
                        <option value="SUSPENSAO">Suspensão</option>
                        <option value="AR_CONDICIONADO">Ar Condicionado</option>
                        <option value="CORREIA_DENTADA">Correia Dentada</option>
                        <option value="FILTROS">Filtros</option>
                        {tiposManutencao?.filter(t => !['TROCA_OLEO', 'REVISAO', 'ALINHAMENTO', 'BALANCEAMENTO', 'FREIOS', 'SUSPENSAO', 'AR_CONDICIONADO', 'CORREIA_DENTADA', 'FILTROS'].includes(t)).map((tipo) => (
                          <option key={tipo} value={tipo}>{tipo}</option>
                        ))}
                      </select>
                      <button
                        type="button"
                        onClick={() => setUseTipoCustom(true)}
                        className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 text-sm sm:text-base w-full sm:w-auto"
                      >
                        Outro
                      </button>
                    </div>
                  ) : (
                    <div className="flex flex-col sm:flex-row gap-2">
                      <input
                        type="text"
                        value={tipoCustom}
                        onChange={(e) => setTipoCustom(e.target.value.toUpperCase())}
                        placeholder="Digite o tipo personalizado"
                        className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm sm:text-base"
                      />
                      <button
                        type="button"
                        onClick={() => setUseTipoCustom(false)}
                        className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 text-sm sm:text-base w-full sm:w-auto"
                      >
                        Lista
                      </button>
                    </div>
                  )}
                  {errors.tipoManutencao && (
                    <p className="mt-1 text-sm text-red-500">{errors.tipoManutencao.message}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Descrição
                  </label>
                  <textarea
                    {...register('descricao')}
                    rows={3}
                    placeholder="Descreva o que este template de manutenção inclui..."
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm sm:text-base"
                  />
                </div>
              </div>
            </div>

            {/* Critérios */}
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-3 sm:p-4">
              <h2 className="text-base sm:text-lg font-semibold text-gray-900 dark:text-white mb-3 sm:mb-4">Critérios de Manutenção</h2>

              <div className="space-y-3 sm:space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Critério *
                  </label>
                  <Controller
                    name="criterio"
                    control={control}
                    render={({ field }) => (
                      <div className="flex flex-col sm:flex-row gap-2">
                        {(['TEMPO', 'KM', 'AMBOS'] as const).map((c) => (
                          <button
                            key={c}
                            type="button"
                            onClick={() => field.onChange(c)}
                            className={`flex-1 px-3 sm:px-4 py-2 rounded-lg border text-sm sm:text-base ${
                              field.value === c
                                ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400'
                                : 'border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700'
                            }`}
                          >
                            {c === 'TEMPO' ? 'Por Tempo' : c === 'KM' ? 'Por KM' : 'Ambos'}
                          </button>
                        ))}
                      </div>
                    )}
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                  {(criterio === 'TEMPO' || criterio === 'AMBOS') && (
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        Intervalo (Dias)
                      </label>
                      <Controller
                        name="intervaloDias"
                        control={control}
                        render={({ field }) => (
                          <input
                            type="number"
                            value={field.value ?? ''}
                            onChange={(e) => field.onChange(e.target.value ? parseInt(e.target.value) : null)}
                            placeholder="Ex: 180"
                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm sm:text-base"
                          />
                        )}
                      />
                    </div>
                  )}

                  {(criterio === 'KM' || criterio === 'AMBOS') && (
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        Intervalo (KM)
                      </label>
                      <Controller
                        name="intervaloKm"
                        control={control}
                        render={({ field }) => (
                          <input
                            type="number"
                            value={field.value ?? ''}
                            onChange={(e) => field.onChange(e.target.value ? parseInt(e.target.value) : null)}
                            placeholder="Ex: 10000"
                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm sm:text-base"
                          />
                        )}
                      />
                    </div>
                  )}
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Antecedência Alerta (Dias)
                    </label>
                    <Controller
                      name="antecedenciaDias"
                      control={control}
                      render={({ field }) => (
                        <input
                          type="number"
                          value={field.value ?? 0}
                          onChange={(e) => field.onChange(parseInt(e.target.value) || 0)}
                          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm sm:text-base"
                        />
                      )}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Antecedência Alerta (KM)
                    </label>
                    <Controller
                      name="antecedenciaKm"
                      control={control}
                      render={({ field }) => (
                        <input
                          type="number"
                          value={field.value ?? 0}
                          onChange={(e) => field.onChange(parseInt(e.target.value) || 0)}
                          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm sm:text-base"
                        />
                      )}
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Right Column - Checklist & Values */}
          <div className="space-y-4 sm:space-y-6">
            {/* Valores */}
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-3 sm:p-4">
              <h2 className="text-base sm:text-lg font-semibold text-gray-900 dark:text-white mb-3 sm:mb-4">Valores Estimados</h2>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Valor Estimado (R$)
                  </label>
                  <Controller
                    name="valorEstimado"
                    control={control}
                    render={({ field }) => (
                      <input
                        type="number"
                        step="0.01"
                        value={field.value ?? ''}
                        onChange={(e) => field.onChange(e.target.value ? parseFloat(e.target.value) : null)}
                        placeholder="0,00"
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm sm:text-base"
                      />
                    )}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Tempo Estimado (min)
                  </label>
                  <Controller
                    name="tempoEstimadoMinutos"
                    control={control}
                    render={({ field }) => (
                      <input
                        type="number"
                        value={field.value ?? ''}
                        onChange={(e) => field.onChange(e.target.value ? parseInt(e.target.value) : null)}
                        placeholder="Ex: 60"
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm sm:text-base"
                      />
                    )}
                  />
                </div>
              </div>
            </div>

            {/* Checklist */}
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-3 sm:p-4">
              <div className="flex items-center justify-between mb-3 sm:mb-4">
                <h2 className="text-base sm:text-lg font-semibold text-gray-900 dark:text-white">Checklist</h2>
                <button
                  type="button"
                  onClick={() => appendChecklist({ item: '', obrigatorio: false })}
                  className="flex items-center gap-1 text-sm text-blue-600 dark:text-blue-400 hover:underline"
                >
                  <Plus className="h-4 w-4" />
                  <span className="hidden sm:inline">Adicionar Item</span>
                  <span className="sm:hidden">Adicionar</span>
                </button>
              </div>

              {checklistFields.length === 0 ? (
                <p className="text-gray-500 dark:text-gray-400 text-sm text-center py-4">
                  Nenhum item no checklist. Clique em "Adicionar" para começar.
                </p>
              ) : (
                <div className="space-y-3">
                  {checklistFields.map((field, index) => (
                    <div key={field.id} className="flex flex-col sm:flex-row items-start gap-2">
                      <input
                        {...register(`checklist.${index}.item`)}
                        placeholder="Descrição do item"
                        className="w-full sm:flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm"
                      />
                      <div className="flex gap-2 w-full sm:w-auto">
                        <label className="flex-1 sm:flex-none flex items-center gap-2 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg cursor-pointer">
                          <Controller
                            name={`checklist.${index}.obrigatorio`}
                            control={control}
                            render={({ field }) => (
                              <input
                                type="checkbox"
                                checked={field.value}
                                onChange={field.onChange}
                                className="rounded border-gray-300 dark:border-gray-600"
                              />
                            )}
                          />
                          <span className="text-sm text-gray-700 dark:text-gray-300 whitespace-nowrap">Obrig.</span>
                        </label>
                        <button
                          type="button"
                          onClick={() => removeChecklist(index)}
                          className="p-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Submit Buttons */}
        <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-2 sm:gap-3">
          <Link
            to="/manutencao-preventiva/templates"
            className="w-full sm:w-auto text-center px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            Cancelar
          </Link>
          <button
            type="submit"
            disabled={criarMutation.isPending || atualizarMutation.isPending}
            className="w-full sm:w-auto flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            <Save className="h-4 w-4" />
            {(criarMutation.isPending || atualizarMutation.isPending) ? 'Salvando...' : 'Salvar'}
          </button>
        </div>
      </form>
    </div>
  );
}
