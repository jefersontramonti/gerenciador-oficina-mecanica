/**
 * Plano Form Page - Create or edit a subscription plan
 */

import { useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  ArrowLeft,
  Save,
  Loader2,
  RefreshCw,
  CreditCard,
  Settings,
  Palette,
  Shield,
} from 'lucide-react';
import { usePlanoDetail, useCreatePlano, useUpdatePlano } from '../hooks/useSaas';
import { showSuccess, showError } from '@/shared/utils/notifications';

const planoSchema = z.object({
  codigo: z
    .string()
    .min(2, 'Código deve ter pelo menos 2 caracteres')
    .max(30, 'Código deve ter no máximo 30 caracteres')
    .regex(/^[A-Z0-9_]+$/, 'Código deve conter apenas letras maiúsculas, números e underscore'),
  nome: z
    .string()
    .min(2, 'Nome deve ter pelo menos 2 caracteres')
    .max(100, 'Nome deve ter no máximo 100 caracteres'),
  descricao: z.string().max(2000, 'Descrição deve ter no máximo 2000 caracteres').optional(),

  // Pricing
  valorMensal: z.number().min(0, 'Valor mensal não pode ser negativo'),
  valorAnual: z.number().min(0, 'Valor anual não pode ser negativo').optional(),
  trialDias: z.number().min(0).max(365).optional(),

  // Limits
  limiteUsuarios: z.number().min(-1),
  limiteOsMes: z.number().min(-1),
  limiteClientes: z.number().min(-1),
  limiteEspacoMb: z.number().min(-1),
  limiteApiCalls: z.number().min(-1),
  limiteWhatsappMensagens: z.number().min(0),
  limiteEmailsMes: z.number().min(0),

  // Features
  emiteNotaFiscal: z.boolean(),
  whatsappAutomatizado: z.boolean(),
  manutencaoPreventiva: z.boolean(),
  anexoImagensDocumentos: z.boolean(),
  relatoriosAvancados: z.boolean(),
  integracaoMercadoPago: z.boolean(),
  suportePrioritario: z.boolean(),
  backupAutomatico: z.boolean(),

  // Display
  ativo: z.boolean(),
  visivel: z.boolean(),
  recomendado: z.boolean(),
  corDestaque: z.string().optional(),
  tagPromocao: z.string().max(50).optional(),
  ordemExibicao: z.number().min(0),
});

type PlanoFormData = z.infer<typeof planoSchema>;

const defaultFeatures = {
  emiteNotaFiscal: false,
  whatsappAutomatizado: false,
  manutencaoPreventiva: false,
  anexoImagensDocumentos: false,
  relatoriosAvancados: false,
  integracaoMercadoPago: false,
  suportePrioritario: false,
  backupAutomatico: true,
};

const defaultValues: PlanoFormData = {
  codigo: '',
  nome: '',
  descricao: '',
  valorMensal: 0,
  valorAnual: 0,
  trialDias: 14,
  limiteUsuarios: 1,
  limiteOsMes: -1,
  limiteClientes: -1,
  limiteEspacoMb: 5120,
  limiteApiCalls: -1,
  limiteWhatsappMensagens: 0,
  limiteEmailsMes: 100,
  ...defaultFeatures,
  ativo: true,
  visivel: true,
  recomendado: false,
  corDestaque: '#3B82F6',
  tagPromocao: '',
  ordemExibicao: 0,
};

