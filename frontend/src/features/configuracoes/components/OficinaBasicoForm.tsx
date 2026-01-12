/**
 * Formulario para edicao dos dados basicos da oficina
 */

import { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Building2,
  Phone,
  Mail,
  MapPin,
  Loader2,
  Save,
} from 'lucide-react';
import { showSuccess, showError } from '@/shared/utils/notifications';
import { useOficina, useUpdateOficinaBasico, useCurrentOficinaId } from '../hooks/useOficina';
import { oficinaBasicoSchema, type OficinaBasicoFormData } from '../utils/validation';
import { InputMask } from '@/shared/components/forms/InputMask';
import { tipoPessoaLabels, ESTADOS_BRASIL } from '../types';

export const OficinaBasicoForm = () => {
  const oficinaId = useCurrentOficinaId();
  const { data: oficina, isLoading } = useOficina();
  const updateMutation = useUpdateOficinaBasico();

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isDirty },
  } = useForm<OficinaBasicoFormData>({
    resolver: zodResolver(oficinaBasicoSchema),
    defaultValues: {
      nome: '',
      nomeFantasia: '',
      tipoPessoa: 'PESSOA_JURIDICA',
      nomeResponsavel: '',
      email: '',
      telefone: '',
      celular: '',
      cep: '',
      logradouro: '',
      numero: '',
      complemento: '',
      bairro: '',
      cidade: '',
      estado: '',
    },
  });

  // Update form when oficina data is loaded
  useEffect(() => {
    if (oficina) {
      reset({
        nome: oficina.razaoSocial || '',
        nomeFantasia: oficina.nomeFantasia || '',
        tipoPessoa: oficina.tipoPessoa || 'PESSOA_JURIDICA',
        nomeResponsavel: oficina.nomeResponsavel || '',
        email: oficina.email || '',
        telefone: oficina.telefone || '',
        celular: oficina.celular || '',
        cep: oficina.endereco?.cep || '',
        logradouro: oficina.endereco?.logradouro || '',
        numero: oficina.endereco?.numero || '',
        complemento: oficina.endereco?.complemento || '',
        bairro: oficina.endereco?.bairro || '',
        cidade: oficina.endereco?.cidade || '',
        estado: oficina.endereco?.estado || '',
      });
    }
  }, [oficina, reset]);

  const onSubmit = async (data: OficinaBasicoFormData) => {
    if (!oficinaId) return;

    try {
      await updateMutation.mutateAsync({
        id: oficinaId,
        data: {
          nome: data.nome,
          nomeFantasia: data.nomeFantasia || undefined,
          tipoPessoa: data.tipoPessoa,
          contato: {
            email: data.email || undefined,
            telefoneFixo: data.telefone || undefined,
            telefoneCelular: data.celular || undefined,
          },
          endereco: {
            cep: data.cep || undefined,
            logradouro: data.logradouro || undefined,
            numero: data.numero || undefined,
            complemento: data.complemento || undefined,
            bairro: data.bairro || undefined,
            cidade: data.cidade || undefined,
            estado: data.estado || undefined,
          },
        },
      });
      showSuccess('Dados basicos salvos com sucesso!');
    } catch (error: unknown) {
      console.error('Erro ao salvar oficina:', error);
      let errorMessage = 'Erro ao salvar os dados';

      // Extract error message from API response
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
      {/* Identificacao */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <Building2 className="h-5 w-5" />
          Identificacao
        </h3>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label
              htmlFor="nome"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Razao Social / Nome *
            </label>
            <input
              id="nome"
              type="text"
              placeholder="Nome da empresa"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('nome')}
            />
            {errors.nome && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.nome.message}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="nomeFantasia"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Nome Fantasia
            </label>
            <input
              id="nomeFantasia"
              type="text"
              placeholder="Nome fantasia"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('nomeFantasia')}
            />
            {errors.nomeFantasia && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.nomeFantasia.message}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="tipoPessoa"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Tipo de Pessoa *
            </label>
            <select
              id="tipoPessoa"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('tipoPessoa')}
            >
              {Object.entries(tipoPessoaLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
            {errors.tipoPessoa && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.tipoPessoa.message}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="nomeResponsavel"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Nome do Responsavel
            </label>
            <input
              id="nomeResponsavel"
              type="text"
              placeholder="Nome do responsavel"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('nomeResponsavel')}
            />
            {errors.nomeResponsavel && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.nomeResponsavel.message}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Contato */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <Phone className="h-5 w-5" />
          Contato
        </h3>

        <div className="grid gap-4 sm:grid-cols-3">
          <div>
            <label
              htmlFor="email"
              className="mb-1 flex items-center gap-1 text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              <Mail className="h-4 w-4" />
              E-mail
            </label>
            <input
              id="email"
              type="email"
              placeholder="email@exemplo.com"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('email')}
            />
            {errors.email && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.email.message}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="telefone"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Telefone
            </label>
            <Controller
              name="telefone"
              control={control}
              render={({ field }) => (
                <InputMask
                  {...field}
                  mask="phone"
                  placeholder="(00) 0000-0000"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
                />
              )}
            />
            {errors.telefone && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.telefone.message}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="celular"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Celular / WhatsApp
            </label>
            <Controller
              name="celular"
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
            {errors.celular && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.celular.message}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Endereco */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <MapPin className="h-5 w-5" />
          Endereco
        </h3>

        <div className="grid gap-4 sm:grid-cols-6">
          <div className="sm:col-span-2">
            <label
              htmlFor="cep"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              CEP
            </label>
            <Controller
              name="cep"
              control={control}
              render={({ field }) => (
                <InputMask
                  {...field}
                  mask="cep"
                  placeholder="00000-000"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
                />
              )}
            />
            {errors.cep && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.cep.message}
              </p>
            )}
          </div>

          <div className="sm:col-span-4">
            <label
              htmlFor="logradouro"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Logradouro
            </label>
            <input
              id="logradouro"
              type="text"
              placeholder="Rua, Avenida, etc."
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('logradouro')}
            />
            {errors.logradouro && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.logradouro.message}
              </p>
            )}
          </div>

          <div className="sm:col-span-1">
            <label
              htmlFor="numero"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Numero
            </label>
            <input
              id="numero"
              type="text"
              placeholder="123"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('numero')}
            />
            {errors.numero && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.numero.message}
              </p>
            )}
          </div>

          <div className="sm:col-span-2">
            <label
              htmlFor="complemento"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Complemento
            </label>
            <input
              id="complemento"
              type="text"
              placeholder="Sala, Bloco, etc."
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('complemento')}
            />
            {errors.complemento && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.complemento.message}
              </p>
            )}
          </div>

          <div className="sm:col-span-3">
            <label
              htmlFor="bairro"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Bairro
            </label>
            <input
              id="bairro"
              type="text"
              placeholder="Bairro"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('bairro')}
            />
            {errors.bairro && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.bairro.message}
              </p>
            )}
          </div>

          <div className="sm:col-span-4">
            <label
              htmlFor="cidade"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Cidade
            </label>
            <input
              id="cidade"
              type="text"
              placeholder="Cidade"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('cidade')}
            />
            {errors.cidade && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.cidade.message}
              </p>
            )}
          </div>

          <div className="sm:col-span-2">
            <label
              htmlFor="estado"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Estado
            </label>
            <select
              id="estado"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('estado')}
            >
              <option value="">Selecione</option>
              {ESTADOS_BRASIL.map((estado) => (
                <option key={estado.value} value={estado.value}>
                  {estado.label}
                </option>
              ))}
            </select>
            {errors.estado && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.estado.message}
              </p>
            )}
          </div>
        </div>
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
