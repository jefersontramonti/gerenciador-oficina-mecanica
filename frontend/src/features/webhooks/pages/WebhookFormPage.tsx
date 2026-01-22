import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  ArrowLeft,
  Save,
  Webhook,
  TestTube,
  Loader2,
  CheckCircle,
  XCircle,
  Plus,
  Trash2,
  Key,
  Link as LinkIcon,
} from 'lucide-react';
import { useWebhook, useCreateWebhook, useUpdateWebhook, useTestWebhook } from '../hooks/useWebhooks';
import { TipoEventoWebhook, eventoDescricoes } from '../types';
import type { WebhookConfigCreateRequest, WebhookTestResult } from '../types';

const webhookSchema = z.object({
  nome: z.string().min(1, 'Nome é obrigatório').max(100),
  descricao: z.string().max(500).optional(),
  url: z.string().min(1, 'URL é obrigatória').url('URL inválida'),
  secret: z.string().max(200).optional(),
  eventos: z.array(z.nativeEnum(TipoEventoWebhook)).min(1, 'Selecione pelo menos um evento'),
  maxTentativas: z.number().min(1).max(10),
  timeoutSegundos: z.number().min(5).max(120),
});

type WebhookFormData = z.infer<typeof webhookSchema>;

export default function WebhookFormPage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEditing = !!id;

  const { data: webhook, isLoading: isLoadingWebhook } = useWebhook(id);
  const createWebhook = useCreateWebhook();
  const updateWebhook = useUpdateWebhook();
  const testWebhook = useTestWebhook();

  const [headers, setHeaders] = useState<{ key: string; value: string }[]>([]);
  const [testResult, setTestResult] = useState<WebhookTestResult | null>(null);
  const [testingEvento, setTestingEvento] = useState<TipoEventoWebhook | null>(null);

  const {
    register,
    handleSubmit,
    control,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<WebhookFormData>({
    resolver: zodResolver(webhookSchema),
    defaultValues: {
      nome: '',
      descricao: '',
      url: '',
      secret: '',
      eventos: [],
      maxTentativas: 3,
      timeoutSegundos: 30,
    },
  });

  const selectedEventos = watch('eventos') || [];

  // Load webhook data when editing
  useEffect(() => {
    if (webhook) {
      reset({
        nome: webhook.nome,
        descricao: webhook.descricao || '',
        url: webhook.url,
        secret: '', // Don't show secret
        eventos: webhook.eventos,
        maxTentativas: webhook.maxTentativas,
        timeoutSegundos: webhook.timeoutSegundos,
      });

      if (webhook.headers) {
        setHeaders(Object.entries(webhook.headers).map(([key, value]) => ({ key, value })));
      }
    }
  }, [webhook, reset]);

  const onSubmit = async (data: WebhookFormData) => {
    try {
      const headersMap = headers.reduce((acc, { key, value }) => {
        if (key && value) acc[key] = value;
        return acc;
      }, {} as Record<string, string>);

      const payload: WebhookConfigCreateRequest = {
        ...data,
        headers: Object.keys(headersMap).length > 0 ? headersMap : undefined,
      };

      if (isEditing && id) {
        await updateWebhook.mutateAsync({ id, data: payload });
      } else {
        await createWebhook.mutateAsync(payload);
      }

      navigate('/webhooks');
    } catch (error: any) {
      console.error('Erro ao salvar webhook:', error);
      alert(error.response?.data?.message || 'Erro ao salvar webhook');
    }
  };

  const handleTest = async (evento: TipoEventoWebhook) => {
    if (!id) return;

    setTestingEvento(evento);
    setTestResult(null);

    try {
      const result = await testWebhook.mutateAsync({ webhookId: id, evento });
      setTestResult(result);
    } catch (error) {
      console.error('Erro ao testar webhook:', error);
    } finally {
      setTestingEvento(null);
    }
  };

  const addHeader = () => {
    setHeaders([...headers, { key: '', value: '' }]);
  };

  const removeHeader = (index: number) => {
    setHeaders(headers.filter((_, i) => i !== index));
  };

  const updateHeader = (index: number, field: 'key' | 'value', value: string) => {
    const updated = [...headers];
    updated[index][field] = value;
    setHeaders(updated);
  };

  if (isEditing && isLoadingWebhook) {
    return (
      <div className="flex items-center justify-center p-8">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/webhooks')}
          className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
        >
          <ArrowLeft className="h-5 w-5 text-gray-600 dark:text-gray-400" />
        </button>
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
            <Webhook className="h-6 w-6" />
            {isEditing ? 'Editar Webhook' : 'Novo Webhook'}
          </h1>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Basic Info */}
        <div className="rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-4 sm:p-6">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            Informações Básicas
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Nome *
              </label>
              <input
                {...register('nome')}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                placeholder="Ex: Integração ERP"
              />
              {errors.nome && (
                <p className="text-xs text-red-500 mt-1">{errors.nome.message}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Descrição
              </label>
              <input
                {...register('descricao')}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                placeholder="Descrição opcional"
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                URL do Webhook *
              </label>
              <div className="relative">
                <LinkIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  {...register('url')}
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white pl-10 pr-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                  placeholder="https://seu-sistema.com/webhook"
                />
              </div>
              {errors.url && (
                <p className="text-xs text-red-500 mt-1">{errors.url.message}</p>
              )}
            </div>

            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Secret (HMAC-SHA256)
              </label>
              <div className="relative">
                <Key className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  {...register('secret')}
                  type="password"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white pl-10 pr-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                  placeholder={isEditing ? '••••••••' : 'Opcional - para assinatura dos payloads'}
                />
              </div>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Se configurado, o payload será assinado com HMAC-SHA256 no header X-Webhook-Signature
              </p>
            </div>
          </div>
        </div>

        {/* Headers */}
        <div className="rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-4 sm:p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              Headers Customizados
            </h2>
            <button
              type="button"
              onClick={addHeader}
              className="flex items-center gap-1 text-sm text-blue-600 dark:text-blue-400 hover:underline"
            >
              <Plus className="h-4 w-4" />
              Adicionar
            </button>
          </div>

          {headers.length === 0 ? (
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Nenhum header customizado configurado
            </p>
          ) : (
            <div className="space-y-2">
              {headers.map((header, index) => (
                <div key={index} className="flex gap-2">
                  <input
                    value={header.key}
                    onChange={(e) => updateHeader(index, 'key', e.target.value)}
                    className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-3 py-2 text-sm"
                    placeholder="Header"
                  />
                  <input
                    value={header.value}
                    onChange={(e) => updateHeader(index, 'value', e.target.value)}
                    className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-3 py-2 text-sm"
                    placeholder="Valor"
                  />
                  <button
                    type="button"
                    onClick={() => removeHeader(index)}
                    className="p-2 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Events */}
        <div className="rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-4 sm:p-6">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            Eventos *
          </h2>
          {errors.eventos && (
            <p className="text-xs text-red-500 mb-2">{errors.eventos.message}</p>
          )}

          <Controller
            name="eventos"
            control={control}
            render={({ field }) => (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {Object.entries(TipoEventoWebhook).map(([key, value]) => {
                  const desc = eventoDescricoes[value];
                  const isSelected = field.value.includes(value);

                  return (
                    <label
                      key={key}
                      className={`flex items-start gap-3 p-3 rounded-lg border cursor-pointer transition-colors ${
                        isSelected
                          ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                          : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
                      }`}
                    >
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={(e) => {
                          if (e.target.checked) {
                            field.onChange([...field.value, value]);
                          } else {
                            field.onChange(field.value.filter((v: string) => v !== value));
                          }
                        }}
                        className="mt-1 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                      />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900 dark:text-white">
                          {desc?.nome || value}
                        </p>
                        <p className="text-xs text-gray-500 dark:text-gray-400">
                          {desc?.descricao || ''}
                        </p>
                      </div>
                    </label>
                  );
                })}
              </div>
            )}
          />
        </div>

        {/* Advanced Settings */}
        <div className="rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-4 sm:p-6">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            Configurações Avançadas
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Máximo de Tentativas
              </label>
              <input
                {...register('maxTentativas', { valueAsNumber: true })}
                type="number"
                min={1}
                max={10}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-3 py-2 text-sm"
              />
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Número de retries em caso de falha (1-10)
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Timeout (segundos)
              </label>
              <input
                {...register('timeoutSegundos', { valueAsNumber: true })}
                type="number"
                min={5}
                max={120}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-3 py-2 text-sm"
              />
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Tempo máximo de espera pela resposta (5-120s)
              </p>
            </div>
          </div>
        </div>

        {/* Test Section (only when editing) */}
        {isEditing && selectedEventos.length > 0 && (
          <div className="rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-4 sm:p-6">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
              Testar Webhook
            </h2>

            <div className="flex flex-wrap gap-2 mb-4">
              {selectedEventos.map((evento) => (
                <button
                  key={evento}
                  type="button"
                  onClick={() => handleTest(evento)}
                  disabled={testingEvento !== null}
                  className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
                >
                  {testingEvento === evento ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <TestTube className="h-4 w-4" />
                  )}
                  {eventoDescricoes[evento]?.nome || evento}
                </button>
              ))}
            </div>

            {testResult && (
              <div
                className={`p-4 rounded-lg border ${
                  testResult.sucesso
                    ? 'border-green-300 dark:border-green-700 bg-green-50 dark:bg-green-900/20'
                    : 'border-red-300 dark:border-red-700 bg-red-50 dark:bg-red-900/20'
                }`}
              >
                <div className="flex items-center gap-2 mb-2">
                  {testResult.sucesso ? (
                    <CheckCircle className="h-5 w-5 text-green-600 dark:text-green-400" />
                  ) : (
                    <XCircle className="h-5 w-5 text-red-600 dark:text-red-400" />
                  )}
                  <span
                    className={`font-medium ${
                      testResult.sucesso
                        ? 'text-green-800 dark:text-green-300'
                        : 'text-red-800 dark:text-red-300'
                    }`}
                  >
                    {testResult.sucesso ? 'Sucesso!' : 'Falha'}
                  </span>
                  {testResult.httpStatus && (
                    <span className="text-sm text-gray-600 dark:text-gray-400">
                      HTTP {testResult.httpStatus}
                    </span>
                  )}
                  {testResult.tempoRespostaMs && (
                    <span className="text-sm text-gray-600 dark:text-gray-400">
                      ({testResult.tempoRespostaMs}ms)
                    </span>
                  )}
                </div>

                {testResult.erro && (
                  <p className="text-sm text-red-600 dark:text-red-400">{testResult.erro}</p>
                )}

                {testResult.responseBody && (
                  <div className="mt-2">
                    <p className="text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Resposta:
                    </p>
                    <pre className="text-xs bg-gray-100 dark:bg-gray-900 p-2 rounded overflow-x-auto">
                      {testResult.responseBody}
                    </pre>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* Actions */}
        <div className="flex flex-col-reverse sm:flex-row justify-end gap-3">
          <button
            type="button"
            onClick={() => navigate('/webhooks')}
            className="rounded-lg border border-gray-300 dark:border-gray-600 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {isSubmitting ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}
            {isEditing ? 'Salvar Alterações' : 'Criar Webhook'}
          </button>
        </div>
      </form>
    </div>
  );
}
