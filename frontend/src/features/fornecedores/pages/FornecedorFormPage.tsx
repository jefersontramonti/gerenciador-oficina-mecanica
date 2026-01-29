import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, Save } from 'lucide-react';
import { ButtonShine } from '@/shared/components/ui/ButtonShine';
import { useFornecedor, useCreateFornecedor, useUpdateFornecedor } from '../hooks/useFornecedores';
import { createFornecedorSchema } from '../utils/validation';
import { InputMask } from '@/shared/components/forms/InputMask';
import { CepInput } from '@/shared/components/forms/CepInput';
import { TipoFornecedor, TipoFornecedorLabel } from '../types';
import type { CreateFornecedorFormData } from '../utils/validation';
import type { CreateFornecedorRequest, UpdateFornecedorRequest } from '../types';

const UFS = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA',
  'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN',
  'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
];

export const FornecedorFormPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEditMode = !!id;

  const { data: fornecedor, isLoading: loadingFornecedor } = useFornecedor(id);
  const createMutation = useCreateFornecedor();
  const updateMutation = useUpdateFornecedor();

  const {
    register,
    handleSubmit,
    control,
    setValue,
    trigger,
    formState: { errors },
  } = useForm<CreateFornecedorFormData>({
    resolver: zodResolver(createFornecedorSchema) as any,
    defaultValues: {
      tipo: TipoFornecedor.DISTRIBUIDOR,
      nomeFantasia: '',
      razaoSocial: '',
      cpfCnpj: '',
      inscricaoEstadual: '',
      email: '',
      telefone: '',
      celular: '',
      website: '',
      contatoNome: '',
      logradouro: '',
      numero: '',
      complemento: '',
      bairro: '',
      cidade: '',
      estado: '',
      cep: '',
      prazoEntrega: '',
      condicoesPagamento: '',
      descontoPadrao: '' as any,
      observacoes: '',
    },
  });

  // Load fornecedor data when editing
  useEffect(() => {
    if (fornecedor && isEditMode) {
      setValue('tipo', fornecedor.tipo);
      setValue('nomeFantasia', fornecedor.nomeFantasia);
      setValue('razaoSocial', fornecedor.razaoSocial || '');
      setValue('cpfCnpj', fornecedor.cpfCnpj || '');
      setValue('inscricaoEstadual', fornecedor.inscricaoEstadual || '');
      setValue('email', fornecedor.email || '');
      setValue('telefone', fornecedor.telefone || '');
      setValue('celular', fornecedor.celular || '');
      setValue('website', fornecedor.website || '');
      setValue('contatoNome', fornecedor.contatoNome || '');
      setValue('prazoEntrega', fornecedor.prazoEntrega || '');
      setValue('condicoesPagamento', fornecedor.condicoesPagamento || '');
      setValue('descontoPadrao', (fornecedor.descontoPadrao != null ? String(fornecedor.descontoPadrao) : '') as any);
      setValue('observacoes', fornecedor.observacoes || '');

      if (fornecedor.endereco) {
        setValue('logradouro', fornecedor.endereco.logradouro || '');
        setValue('numero', fornecedor.endereco.numero || '');
        setValue('complemento', fornecedor.endereco.complemento || '');
        setValue('bairro', fornecedor.endereco.bairro || '');
        setValue('cidade', fornecedor.endereco.cidade || '');
        setValue('estado', fornecedor.endereco.estado || '');
        setValue('cep', fornecedor.endereco.cep || '');
      }
    }
  }, [fornecedor, isEditMode, setValue]);

  const onSubmit = async (data: CreateFornecedorFormData) => {
    if (isEditMode) {
      await updateMutation.mutateAsync({
        id: id!,
        data: data as unknown as UpdateFornecedorRequest,
      });
    } else {
      await createMutation.mutateAsync(data as unknown as CreateFornecedorRequest);
    }
  };

  const handleFormSubmit = async () => {
    const isValid = await trigger();

    if (!isValid) {
      throw new Error('Verifique os campos destacados em vermelho');
    }

    await handleSubmit(onSubmit as any)();
  };

  if (loadingFornecedor) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Carregando...</div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-4 sm:mb-6 flex items-center gap-3 sm:gap-4">
        <button
          onClick={() => navigate('/fornecedores')}
          className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
        >
          <ArrowLeft className="h-5 w-5 text-gray-900 dark:text-gray-100" />
        </button>
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100">
            {isEditMode ? 'Editar Fornecedor' : 'Novo Fornecedor'}
          </h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            {isEditMode
              ? 'Atualize os dados do fornecedor'
              : 'Preencha os dados para cadastrar um novo fornecedor'}
          </p>
        </div>
      </div>

      {/* Form */}
      <form className="mx-auto max-w-4xl">
        <div className="space-y-4 sm:space-y-6">
          {/* Dados Básicos */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Dados Básicos</h2>

            <div className="grid gap-4 grid-cols-1 sm:grid-cols-2">
              {/* Tipo */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Tipo <span className="text-red-500 dark:text-red-400">*</span>
                </label>
                <select
                  {...register('tipo')}
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                >
                  {Object.entries(TipoFornecedorLabel).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
                {errors.tipo && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.tipo.message}</p>
                )}
              </div>

              {/* Nome Fantasia */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Nome Fantasia <span className="text-red-500 dark:text-red-400">*</span>
                </label>
                <input
                  {...register('nomeFantasia')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Nome fantasia do fornecedor"
                />
                {errors.nomeFantasia && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.nomeFantasia.message}</p>
                )}
              </div>

              {/* Razão Social */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Razão Social
                </label>
                <input
                  {...register('razaoSocial')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Razão social"
                />
                {errors.razaoSocial && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.razaoSocial.message}</p>
                )}
              </div>

              {/* CPF/CNPJ */}
              <Controller
                name="cpfCnpj"
                control={control}
                render={({ field }) => (
                  <InputMask
                    {...field}
                    mask="cpfCnpj"
                    label="CPF/CNPJ"
                    placeholder="000.000.000-00"
                    error={errors.cpfCnpj?.message}
                  />
                )}
              />

              {/* Inscrição Estadual */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Inscrição Estadual
                </label>
                <input
                  {...register('inscricaoEstadual')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Inscrição estadual"
                />
                {errors.inscricaoEstadual && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.inscricaoEstadual.message}</p>
                )}
              </div>
            </div>
          </div>

          {/* Contato */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Contato</h2>

            <div className="grid gap-4 grid-cols-1 sm:grid-cols-2">
              {/* Email */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Email</label>
                <input
                  {...register('email')}
                  type="email"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="email@fornecedor.com"
                />
                {errors.email && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.email.message}</p>
                )}
              </div>

              {/* Website */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Website</label>
                <input
                  {...register('website')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="www.fornecedor.com.br"
                />
                {errors.website && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.website.message}</p>
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
                    placeholder="(00) 90000-0000"
                    error={errors.celular?.message}
                  />
                )}
              />

              {/* Nome do Contato */}
              <div className="sm:col-span-2">
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Nome do Contato
                </label>
                <input
                  {...register('contatoNome')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Nome da pessoa de contato"
                />
                {errors.contatoNome && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.contatoNome.message}</p>
                )}
              </div>
            </div>
          </div>

          {/* Endereço */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Endereço</h2>

            <div className="grid gap-4 grid-cols-1 sm:grid-cols-2 md:grid-cols-4">
              {/* CEP */}
              <div className="sm:col-span-1 md:col-span-2">
                <Controller
                  name="cep"
                  control={control}
                  render={({ field }) => (
                    <CepInput
                      {...field}
                      label="CEP"
                      error={errors.cep?.message}
                      onAddressFound={(endereco) => {
                        setValue('logradouro', endereco.logradouro);
                        setValue('bairro', endereco.bairro);
                        setValue('cidade', endereco.cidade);
                        setValue('estado', endereco.estado);
                      }}
                    />
                  )}
                />
              </div>

              {/* Logradouro */}
              <div className="sm:col-span-2 md:col-span-3">
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Logradouro
                </label>
                <input
                  {...register('logradouro')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Rua, Avenida, etc."
                />
                {errors.logradouro && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.logradouro.message}</p>
                )}
              </div>

              {/* Número */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Número</label>
                <input
                  {...register('numero')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="123"
                />
                {errors.numero && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.numero.message}</p>
                )}
              </div>

              {/* Complemento */}
              <div className="sm:col-span-2 md:col-span-2">
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Complemento
                </label>
                <input
                  {...register('complemento')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Sala, Galpão, etc."
                />
                {errors.complemento && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.complemento.message}</p>
                )}
              </div>

              {/* Bairro */}
              <div className="sm:col-span-1 md:col-span-2">
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Bairro</label>
                <input
                  {...register('bairro')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Centro, Industrial, etc."
                />
                {errors.bairro && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.bairro.message}</p>
                )}
              </div>

              {/* Cidade */}
              <div className="sm:col-span-1 md:col-span-2">
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Cidade</label>
                <input
                  {...register('cidade')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="São Paulo"
                />
                {errors.cidade && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.cidade.message}</p>
                )}
              </div>

              {/* Estado */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Estado</label>
                <select
                  {...register('estado')}
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                >
                  <option value="">Selecione</option>
                  {UFS.map((uf) => (
                    <option key={uf} value={uf}>
                      {uf}
                    </option>
                  ))}
                </select>
                {errors.estado && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.estado.message}</p>
                )}
              </div>
            </div>
          </div>

          {/* Dados Comerciais */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Dados Comerciais</h2>

            <div className="grid gap-4 grid-cols-1 sm:grid-cols-3">
              {/* Prazo de Entrega */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Prazo de Entrega
                </label>
                <input
                  {...register('prazoEntrega')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Ex: 3 dias úteis"
                />
                {errors.prazoEntrega && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.prazoEntrega.message}</p>
                )}
              </div>

              {/* Condições de Pagamento */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Condições de Pagamento
                </label>
                <input
                  {...register('condicoesPagamento')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Ex: 30/60/90 dias"
                />
                {errors.condicoesPagamento && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.condicoesPagamento.message}</p>
                )}
              </div>

              {/* Desconto Padrão */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Desconto Padrão (%)
                </label>
                <input
                  {...register('descontoPadrao')}
                  type="number"
                  step="0.01"
                  min="0"
                  max="100"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="0.00"
                />
                {errors.descontoPadrao && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.descontoPadrao.message}</p>
                )}
              </div>
            </div>
          </div>

          {/* Observações */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Observações</h2>

            <div>
              <textarea
                {...register('observacoes')}
                rows={4}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                placeholder="Observações sobre o fornecedor..."
              />
              {errors.observacoes && (
                <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.observacoes.message}</p>
              )}
            </div>
          </div>

          {/* Actions */}
          <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-3 sm:gap-4">
            <button
              type="button"
              onClick={() => navigate('/fornecedores')}
              className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-6 py-2 font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 w-full sm:w-auto"
            >
              Cancelar
            </button>
            <ButtonShine
              onClick={handleFormSubmit}
              loadingText={isEditMode ? 'Atualizando...' : 'Cadastrando...'}
              successMessage={isEditMode ? 'Fornecedor atualizado com sucesso!' : 'Fornecedor cadastrado com sucesso!'}
              errorMessage="Erro ao salvar. Verifique os dados e tente novamente."
              onSuccess={() => navigate('/fornecedores')}
              color="blue"
              size="md"
            >
              <Save className="h-5 w-5" />
              {isEditMode ? 'Atualizar' : 'Cadastrar'}
            </ButtonShine>
          </div>
        </div>
      </form>
    </div>
  );
};
