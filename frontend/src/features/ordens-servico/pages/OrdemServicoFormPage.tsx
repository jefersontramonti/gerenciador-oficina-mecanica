/**
 * Página de formulário para criar/editar Ordem de Serviço
 * Suporta itens dinâmicos com cálculos automáticos
 */

import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, useFieldArray, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, Save, Plus, Trash2, Package } from 'lucide-react';
import { showError } from '@/shared/utils/notifications';
import { useOrdemServico, useCreateOrdemServico, useUpdateOrdemServico } from '../hooks/useOrdensServico';
import { ordemServicoFormSchema } from '../utils/validation';
import { canEdit } from '../utils/statusTransitions';
import { TipoItem } from '../types';
import type { OrdemServicoFormData } from '../utils/validation';
import { VeiculoAutocomplete } from '../components/VeiculoAutocomplete';
import { MecanicoSelect } from '../components/MecanicoSelect';

export const OrdemServicoFormPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEditMode = !!id;

  const { data: ordemServico, isLoading: loadingOS } = useOrdemServico(id);
  const createMutation = useCreateOrdemServico();
  const updateMutation = useUpdateOrdemServico();

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(ordemServicoFormSchema),
    defaultValues: {
      dataAbertura: new Date().toISOString().split('T')[0],
      diagnostico: '',
      observacoes: '',
      dataPrevisao: '',
      valorMaoObra: 0,
      valorPecas: 0,
      valorTotal: 0,
      descontoPercentual: 0,
      descontoValor: 0,
      valorFinal: 0,
      itens: [],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'itens',
  });

  // Watch para cálculos automáticos
  const itens = watch('itens');
  const valorMaoObra = watch('valorMaoObra') || 0;
  const descontoPercentual = watch('descontoPercentual') || 0;
  const descontoValor = watch('descontoValor') || 0;

  // Cálculos automáticos de valores
  useEffect(() => {
    // Calcular valor de peças
    const valorPecas = itens
      .filter((item) => item.tipo === TipoItem.PECA)
      .reduce((sum, item) => {
        const valorItem = (item.quantidade || 0) * (item.valorUnitario || 0) - (item.desconto || 0);
        return sum + valorItem;
      }, 0);

    // Calcular valor total (mão de obra + peças)
    const valorTotal = valorMaoObra + valorPecas;

    // Calcular desconto (prioriza percentual)
    const desconto = descontoPercentual > 0
      ? (valorTotal * descontoPercentual) / 100
      : descontoValor;

    // Calcular valor final
    const valorFinal = Math.max(0, valorTotal - desconto);

    // Atualizar campos calculados
    setValue('valorPecas', valorPecas);
    setValue('valorTotal', valorTotal);
    setValue('valorFinal', valorFinal);

    // Se descontoPercentual mudou, zerar descontoValor
    if (descontoPercentual > 0) {
      setValue('descontoValor', 0);
    }
  }, [itens, valorMaoObra, descontoPercentual, descontoValor, setValue]);

  // Calcular valorTotal de cada item
  useEffect(() => {
    itens.forEach((item, index) => {
      const valorItem =
        (item.quantidade || 0) * (item.valorUnitario || 0) - (item.desconto || 0);
      if (item.valorTotal !== valorItem) {
        setValue(`itens.${index}.valorTotal`, Math.max(0, valorItem));
      }
    });
  }, [itens, setValue]);

  // Load OS data when editing
  useEffect(() => {
    if (ordemServico && isEditMode) {
      // Verificar se pode editar
      if (!canEdit(ordemServico.status)) {
        showError('Esta OS não pode ser editada neste status.');
        navigate(`/ordens-servico/${id}`);
        return;
      }

      if (ordemServico.veiculoId) {
        setValue('veiculoId', ordemServico.veiculoId);
      }
      if (ordemServico.usuarioId) {
        setValue('usuarioId', ordemServico.usuarioId);
      }
      setValue('problemasRelatados', ordemServico.problemasRelatados);
      setValue('diagnostico', ordemServico.diagnostico || '');
      setValue('observacoes', ordemServico.observacoes || '');
      setValue('valorMaoObra', ordemServico.valorMaoObra);
      setValue('descontoPercentual', ordemServico.descontoPercentual);
      setValue('descontoValor', ordemServico.descontoValor);

      // Data de abertura (formato YYYY-MM-DD)
      if (ordemServico.dataAbertura) {
        const dataAbertura = Array.isArray(ordemServico.dataAbertura)
          ? new Date(ordemServico.dataAbertura[0], ordemServico.dataAbertura[1] - 1, ordemServico.dataAbertura[2])
          : new Date(ordemServico.dataAbertura);
        setValue('dataAbertura', dataAbertura.toISOString().split('T')[0]);
      }

      // Data de previsão
      if (ordemServico.dataPrevisao) {
        const dataPrevisao = Array.isArray(ordemServico.dataPrevisao)
          ? new Date(ordemServico.dataPrevisao[0], ordemServico.dataPrevisao[1] - 1, ordemServico.dataPrevisao[2])
          : new Date(ordemServico.dataPrevisao);
        setValue('dataPrevisao', dataPrevisao.toISOString().split('T')[0]);
      }

      // Itens
      if (ordemServico.itens && ordemServico.itens.length > 0) {
        setValue('itens', ordemServico.itens);
      }
    }
  }, [ordemServico, isEditMode, setValue, navigate, id]);

  const onSubmit = async (data: OrdemServicoFormData) => {
    try {
      const payload = {
        veiculoId: data.veiculoId,
        usuarioId: data.usuarioId,
        problemasRelatados: data.problemasRelatados,
        diagnostico: data.diagnostico || undefined,
        observacoes: data.observacoes || undefined,
        dataPrevisao: data.dataPrevisao || undefined,
        valorMaoObra: data.valorMaoObra,
        descontoPercentual: data.descontoPercentual || 0,
        descontoValor: data.descontoValor || 0,
        itens: data.itens.map((item) => ({
          tipo: item.tipo,
          pecaId: item.pecaId || undefined,
          descricao: item.descricao,
          quantidade: item.quantidade,
          valorUnitario: item.valorUnitario,
          desconto: item.desconto || 0,
        })),
      };

      if (isEditMode) {
        await updateMutation.mutateAsync({
          id: id!,
          data: payload,
        });
      } else {
        await createMutation.mutateAsync(payload);
      }
      navigate('/ordens-servico');
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'Erro ao salvar OS';
      showError(`Erro: ${errorMessage}`);
    }
  };

  const handleAddItem = () => {
    append({
      tipo: TipoItem.SERVICO,
      descricao: '',
      quantidade: 1,
      valorUnitario: 0,
      desconto: 0,
      valorTotal: 0,
    });
  };

  if (loadingOS) {
    return (
      <div className="flex h-64 items-center justify-center">
        <p className="text-gray-600">Carregando...</p>
      </div>
    );
  }

  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center gap-4">
        <button
          type="button"
          onClick={() => navigate('/ordens-servico')}
          className="rounded-lg border border-gray-300 p-2 text-gray-700 hover:bg-gray-50"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {isEditMode ? 'Editar Ordem de Serviço' : 'Nova Ordem de Serviço'}
          </h1>
          <p className="mt-1 text-sm text-gray-600">
            {isEditMode
              ? 'Atualize os dados da ordem de serviço'
              : 'Preencha os dados para criar uma nova OS'}
          </p>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Seção: Veículo e Mecânico */}
        <div className="rounded-lg bg-white p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900">Veículo e Mecânico</h2>
          <div className="grid gap-4 md:grid-cols-2">
            {/* Veículo - Autocomplete */}
            <Controller
              name="veiculoId"
              control={control}
              render={({ field }) => (
                <VeiculoAutocomplete
                  value={field.value}
                  onChange={field.onChange}
                  error={errors.veiculoId?.message}
                  required
                />
              )}
            />

            {/* Mecânico - Select */}
            <Controller
              name="usuarioId"
              control={control}
              render={({ field }) => (
                <MecanicoSelect
                  value={field.value}
                  onChange={field.onChange}
                  error={errors.usuarioId?.message}
                  required
                />
              )}
            />
          </div>
        </div>

        {/* Seção: Problemas e Diagnóstico */}
        <div className="rounded-lg bg-white p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900">
            Problemas e Diagnóstico
          </h2>
          <div className="space-y-4">
            {/* Problemas Relatados */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Problemas Relatados <span className="text-red-500">*</span>
              </label>
              <textarea
                {...register('problemasRelatados')}
                rows={3}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                placeholder="Descreva os problemas relatados pelo cliente..."
              />
              {errors.problemasRelatados && (
                <p className="mt-1 text-sm text-red-500">{errors.problemasRelatados.message}</p>
              )}
            </div>

            {/* Diagnóstico */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">Diagnóstico</label>
              <textarea
                {...register('diagnostico')}
                rows={3}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                placeholder="Diagnóstico técnico (opcional)..."
              />
              {errors.diagnostico && (
                <p className="mt-1 text-sm text-red-500">{errors.diagnostico.message}</p>
              )}
            </div>

            {/* Observações */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">Observações</label>
              <textarea
                {...register('observacoes')}
                rows={2}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                placeholder="Observações adicionais (opcional)..."
              />
              {errors.observacoes && (
                <p className="mt-1 text-sm text-red-500">{errors.observacoes.message}</p>
              )}
            </div>
          </div>
        </div>

        {/* Seção: Datas */}
        <div className="rounded-lg bg-white p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900">Datas</h2>
          <div className="grid gap-4 md:grid-cols-2">
            {/* Data de Abertura */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Data de Abertura <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                {...register('dataAbertura')}
                disabled={isEditMode}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 disabled:bg-gray-100"
              />
              {errors.dataAbertura && (
                <p className="mt-1 text-sm text-red-500">{errors.dataAbertura.message}</p>
              )}
            </div>

            {/* Data de Previsão */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Previsão de Entrega
              </label>
              <input
                type="date"
                {...register('dataPrevisao')}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
              {errors.dataPrevisao && (
                <p className="mt-1 text-sm text-red-500">{errors.dataPrevisao.message}</p>
              )}
            </div>
          </div>
        </div>

        {/* Seção: Itens de Serviço */}
        <div className="rounded-lg bg-white p-6 shadow">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">Itens de Serviço</h2>
            <button
              type="button"
              onClick={handleAddItem}
              className="flex items-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm text-white hover:bg-green-700"
            >
              <Plus className="h-4 w-4" />
              Adicionar Item
            </button>
          </div>

          {errors.itens && typeof errors.itens === 'object' && 'message' in errors.itens && (
            <p className="mb-4 text-sm text-red-500">{errors.itens.message as string}</p>
          )}

          <div className="space-y-4">
            {fields.map((field, index) => (
              <div key={field.id} className="rounded-lg border border-gray-200 p-4">
                <div className="mb-3 flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-700">Item #{index + 1}</span>
                  <button
                    type="button"
                    onClick={() => remove(index)}
                    className="rounded p-1 text-red-600 hover:bg-red-50"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>

                <div className="grid gap-4 md:grid-cols-6">
                  {/* Tipo */}
                  <div className="md:col-span-1">
                    <label className="mb-1 block text-sm font-medium text-gray-700">Tipo</label>
                    <select
                      {...register(`itens.${index}.tipo`)}
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                    >
                      <option value={TipoItem.SERVICO}>Serviço</option>
                      <option value={TipoItem.PECA}>Peça</option>
                    </select>
                  </div>

                  {/* Descrição */}
                  <div className="md:col-span-2">
                    <label className="mb-1 block text-sm font-medium text-gray-700">
                      Descrição
                    </label>
                    <input
                      type="text"
                      {...register(`itens.${index}.descricao`)}
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                      placeholder="Descrição do item"
                    />
                    {errors.itens?.[index]?.descricao && (
                      <p className="mt-1 text-sm text-red-500">
                        {errors.itens[index]?.descricao?.message}
                      </p>
                    )}
                  </div>

                  {/* Quantidade */}
                  <div className="md:col-span-1">
                    <label className="mb-1 block text-sm font-medium text-gray-700">Qtd.</label>
                    <input
                      type="number"
                      {...register(`itens.${index}.quantidade`, { valueAsNumber: true })}
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                      min="1"
                    />
                  </div>

                  {/* Valor Unitário */}
                  <div className="md:col-span-1">
                    <label className="mb-1 block text-sm font-medium text-gray-700">
                      Valor Unit.
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      {...register(`itens.${index}.valorUnitario`, { valueAsNumber: true })}
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                      min="0"
                    />
                  </div>

                  {/* Desconto */}
                  <div className="md:col-span-1">
                    <label className="mb-1 block text-sm font-medium text-gray-700">
                      Desconto
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      {...register(`itens.${index}.desconto`, { valueAsNumber: true })}
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                      min="0"
                    />
                  </div>
                </div>
              </div>
            ))}

            {fields.length === 0 && (
              <div className="rounded-lg border border-dashed border-gray-300 bg-gray-50 p-8 text-center">
                <Package className="mx-auto h-12 w-12 text-gray-400" />
                <p className="mt-2 text-sm text-gray-600">
                  Nenhum item adicionado. Clique em "Adicionar Item" para começar.
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Seção: Valores Financeiros */}
        <div className="rounded-lg bg-white p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900">Valores Financeiros</h2>
          <div className="space-y-4">
            {/* Valor Mão de Obra */}
            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">
                  Valor Mão de Obra (R$)
                </label>
                <input
                  type="number"
                  step="0.01"
                  {...register('valorMaoObra', { valueAsNumber: true })}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  min="0"
                />
                {errors.valorMaoObra && (
                  <p className="mt-1 text-sm text-red-500">{errors.valorMaoObra.message}</p>
                )}
              </div>

              {/* Valor Peças (calculado) */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">
                  Valor Peças (R$)
                </label>
                <input
                  type="text"
                  value={new Intl.NumberFormat('pt-BR', {
                    style: 'currency',
                    currency: 'BRL',
                  }).format(watch('valorPecas') || 0)}
                  disabled
                  className="w-full rounded-lg border border-gray-300 bg-gray-100 px-3 py-2"
                />
                <p className="mt-1 text-xs text-gray-500">Calculado automaticamente</p>
              </div>
            </div>

            {/* Valor Total (calculado) */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Valor Total (R$)
              </label>
              <input
                type="text"
                value={new Intl.NumberFormat('pt-BR', {
                  style: 'currency',
                  currency: 'BRL',
                }).format(watch('valorTotal') || 0)}
                disabled
                className="w-full rounded-lg border border-gray-300 bg-gray-100 px-3 py-2 font-medium"
              />
              <p className="mt-1 text-xs text-gray-500">Mão de Obra + Peças</p>
            </div>

            {/* Descontos */}
            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">
                  Desconto (%)
                </label>
                <input
                  type="number"
                  step="0.01"
                  {...register('descontoPercentual', { valueAsNumber: true })}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  min="0"
                  max="100"
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">
                  Desconto (R$)
                </label>
                <input
                  type="number"
                  step="0.01"
                  {...register('descontoValor', { valueAsNumber: true })}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  min="0"
                  disabled={descontoPercentual > 0}
                />
                {descontoPercentual > 0 && (
                  <p className="mt-1 text-xs text-gray-500">
                    Desabilitado (usando desconto percentual)
                  </p>
                )}
              </div>
            </div>

            {/* Valor Final (calculado) */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Valor Final (R$)
              </label>
              <input
                type="text"
                value={new Intl.NumberFormat('pt-BR', {
                  style: 'currency',
                  currency: 'BRL',
                }).format(watch('valorFinal') || 0)}
                disabled
                className="w-full rounded-lg border-2 border-green-500 bg-green-50 px-3 py-2 text-lg font-bold text-green-900"
              />
              <p className="mt-1 text-xs text-gray-500">Valor Total - Desconto</p>
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-3">
          <button
            type="button"
            onClick={() => navigate('/ordens-servico')}
            className="rounded-lg border border-gray-300 px-6 py-2 text-gray-700 hover:bg-gray-50"
            disabled={isSubmitting}
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-6 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
          >
            <Save className="h-5 w-5" />
            {isSubmitting ? 'Salvando...' : 'Salvar OS'}
          </button>
        </div>
      </form>
    </div>
  );
};
