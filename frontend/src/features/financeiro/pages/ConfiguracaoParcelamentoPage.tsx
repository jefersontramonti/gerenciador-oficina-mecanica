import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  ArrowLeft,
  Save,
  Plus,
  Trash2,
  Edit2,
  CreditCard,
  Percent,
  AlertCircle,
  Check,
  X,
} from 'lucide-react';
import {
  useConfiguracaoParcelamento,
  useSalvarConfiguracaoParcelamento,
  useCriarFaixaJuros,
  useAtualizarFaixaJuros,
  useRemoverFaixaJuros,
} from '../hooks/useParcelamento';
import type { TabelaJuros, TabelaJurosRequest, TipoJuros } from '../types/parcelamento';
import { TIPO_JUROS_LABELS } from '../types/parcelamento';
import { FeatureGate } from '@/shared/components/FeatureGate';

// Validation schemas
const configuracaoSchema = z.object({
  parcelasMaximas: z.number().min(1).max(24),
  valorMinimoParcela: z.number().min(0),
  valorMinimoParcelamento: z.number().min(0),
  aceitaVisa: z.boolean(),
  aceitaMastercard: z.boolean(),
  aceitaElo: z.boolean(),
  aceitaAmex: z.boolean(),
  aceitaHipercard: z.boolean(),
  exibirValorTotal: z.boolean(),
  exibirJuros: z.boolean(),
  ativo: z.boolean(),
});

const faixaJurosSchema = z.object({
  parcelasMinimo: z.number().min(1).max(24),
  parcelasMaximo: z.number().min(1).max(24),
  percentualJuros: z.number().min(0).max(100),
  tipoJuros: z.enum(['SEM_JUROS', 'JUROS_SIMPLES', 'JUROS_COMPOSTO']),
  repassarCliente: z.boolean(),
  ativo: z.boolean(),
});

type ConfiguracaoFormData = z.infer<typeof configuracaoSchema>;
type FaixaJurosFormData = z.infer<typeof faixaJurosSchema>;

