/**
 * Formulario para edicao dos dados financeiros/bancarios da oficina
 */

import { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Building,
  CreditCard,
  QrCode,
  Loader2,
  Save,
  User,
} from 'lucide-react';
import { showSuccess, showError } from '@/shared/utils/notifications';
import { useOficina, useUpdateOficinaFinanceiro, useCurrentOficinaId } from '../hooks/useOficina';
import { oficinaFinanceiroSchema, type OficinaFinanceiroFormData } from '../utils/validation';
import { InputMask } from '@/shared/components/forms/InputMask';
import { BANCOS_BRASIL } from '../types';

const TIPOS_CONTA = [
  { value: 'CORRENTE', label: 'Conta Corrente' },
  { value: 'POUPANCA', label: 'Conta Poupanca' },
] as const;

const TIPOS_CHAVE_PIX = [
  { value: 'CPF', label: 'CPF' },
  { value: 'CNPJ', label: 'CNPJ' },
  { value: 'EMAIL', label: 'E-mail' },
  { value: 'TELEFONE', label: 'Telefone' },
  { value: 'ALEATORIA', label: 'Chave Aleatoria' },
] as const;

export const OficinaFinanceiroForm = () => {
  const oficinaId = useCurrentOficinaId();
  const { data: oficina, isLoading } = useOficina();
  const updateMutation = useUpdateOficinaFinanceiro();

  const {
    register,
    handleSubmit,
    control,
    reset,
    watch,
    formState: { errors, isDirty },
  } = useForm<OficinaFinanceiroFormData>({
    resolver: zodResolver(oficinaFinanceiroSchema),
    defaultValues: {
      banco: '',
      agencia: '',
      conta: '',
      tipoConta: undefined,
      titularConta: '',
      cpfCnpjTitular: '',
      chavePix: '',
      tipoChavePix: undefined,
    },
  });

  const tipoChavePix = watch('tipoChavePix');

  // Update form when oficina data is loaded
  useEffect(() => {
    if (oficina) {
      const dados = oficina.dadosBancarios;
      reset({
        banco: dados?.banco || '',
        agencia: dados?.agencia || '',
        conta: dados?.conta || '',
        tipoConta: dados?.tipoConta,
        titularConta: dados?.titularConta || '',
        cpfCnpjTitular: dados?.cpfCnpjTitular || '',
        chavePix: dados?.chavePix || '',
        tipoChavePix: dados?.tipoChavePix,
      });
    }
  }, [oficina, reset]);

  const onSubmit = async (data: OficinaFinanceiroFormData) => {
    if (!oficinaId) return;

    try {
      await updateMutation.mutateAsync({
        id: oficinaId,
        data: {
          dadosBancarios: {
            banco: data.banco || undefined,
            agencia: data.agencia || undefined,
            conta: data.conta || undefined,
            tipoConta: data.tipoConta,
            titularConta: data.titularConta || undefined,
            cpfCnpjTitular: data.cpfCnpjTitular || undefined,
            chavePix: data.chavePix || undefined,
            tipoChavePix: data.tipoChavePix,
          },
        },
      });
      showSuccess('Dados financeiros salvos com sucesso!');
    } catch (error: unknown) {
      console.error('Erro ao salvar dados financeiros:', error);
      let errorMessage = 'Erro ao salvar os dados financeiros';

      if (error && typeof error === 'object') {
        const axiosError = error as { response?: { data?: { message?: string; error?: string } }; message?: string };
        if (axiosError.response?.data?.message) {
          errorMessage = axiosError.response.data.message;
        } else if (axiosError.response?.data?.error) {
          errorMessage = axiosError.response.data.error;
        } else if (axiosError.message) {
          errorMessage = axiosError.message;
        }
      }

      showError(errorMessage);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (!oficinaId) {
    return (
      <div className="rounded-lg border border-yellow-200 bg-yellow-50 p-4 text-yellow-800 dark:border-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-200">
        Voce nao possui uma oficina vinculada.
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      {/* Aviso de seguranca */}
      <div className="rounded-lg border border-blue-200 bg-blue-50 p-4 dark:border-blue-800 dark:bg-blue-900/20">
        <p className="text-sm text-blue-800 dark:text-blue-200">
          <strong>Importante:</strong> Os dados bancarios sao utilizados para
          recebimento de pagamentos e emissao de notas fiscais. Mantenha-os
          sempre atualizados.
        </p>
      </div>

      {/* Dados Bancarios */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <Building className="h-5 w-5" />
          Dados Bancarios
        </h3>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div className="sm:col-span-2">
            <label
              htmlFor="banco"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Banco
            </label>
            <select
              id="banco"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('banco')}
            >
              <option value="">Selecione o banco</option>
              {BANCOS_BRASIL.map((banco) => (
                <option key={banco.value} value={banco.value}>
                  {banco.value} - {banco.label}
                </option>
              ))}
            </select>
            {errors.banco && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.banco.message}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="agencia"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Agencia
            </label>
            <input
              id="agencia"
              type="text"
              placeholder="0000"
              maxLength={6}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('agencia')}
            />
            {errors.agencia && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.agencia.message}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="conta"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Conta
            </label>
            <input
              id="conta"
              type="text"
              placeholder="00000-0"
              maxLength={15}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('conta')}
            />
            {errors.conta && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.conta.message}
              </p>
            )}
          </div>

          <div className="sm:col-span-2 lg:col-span-1">
            <label
              htmlFor="tipoConta"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Tipo de Conta
            </label>
            <select
              id="tipoConta"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('tipoConta')}
            >
              <option value="">Selecione</option>
              {TIPOS_CONTA.map((tipo) => (
                <option key={tipo.value} value={tipo.value}>
                  {tipo.label}
                </option>
              ))}
            </select>
            {errors.tipoConta && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.tipoConta.message}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Titular da Conta */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <User className="h-5 w-5" />
          Titular da Conta
        </h3>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label
              htmlFor="titularConta"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Nome do Titular
            </label>
            <input
              id="titularConta"
              type="text"
              placeholder="Nome completo do titular"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('titularConta')}
            />
            {errors.titularConta && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.titularConta.message}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="cpfCnpjTitular"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              CPF/CNPJ do Titular
            </label>
            <Controller
              name="cpfCnpjTitular"
              control={control}
              render={({ field }) => (
                <InputMask
                  {...field}
                  mask="cpfCnpj"
                  placeholder="000.000.000-00"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
                />
              )}
            />
            {errors.cpfCnpjTitular && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.cpfCnpjTitular.message}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* PIX */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <QrCode className="h-5 w-5" />
          Chave PIX
        </h3>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label
              htmlFor="tipoChavePix"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Tipo de Chave
            </label>
            <select
              id="tipoChavePix"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('tipoChavePix')}
            >
              <option value="">Selecione o tipo</option>
              {TIPOS_CHAVE_PIX.map((tipo) => (
                <option key={tipo.value} value={tipo.value}>
                  {tipo.label}
                </option>
              ))}
            </select>
            {errors.tipoChavePix && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.tipoChavePix.message}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="chavePix"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Chave PIX
            </label>
            {tipoChavePix === 'CPF' ? (
              <Controller
                name="chavePix"
                control={control}
                render={({ field }) => (
                  <InputMask
                    {...field}
                    mask="cpf"
                    placeholder="000.000.000-00"
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
                  />
                )}
              />
            ) : tipoChavePix === 'CNPJ' ? (
              <Controller
                name="chavePix"
                control={control}
                render={({ field }) => (
                  <InputMask
                    {...field}
                    mask="cnpj"
                    placeholder="00.000.000/0000-00"
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
                  />
                )}
              />
            ) : tipoChavePix === 'TELEFONE' ? (
              <Controller
                name="chavePix"
                control={control}
                render={({ field }) => (
                  <InputMask
                    {...field}
                    mask="phone"
                    placeholder="(00) 00000-0000"
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
                  />
                )}
              />
            ) : (
              <input
                id="chavePix"
                type="text"
                placeholder={
                  tipoChavePix === 'EMAIL'
                    ? 'email@exemplo.com'
                    : tipoChavePix === 'ALEATORIA'
                      ? 'Chave aleatoria'
                      : 'Informe a chave PIX'
                }
                className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
                {...register('chavePix')}
              />
            )}
            {errors.chavePix && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.chavePix.message}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Info adicional */}
      <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-800">
        <h4 className="mb-2 flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
          <CreditCard className="h-4 w-4" />
          Integracao com Gateway de Pagamento
        </h4>
        <p className="text-sm text-gray-600 dark:text-gray-400">
          Para receber pagamentos online (cartao de credito, boleto, PIX), acesse
          a aba de <strong>Configuracao de Pagamentos</strong> no menu Financeiro.
        </p>
      </div>

      {/* Submit */}
      <div className="flex justify-end border-t border-gray-200 pt-4 dark:border-gray-700">
        <button
          type="submit"
          disabled={updateMutation.isPending || !isDirty}
          className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {updateMutation.isPending ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              Salvando...
            </>
          ) : (
            <>
              <Save className="h-4 w-4" />
              Salvar Alteracoes
            </>
          )}
        </button>
      </div>
    </form>
  );
};