export const PlanoFormPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditing = !!id;

  const { data: plano, isLoading: isLoadingPlano } = usePlanoDetail(id);
  const createMutation = useCreatePlano();
  const updateMutation = useUpdatePlano();

  const {
    register,
    handleSubmit,
    control,
    reset,
    watch,
    formState: { errors, isDirty },
  } = useForm<PlanoFormData>({
    resolver: zodResolver(planoSchema),
    defaultValues,
  });

  const valorMensal = watch('valorMensal');

  // Populate form with existing data
  useEffect(() => {
    if (plano && isEditing) {
      reset({
        codigo: plano.codigo,
        nome: plano.nome,
        descricao: plano.descricao || '',
        valorMensal: plano.valorMensal,
        valorAnual: plano.valorAnual || 0,
        trialDias: plano.trialDias,
        limiteUsuarios: plano.limiteUsuarios,
        limiteOsMes: plano.limiteOsMes,
        limiteClientes: plano.limiteClientes,
        limiteEspacoMb: plano.limiteEspacoMb,
        limiteApiCalls: plano.limiteApiCalls,
        limiteWhatsappMensagens: plano.limiteWhatsappMensagens,
        limiteEmailsMes: plano.limiteEmailsMes,
        emiteNotaFiscal: plano.features?.emiteNotaFiscal ?? false,
        whatsappAutomatizado: plano.features?.whatsappAutomatizado ?? false,
        manutencaoPreventiva: plano.features?.manutencaoPreventiva ?? false,
        anexoImagensDocumentos: plano.features?.anexoImagensDocumentos ?? false,
        relatoriosAvancados: plano.features?.relatoriosAvancados ?? false,
        integracaoMercadoPago: plano.features?.integracaoMercadoPago ?? false,
        suportePrioritario: plano.features?.suportePrioritario ?? false,
        backupAutomatico: plano.features?.backupAutomatico ?? true,
        ativo: plano.ativo,
        visivel: plano.visivel,
        recomendado: plano.recomendado,
        corDestaque: plano.corDestaque || '#3B82F6',
        tagPromocao: plano.tagPromocao || '',
        ordemExibicao: plano.ordemExibicao,
      });
    }
  }, [plano, isEditing, reset]);

  const onSubmit = async (data: PlanoFormData) => {
    try {
      const features = {
        emiteNotaFiscal: data.emiteNotaFiscal,
        whatsappAutomatizado: data.whatsappAutomatizado,
        manutencaoPreventiva: data.manutencaoPreventiva,
        anexoImagensDocumentos: data.anexoImagensDocumentos,
        relatoriosAvancados: data.relatoriosAvancados,
        integracaoMercadoPago: data.integracaoMercadoPago,
        suportePrioritario: data.suportePrioritario,
        backupAutomatico: data.backupAutomatico,
      };

      const payload = {
        codigo: data.codigo,
        nome: data.nome,
        descricao: data.descricao || undefined,
        valorMensal: data.valorMensal,
        valorAnual: data.valorAnual || undefined,
        trialDias: data.trialDias,
        limiteUsuarios: data.limiteUsuarios,
        limiteOsMes: data.limiteOsMes,
        limiteClientes: data.limiteClientes,
        limiteEspacoMb: data.limiteEspacoMb,
        limiteApiCalls: data.limiteApiCalls,
        limiteWhatsappMensagens: data.limiteWhatsappMensagens,
        limiteEmailsMes: data.limiteEmailsMes,
        features,
        ativo: data.ativo,
        visivel: data.visivel,
        recomendado: data.recomendado,
        corDestaque: data.corDestaque || undefined,
        tagPromocao: data.tagPromocao || undefined,
        ordemExibicao: data.ordemExibicao,
      };

      if (isEditing) {
        await updateMutation.mutateAsync({ id, data: payload });
        showSuccess('Plano atualizado com sucesso!');
      } else {
        await createMutation.mutateAsync(payload);
        showSuccess('Plano criado com sucesso!');
      }

      navigate('/admin/planos');
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao salvar plano');
    }
  };

  if (isEditing && isLoadingPlano) {
    return (
      <div className="flex h-96 items-center justify-center">
        <RefreshCw className="h-8 w-8 animate-spin text-gray-400" />
      </div>
    );
  }

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="min-h-screen bg-gray-50 p-6 dark:bg-gray-900">
      {/* Header */}
      <div className="mb-8">
        <Link
          to="/admin/planos"
          className="mb-4 inline-flex items-center gap-2 text-sm text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-white"
        >
          <ArrowLeft className="h-4 w-4" />
          Voltar para Planos
        </Link>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          {isEditing ? 'Editar Plano' : 'Novo Plano'}
        </h1>
        {isEditing && plano && (
          <p className="mt-1 text-gray-600 dark:text-gray-400">
            {plano.nome} ({plano.codigo})
          </p>
        )}
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="mx-auto max-w-4xl space-y-6">
        {/* Basic Info */}
        <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
          <div className="mb-4 flex items-center gap-2">
            <CreditCard className="h-5 w-5 text-gray-600 dark:text-gray-400" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Informações Básicas
            </h2>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Código *
              </label>
              <input
                {...register('codigo')}
                placeholder="EX: PREMIUM_PLUS"
                disabled={isEditing}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 uppercase disabled:bg-gray-100 dark:border-gray-600 dark:bg-gray-700 dark:text-white dark:disabled:bg-gray-600"
              />
              {errors.codigo && (
                <p className="mt-1 text-sm text-red-500">{errors.codigo.message}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Nome *
              </label>
              <input
                {...register('nome')}
                placeholder="Ex: Premium Plus"
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
              {errors.nome && (
                <p className="mt-1 text-sm text-red-500">{errors.nome.message}</p>
              )}
            </div>

            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Descrição
              </label>
              <textarea
                {...register('descricao')}
                rows={3}
                placeholder="Descrição detalhada do plano..."
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
          </div>
        </div>

        {/* Pricing */}
        <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
          <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
            Preços
          </h2>

          <div className="grid gap-4 md:grid-cols-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Valor Mensal (R$) *
              </label>
              <input
                type="number"
                step="0.01"
                {...register('valorMensal', { valueAsNumber: true })}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
              <p className="mt-1 text-xs text-gray-500">
                Use 0 para preço sob consulta
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Valor Anual (R$)
              </label>
              <input
                type="number"
                step="0.01"
                {...register('valorAnual', { valueAsNumber: true })}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
              {valorMensal > 0 && (
                <p className="mt-1 text-xs text-gray-500">
                  Sugestão com 20% off: R$ {(valorMensal * 12 * 0.8).toFixed(2)}
                </p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Dias de Trial
              </label>
              <input
                type="number"
                {...register('trialDias', { valueAsNumber: true })}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
          </div>
        </div>

        {/* Limits */}
        <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
          <div className="mb-4 flex items-center gap-2">
            <Settings className="h-5 w-5 text-gray-600 dark:text-gray-400" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Limites
            </h2>
          </div>
          <p className="mb-4 text-sm text-gray-500">
            Use -1 para indicar "ilimitado"
          </p>

          <div className="grid gap-4 md:grid-cols-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Usuários
              </label>
              <input
                type="number"
                {...register('limiteUsuarios', { valueAsNumber: true })}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                OS/Mês
              </label>
              <input
                type="number"
                {...register('limiteOsMes', { valueAsNumber: true })}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Clientes
              </label>
              <input
                type="number"
                {...register('limiteClientes', { valueAsNumber: true })}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Espaço (MB)
              </label>
              <input
                type="number"
                {...register('limiteEspacoMb', { valueAsNumber: true })}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                API Calls
              </label>
              <input
                type="number"
                {...register('limiteApiCalls', { valueAsNumber: true })}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                WhatsApp/Mês
              </label>
              <input
                type="number"
                {...register('limiteWhatsappMensagens', { valueAsNumber: true })}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Emails/Mês
              </label>
              <input
                type="number"
                {...register('limiteEmailsMes', { valueAsNumber: true })}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
          </div>
        </div>

        {/* Features */}
        <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
          <div className="mb-4 flex items-center gap-2">
            <Shield className="h-5 w-5 text-gray-600 dark:text-gray-400" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Funcionalidades
            </h2>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            {[
              { key: 'emiteNotaFiscal', label: 'Emite Nota Fiscal' },
              { key: 'whatsappAutomatizado', label: 'WhatsApp Automatizado' },
              { key: 'manutencaoPreventiva', label: 'Manutenção Preventiva' },
              { key: 'anexoImagensDocumentos', label: 'Anexo de Imagens/Documentos' },
              { key: 'relatoriosAvancados', label: 'Relatórios Avançados' },
              { key: 'integracaoMercadoPago', label: 'Integração Mercado Pago' },
              { key: 'suportePrioritario', label: 'Suporte Prioritário' },
              { key: 'backupAutomatico', label: 'Backup Automático' },
            ].map(({ key, label }) => (
              <label key={key} className="flex items-center gap-3 cursor-pointer">
                <Controller
                  name={key as keyof PlanoFormData}
                  control={control}
                  render={({ field }) => (
                    <input
                      type="checkbox"
                      checked={field.value as boolean}
                      onChange={field.onChange}
                      className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                  )}
                />
                <span className="text-gray-700 dark:text-gray-300">{label}</span>
              </label>
            ))}
          </div>
        </div>

        {/* Display Settings */}
        <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
          <div className="mb-4 flex items-center gap-2">
            <Palette className="h-5 w-5 text-gray-600 dark:text-gray-400" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Exibição
            </h2>
          </div>

          <div className="grid gap-4 md:grid-cols-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Cor de Destaque
              </label>
              <input
                type="color"
                {...register('corDestaque')}
                className="mt-1 h-10 w-full rounded-lg border border-gray-300 p-1 dark:border-gray-600"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Tag de Promoção
              </label>
              <input
                {...register('tagPromocao')}
                placeholder="Ex: Mais Popular"
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Ordem de Exibição
              </label>
              <input
                type="number"
                {...register('ordemExibicao', { valueAsNumber: true })}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
          </div>

          <div className="mt-4 flex flex-wrap gap-6">
            <label className="flex items-center gap-3 cursor-pointer">
              <Controller
                name="ativo"
                control={control}
                render={({ field }) => (
                  <input
                    type="checkbox"
                    checked={field.value}
                    onChange={field.onChange}
                    className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                )}
              />
              <span className="text-gray-700 dark:text-gray-300">Plano Ativo</span>
            </label>

            <label className="flex items-center gap-3 cursor-pointer">
              <Controller
                name="visivel"
                control={control}
                render={({ field }) => (
                  <input
                    type="checkbox"
                    checked={field.value}
                    onChange={field.onChange}
                    className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                )}
              />
              <span className="text-gray-700 dark:text-gray-300">Visível na página de preços</span>
            </label>

            <label className="flex items-center gap-3 cursor-pointer">
              <Controller
                name="recomendado"
                control={control}
                render={({ field }) => (
                  <input
                    type="checkbox"
                    checked={field.value}
                    onChange={field.onChange}
                    className="h-4 w-4 rounded border-gray-300 text-purple-600 focus:ring-purple-500"
                  />
                )}
              />
              <span className="text-gray-700 dark:text-gray-300">Marcar como Recomendado</span>
            </label>
          </div>
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-4">
          <Link
            to="/admin/planos"
            className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
          >
            Cancelar
          </Link>
          <button
            type="submit"
            disabled={isPending || (!isDirty && isEditing)}
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {isPending ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Salvando...
              </>
            ) : (
              <>
                <Save className="h-4 w-4" />
                {isEditing ? 'Salvar Alterações' : 'Criar Plano'}
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};