export function ConfiguracaoParcelamentoPage() {
  const navigate = useNavigate();
  const [editingFaixa, setEditingFaixa] = useState<TabelaJuros | null>(null);
  const [showFaixaForm, setShowFaixaForm] = useState(false);

  const { data: configuracao, isLoading } = useConfiguracaoParcelamento();
  const salvarConfiguracao = useSalvarConfiguracaoParcelamento();
  const criarFaixa = useCriarFaixaJuros();
  const atualizarFaixa = useAtualizarFaixaJuros();
  const removerFaixa = useRemoverFaixaJuros();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ConfiguracaoFormData>({
    resolver: zodResolver(configuracaoSchema),
    values: configuracao
      ? {
          parcelasMaximas: configuracao.parcelasMaximas,
          valorMinimoParcela: configuracao.valorMinimoParcela,
          valorMinimoParcelamento: configuracao.valorMinimoParcelamento,
          aceitaVisa: configuracao.aceitaVisa,
          aceitaMastercard: configuracao.aceitaMastercard,
          aceitaElo: configuracao.aceitaElo,
          aceitaAmex: configuracao.aceitaAmex,
          aceitaHipercard: configuracao.aceitaHipercard,
          exibirValorTotal: configuracao.exibirValorTotal,
          exibirJuros: configuracao.exibirJuros,
          ativo: configuracao.ativo,
        }
      : undefined,
  });

  const {
    register: registerFaixa,
    handleSubmit: handleSubmitFaixa,
    reset: resetFaixa,
  } = useForm<FaixaJurosFormData>({
    resolver: zodResolver(faixaJurosSchema),
    defaultValues: {
      parcelasMinimo: 2,
      parcelasMaximo: 6,
      percentualJuros: 0,
      tipoJuros: 'SEM_JUROS',
      repassarCliente: true,
      ativo: true,
    },
  });

  const onSubmitConfiguracao = async (data: ConfiguracaoFormData) => {
    try {
      await salvarConfiguracao.mutateAsync(data);
      alert('Configuração salva com sucesso!');
    } catch (error) {
      alert('Erro ao salvar configuração');
    }
  };

  const onSubmitFaixa = async (data: FaixaJurosFormData) => {
    try {
      const request: TabelaJurosRequest = {
        ...data,
        tipoJuros: data.tipoJuros as TipoJuros,
      };

      if (editingFaixa) {
        await atualizarFaixa.mutateAsync({ id: editingFaixa.id, data: request });
      } else {
        await criarFaixa.mutateAsync(request);
      }

      setShowFaixaForm(false);
      setEditingFaixa(null);
      resetFaixa();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao salvar faixa de juros');
    }
  };

  const handleEditFaixa = (faixa: TabelaJuros) => {
    setEditingFaixa(faixa);
    resetFaixa({
      parcelasMinimo: faixa.parcelasMinimo,
      parcelasMaximo: faixa.parcelasMaximo,
      percentualJuros: faixa.percentualJuros,
      tipoJuros: faixa.tipoJuros,
      repassarCliente: faixa.repassarCliente,
      ativo: faixa.ativo,
    });
    setShowFaixaForm(true);
  };

  const handleDeleteFaixa = async (id: string) => {
    if (confirm('Tem certeza que deseja remover esta faixa de juros?')) {
      try {
        await removerFaixa.mutateAsync(id);
      } catch (error) {
        alert('Erro ao remover faixa');
      }
    }
  };

  const handleCancelFaixa = () => {
    setShowFaixaForm(false);
    setEditingFaixa(null);
    resetFaixa();
  };

  return (
    <FeatureGate feature="PARCELAMENTO_CARTAO">
      <div className="p-4 sm:p-6 space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate(-1)}
              className="rounded-lg p-2 text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-700"
            >
              <ArrowLeft className="h-5 w-5" />
            </button>
            <div>
              <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
                Configuração de Parcelamento
              </h1>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Configure as regras de parcelamento para sua oficina
              </p>
            </div>
          </div>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-12">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 border-t-transparent" />
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Configurações Gerais */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 p-4 sm:p-6">
              <div className="flex items-center gap-3 mb-6">
                <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                  <CreditCard className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                </div>
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                  Configurações Gerais
                </h2>
              </div>

              <form onSubmit={handleSubmit(onSubmitConfiguracao)} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Parcelas Máximas
                    </label>
                    <input
                      type="number"
                      {...register('parcelasMaximas', { valueAsNumber: true })}
                      className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-3 py-2 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                      min={1}
                      max={24}
                    />
                    {errors.parcelasMaximas && (
                      <p className="mt-1 text-sm text-red-500">{errors.parcelasMaximas.message}</p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Valor Mínimo da Parcela
                    </label>
                    <div className="relative">
                      <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500">R$</span>
                      <input
                        type="number"
                        step="0.01"
                        {...register('valorMinimoParcela', { valueAsNumber: true })}
                        className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 pl-10 pr-3 py-2 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                  </div>

                  <div className="sm:col-span-2">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Valor Mínimo para Parcelar
                    </label>
                    <div className="relative">
                      <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500">R$</span>
                      <input
                        type="number"
                        step="0.01"
                        {...register('valorMinimoParcelamento', { valueAsNumber: true })}
                        className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 pl-10 pr-3 py-2 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                      />
                    </div>
                  </div>
                </div>

                <div>
                  <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                    Bandeiras Aceitas
                  </h3>
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                    {[
                      { name: 'aceitaVisa', label: 'Visa' },
                      { name: 'aceitaMastercard', label: 'Mastercard' },
                      { name: 'aceitaElo', label: 'Elo' },
                      { name: 'aceitaAmex', label: 'Amex' },
                      { name: 'aceitaHipercard', label: 'Hipercard' },
                    ].map((bandeira) => (
                      <label
                        key={bandeira.name}
                        className="flex items-center gap-2 p-2 rounded-lg border border-gray-200 dark:border-gray-600 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700"
                      >
                        <input
                          type="checkbox"
                          {...register(bandeira.name as any)}
                          className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                        />
                        <span className="text-sm text-gray-900 dark:text-white">{bandeira.label}</span>
                      </label>
                    ))}
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      {...register('exibirValorTotal')}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="text-sm text-gray-700 dark:text-gray-300">
                      Exibir valor total com juros
                    </span>
                  </label>
                  <label className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      {...register('exibirJuros')}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="text-sm text-gray-700 dark:text-gray-300">
                      Exibir informações de juros ao cliente
                    </span>
                  </label>
                  <label className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      {...register('ativo')}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="text-sm text-gray-700 dark:text-gray-300">
                      Parcelamento ativo
                    </span>
                  </label>
                </div>

                <button
                  type="submit"
                  disabled={salvarConfiguracao.isPending}
                  className="w-full flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  <Save className="h-4 w-4" />
                  {salvarConfiguracao.isPending ? 'Salvando...' : 'Salvar Configurações'}
                </button>
              </form>
            </div>

            {/* Faixas de Juros */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 p-4 sm:p-6">
              <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
                    <Percent className="h-5 w-5 text-purple-600 dark:text-purple-400" />
                  </div>
                  <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                    Faixas de Juros
                  </h2>
                </div>
                {!showFaixaForm && (
                  <button
                    onClick={() => setShowFaixaForm(true)}
                    className="flex items-center gap-2 rounded-lg bg-purple-600 px-3 py-2 text-sm text-white hover:bg-purple-700"
                  >
                    <Plus className="h-4 w-4" />
                    Nova Faixa
                  </button>
                )}
              </div>

              {/* Form de Faixa */}
              {showFaixaForm && (
                <form
                  onSubmit={handleSubmitFaixa(onSubmitFaixa)}
                  className="mb-6 p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg space-y-4"
                >
                  <h3 className="font-medium text-gray-900 dark:text-white">
                    {editingFaixa ? 'Editar Faixa' : 'Nova Faixa de Juros'}
                  </h3>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        De (parcelas)
                      </label>
                      <input
                        type="number"
                        {...registerFaixa('parcelasMinimo', { valueAsNumber: true })}
                        className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-3 py-2 text-gray-900 dark:text-white"
                        min={1}
                        max={24}
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        Até (parcelas)
                      </label>
                      <input
                        type="number"
                        {...registerFaixa('parcelasMaximo', { valueAsNumber: true })}
                        className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-3 py-2 text-gray-900 dark:text-white"
                        min={1}
                        max={24}
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        Tipo de Juros
                      </label>
                      <select
                        {...registerFaixa('tipoJuros')}
                        className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-3 py-2 text-gray-900 dark:text-white"
                      >
                        {Object.entries(TIPO_JUROS_LABELS).map(([value, label]) => (
                          <option key={value} value={value}>
                            {label}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        % Juros (a.m.)
                      </label>
                      <input
                        type="number"
                        step="0.01"
                        {...registerFaixa('percentualJuros', { valueAsNumber: true })}
                        className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-3 py-2 text-gray-900 dark:text-white"
                      />
                    </div>
                  </div>

                  <div className="flex items-center gap-4">
                    <label className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        {...registerFaixa('repassarCliente')}
                        className="rounded border-gray-300 text-purple-600 focus:ring-purple-500"
                      />
                      <span className="text-sm text-gray-700 dark:text-gray-300">
                        Repassar juros ao cliente
                      </span>
                    </label>
                    <label className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        {...registerFaixa('ativo')}
                        className="rounded border-gray-300 text-purple-600 focus:ring-purple-500"
                      />
                      <span className="text-sm text-gray-700 dark:text-gray-300">Ativo</span>
                    </label>
                  </div>

                  <div className="flex gap-2">
                    <button
                      type="submit"
                      disabled={criarFaixa.isPending || atualizarFaixa.isPending}
                      className="flex items-center gap-2 rounded-lg bg-purple-600 px-4 py-2 text-sm text-white hover:bg-purple-700 disabled:opacity-50"
                    >
                      <Check className="h-4 w-4" />
                      {editingFaixa ? 'Salvar' : 'Criar'}
                    </button>
                    <button
                      type="button"
                      onClick={handleCancelFaixa}
                      className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                    >
                      <X className="h-4 w-4" />
                      Cancelar
                    </button>
                  </div>
                </form>
              )}

              {/* Lista de Faixas */}
              <div className="space-y-3">
                {configuracao?.faixasJuros && configuracao.faixasJuros.length > 0 ? (
                  configuracao.faixasJuros.map((faixa) => (
                    <div
                      key={faixa.id}
                      className={`p-3 rounded-lg border ${
                        faixa.ativo
                          ? 'border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800'
                          : 'border-gray-200 dark:border-gray-700 bg-gray-100 dark:bg-gray-900 opacity-60'
                      }`}
                    >
                      {/* Mobile Layout */}
                      <div className="sm:hidden space-y-2">
                        <div className="flex items-start justify-between gap-2">
                          <div className="min-w-0 flex-1">
                            <p className="text-sm font-medium text-gray-900 dark:text-white">
                              {faixa.descricaoFaixa}
                            </p>
                            <p className="text-xs text-gray-600 dark:text-gray-400 mt-1">
                              {faixa.tipoJurosDescricao}
                              {!faixa.repassarCliente && ' (oficina absorve)'}
                            </p>
                          </div>
                          <div className="flex items-center gap-1 flex-shrink-0">
                            <button
                              onClick={() => handleEditFaixa(faixa)}
                              className="p-1.5 text-gray-600 hover:text-blue-600 dark:text-gray-400 dark:hover:text-blue-400 rounded"
                            >
                              <Edit2 className="h-4 w-4" />
                            </button>
                            <button
                              onClick={() => handleDeleteFaixa(faixa.id)}
                              className="p-1.5 text-gray-600 hover:text-red-600 dark:text-gray-400 dark:hover:text-red-400 rounded"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          </div>
                        </div>
                      </div>

                      {/* Desktop Layout */}
                      <div className="hidden sm:flex sm:items-center sm:justify-between">
                        <div>
                          <p className="font-medium text-gray-900 dark:text-white">
                            {faixa.descricaoFaixa}
                          </p>
                          <p className="text-sm text-gray-600 dark:text-gray-400">
                            {faixa.tipoJurosDescricao}
                            {!faixa.repassarCliente && ' (oficina absorve)'}
                          </p>
                        </div>
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => handleEditFaixa(faixa)}
                            className="p-2 text-gray-600 hover:text-blue-600 dark:text-gray-400 dark:hover:text-blue-400"
                          >
                            <Edit2 className="h-4 w-4" />
                          </button>
                          <button
                            onClick={() => handleDeleteFaixa(faixa.id)}
                            className="p-2 text-gray-600 hover:text-red-600 dark:text-gray-400 dark:hover:text-red-400"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="text-center py-8 text-gray-500 dark:text-gray-400">
                    <AlertCircle className="h-6 w-6 sm:h-8 sm:w-8 mx-auto mb-2 opacity-50" />
                    <p className="text-sm sm:text-base">Nenhuma faixa de juros configurada</p>
                    <p className="text-xs sm:text-sm">
                      Clique em "Nova Faixa" para adicionar
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </FeatureGate>
  );
}
