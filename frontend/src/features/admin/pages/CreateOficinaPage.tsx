/**
 * Create Oficina Page - Wizard for creating new workshops
 */

import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { CepInput } from '@/shared/components/forms/CepInput';
import {
  ArrowLeft,
  ArrowRight,
  Building2,
  MapPin,
  CreditCard,
  UserPlus,
  Check,
  Loader2,
  RefreshCw,
  AlertCircle,
} from 'lucide-react';
import { useCreateOficina, usePlanosActive } from '../hooks/useSaas';
import { showSuccess, showError } from '@/shared/utils/notifications';
import { formatCurrency } from '@/shared/utils/formatters';

// Validation schemas for each step
const dadosBasicosSchema = z.object({
  razaoSocial: z.string().min(3, 'Razão social deve ter pelo menos 3 caracteres'),
  nomeFantasia: z.string().min(3, 'Nome fantasia deve ter pelo menos 3 caracteres'),
  cnpj: z.string().regex(/^\d{14}$/, 'CNPJ deve ter 14 dígitos'),
  email: z.string().email('Email inválido'),
  telefone: z.string().regex(/^\d{10,11}$/, 'Telefone deve ter 10 ou 11 dígitos'),
});

const enderecoSchema = z.object({
  cep: z.string().regex(/^\d{5}-?\d{3}$/, 'CEP inválido'),
  logradouro: z.string().min(3, 'Logradouro é obrigatório'),
  numero: z.string().min(1, 'Número é obrigatório'),
  complemento: z.string().optional(),
  bairro: z.string().min(2, 'Bairro é obrigatório'),
  cidade: z.string().min(2, 'Cidade é obrigatória'),
  estado: z.string().regex(/^[A-Z]{2}$/, 'Estado deve ser UF válida'),
});

const planoSchema = z.object({
  plano: z.string().min(1, 'Selecione um plano'),
});

const adminSchema = z.object({
  nomeAdmin: z.string().min(3, 'Nome deve ter pelo menos 3 caracteres'),
  emailAdmin: z.string().email('Email inválido'),
  senhaAdmin: z.string().min(8, 'Senha deve ter pelo menos 8 caracteres'),
  confirmarSenha: z.string(),
}).refine(data => data.senhaAdmin === data.confirmarSenha, {
  message: 'Senhas não conferem',
  path: ['confirmarSenha'],
});

const fullSchema = dadosBasicosSchema.merge(enderecoSchema).merge(planoSchema).merge(adminSchema);
type FullFormData = z.infer<typeof fullSchema>;

const steps = [
  { id: 1, title: 'Dados Básicos', icon: Building2 },
  { id: 2, title: 'Endereço', icon: MapPin },
  { id: 3, title: 'Plano', icon: CreditCard },
  { id: 4, title: 'Administrador', icon: UserPlus },
];

