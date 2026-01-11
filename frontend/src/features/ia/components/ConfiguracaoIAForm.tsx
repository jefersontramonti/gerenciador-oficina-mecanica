/**
 * Formulário de Configuração de IA
 *
 * Permite configurar API key, modelos e opções de otimização.
 */

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Bot,
  Key,
  Settings2,
  BarChart3,
  AlertTriangle,
  Check,
  Loader2,
  Eye,
  EyeOff,
  Trash2,
  RefreshCw,
} from 'lucide-react';
import {
  useConfiguracaoIA,
  useAtualizarConfiguracaoIA,
  useAtualizarApiKey,
  useRemoverApiKey,
  useEstatisticasIA,
} from '../hooks/useIA';

// ===== Schemas de Validação =====

const apiKeySchema = z.object({
  apiKey: z.string().min(10, 'API Key deve ter pelo menos 10 caracteres'),
});

const configSchema = z.object({
  iaHabilitada: z.boolean(),
  usarPreValidacao: z.boolean(),
  usarCache: z.boolean(),
  usarRoteamentoInteligente: z.boolean(),
  maxRequisicoesDia: z.number().min(1).max(1000),
  maxTokensResposta: z.number().min(100).max(4000),
});

type ApiKeyFormData = z.infer<typeof apiKeySchema>;
type ConfigFormData = z.infer<typeof configSchema>;

