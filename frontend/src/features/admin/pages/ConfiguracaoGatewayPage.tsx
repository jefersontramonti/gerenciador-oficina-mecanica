/**
 * Página de configuração do gateway de pagamento (SUPER_ADMIN)
 * Permite configurar o Mercado Pago para receber pagamentos das oficinas
 */

import { useState } from 'react';
import { format, isValid, parseISO } from 'date-fns';
import { ptBR } from 'date-fns/locale';

/**
 * Safely parse a date that might come as ISO string or array from backend.
 */
function parseDate(value: unknown): Date | null {
  if (!value) return null;

  // If it's an array [year, month, day, hour, minute, second?]
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    const date = new Date(year, month - 1, day, hour, minute, second);
    return isValid(date) ? date : null;
  }

  // If it's a string, try to parse as ISO
  if (typeof value === 'string') {
    const date = parseISO(value);
    return isValid(date) ? date : null;
  }

  return null;
}
import {
  CreditCard,
  Settings,
  CheckCircle,
  XCircle,
  AlertCircle,
  Eye,
  EyeOff,
  RefreshCw,
  Save,
  ExternalLink,
  Shield,
  Loader2,
} from 'lucide-react';
import {
  useConfigMercadoPago,
  useSalvarMercadoPago,
  useValidarMercadoPago,
} from '../hooks/useGateway';
import type { ConfiguracaoGatewayRequest } from '../types/gateway';

