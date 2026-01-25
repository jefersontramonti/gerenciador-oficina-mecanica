/**
 * Edit Oficina Page - Edit workshop details
 */

import { useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ArrowLeft, Save, Loader2, RefreshCw, Check, AlertCircle } from 'lucide-react';
import { CepInput } from '@/shared/components/forms/CepInput';
import { useOficinaDetail, useUpdateOficina, usePlanosActive } from '../hooks/useSaas';
import { showSuccess, showError } from '@/shared/utils/notifications';
import { formatCurrency } from '@/shared/utils/formatters';

const editSchema = z.object({
  razaoSocial: z.string().min(3, 'Razão social deve ter pelo menos 3 caracteres'),
  nomeFantasia: z.string().min(3, 'Nome fantasia deve ter pelo menos 3 caracteres'),
  email: z.string().email('Email inválido'),
  telefone: z.string().regex(/^\d{10,11}$/, 'Telefone deve ter 10 ou 11 dígitos'),
  plano: z.string().min(1, 'Selecione um plano'),
  valorMensalidade: z.number().min(0, 'Valor deve ser positivo'),
  cep: z.string().regex(/^\d{5}-?\d{3}$/, 'CEP inválido'),
  logradouro: z.string().min(3, 'Logradouro é obrigatório'),
  numero: z.string().min(1, 'Número é obrigatório'),
  complemento: z.string().optional(),
  bairro: z.string().min(2, 'Bairro é obrigatório'),
  cidade: z.string().min(2, 'Cidade é obrigatória'),
  estado: z.string().regex(/^[A-Z]{2}$/, 'Estado deve ser UF válida'),
});

type EditFormData = z.infer<typeof editSchema>;

