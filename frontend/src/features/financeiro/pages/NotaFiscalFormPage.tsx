/**
 * Página de formulário para criar/editar Nota Fiscal
 */

import { useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ArrowLeft, Save } from 'lucide-react';
import {
  useNotaFiscal,
  useCriarNotaFiscal,
  useAtualizarNotaFiscal,
  useProximoNumero,
} from '../hooks/useNotasFiscais';
import {
  TipoNotaFiscal,
  TipoNotaFiscalLabels,
  type NotaFiscalRequestDTO,
} from '../types/notaFiscal';

const notaFiscalSchema = z.object({
  ordemServicoId: z.string().min(1, 'Ordem de Serviço é obrigatória'),
  tipo: z.nativeEnum(TipoNotaFiscal, {
    message: 'Tipo de nota fiscal é obrigatório',
  }),
  serie: z
    .number({
      message: 'Série deve ser um número',
    })
    .int('Série deve ser um número inteiro')
    .min(1, 'Série mínima é 1'),
  valorTotal: z
    .number({
      message: 'Valor deve ser um número',
    })
    .positive('Valor deve ser maior que zero')
    .min(0.01, 'Valor mínimo é R$ 0,01'),
  naturezaOperacao: z
    .string()
    .max(60, 'Natureza da operação deve ter no máximo 60 caracteres')
    .optional(),
  cfop: z
    .string()
    .regex(/^\d{4}$/, 'CFOP deve ter 4 dígitos')
    .optional()
    .or(z.literal('')),
  informacoesComplementares: z
    .string()
    .max(1000, 'Informações complementares devem ter no máximo 1000 caracteres')
    .optional(),
  dataEmissao: z.string().min(1, 'Data de emissão é obrigatória'),
});

type NotaFiscalFormData = z.infer<typeof notaFiscalSchema>;

