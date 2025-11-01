import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, Save } from 'lucide-react';
import { useCliente, useCreateCliente, useUpdateCliente } from '../hooks/useClientes';
import { createClienteSchema, updateClienteSchema } from '../utils/validation';
import { InputMask } from '@/shared/components/forms/InputMask';
import { TipoCliente } from '../types';
import type { CreateClienteFormData, UpdateClienteFormData } from '../utils/validation';

const UFS = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA',
  'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN',
  'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
];

export const ClienteFormPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEditMode = !!id;

  const { data: cliente, isLoading: loadingCliente } = useCliente(id!, !!id);
  const createMutation = useCreateCliente();
  const updateMutation = useUpdateCliente();

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm<CreateClienteFormData | UpdateClienteFormData>({
    resolver: zodResolver(isEditMode ? updateClienteSchema : createClienteSchema),
    defaultValues: {
      tipo: TipoCliente.PESSOA_FISICA,
    },
  });

  const tipoCliente = watch('tipo');

  // Load cliente data when editing
  useEffect(() => {
    if (cliente && isEditMode) {
      setValue('nome', cliente.nome);
      setValue('email', cliente.email || '');
      setValue('telefone', cliente.telefone || '');
      setValue('celular', cliente.celular || '');

      if (cliente.endereco) {
        setValue('logradouro', cliente.endereco.logradouro || '');
        setValue('numero', cliente.endereco.numero || '');
        setValue('complemento', cliente.endereco.complemento || '');
        setValue('bairro', cliente.endereco.bairro || '');
        setValue('cidade', cliente.endereco.cidade || '');
        setValue('estado', cliente.endereco.estado || '');
        setValue('cep', cliente.endereco.cep || '');
      }
    }
  }, [cliente, isEditMode, setValue]);

  const onSubmit = async (data: CreateClienteFormData | UpdateClienteFormData) => {
    try {
      if (isEditMode) {
        await updateMutation.mutateAsync({
          id: id!,
          data: data as UpdateClienteFormData,
        });
      } else {
        await createMutation.mutateAsync(data as CreateClienteFormData);
      }
      navigate('/clientes');
    } catch (error: any) {
      alert(error.message || 'Erro ao salvar cliente');
    }
  };

  if (loadingCliente) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="text-gray-500">Carregando...</div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center gap-4">
        <button
          onClick={() => navigate('/clientes')}
          className="rounded-lg p-2 hover:bg-gray-100"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {isEditMode ? 'Editar Cliente' : 'Novo Cliente'}
          </h1>
          <p className="mt-1 text-sm text-gray-600">
            {isEditMode
              ? 'Atualize os dados do cliente'
              : 'Preencha os dados para cadastrar um novo cliente'}
          </p>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="mx-auto max-w-4xl">
        <div className="space-y-6">
          {/* Tipo e Dados Básicos */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900">Dados Básicos</h2>

            <div className="grid gap-4 md:grid-cols-2">
              {/* Tipo - Only on create */}
              {!isEditMode && (
                <div>
                  <label className="mb-1 block text-sm font-medium text-gray-700">
                    Tipo <span className="text-red-500">*</span>
                  </label>
                  <select
                    {...register('tipo')}
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  >
                    <option value={TipoCliente.PESSOA_FISICA}>Pessoa Física</option>
                    <option value={TipoCliente.PESSOA_JURIDICA}>Pessoa Jurídica</option>
                  </select>
                  {'tipo' in errors && errors.tipo && (
                    <p className="mt-1 text-sm text-red-500">{errors.tipo.message}</p>
                  )}
                </div>
              )}

              {/* CPF/CNPJ - Only on create */}
              {!isEditMode && (
                <Controller
                  name="cpfCnpj"
                  control={control}
                  render={({ field }) => (
                    <InputMask
                      {...field}
                      mask="cpfCnpj"
                      label={tipoCliente === TipoCliente.PESSOA_FISICA ? 'CPF' : 'CNPJ'}
                      placeholder={
                        tipoCliente === TipoCliente.PESSOA_FISICA
                          ? '000.000.000-00'
                          : '00.000.000/0000-00'
                      }
                      required
                      error={'cpfCnpj' in errors ? errors.cpfCnpj?.message : undefined}
                    />
                  )}
                />
              )}

              {/* Nome */}
              <div className={isEditMode ? 'md:col-span-2' : ''}>
                <label className="mb-1 block text-sm font-medium text-gray-700">
                  {tipoCliente === TipoCliente.PESSOA_FISICA ? 'Nome Completo' : 'Razão Social'}{' '}
                  <span className="text-red-500">*</span>
                </label>
                <input
                  {...register('nome')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Digite o nome"
                />
                {errors.nome && (
                  <p className="mt-1 text-sm text-red-500">{errors.nome.message}</p>
                )}
              </div>

              {/* Email */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">Email</label>
                <input
                  {...register('email')}
                  type="email"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="email@exemplo.com"
                />
                {errors.email && (
                  <p className="mt-1 text-sm text-red-500">{errors.email.message}</p>
                )}
              </div>

              {/* Telefone */}
              <Controller
                name="telefone"
                control={control}
                render={({ field }) => (
                  <InputMask
                    {...field}
                    mask="phone"
                    label="Telefone"
                    placeholder="(00) 0000-0000"
                    error={errors.telefone?.message}
                  />
                )}
              />

              {/* Celular */}
              <Controller
                name="celular"
                control={control}
                render={({ field }) => (
                  <InputMask
                    {...field}
                    mask="phone"
                    label="Celular"
                    placeholder="(00) 00000-0000"
                    error={errors.celular?.message}
                  />
                )}
              />
            </div>
          </div>

          {/* Endereço */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900">Endereço</h2>

            <div className="grid gap-4 md:grid-cols-4">
              {/* CEP */}
              <div className="md:col-span-2">
                <Controller
                  name="cep"
                  control={control}
                  render={({ field }) => (
                    <InputMask
                      {...field}
                      mask="cep"
                      label="CEP"
                      placeholder="00000-000"
                      error={errors.cep?.message}
                    />
                  )}
                />
              </div>

              {/* Logradouro */}
              <div className="md:col-span-3">
                <label className="mb-1 block text-sm font-medium text-gray-700">
                  Logradouro
                </label>
                <input
                  {...register('logradouro')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Rua, Avenida, etc."
                />
                {errors.logradouro && (
                  <p className="mt-1 text-sm text-red-500">{errors.logradouro.message}</p>
                )}
              </div>

              {/* Número */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">Número</label>
                <input
                  {...register('numero')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="123"
                />
                {errors.numero && (
                  <p className="mt-1 text-sm text-red-500">{errors.numero.message}</p>
                )}
              </div>

              {/* Complemento */}
              <div className="md:col-span-2">
                <label className="mb-1 block text-sm font-medium text-gray-700">
                  Complemento
                </label>
                <input
                  {...register('complemento')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Apt, Sala, etc."
                />
                {errors.complemento && (
                  <p className="mt-1 text-sm text-red-500">{errors.complemento.message}</p>
                )}
              </div>

              {/* Bairro */}
              <div className="md:col-span-2">
                <label className="mb-1 block text-sm font-medium text-gray-700">Bairro</label>
                <input
                  {...register('bairro')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Centro, Jardim, etc."
                />
                {errors.bairro && (
                  <p className="mt-1 text-sm text-red-500">{errors.bairro.message}</p>
                )}
              </div>

              {/* Cidade */}
              <div className="md:col-span-2">
                <label className="mb-1 block text-sm font-medium text-gray-700">Cidade</label>
                <input
                  {...register('cidade')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="São Paulo"
                />
                {errors.cidade && (
                  <p className="mt-1 text-sm text-red-500">{errors.cidade.message}</p>
                )}
              </div>

              {/* Estado */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">Estado</label>
                <select
                  {...register('estado')}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                >
                  <option value="">Selecione</option>
                  {UFS.map((uf) => (
                    <option key={uf} value={uf}>
                      {uf}
                    </option>
                  ))}
                </select>
                {errors.estado && (
                  <p className="mt-1 text-sm text-red-500">{errors.estado.message}</p>
                )}
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-4">
            <button
              type="button"
              onClick={() => navigate('/clientes')}
              className="rounded-lg border border-gray-300 px-6 py-2 font-medium text-gray-700 hover:bg-gray-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending || updateMutation.isPending}
              className="flex items-center gap-2 rounded-lg bg-blue-600 px-6 py-2 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Save className="h-5 w-5" />
              {isEditMode ? 'Atualizar' : 'Cadastrar'}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};