export const EditOficinaPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: oficina, isLoading, error } = useOficinaDetail(id);
  const { data: planos, isLoading: isLoadingPlanos } = usePlanosActive();
  const updateMutation = useUpdateOficina();

  const {
    register,
    handleSubmit,
    watch,
    reset,
    setValue,
    control,
    formState: { errors, isDirty },
  } = useForm<EditFormData>({
    resolver: zodResolver(editSchema),
  });

  const selectedPlano = watch('plano');

  // Populate form with existing data
  useEffect(() => {
    if (oficina) {
      // Remove formatting from phone and CEP
      const telefoneClean = oficina.telefone?.replace(/\D/g, '') || '';
      const cepClean = oficina.cep?.replace(/\D/g, '') || '';

      reset({
        razaoSocial: oficina.razaoSocial,
        nomeFantasia: oficina.nomeFantasia,
        email: oficina.email,
        telefone: telefoneClean,
        plano: oficina.plano, // Already a string code
        valorMensalidade: oficina.valorMensalidade || 0,
        cep: cepClean,
        logradouro: oficina.logradouro || '',
        numero: oficina.numero || '',
        complemento: oficina.complemento || '',
        bairro: oficina.bairro || '',
        cidade: oficina.cidade || '',
        estado: oficina.estado || '',
      });
    }
  }, [oficina, reset]);

  const onSubmit = async (data: EditFormData) => {
    if (!id) return;

    try {
      await updateMutation.mutateAsync({ id, data });
      showSuccess('Oficina atualizada com sucesso!');
      navigate(`/admin/oficinas/${id}`);
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao atualizar oficina');
    }
  };

  if (isLoading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <RefreshCw className="h-8 w-8 animate-spin text-gray-400" />
      </div>
    );
  }

  if (error || !oficina) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-300 bg-red-50 p-4 text-red-800 dark:border-red-800 dark:bg-red-900/20 dark:text-red-400">
          Erro ao carregar oficina. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6 dark:bg-gray-900">
      {/* Header */}
      <div className="mb-8">
        <Link
          to={`/admin/oficinas/${id}`}
          className="mb-4 inline-flex items-center gap-2 text-sm text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-white"
        >
          <ArrowLeft className="h-4 w-4" />
          Voltar para detalhes
        </Link>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          Editar Oficina
        </h1>
        <p className="mt-1 text-gray-600 dark:text-gray-400">
          {oficina.nomeFantasia} - CNPJ: {oficina.cnpjCpf}
        </p>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="mx-auto max-w-3xl">
        <div className="space-y-6">
          {/* Dados Básicos */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
              Dados Básicos
            </h2>

            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Razão Social *
                </label>
                <input
                  {...register('razaoSocial')}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
                {errors.razaoSocial && (
                  <p className="mt-1 text-sm text-red-500">{errors.razaoSocial.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Nome Fantasia *
                </label>
                <input
                  {...register('nomeFantasia')}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
                {errors.nomeFantasia && (
                  <p className="mt-1 text-sm text-red-500">{errors.nomeFantasia.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Email *
                </label>
                <input
                  {...register('email')}
                  type="email"
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
                {errors.email && (
                  <p className="mt-1 text-sm text-red-500">{errors.email.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Telefone * (apenas números)
                </label>
                <input
                  {...register('telefone')}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  maxLength={11}
                />
                {errors.telefone && (
                  <p className="mt-1 text-sm text-red-500">{errors.telefone.message}</p>
                )}
              </div>
            </div>
          </div>

          {/* Endereço */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
              Endereço
            </h2>

            <div className="grid gap-4 md:grid-cols-3">
              <div>
                <Controller
                  name="cep"
                  control={control}
                  render={({ field }) => (
                    <CepInput
                      {...field}
                      label="CEP *"
                      error={errors.cep?.message}
                      onAddressFound={(endereco) => {
                        setValue('logradouro', endereco.logradouro, { shouldDirty: true });
                        setValue('bairro', endereco.bairro, { shouldDirty: true });
                        setValue('cidade', endereco.cidade, { shouldDirty: true });
                        setValue('estado', endereco.estado, { shouldDirty: true });
                      }}
                    />
                  )}
                />
              </div>

              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Logradouro *
                </label>
                <input
                  {...register('logradouro')}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
                {errors.logradouro && (
                  <p className="mt-1 text-sm text-red-500">{errors.logradouro.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Número *
                </label>
                <input
                  {...register('numero')}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
                {errors.numero && (
                  <p className="mt-1 text-sm text-red-500">{errors.numero.message}</p>
                )}
              </div>

              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Complemento
                </label>
                <input
                  {...register('complemento')}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Bairro *
                </label>
                <input
                  {...register('bairro')}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
                {errors.bairro && (
                  <p className="mt-1 text-sm text-red-500">{errors.bairro.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Cidade *
                </label>
                <input
                  {...register('cidade')}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                />
                {errors.cidade && (
                  <p className="mt-1 text-sm text-red-500">{errors.cidade.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Estado (UF) *
                </label>
                <input
                  {...register('estado')}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 uppercase dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  maxLength={2}
                />
                {errors.estado && (
                  <p className="mt-1 text-sm text-red-500">{errors.estado.message}</p>
                )}
              </div>
            </div>
          </div>

          {/* Plano */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
              Plano
            </h2>

            {isLoadingPlanos ? (
              <div className="flex h-32 items-center justify-center">
                <RefreshCw className="h-6 w-6 animate-spin text-gray-400" />
              </div>
            ) : !planos || planos.length === 0 ? (
              <div className="flex flex-col items-center justify-center rounded-lg border border-amber-200 bg-amber-50 p-4 dark:border-amber-800 dark:bg-amber-900/20">
                <AlertCircle className="h-6 w-6 text-amber-500 mb-2" />
                <p className="text-amber-800 dark:text-amber-300 text-sm">
                  Nenhum plano ativo. Configure os planos em Admin {">"} Planos.
                </p>
              </div>
            ) : (
              <div className="grid gap-4 md:grid-cols-3">
                {planos.map((plano) => (
                  <label
                    key={plano.id}
                    className={`relative flex cursor-pointer rounded-lg border-2 p-4 ${
                      selectedPlano === plano.codigo
                        ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                        : 'border-gray-200 hover:border-gray-300 dark:border-gray-700'
                    }`}
                  >
                    <input
                      type="radio"
                      {...register('plano')}
                      value={plano.codigo}
                      className="sr-only"
                    />
                    <div className="text-center w-full">
                      <div className="flex items-center justify-center gap-2">
                        <p className="font-semibold text-gray-900 dark:text-white">
                          {plano.nome}
                        </p>
                        {plano.recomendado && (
                          <span className="rounded-full bg-purple-100 px-2 py-0.5 text-xs font-medium text-purple-700 dark:bg-purple-900/30 dark:text-purple-400">
                            Recomendado
                          </span>
                        )}
                      </div>
                      <p className="text-lg font-bold text-blue-600 dark:text-blue-400">
                        {plano.precoSobConsulta ? 'Sob consulta' : `${formatCurrency(plano.valorMensal)}/mês`}
                      </p>
                      <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                        {plano.limiteUsuarios === -1 ? 'Usuários ilimitados' : `${plano.limiteUsuarios} usuário(s)`}
                      </p>
                    </div>
                    {selectedPlano === plano.codigo && (
                      <div className="absolute right-2 top-2">
                        <Check className="h-5 w-5 text-blue-500" />
                      </div>
                    )}
                  </label>
                ))}
              </div>
            )}

            {/* Valor Personalizado */}
            <div className="mt-6 rounded-lg border border-amber-200 bg-amber-50 p-4 dark:border-amber-700 dark:bg-amber-900/20">
              <label className="block text-sm font-medium text-amber-800 dark:text-amber-300">
                Valor Mensal Personalizado (R$)
              </label>
              <p className="text-xs text-amber-700 dark:text-amber-400 mb-2">
                Define um valor específico para esta oficina, independente do preço padrão do plano
              </p>
              <input
                type="number"
                step="0.01"
                min="0"
                {...register('valorMensalidade', { valueAsNumber: true })}
                className="mt-1 w-full max-w-xs rounded-lg border border-amber-300 bg-white px-3 py-2 dark:border-amber-600 dark:bg-gray-700 dark:text-white"
                placeholder="0.00"
              />
              {errors.valorMensalidade && (
                <p className="mt-1 text-sm text-red-500">{errors.valorMensalidade.message}</p>
              )}
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-4">
            <Link
              to={`/admin/oficinas/${id}`}
              className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Cancelar
            </Link>
            <button
              type="submit"
              disabled={!isDirty || updateMutation.isPending}
              className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {updateMutation.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Salvando...
                </>
              ) : (
                <>
                  <Save className="h-4 w-4" />
                  Salvar Alterações
                </>
              )}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};