export function NotaFiscalFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditing = !!id;

  const { data: notaFiscal } = useNotaFiscal(id);
  const criarMutation = useCriarNotaFiscal();
  const atualizarMutation = useAtualizarNotaFiscal();

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm<NotaFiscalFormData>({
    resolver: zodResolver(notaFiscalSchema),
    defaultValues: {
      serie: 1,
      tipo: TipoNotaFiscal.NFSE,
      dataEmissao: new Date().toISOString().split('T')[0],
    },
  });

  const serie = watch('serie');
  const { data: proximoNumero } = useProximoNumero(serie);

  // Preencher form ao editar
  useEffect(() => {
    if (notaFiscal) {
      setValue('ordemServicoId', notaFiscal.ordemServicoId);
      setValue('tipo', notaFiscal.tipo);
      setValue('serie', notaFiscal.serie);
      setValue('valorTotal', notaFiscal.valorTotal);
      setValue('naturezaOperacao', notaFiscal.naturezaOperacao || '');
      setValue('cfop', notaFiscal.cfop || '');
      setValue(
        'informacoesComplementares',
        notaFiscal.informacoesComplementares || ''
      );
      setValue('dataEmissao', notaFiscal.dataEmissao.split('T')[0]);
    }
  }, [notaFiscal, setValue]);

  const onSubmit = async (data: NotaFiscalFormData) => {
    const request: NotaFiscalRequestDTO = {
      ordemServicoId: data.ordemServicoId,
      tipo: data.tipo,
      serie: data.serie,
      valorTotal: data.valorTotal,
      naturezaOperacao: data.naturezaOperacao || undefined,
      cfop: data.cfop || undefined,
      informacoesComplementares: data.informacoesComplementares || undefined,
      dataEmissao: `${data.dataEmissao}T00:00:00`,
    };

    try {
      if (isEditing) {
        await atualizarMutation.mutateAsync({ id, data: request });
      } else {
        await criarMutation.mutateAsync(request);
      }
      navigate('/financeiro/notas-fiscais');
    } catch (error: any) {
      // Erro já tratado pelo hook
    }
  };

  const isPending = criarMutation.isPending || atualizarMutation.isPending;

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center gap-4">
        <Link
          to="/financeiro/notas-fiscais"
          className="rounded-lg border border-gray-300 p-2 text-gray-700 hover:bg-gray-50"
        >
          <ArrowLeft className="h-5 w-5" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {isEditing ? 'Editar Nota Fiscal' : 'Nova Nota Fiscal'}
          </h1>
          <p className="mt-1 text-sm text-gray-600">
            {isEditing
              ? 'Apenas notas em digitação podem ser editadas'
              : 'Preencha os dados da nota fiscal'}
          </p>
        </div>
      </div>

      {/* Alert */}
      <div className="mb-6 rounded-lg border border-yellow-200 bg-yellow-50 p-4">
        <p className="text-sm text-yellow-800">
          <strong>⚠️ Atenção:</strong> Esta é uma implementação básica de CRUD para
          Notas Fiscais. A integração com SEFAZ (emissão, autorização, cancelamento)
          está planejada para Phase 3 do projeto.
        </p>
      </div>

      {/* Formulário */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="rounded-lg bg-white p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900">
            Informações Básicas
          </h2>

          <div className="grid gap-6 md:grid-cols-2">
            {/* Ordem de Serviço ID */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                ID da Ordem de Serviço <span className="text-red-600">*</span>
              </label>
              <input
                type="text"
                {...register('ordemServicoId')}
                className={`
                  w-full rounded-lg border px-3 py-2
                  focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                  ${errors.ordemServicoId ? 'border-red-500' : 'border-gray-300'}
                `}
                placeholder="UUID da OS"
              />
              {errors.ordemServicoId && (
                <p className="mt-1 text-sm text-red-600">
                  {errors.ordemServicoId.message}
                </p>
              )}
            </div>

            {/* Tipo */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Tipo de Nota <span className="text-red-600">*</span>
              </label>
              <Controller
                name="tipo"
                control={control}
                render={({ field }) => (
                  <select
                    {...field}
                    className={`
                      w-full rounded-lg border px-3 py-2
                      focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                      ${errors.tipo ? 'border-red-500' : 'border-gray-300'}
                    `}
                  >
                    {Object.values(TipoNotaFiscal).map((tipo) => (
                      <option key={tipo} value={tipo}>
                        {TipoNotaFiscalLabels[tipo]}
                      </option>
                    ))}
                  </select>
                )}
              />
              {errors.tipo && (
                <p className="mt-1 text-sm text-red-600">{errors.tipo.message}</p>
              )}
            </div>

            {/* Série */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Série <span className="text-red-600">*</span>
              </label>
              <input
                type="number"
                min="1"
                {...register('serie', { valueAsNumber: true })}
                className={`
                  w-full rounded-lg border px-3 py-2
                  focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                  ${errors.serie ? 'border-red-500' : 'border-gray-300'}
                `}
              />
              {errors.serie && (
                <p className="mt-1 text-sm text-red-600">{errors.serie.message}</p>
              )}
              {proximoNumero && !isEditing && (
                <p className="mt-1 text-xs text-green-600">
                  Próximo número disponível: {proximoNumero}
                </p>
              )}
            </div>

            {/* Valor Total */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Valor Total (R$) <span className="text-red-600">*</span>
              </label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                {...register('valorTotal', { valueAsNumber: true })}
                className={`
                  w-full rounded-lg border px-3 py-2
                  focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                  ${errors.valorTotal ? 'border-red-500' : 'border-gray-300'}
                `}
                placeholder="0.00"
              />
              {errors.valorTotal && (
                <p className="mt-1 text-sm text-red-600">
                  {errors.valorTotal.message}
                </p>
              )}
            </div>

            {/* Data de Emissão */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Data de Emissão <span className="text-red-600">*</span>
              </label>
              <input
                type="date"
                {...register('dataEmissao')}
                className={`
                  w-full rounded-lg border px-3 py-2
                  focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                  ${errors.dataEmissao ? 'border-red-500' : 'border-gray-300'}
                `}
              />
              {errors.dataEmissao && (
                <p className="mt-1 text-sm text-red-600">
                  {errors.dataEmissao.message}
                </p>
              )}
            </div>

            {/* Natureza da Operação */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Natureza da Operação
              </label>
              <input
                type="text"
                {...register('naturezaOperacao')}
                className={`
                  w-full rounded-lg border px-3 py-2
                  focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                  ${errors.naturezaOperacao ? 'border-red-500' : 'border-gray-300'}
                `}
                placeholder="Ex: PRESTACAO DE SERVICOS"
                maxLength={60}
              />
              {errors.naturezaOperacao && (
                <p className="mt-1 text-sm text-red-600">
                  {errors.naturezaOperacao.message}
                </p>
              )}
            </div>

            {/* CFOP */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                CFOP
              </label>
              <input
                type="text"
                {...register('cfop')}
                className={`
                  w-full rounded-lg border px-3 py-2
                  focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                  ${errors.cfop ? 'border-red-500' : 'border-gray-300'}
                `}
                placeholder="5933"
                maxLength={4}
              />
              {errors.cfop && (
                <p className="mt-1 text-sm text-red-600">{errors.cfop.message}</p>
              )}
              <p className="mt-1 text-xs text-gray-500">
                Código Fiscal de Operações (4 dígitos)
              </p>
            </div>
          </div>

          {/* Informações Complementares */}
          <div className="mt-6">
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Informações Complementares
            </label>
            <textarea
              {...register('informacoesComplementares')}
              rows={4}
              className={`
                w-full rounded-lg border px-3 py-2
                focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                ${
                  errors.informacoesComplementares
                    ? 'border-red-500'
                    : 'border-gray-300'
                }
              `}
              placeholder="Adicione informações complementares da nota..."
              maxLength={1000}
            />
            {errors.informacoesComplementares && (
              <p className="mt-1 text-sm text-red-600">
                {errors.informacoesComplementares.message}
              </p>
            )}
          </div>
        </div>

        {/* Botões */}
        <div className="flex justify-end gap-3">
          <Link
            to="/financeiro/notas-fiscais"
            className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50"
          >
            Cancelar
          </Link>
          <button
            type="submit"
            disabled={isPending}
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:bg-gray-400"
          >
            <Save className="h-5 w-5" />
            {isPending ? 'Salvando...' : isEditing ? 'Atualizar' : 'Criar'}
          </button>
        </div>
      </form>
    </div>
  );
}