export function ConfiguracaoGatewayPage() {
  const [showAccessToken, setShowAccessToken] = useState(false);
  const [showPublicKey, setShowPublicKey] = useState(false);
  const [showWebhookSecret, setShowWebhookSecret] = useState(false);

  // Form state
  const [accessToken, setAccessToken] = useState('');
  const [publicKey, setPublicKey] = useState('');
  const [webhookSecret, setWebhookSecret] = useState('');
  const [ativo, setAtivo] = useState(false);
  const [sandbox, setSandbox] = useState(true);

  const { data: config, isLoading, error } = useConfigMercadoPago();
  const salvarMutation = useSalvarMercadoPago();
  const validarMutation = useValidarMercadoPago();

  const handleSalvar = async () => {
    const request: ConfiguracaoGatewayRequest = {
      ativo,
      sandbox,
    };

    // Only include credentials if they were changed (not empty)
    if (accessToken.trim()) {
      request.accessToken = accessToken;
    }
    if (publicKey.trim()) {
      request.publicKey = publicKey;
    }
    if (webhookSecret.trim()) {
      request.webhookSecret = webhookSecret;
    }

    try {
      await salvarMutation.mutateAsync(request);
      // Clear sensitive fields after save
      setAccessToken('');
      setPublicKey('');
      setWebhookSecret('');
      alert('Configuração salva com sucesso!');
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao salvar configuração');
    }
  };

  const handleValidar = async () => {
    try {
      const result = await validarMutation.mutateAsync();
      if (result.validacaoSucesso) {
        alert('Credenciais válidas! ' + result.mensagemValidacao);
      } else {
        alert('Credenciais inválidas: ' + result.mensagemValidacao);
      }
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao validar credenciais');
    }
  };

  // Update form state when config loads
  useState(() => {
    if (config) {
      setAtivo(config.ativo);
      setSandbox(config.sandbox);
    }
  });

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[50vh]">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 dark:border-blue-400 border-t-transparent"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 sm:p-6">
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400">
          <p className="font-semibold">Erro ao carregar configuração</p>
          <p className="mt-1 text-sm">{(error as Error).message}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
            <CreditCard className="h-6 w-6 text-blue-600 dark:text-blue-400" />
            Gateway de Pagamento
          </h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            Configure o Mercado Pago para receber pagamentos das oficinas
          </p>
        </div>
      </div>

      {/* Status Card */}
      <div className="rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700 p-4 sm:p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            {config?.configurado ? (
              <div className="p-2 rounded-full bg-green-100 dark:bg-green-900/30">
                <CheckCircle className="h-6 w-6 text-green-600 dark:text-green-400" />
              </div>
            ) : (
              <div className="p-2 rounded-full bg-yellow-100 dark:bg-yellow-900/30">
                <AlertCircle className="h-6 w-6 text-yellow-600 dark:text-yellow-400" />
              </div>
            )}
            <div>
              <h3 className="font-semibold text-gray-900 dark:text-white">
                {config?.tipoNome || 'Mercado Pago'}
              </h3>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                {config?.configurado
                  ? config.ativo
                    ? 'Configurado e ativo'
                    : 'Configurado mas inativo'
                  : 'Não configurado'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {config?.sandbox && (
              <span className="px-2 py-1 text-xs font-medium rounded-full bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400">
                Sandbox
              </span>
            )}
            {config?.ativo && (
              <span className="px-2 py-1 text-xs font-medium rounded-full bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400">
                Ativo
              </span>
            )}
          </div>
        </div>

        {/* Validation Status */}
        {config?.ultimaValidacao && (() => {
          const validacaoDate = parseDate(config.ultimaValidacao);
          if (!validacaoDate) return null;
          return (
            <div className="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-2 text-sm">
                {config.validacaoSucesso ? (
                  <CheckCircle className="h-4 w-4 text-green-500" />
                ) : (
                  <XCircle className="h-4 w-4 text-red-500" />
                )}
                <span className="text-gray-600 dark:text-gray-400">
                  Última validação:{' '}
                  {format(validacaoDate, "dd/MM/yyyy 'às' HH:mm", {
                    locale: ptBR,
                  })}
                </span>
              </div>
              {config.mensagemValidacao && (
                <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                  {config.mensagemValidacao}
                </p>
              )}
            </div>
          );
        })()}
      </div>

      {/* Configuration Form */}
      <div className="rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div className="px-4 sm:px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
            <Settings className="h-5 w-5" />
            Configuração
          </h2>
        </div>

        <div className="p-4 sm:p-6 space-y-6">
          {/* Status Toggles */}
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="flex items-center gap-3 p-4 rounded-lg border border-gray-200 dark:border-gray-700 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50">
              <input
                type="checkbox"
                checked={ativo}
                onChange={(e) => setAtivo(e.target.checked)}
                className="h-5 w-5 rounded border-gray-300 dark:border-gray-600 text-blue-600 focus:ring-blue-500"
              />
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Ativo</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Habilitar recebimento de pagamentos
                </p>
              </div>
            </label>

            <label className="flex items-center gap-3 p-4 rounded-lg border border-gray-200 dark:border-gray-700 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50">
              <input
                type="checkbox"
                checked={sandbox}
                onChange={(e) => setSandbox(e.target.checked)}
                className="h-5 w-5 rounded border-gray-300 dark:border-gray-600 text-yellow-600 focus:ring-yellow-500"
              />
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Modo Sandbox</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Usar ambiente de testes
                </p>
              </div>
            </label>
          </div>

          {/* Credentials */}
          <div className="space-y-4">
            <h3 className="font-medium text-gray-900 dark:text-white flex items-center gap-2">
              <Shield className="h-4 w-4" />
              Credenciais
            </h3>

            {/* Access Token */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Access Token *
              </label>
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <input
                    type={showAccessToken ? 'text' : 'password'}
                    value={accessToken}
                    onChange={(e) => setAccessToken(e.target.value)}
                    placeholder={config?.accessTokenMasked || 'APP_USR-xxxxxxxx'}
                    className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-4 py-2 pr-10 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 focus:border-transparent"
                  />
                  <button
                    type="button"
                    onClick={() => setShowAccessToken(!showAccessToken)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                  >
                    {showAccessToken ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>
              </div>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Obtenha em: Mercado Pago {'>'} Seu negócio {'>'} Configurações {'>'} Credenciais
              </p>
            </div>

            {/* Public Key */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Public Key (opcional)
              </label>
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <input
                    type={showPublicKey ? 'text' : 'password'}
                    value={publicKey}
                    onChange={(e) => setPublicKey(e.target.value)}
                    placeholder={config?.publicKeyMasked || 'APP_USR-xxxxxxxx'}
                    className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-4 py-2 pr-10 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 focus:border-transparent"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPublicKey(!showPublicKey)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                  >
                    {showPublicKey ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>
              </div>
            </div>

            {/* Webhook Secret */}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Webhook Secret (opcional)
              </label>
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <input
                    type={showWebhookSecret ? 'text' : 'password'}
                    value={webhookSecret}
                    onChange={(e) => setWebhookSecret(e.target.value)}
                    placeholder={config?.temWebhookSecret ? '••••••••' : 'Secret do webhook'}
                    className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-4 py-2 pr-10 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 focus:border-transparent"
                  />
                  <button
                    type="button"
                    onClick={() => setShowWebhookSecret(!showWebhookSecret)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                  >
                    {showWebhookSecret ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Webhook URL */}
          {config?.webhookUrl && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                URL do Webhook
              </label>
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  readOnly
                  value={config.webhookUrl}
                  className="flex-1 rounded-lg border border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-300 px-4 py-2"
                />
                <button
                  type="button"
                  onClick={() => {
                    navigator.clipboard.writeText(config.webhookUrl);
                    alert('URL copiada!');
                  }}
                  className="px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  Copiar
                </button>
              </div>
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Configure esta URL no painel do Mercado Pago para receber notificações de pagamento
              </p>
            </div>
          )}

          {/* Actions */}
          <div className="flex flex-col sm:flex-row gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
            <button
              onClick={handleSalvar}
              disabled={salvarMutation.isPending}
              className="flex items-center justify-center gap-2 px-4 py-2 rounded-lg bg-blue-600 dark:bg-blue-700 text-white hover:bg-blue-700 dark:hover:bg-blue-600 disabled:opacity-50"
            >
              {salvarMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Save className="h-4 w-4" />
              )}
              Salvar Configuração
            </button>

            <button
              onClick={handleValidar}
              disabled={validarMutation.isPending || !config?.configurado}
              className="flex items-center justify-center gap-2 px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
            >
              {validarMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <RefreshCw className="h-4 w-4" />
              )}
              Validar Credenciais
            </button>

            <a
              href="https://www.mercadopago.com.br/developers/pt/docs"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center justify-center gap-2 px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              <ExternalLink className="h-4 w-4" />
              Documentação
            </a>
          </div>
        </div>
      </div>

      {/* Help */}
      <div className="rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 p-4">
        <h3 className="font-medium text-blue-800 dark:text-blue-300 mb-2">
          Como configurar o Mercado Pago
        </h3>
        <ol className="list-decimal list-inside text-sm text-blue-700 dark:text-blue-400 space-y-1">
          <li>
            Acesse{' '}
            <a
              href="https://www.mercadopago.com.br/developers/panel/app"
              target="_blank"
              rel="noopener noreferrer"
              className="underline"
            >
              mercadopago.com.br/developers
            </a>
          </li>
          <li>Crie uma aplicação ou acesse uma existente</li>
          <li>Vá em "Credenciais de produção" (ou sandbox para testes)</li>
          <li>Copie o "Access Token" e cole acima</li>
          <li>Configure o webhook com a URL fornecida acima</li>
          <li>Salve e valide as credenciais</li>
        </ol>
      </div>
    </div>
  );
}