export const ConfiguracaoIAForm = () => {
  const [showApiKey, setShowApiKey] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Hooks de dados
  const { data: config, isLoading: isLoadingConfig, error: configError } = useConfiguracaoIA();
  const { data: estatisticas, isLoading: isLoadingStats } = useEstatisticasIA();

  // Hooks de mutação
  const atualizarConfig = useAtualizarConfiguracaoIA();
  const atualizarApiKey = useAtualizarApiKey();
  const removerApiKey = useRemoverApiKey();

  // Formulários
  const apiKeyForm = useForm<ApiKeyFormData>({
    resolver: zodResolver(apiKeySchema),
    defaultValues: { apiKey: '' },
  });

  const configForm = useForm<ConfigFormData>({
    resolver: zodResolver(configSchema),
    values: config
      ? {
          iaHabilitada: config.iaHabilitada,
          usarPreValidacao: config.usarPreValidacao,
          usarCache: config.usarCache,
          usarRoteamentoInteligente: config.usarRoteamentoInteligente,
          maxRequisicoesDia: config.maxRequisicoesDia,
          maxTokensResposta: config.maxTokensResposta,
        }
      : undefined,
  });

  // Handlers
  const handleSaveApiKey = async (data: ApiKeyFormData) => {
    try {
      await atualizarApiKey.mutateAsync(data);
      apiKeyForm.reset({ apiKey: '' });
    } catch (error: unknown) {
      console.error('Erro ao salvar API key:', error);
    }
  };

  const handleRemoveApiKey = async () => {
    try {
      await removerApiKey.mutateAsync();
      setShowDeleteConfirm(false);
    } catch (error: unknown) {
      console.error('Erro ao remover API key:', error);
    }
  };

  const handleSaveConfig = async (data: ConfigFormData) => {
    try {
      await atualizarConfig.mutateAsync(data);
    } catch (error: unknown) {
      console.error('Erro ao salvar configuração:', error);
    }
  };

  // Loading state
  if (isLoadingConfig) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
        <span className="ml-2 text-gray-600 dark:text-gray-400">Carregando configuração...</span>
      </div>
    );
  }

  // Error state
  if (configError) {
    return (
      <div className="rounded-lg border border-yellow-300 bg-yellow-50 p-4 dark:border-yellow-800 dark:bg-yellow-900/20">
        <div className="flex items-center">
          <AlertTriangle className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
          <span className="ml-2 text-yellow-700 dark:text-yellow-300">
            Configure sua API key para começar a usar o diagnóstico por IA
          </span>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Status Card */}
      <div
        className={`rounded-lg border p-4 ${
          config?.apiKeyConfigurada && config?.iaHabilitada
            ? 'border-green-300 bg-green-50 dark:border-green-800 dark:bg-green-900/20'
            : 'border-yellow-300 bg-yellow-50 dark:border-yellow-800 dark:bg-yellow-900/20'
        }`}
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center">
            {config?.apiKeyConfigurada && config?.iaHabilitada ? (
              <>
                <Check className="h-5 w-5 text-green-600 dark:text-green-400" />
                <span className="ml-2 font-medium text-green-700 dark:text-green-300">
                  IA configurada e ativa
                </span>
              </>
            ) : (
              <>
                <AlertTriangle className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
                <span className="ml-2 font-medium text-yellow-700 dark:text-yellow-300">
                  {!config?.apiKeyConfigurada
                    ? 'API key não configurada'
                    : 'IA desabilitada'}
                </span>
              </>
            )}
          </div>
          {config?.apiKeyConfigurada && (
            <span className="text-sm text-gray-500 dark:text-gray-400">
              {config.requisicoesHoje} / {config.maxRequisicoesDia} requisições hoje
            </span>
          )}
        </div>
      </div>

      {/* API Key Section */}
      <div className="rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-800">
        <div className="mb-4 flex items-center">
          <Key className="h-5 w-5 text-gray-500" />
          <h3 className="ml-2 text-lg font-semibold text-gray-900 dark:text-white">
            API Key da Anthropic
          </h3>
        </div>

        <p className="mb-4 text-sm text-gray-600 dark:text-gray-400">
          A API key é necessária para utilizar o diagnóstico assistido por IA. Sua chave é
          armazenada de forma segura com criptografia AES-256-GCM.
        </p>

        {config?.apiKeyConfigurada ? (
          <div className="space-y-4">
            <div className="flex items-center justify-between rounded-lg bg-gray-100 px-4 py-3 dark:bg-gray-700">
              <span className="font-mono text-sm text-gray-600 dark:text-gray-300">
                ••••••••••••••••••••••••••••••••
              </span>
              <div className="flex items-center space-x-2">
                <span className="rounded-full bg-green-100 px-2 py-1 text-xs text-green-700 dark:bg-green-900/30 dark:text-green-400">
                  Configurada
                </span>
                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(true)}
                  className="p-1 text-red-500 hover:text-red-700"
                  title="Remover API key"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>

            {showDeleteConfirm && (
              <div className="rounded-lg border border-red-300 bg-red-50 p-4 dark:border-red-800 dark:bg-red-900/20">
                <p className="mb-3 text-sm text-red-700 dark:text-red-300">
                  Tem certeza que deseja remover a API key? O diagnóstico por IA será desabilitado.
                </p>
                <div className="flex space-x-2">
                  <button
                    type="button"
                    onClick={handleRemoveApiKey}
                    disabled={removerApiKey.isPending}
                    className="rounded-lg bg-red-600 px-3 py-1.5 text-sm text-white hover:bg-red-700 disabled:opacity-50"
                  >
                    {removerApiKey.isPending ? 'Removendo...' : 'Confirmar'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowDeleteConfirm(false)}
                    className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm hover:bg-gray-50 dark:border-gray-600 dark:hover:bg-gray-700"
                  >
                    Cancelar
                  </button>
                </div>
              </div>
            )}

            <form onSubmit={apiKeyForm.handleSubmit(handleSaveApiKey)} className="mt-4">
              <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Atualizar API Key
              </label>
              <div className="flex space-x-2">
                <div className="relative flex-1">
                  <input
                    {...apiKeyForm.register('apiKey')}
                    type={showApiKey ? 'text' : 'password'}
                    placeholder="sk-ant-api..."
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 pr-10 font-mono text-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  />
                  <button
                    type="button"
                    onClick={() => setShowApiKey(!showApiKey)}
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    {showApiKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                <button
                  type="submit"
                  disabled={atualizarApiKey.isPending}
                  className="rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  {atualizarApiKey.isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <RefreshCw className="h-4 w-4" />
                  )}
                </button>
              </div>
              {apiKeyForm.formState.errors.apiKey && (
                <p className="mt-1 text-sm text-red-500">
                  {apiKeyForm.formState.errors.apiKey.message}
                </p>
              )}
            </form>
          </div>
        ) : (
          <form onSubmit={apiKeyForm.handleSubmit(handleSaveApiKey)}>
            <div className="relative">
              <input
                {...apiKeyForm.register('apiKey')}
                type={showApiKey ? 'text' : 'password'}
                placeholder="sk-ant-api..."
                className="w-full rounded-lg border border-gray-300 px-3 py-2 pr-10 font-mono text-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
              <button
                type="button"
                onClick={() => setShowApiKey(!showApiKey)}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                {showApiKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
            {apiKeyForm.formState.errors.apiKey && (
              <p className="mt-1 text-sm text-red-500">
                {apiKeyForm.formState.errors.apiKey.message}
              </p>
            )}
            <button
              type="submit"
              disabled={atualizarApiKey.isPending}
              className="mt-3 w-full rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {atualizarApiKey.isPending ? 'Salvando...' : 'Salvar API Key'}
            </button>
          </form>
        )}

        <p className="mt-4 text-xs text-gray-500 dark:text-gray-400">
          Obtenha sua API key em{' '}
          <a
            href="https://console.anthropic.com/settings/keys"
            target="_blank"
            rel="noopener noreferrer"
            className="text-blue-600 hover:underline dark:text-blue-400"
          >
            console.anthropic.com
          </a>
        </p>
      </div>

      {/* Configurações Section */}
      {config?.apiKeyConfigurada && (
        <form
          onSubmit={configForm.handleSubmit(handleSaveConfig)}
          className="rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-800"
        >
          <div className="mb-4 flex items-center">
            <Settings2 className="h-5 w-5 text-gray-500" />
            <h3 className="ml-2 text-lg font-semibold text-gray-900 dark:text-white">
              Configurações
            </h3>
          </div>

          <div className="space-y-4">
            {/* Toggle IA Habilitada */}
            <div className="flex items-center justify-between">
              <div>
                <label className="font-medium text-gray-900 dark:text-white">
                  Habilitar IA
                </label>
                <p className="text-sm text-gray-500">
                  Ativa o diagnóstico assistido por IA nas ordens de serviço
                </p>
              </div>
              <label className="relative inline-flex cursor-pointer items-center">
                <input
                  type="checkbox"
                  {...configForm.register('iaHabilitada')}
                  className="peer sr-only"
                />
                <div className="peer h-6 w-11 rounded-full bg-gray-200 after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:border-gray-300 after:bg-white after:transition-all after:content-[''] peer-checked:bg-blue-600 peer-checked:after:translate-x-full peer-checked:after:border-white peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:border-gray-600 dark:bg-gray-700 dark:peer-focus:ring-blue-800"></div>
              </label>
            </div>

            {/* Toggle Pré-Validação */}
            <div className="flex items-center justify-between">
              <div>
                <label className="font-medium text-gray-900 dark:text-white">
                  Pré-validação com Templates
                </label>
                <p className="text-sm text-gray-500">
                  Usa templates locais para problemas comuns (economia de custos)
                </p>
              </div>
              <label className="relative inline-flex cursor-pointer items-center">
                <input
                  type="checkbox"
                  {...configForm.register('usarPreValidacao')}
                  className="peer sr-only"
                />
                <div className="peer h-6 w-11 rounded-full bg-gray-200 after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:border-gray-300 after:bg-white after:transition-all after:content-[''] peer-checked:bg-blue-600 peer-checked:after:translate-x-full peer-checked:after:border-white peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:border-gray-600 dark:bg-gray-700 dark:peer-focus:ring-blue-800"></div>
              </label>
            </div>

            {/* Toggle Cache */}
            <div className="flex items-center justify-between">
              <div>
                <label className="font-medium text-gray-900 dark:text-white">
                  Cache de Diagnósticos
                </label>
                <p className="text-sm text-gray-500">
                  Reutiliza diagnósticos similares (economia de custos)
                </p>
              </div>
              <label className="relative inline-flex cursor-pointer items-center">
                <input
                  type="checkbox"
                  {...configForm.register('usarCache')}
                  className="peer sr-only"
                />
                <div className="peer h-6 w-11 rounded-full bg-gray-200 after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:border-gray-300 after:bg-white after:transition-all after:content-[''] peer-checked:bg-blue-600 peer-checked:after:translate-x-full peer-checked:after:border-white peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:border-gray-600 dark:bg-gray-700 dark:peer-focus:ring-blue-800"></div>
              </label>
            </div>

            {/* Toggle Roteamento Inteligente */}
            <div className="flex items-center justify-between">
              <div>
                <label className="font-medium text-gray-900 dark:text-white">
                  Roteamento Inteligente
                </label>
                <p className="text-sm text-gray-500">
                  Usa Haiku para problemas simples, Sonnet para complexos
                </p>
              </div>
              <label className="relative inline-flex cursor-pointer items-center">
                <input
                  type="checkbox"
                  {...configForm.register('usarRoteamentoInteligente')}
                  className="peer sr-only"
                />
                <div className="peer h-6 w-11 rounded-full bg-gray-200 after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:border-gray-300 after:bg-white after:transition-all after:content-[''] peer-checked:bg-blue-600 peer-checked:after:translate-x-full peer-checked:after:border-white peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:border-gray-600 dark:bg-gray-700 dark:peer-focus:ring-blue-800"></div>
              </label>
            </div>

            {/* Limite Diário */}
            <div>
              <label className="mb-2 block font-medium text-gray-900 dark:text-white">
                Limite de Requisições/Dia
              </label>
              <input
                type="number"
                {...configForm.register('maxRequisicoesDia', { valueAsNumber: true })}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>

            {/* Max Tokens */}
            <div>
              <label className="mb-2 block font-medium text-gray-900 dark:text-white">
                Máx. Tokens por Resposta
              </label>
              <input
                type="number"
                {...configForm.register('maxTokensResposta', { valueAsNumber: true })}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
              <p className="mt-1 text-xs text-gray-500">
                Recomendado: 500-1000 (mais tokens = respostas mais detalhadas e maior custo)
              </p>
            </div>

            <button
              type="submit"
              disabled={atualizarConfig.isPending}
              className="mt-4 w-full rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {atualizarConfig.isPending ? 'Salvando...' : 'Salvar Configurações'}
            </button>
          </div>
        </form>
      )}

      {/* Estatísticas Section */}
      {config?.apiKeyConfigurada && (
        <div className="rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-800">
          <div className="mb-4 flex items-center">
            <BarChart3 className="h-5 w-5 text-gray-500" />
            <h3 className="ml-2 text-lg font-semibold text-gray-900 dark:text-white">
              Estatísticas de Uso
            </h3>
          </div>

          {isLoadingStats ? (
            <div className="flex items-center justify-center py-4">
              <Loader2 className="h-6 w-6 animate-spin text-blue-500" />
            </div>
          ) : estatisticas ? (
            <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
              <div className="rounded-lg bg-gray-50 p-4 dark:bg-gray-700">
                <p className="text-sm text-gray-500 dark:text-gray-400">Total Requisições</p>
                <p className="text-2xl font-bold text-gray-900 dark:text-white">
                  {estatisticas.totalRequisicoes}
                </p>
              </div>
              <div className="rounded-lg bg-green-50 p-4 dark:bg-green-900/20">
                <p className="text-sm text-green-600 dark:text-green-400">Taxa de Economia</p>
                <p className="text-2xl font-bold text-green-700 dark:text-green-300">
                  {estatisticas.taxaEconomia.toFixed(1)}%
                </p>
              </div>
              <div className="rounded-lg bg-blue-50 p-4 dark:bg-blue-900/20">
                <p className="text-sm text-blue-600 dark:text-blue-400">Cache Hits</p>
                <p className="text-2xl font-bold text-blue-700 dark:text-blue-300">
                  {estatisticas.cacheHits}
                </p>
              </div>
              <div className="rounded-lg bg-purple-50 p-4 dark:bg-purple-900/20">
                <p className="text-sm text-purple-600 dark:text-purple-400">Template Hits</p>
                <p className="text-2xl font-bold text-purple-700 dark:text-purple-300">
                  {estatisticas.templateHits}
                </p>
              </div>
              <div className="rounded-lg bg-orange-50 p-4 dark:bg-orange-900/20">
                <p className="text-sm text-orange-600 dark:text-orange-400">Tokens Consumidos</p>
                <p className="text-2xl font-bold text-orange-700 dark:text-orange-300">
                  {(estatisticas.tokensConsumidos / 1000).toFixed(1)}k
                </p>
              </div>
              <div className="rounded-lg bg-red-50 p-4 dark:bg-red-900/20">
                <p className="text-sm text-red-600 dark:text-red-400">Custo Estimado</p>
                <p className="text-2xl font-bold text-red-700 dark:text-red-300">
                  ${estatisticas.custoTotal.toFixed(2)}
                </p>
              </div>
              <div className="rounded-lg bg-yellow-50 p-4 dark:bg-yellow-900/20">
                <p className="text-sm text-yellow-600 dark:text-yellow-400">Hoje</p>
                <p className="text-2xl font-bold text-yellow-700 dark:text-yellow-300">
                  {estatisticas.requisicoesHoje}
                </p>
              </div>
              <div className="rounded-lg bg-cyan-50 p-4 dark:bg-cyan-900/20">
                <p className="text-sm text-cyan-600 dark:text-cyan-400">Restantes Hoje</p>
                <p className="text-2xl font-bold text-cyan-700 dark:text-cyan-300">
                  {estatisticas.requisicoesRestantes}
                </p>
              </div>
            </div>
          ) : (
            <p className="text-gray-500">Nenhuma estatística disponível</p>
          )}
        </div>
      )}

      {/* Tabela de Preços */}
      <div className="rounded-lg border border-gray-200 bg-white p-6 dark:border-gray-700 dark:bg-gray-800">
        <div className="mb-4 flex items-center">
          <svg className="h-5 w-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <h3 className="ml-2 text-lg font-semibold text-gray-900 dark:text-white">
            Tabela de Preços - API Anthropic (Janeiro 2025)
          </h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-700">
                <th className="px-3 py-2 text-left font-medium text-gray-700 dark:text-gray-300">Modelo</th>
                <th className="px-3 py-2 text-right font-medium text-gray-700 dark:text-gray-300">Input/1M</th>
                <th className="px-3 py-2 text-right font-medium text-gray-700 dark:text-gray-300">Output/1M</th>
                <th className="px-3 py-2 text-right font-medium text-gray-700 dark:text-gray-300">Uso Típico*</th>
                <th className="px-3 py-2 text-left font-medium text-gray-700 dark:text-gray-300">Recomendação</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              <tr className="bg-green-50 dark:bg-green-900/20">
                <td className="px-3 py-3">
                  <span className="font-medium text-gray-900 dark:text-white">Claude 3.5 Haiku</span>
                  <span className="ml-2 rounded bg-green-100 px-2 py-0.5 text-xs text-green-700 dark:bg-green-800 dark:text-green-300">
                    Padrão
                  </span>
                </td>
                <td className="px-3 py-3 text-right font-mono text-gray-700 dark:text-gray-300">$1.00</td>
                <td className="px-3 py-3 text-right font-mono text-gray-700 dark:text-gray-300">$5.00</td>
                <td className="px-3 py-3 text-right">
                  <span className="font-mono text-green-600 dark:text-green-400">~$0.02</span>
                  <span className="ml-1 text-gray-500">(R$ 0,12)</span>
                </td>
                <td className="px-3 py-3 text-green-600 dark:text-green-400">Diagnósticos simples</td>
              </tr>
              <tr>
                <td className="px-3 py-3">
                  <span className="font-medium text-gray-900 dark:text-white">Claude 3.5 Sonnet</span>
                  <span className="ml-2 rounded bg-blue-100 px-2 py-0.5 text-xs text-blue-700 dark:bg-blue-800 dark:text-blue-300">
                    Avançado
                  </span>
                </td>
                <td className="px-3 py-3 text-right font-mono text-gray-700 dark:text-gray-300">$3.00</td>
                <td className="px-3 py-3 text-right font-mono text-gray-700 dark:text-gray-300">$15.00</td>
                <td className="px-3 py-3 text-right">
                  <span className="font-mono text-blue-600 dark:text-blue-400">~$0.06</span>
                  <span className="ml-1 text-gray-500">(R$ 0,35)</span>
                </td>
                <td className="px-3 py-3 text-blue-600 dark:text-blue-400">Problemas complexos</td>
              </tr>
              <tr className="bg-purple-50 dark:bg-purple-900/20">
                <td className="px-3 py-3">
                  <span className="font-medium text-gray-900 dark:text-white">Claude Opus 4</span>
                  <span className="ml-2 rounded bg-purple-100 px-2 py-0.5 text-xs text-purple-700 dark:bg-purple-800 dark:text-purple-300">
                    Premium
                  </span>
                </td>
                <td className="px-3 py-3 text-right font-mono text-gray-700 dark:text-gray-300">$15.00</td>
                <td className="px-3 py-3 text-right font-mono text-gray-700 dark:text-gray-300">$75.00</td>
                <td className="px-3 py-3 text-right">
                  <span className="font-mono text-purple-600 dark:text-purple-400">~$0.30</span>
                  <span className="ml-1 text-gray-500">(R$ 1,75)</span>
                </td>
                <td className="px-3 py-3 text-purple-600 dark:text-purple-400">Casos críticos</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div className="mt-4 rounded-lg bg-gray-50 p-3 dark:bg-gray-700">
          <p className="text-xs text-gray-600 dark:text-gray-400">
            <strong>*Uso típico:</strong> Conversa de 10 mensagens (~5.000 tokens input + ~3.000 tokens output).
            Valores em R$ com câmbio aproximado de $1 = R$ 5,80.
          </p>
          <p className="mt-2 text-xs text-gray-600 dark:text-gray-400">
            <strong>Context Window:</strong> Todos os modelos suportam até 200K tokens (~150K palavras).
            1 token ≈ 4 caracteres em português.
          </p>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
          <div className="rounded-lg border border-green-200 bg-green-50 p-3 dark:border-green-800 dark:bg-green-900/20">
            <p className="text-sm font-medium text-green-700 dark:text-green-300">Com Roteamento Inteligente</p>
            <p className="mt-1 text-xs text-green-600 dark:text-green-400">
              Haiku para problemas simples (70%), Sonnet apenas quando necessário. Economia média de 60%.
            </p>
          </div>
          <div className="rounded-lg border border-blue-200 bg-blue-50 p-3 dark:border-blue-800 dark:bg-blue-900/20">
            <p className="text-sm font-medium text-blue-700 dark:text-blue-300">Com Cache Habilitado</p>
            <p className="mt-1 text-xs text-blue-600 dark:text-blue-400">
              Problemas similares reutilizam diagnósticos. Economia de 100% em cache hits.
            </p>
          </div>
          <div className="rounded-lg border border-purple-200 bg-purple-50 p-3 dark:border-purple-800 dark:bg-purple-900/20">
            <p className="text-sm font-medium text-purple-700 dark:text-purple-300">Com Pré-validação</p>
            <p className="mt-1 text-xs text-purple-600 dark:text-purple-400">
              Templates locais para problemas comuns. Custo zero para matches.
            </p>
          </div>
        </div>
      </div>

      {/* Info Section */}
      <div className="rounded-lg border border-blue-200 bg-blue-50 p-4 dark:border-blue-800 dark:bg-blue-900/20">
        <div className="flex items-start">
          <Bot className="mt-0.5 h-5 w-5 text-blue-600 dark:text-blue-400" />
          <div className="ml-3">
            <h4 className="font-medium text-blue-700 dark:text-blue-300">
              Sobre o Diagnóstico por IA
            </h4>
            <p className="mt-1 text-sm text-blue-600 dark:text-blue-400">
              O diagnóstico assistido utiliza o Claude (Anthropic) para analisar os problemas
              relatados e sugerir causas prováveis, peças necessárias e estimativas de custo.
              As otimizações de pré-validação, cache e roteamento ajudam a reduzir custos
              significativamente.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