export const CreateOficinaPage = () => {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(1);
  const createMutation = useCreateOficina();

  // Fetch active plans from API
  const { data: planos, isLoading: isLoadingPlanos, error: planosError } = usePlanosActive();

  const {
    register,
    handleSubmit,
    watch,
    trigger,
    setValue,
    control,
    formState: { errors },
  } = useForm<FullFormData>({
    resolver: zodResolver(fullSchema),
    defaultValues: {
      razaoSocial: '',
      nomeFantasia: '',
      cnpj: '',
      email: '',
      telefone: '',
      cep: '',
      logradouro: '',
      numero: '',
      complemento: '',
      bairro: '',
      cidade: '',
      estado: '',
      plano: '',
      nomeAdmin: '',
      emailAdmin: '',
      senhaAdmin: '',
      confirmarSenha: '',
    },
  });

  const selectedPlano = watch('plano');

  // Set default plan when plans are loaded (prefer recommended plan)
  useEffect(() => {
    if (planos && planos.length > 0 && !selectedPlano) {
      const recommendedPlan = planos.find(p => p.recomendado);
      setValue('plano', recommendedPlan?.codigo || planos[0].codigo);
    }
  }, [planos, selectedPlano, setValue]);

  const validateCurrentStep = async (): Promise<boolean> => {
    let fields: (keyof FullFormData)[] = [];

    switch (currentStep) {
      case 1:
        fields = ['razaoSocial', 'nomeFantasia', 'cnpj', 'email', 'telefone'];
        break;
      case 2:
        fields = ['cep', 'logradouro', 'numero', 'bairro', 'cidade', 'estado'];
        break;
      case 3:
        fields = ['plano'];
        break;
      case 4:
        fields = ['nomeAdmin', 'emailAdmin', 'senhaAdmin', 'confirmarSenha'];
        break;
    }

    return await trigger(fields);
  };

  const nextStep = async () => {
    const isValid = await validateCurrentStep();
    if (isValid && currentStep < 4) {
      setCurrentStep(currentStep + 1);
    }
  };

  const prevStep = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
    }
  };

  const onSubmit = async (data: FullFormData) => {
    try {
      // Remove confirmarSenha before sending
      const { confirmarSenha, ...submitData } = data;
      await createMutation.mutateAsync(submitData);
      showSuccess('Oficina criada com sucesso!');
      navigate('/admin/oficinas');
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao criar oficina');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6 dark:bg-gray-900">
      {/* Header */}
      <div className="mb-8">
        <Link
          to="/admin/oficinas"
          className="mb-4 inline-flex items-center gap-2 text-sm text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-white"
        >
          <ArrowLeft className="h-4 w-4" />
          Voltar para lista
        </Link>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          Nova Oficina
        </h1>
        <p className="mt-1 text-gray-600 dark:text-gray-400">
          Cadastre uma nova oficina no sistema
        </p>
      </div>

      {/* Stepper */}
      <div className="mb-8">
        <div className="flex items-center justify-between">
          {steps.map((step, index) => (
            <div key={step.id} className="flex items-center">
              <div className="flex flex-col items-center">
                <div
                  className={`flex h-10 w-10 items-center justify-center rounded-full ${
                    currentStep > step.id
                      ? 'bg-green-500 text-white'
                      : currentStep === step.id
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-200 text-gray-500 dark:bg-gray-700 dark:text-gray-400'
                  }`}
                >
                  {currentStep > step.id ? (
                    <Check className="h-5 w-5" />
                  ) : (
                    <step.icon className="h-5 w-5" />
                  )}
                </div>
                <span
                  className={`mt-2 text-xs font-medium ${
                    currentStep >= step.id
                      ? 'text-gray-900 dark:text-white'
                      : 'text-gray-500 dark:text-gray-400'
                  }`}
                >
                  {step.title}
                </span>
              </div>
              {index < steps.length - 1 && (
                <div
                  className={`mx-4 h-1 w-24 ${
                    currentStep > step.id
                      ? 'bg-green-500'
                      : 'bg-gray-200 dark:bg-gray-700'
                  }`}
                />
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)}>
        <div className="mx-auto max-w-2xl rounded-lg bg-white p-6 shadow dark:bg-gray-800">
          {/* Step 1: Dados Básicos */}
          {currentStep === 1 && (
            <div className="space-y-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Dados Básicos da Oficina
              </h2>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Razão Social *
                </label>
                <input
                  {...register('razaoSocial')}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  placeholder="Razão Social Ltda"
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
                  placeholder="Nome da Oficina"
                />
                {errors.nomeFantasia && (
                  <p className="mt-1 text-sm text-red-500">{errors.nomeFantasia.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  CNPJ * (apenas números)
                </label>
                <input
                  {...register('cnpj')}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  placeholder="12345678000199"
                  maxLength={14}
                />
                {errors.cnpj && (
                  <p className="mt-1 text-sm text-red-500">{errors.cnpj.message}</p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Email *
                  </label>
                  <input
                    {...register('email')}
                    type="email"
                    className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    placeholder="contato@oficina.com"
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
                    placeholder="11987654321"
                    maxLength={11}
                  />
                  {errors.telefone && (
                    <p className="mt-1 text-sm text-red-500">{errors.telefone.message}</p>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Step 2: Endereço */}
          {currentStep === 2 && (
            <div className="space-y-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Endereço
              </h2>

              <div className="grid grid-cols-3 gap-4">
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
                          setValue('logradouro', endereco.logradouro);
                          setValue('bairro', endereco.bairro);
                          setValue('cidade', endereco.cidade);
                          setValue('estado', endereco.estado);
                        }}
                      />
                    )}
                  />
                </div>

                <div className="col-span-2">
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Logradouro *
                  </label>
                  <input
                    {...register('logradouro')}
                    className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    placeholder="Rua das Flores"
                  />
                  {errors.logradouro && (
                    <p className="mt-1 text-sm text-red-500">{errors.logradouro.message}</p>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Número *
                  </label>
                  <input
                    {...register('numero')}
                    className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    placeholder="123"
                  />
                  {errors.numero && (
                    <p className="mt-1 text-sm text-red-500">{errors.numero.message}</p>
                  )}
                </div>

                <div className="col-span-2">
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Complemento
                  </label>
                  <input
                    {...register('complemento')}
                    className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    placeholder="Sala 1"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Bairro *
                  </label>
                  <input
                    {...register('bairro')}
                    className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    placeholder="Centro"
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
                    placeholder="São Paulo"
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
                    placeholder="SP"
                    maxLength={2}
                  />
                  {errors.estado && (
                    <p className="mt-1 text-sm text-red-500">{errors.estado.message}</p>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Step 3: Plano */}
          {currentStep === 3 && (
            <div className="space-y-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Escolha o Plano
              </h2>

              {isLoadingPlanos ? (
                <div className="flex h-48 items-center justify-center">
                  <RefreshCw className="h-8 w-8 animate-spin text-gray-400" />
                </div>
              ) : planosError ? (
                <div className="flex flex-col items-center justify-center rounded-lg border border-red-200 bg-red-50 p-6 dark:border-red-800 dark:bg-red-900/20">
                  <AlertCircle className="h-8 w-8 text-red-500 mb-2" />
                  <p className="text-red-800 dark:text-red-300">
                    Erro ao carregar planos. Tente novamente.
                  </p>
                </div>
              ) : !planos || planos.length === 0 ? (
                <div className="flex flex-col items-center justify-center rounded-lg border border-amber-200 bg-amber-50 p-6 dark:border-amber-800 dark:bg-amber-900/20">
                  <AlertCircle className="h-8 w-8 text-amber-500 mb-2" />
                  <p className="text-amber-800 dark:text-amber-300">
                    Nenhum plano ativo encontrado. Configure os planos em Admin {">"} Planos.
                  </p>
                  <Link
                    to="/admin/planos"
                    className="mt-3 text-blue-600 hover:text-blue-700 dark:text-blue-400"
                  >
                    Ir para Planos
                  </Link>
                </div>
              ) : (
                <div className="grid gap-4">
                  {planos.map((plano) => (
                    <label
                      key={plano.id}
                      className={`relative flex cursor-pointer rounded-lg border-2 p-4 ${
                        selectedPlano === plano.codigo
                          ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                          : 'border-gray-200 hover:border-gray-300 dark:border-gray-700'
                      }`}
                      style={
                        plano.corDestaque && selectedPlano === plano.codigo
                          ? { borderColor: plano.corDestaque }
                          : undefined
                      }
                    >
                      <input
                        type="radio"
                        {...register('plano')}
                        value={plano.codigo}
                        className="sr-only"
                      />
                      <div className="flex flex-1 items-center justify-between">
                        <div>
                          <div className="flex items-center gap-2">
                            <p className="text-lg font-semibold text-gray-900 dark:text-white">
                              {plano.nome}
                            </p>
                            {plano.recomendado && (
                              <span className="rounded-full bg-purple-100 px-2 py-0.5 text-xs font-medium text-purple-700 dark:bg-purple-900/30 dark:text-purple-400">
                                Recomendado
                              </span>
                            )}
                            {plano.tagPromocao && (
                              <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700 dark:bg-amber-900/30 dark:text-amber-400">
                                {plano.tagPromocao}
                              </span>
                            )}
                          </div>
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            {plano.descricao || `${plano.limiteUsuarios === -1 ? 'Ilimitado' : plano.limiteUsuarios} usuário(s) • ${plano.limiteOsMes === -1 ? 'Ilimitado' : plano.limiteOsMes} OS/mês`}
                          </p>
                        </div>
                        <div className="text-right">
                          {plano.precoSobConsulta ? (
                            <p className="text-xl font-bold text-gray-900 dark:text-white">
                              Sob consulta
                            </p>
                          ) : (
                            <>
                              <p className="text-2xl font-bold text-gray-900 dark:text-white">
                                {formatCurrency(plano.valorMensal)}
                              </p>
                              <p className="text-sm text-gray-500 dark:text-gray-400">/mês</p>
                            </>
                          )}
                        </div>
                      </div>
                      {selectedPlano === plano.codigo && (
                        <div className="absolute right-4 top-4">
                          <Check className="h-5 w-5 text-blue-500" />
                        </div>
                      )}
                    </label>
                  ))}
                </div>
              )}

              {planos && planos.length > 0 && (
                <div className="rounded-lg border border-blue-200 bg-blue-50 p-4 dark:border-blue-800 dark:bg-blue-900/20">
                  <p className="text-sm text-blue-800 dark:text-blue-300">
                    A oficina terá <strong>{planos.find(p => p.codigo === selectedPlano)?.trialDias || 14} dias de trial</strong> gratuito.
                    Após esse período, será cobrado o valor do plano selecionado.
                  </p>
                </div>
              )}
            </div>
          )}

          {/* Step 4: Admin */}
          {currentStep === 4 && (
            <div className="space-y-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Dados do Administrador
              </h2>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Este será o usuário principal da oficina com acesso total ao sistema.
              </p>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Nome Completo *
                </label>
                <input
                  {...register('nomeAdmin')}
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  placeholder="João da Silva"
                />
                {errors.nomeAdmin && (
                  <p className="mt-1 text-sm text-red-500">{errors.nomeAdmin.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Email de Acesso *
                </label>
                <input
                  {...register('emailAdmin')}
                  type="email"
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  placeholder="admin@oficina.com"
                />
                {errors.emailAdmin && (
                  <p className="mt-1 text-sm text-red-500">{errors.emailAdmin.message}</p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Senha *
                  </label>
                  <input
                    {...register('senhaAdmin')}
                    type="password"
                    className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    placeholder="Mínimo 8 caracteres"
                  />
                  {errors.senhaAdmin && (
                    <p className="mt-1 text-sm text-red-500">{errors.senhaAdmin.message}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Confirmar Senha *
                  </label>
                  <input
                    {...register('confirmarSenha')}
                    type="password"
                    className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                    placeholder="Confirme a senha"
                  />
                  {errors.confirmarSenha && (
                    <p className="mt-1 text-sm text-red-500">{errors.confirmarSenha.message}</p>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Navigation Buttons */}
          <div className="mt-8 flex justify-between">
            <button
              type="button"
              onClick={prevStep}
              disabled={currentStep === 1}
              className="flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              <ArrowLeft className="h-4 w-4" />
              Anterior
            </button>

            {currentStep < 4 ? (
              <button
                type="button"
                onClick={nextStep}
                className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
              >
                Próximo
                <ArrowRight className="h-4 w-4" />
              </button>
            ) : (
              <button
                type="submit"
                disabled={createMutation.isPending}
                className="flex items-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-white hover:bg-green-700 disabled:opacity-50"
              >
                {createMutation.isPending ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Criando...
                  </>
                ) : (
                  <>
                    <Check className="h-4 w-4" />
                    Criar Oficina
                  </>
                )}
              </button>
            )}
          </div>
        </div>
      </form>
    </div>
  );
};
