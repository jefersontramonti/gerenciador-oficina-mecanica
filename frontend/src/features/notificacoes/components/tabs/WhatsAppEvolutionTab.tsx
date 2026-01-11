import { useState, useEffect } from 'react';
import { MessageSquare, CheckCircle, XCircle, AlertCircle, Loader2, Copy, Send, Plus, Trash2, Power, RefreshCw, QrCode } from 'lucide-react';
import {
  useConfiguracoes,
  useUpdateConfiguracao,
  useWhatsAppStatus,
  useTestarNotificacao,
  useCreateWhatsAppInstance,
  useDeleteWhatsAppInstance,
  useDisconnectWhatsApp,
  useReconnectWhatsApp,
} from '../../hooks/useNotificacoes';
import type { UpdateConfiguracaoNotificacaoRequest } from '../../types';

export function WhatsAppEvolutionTab() {
  const { data: config, isLoading: configLoading, refetch: refetchConfig } = useConfiguracoes();
  const { data: status, isLoading: statusLoading, refetch: refetchStatus } = useWhatsAppStatus();
  const updateMutation = useUpdateConfiguracao();
  const testMutation = useTestarNotificacao();
  const createInstanceMutation = useCreateWhatsAppInstance();
  const deleteInstanceMutation = useDeleteWhatsAppInstance();
  const disconnectMutation = useDisconnectWhatsApp();
  const reconnectMutation = useReconnectWhatsApp();

  const [formData, setFormData] = useState({
    evolutionApiUrl: '',
    evolutionApiToken: '',
    evolutionInstanceName: '',
    whatsappNumero: '',
  });

  const [testData, setTestData] = useState({
    destinatario: '',
    mensagem: 'Mensagem de teste do sistema PitStop',
  });

  const [copied, setCopied] = useState(false);
  const [qrCode, setQrCode] = useState<string | null>(null);
  const [showManualConfig, setShowManualConfig] = useState(false);

  // Initialize form data when config loads
  useEffect(() => {
    if (config) {
      setFormData({
        evolutionApiUrl: config.evolutionApiUrl || '',
        evolutionApiToken: '',
        evolutionInstanceName: config.evolutionInstanceName || '',
        whatsappNumero: config.whatsappNumero || '',
      });
    }
  }, [config]);

  // Auto-refresh status when QR code is showing
  useEffect(() => {
    if (qrCode && !status?.conectado) {
      const interval = setInterval(() => {
        refetchStatus();
      }, 5000);
      return () => clearInterval(interval);
    }
  }, [qrCode, status?.conectado, refetchStatus]);

  // Clear QR code when connected
  useEffect(() => {
    if (status?.conectado && qrCode) {
      setQrCode(null);
    }
  }, [status?.conectado, qrCode]);

  const handleCreateInstance = async () => {
    try {
      const result = await createInstanceMutation.mutateAsync();
      if (result.sucesso) {
        if (result.qrCode) {
          setQrCode(result.qrCode);
        }
        await refetchConfig();
        await refetchStatus();
        alert(`Instância criada com sucesso! ${result.mensagem}`);
      } else {
        alert(`Erro ao criar instância: ${result.mensagem}`);
      }
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao criar instância');
    }
  };

  const handleDeleteInstance = async () => {
    if (!confirm('Tem certeza que deseja excluir a instância do WhatsApp? Esta ação não pode ser desfeita.')) {
      return;
    }

    try {
      const result = await deleteInstanceMutation.mutateAsync();
      if (result.sucesso) {
        setQrCode(null);
        await refetchConfig();
        await refetchStatus();
        alert('Instância excluída com sucesso!');
      } else {
        alert(`Erro ao excluir instância: ${result.mensagem}`);
      }
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao excluir instância');
    }
  };

  const handleDisconnect = async () => {
    if (!confirm('Tem certeza que deseja desconectar o WhatsApp? Você precisará escanear o QR code novamente.')) {
      return;
    }

    try {
      const result = await disconnectMutation.mutateAsync();
      if (result.sucesso) {
        await refetchStatus();
        alert('WhatsApp desconectado com sucesso!');
      } else {
        alert(`Erro ao desconectar: ${result.mensagem}`);
      }
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao desconectar');
    }
  };

  const handleReconnect = async () => {
    try {
      const result = await reconnectMutation.mutateAsync();
      if (result.sucesso) {
        await refetchStatus();
        alert('Reconectando WhatsApp...');
      } else {
        alert(`Erro ao reconectar: ${result.mensagem}`);
      }
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao reconectar');
    }
  };

  const handleSave = async () => {
    try {
      const request: UpdateConfiguracaoNotificacaoRequest = {
        evolutionApiUrl: formData.evolutionApiUrl || undefined,
        evolutionApiToken: formData.evolutionApiToken || undefined,
        evolutionInstanceName: formData.evolutionInstanceName || undefined,
        whatsappNumero: formData.whatsappNumero || undefined,
      };
      await updateMutation.mutateAsync({ data: request });
      alert('Configuração salva com sucesso!');
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao salvar configuração');
    }
  };

  const handleTest = async () => {
    if (!testData.destinatario) {
      alert('Informe o número de destino para o teste');
      return;
    }

    try {
      const result = await testMutation.mutateAsync({
        tipo: 'WHATSAPP',
        destinatario: testData.destinatario,
        mensagem: testData.mensagem,
      });
      if (result.sucesso) {
        alert('Teste enviado com sucesso! Verifique o destinatário.');
      } else {
        alert(`Erro no teste: ${result.mensagem}`);
      }
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao enviar teste');
    }
  };

  const handleCopyWebhook = () => {
    const webhookUrl = `${window.location.origin}/api/webhook/evolution`;
    navigator.clipboard.writeText(webhookUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const getStatusConfig = () => {
    if (statusLoading) {
      return {
        icon: Loader2,
        color: 'text-gray-500 dark:text-gray-400',
        bgColor: 'bg-gray-100 dark:bg-gray-800',
        borderColor: 'border-gray-200 dark:border-gray-700',
        label: 'Verificando...',
        animate: true,
      };
    }

    if (!status || !config?.temEvolutionApiConfigurada) {
      return {
        icon: AlertCircle,
        color: 'text-yellow-700 dark:text-yellow-400',
        bgColor: 'bg-yellow-50 dark:bg-yellow-900/30',
        borderColor: 'border-yellow-200 dark:border-yellow-800',
        label: 'Não configurado',
        animate: false,
      };
    }

    if (status.conectado) {
      return {
        icon: CheckCircle,
        color: 'text-green-700 dark:text-green-400',
        bgColor: 'bg-green-50 dark:bg-green-900/30',
        borderColor: 'border-green-200 dark:border-green-800',
        label: 'Conectado',
        animate: false,
      };
    }

    return {
      icon: XCircle,
      color: 'text-red-700 dark:text-red-400',
      bgColor: 'bg-red-50 dark:bg-red-900/30',
      borderColor: 'border-red-200 dark:border-red-800',
      label: 'Desconectado',
      animate: false,
    };
  };

  const statusConfig = getStatusConfig();
  const StatusIcon = statusConfig.icon;
  const hasInstance = !!config?.evolutionInstanceName;
  const isAnyMutationPending = createInstanceMutation.isPending || deleteInstanceMutation.isPending || disconnectMutation.isPending || reconnectMutation.isPending;

  if (configLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 dark:border-gray-700 border-t-blue-600 dark:border-t-blue-400" />
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
      {/* Main Content */}
      <div className="lg:col-span-2">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-base font-semibold text-gray-900 dark:text-white">WhatsApp (Evolution API)</h2>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              Configure número, instância e teste de envio.
            </p>
          </div>

          <span
            className={`inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-medium ${statusConfig.bgColor} ${statusConfig.color} ${statusConfig.borderColor}`}
          >
            <StatusIcon className={`h-3.5 w-3.5 ${statusConfig.animate ? 'animate-spin' : ''}`} />
            {statusConfig.label}
          </span>
        </div>

        {/* Quick Setup Section - Show when no instance */}
        {!hasInstance && (
          <div className="mt-6 rounded-xl border-2 border-dashed border-blue-300 dark:border-blue-700 bg-blue-50 dark:bg-blue-900/20 p-6">
            <div className="text-center">
              <QrCode className="mx-auto h-12 w-12 text-blue-500 dark:text-blue-400" />
              <h3 className="mt-4 text-lg font-semibold text-gray-900 dark:text-white">
                Configure o WhatsApp Rapidamente
              </h3>
              <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
                Clique no botão abaixo para criar automaticamente uma instância do WhatsApp para sua oficina.
                Após criar, escaneie o QR code com seu celular.
              </p>
              <div className="mt-6 flex flex-wrap justify-center gap-3">
                <button
                  onClick={handleCreateInstance}
                  disabled={createInstanceMutation.isPending}
                  className="flex items-center gap-2 rounded-lg bg-green-600 px-6 py-3 text-sm font-medium text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {createInstanceMutation.isPending ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Criando...
                    </>
                  ) : (
                    <>
                      <Plus className="h-4 w-4" />
                      Criar Instância Automaticamente
                    </>
                  )}
                </button>
                <button
                  onClick={() => setShowManualConfig(!showManualConfig)}
                  className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-3 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
                >
                  Configurar Manualmente
                </button>
              </div>
            </div>
          </div>
        )}

        {/* QR Code Display */}
        {qrCode && (
          <div className="mt-6 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-6">
            <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white">
              <QrCode className="h-4 w-4" />
              Escaneie o QR Code
            </h3>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              Abra o WhatsApp no seu celular, vá em Configurações {">"} Aparelhos conectados {">"} Conectar um aparelho
            </p>
            <div className="mt-4 flex justify-center">
              <div className="rounded-lg bg-white p-4 shadow-lg">
                <img
                  src={qrCode.startsWith('data:') ? qrCode : `data:image/png;base64,${qrCode}`}
                  alt="QR Code WhatsApp"
                  className="h-64 w-64"
                />
              </div>
            </div>
            <p className="mt-4 text-center text-xs text-gray-500 dark:text-gray-400">
              <Loader2 className="inline h-3 w-3 animate-spin mr-1" />
              Aguardando conexão... A página será atualizada automaticamente.
            </p>
          </div>
        )}

        {/* Instance Management - Show when has instance */}
        {hasInstance && (
          <div className="mt-6 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-5">
            <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white">
              <MessageSquare className="h-4 w-4" />
              Gerenciar Instância
            </h3>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              Instância: <span className="font-mono text-blue-600 dark:text-blue-400">{config?.evolutionInstanceName}</span>
            </p>

            <div className="mt-4 flex flex-wrap gap-3">
              {status?.conectado ? (
                <>
                  <button
                    onClick={handleDisconnect}
                    disabled={isAnyMutationPending}
                    className="flex items-center gap-2 rounded-lg border border-yellow-300 dark:border-yellow-600 bg-yellow-50 dark:bg-yellow-900/30 px-4 py-2 text-sm font-medium text-yellow-700 dark:text-yellow-300 hover:bg-yellow-100 dark:hover:bg-yellow-900/50 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {disconnectMutation.isPending ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Desconectando...
                      </>
                    ) : (
                      <>
                        <Power className="h-4 w-4" />
                        Desconectar
                      </>
                    )}
                  </button>
                  <button
                    onClick={handleReconnect}
                    disabled={isAnyMutationPending}
                    className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {reconnectMutation.isPending ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Reconectando...
                      </>
                    ) : (
                      <>
                        <RefreshCw className="h-4 w-4" />
                        Reiniciar
                      </>
                    )}
                  </button>
                </>
              ) : (
                <button
                  onClick={handleReconnect}
                  disabled={isAnyMutationPending}
                  className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {reconnectMutation.isPending ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Reconectando...
                    </>
                  ) : (
                    <>
                      <RefreshCw className="h-4 w-4" />
                      Reconectar / Novo QR Code
                    </>
                  )}
                </button>
              )}
              <button
                onClick={handleDeleteInstance}
                disabled={isAnyMutationPending}
                className="flex items-center gap-2 rounded-lg border border-red-300 dark:border-red-600 bg-red-50 dark:bg-red-900/30 px-4 py-2 text-sm font-medium text-red-700 dark:text-red-300 hover:bg-red-100 dark:hover:bg-red-900/50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {deleteInstanceMutation.isPending ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Excluindo...
                  </>
                ) : (
                  <>
                    <Trash2 className="h-4 w-4" />
                    Excluir Instância
                  </>
                )}
              </button>
            </div>
          </div>
        )}

        {/* Manual Configuration Form - Always show when has instance OR when manually requested */}
        {(hasInstance || showManualConfig) && (
          <div className="mt-6 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-5">
            <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white">
              <MessageSquare className="h-4 w-4" />
              Configuração Manual da Evolution API
            </h3>
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              Use esta opção se você já possui uma instância configurada na Evolution API
            </p>

            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  URL da Evolution API
                </label>
                <input
                  type="url"
                  value={formData.evolutionApiUrl}
                  onChange={(e) => setFormData({ ...formData, evolutionApiUrl: e.target.value })}
                  placeholder="https://evolution.seudominio.com"
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  URL completa do servidor Evolution API (sem barra no final)
                </p>
              </div>

              <div className="sm:col-span-2">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  API Key / Token
                </label>
                <input
                  type="password"
                  value={formData.evolutionApiToken}
                  onChange={(e) => setFormData({ ...formData, evolutionApiToken: e.target.value })}
                  placeholder="••••••••••••••••"
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Token de autenticação da instância na Evolution API
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Nome da Instância
                </label>
                <input
                  type="text"
                  value={formData.evolutionInstanceName}
                  onChange={(e) => setFormData({ ...formData, evolutionInstanceName: e.target.value })}
                  placeholder="oficina_principal"
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Nome exato da instância criada na Evolution API
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Número do WhatsApp
                </label>
                <input
                  type="text"
                  value={formData.whatsappNumero}
                  onChange={(e) => setFormData({ ...formData, whatsappNumero: e.target.value })}
                  placeholder="+55 11 99999-9999"
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Formato internacional recomendado
                </p>
              </div>

              <div className="sm:col-span-2">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Webhook de Status
                </label>
                <div className="mt-1 flex gap-2">
                  <input
                    type="text"
                    value={`${window.location.origin}/api/webhook/evolution`}
                    readOnly
                    className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-900 px-3 py-2 text-sm text-gray-600 dark:text-gray-400"
                  />
                  <button
                    onClick={handleCopyWebhook}
                    className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
                  >
                    <Copy className="h-4 w-4" />
                    {copied ? 'Copiado!' : 'Copiar'}
                  </button>
                </div>
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Configure este webhook na Evolution API para receber status de entrega
                </p>
              </div>
            </div>

            <div className="mt-6 flex flex-wrap gap-3">
              <button
                onClick={handleSave}
                disabled={updateMutation.isPending}
                className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {updateMutation.isPending ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Salvando...
                  </>
                ) : (
                  'Salvar Configuração'
                )}
              </button>
            </div>
          </div>
        )}

        {/* Test Section - Only show when connected */}
        {hasInstance && status?.conectado && (
          <div className="mt-6 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-5">
            <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white">
              <Send className="h-4 w-4" />
              Testar Envio
            </h3>

            <div className="mt-4 grid grid-cols-1 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Número de Destino
                </label>
                <input
                  type="text"
                  value={testData.destinatario}
                  onChange={(e) => setTestData({ ...testData, destinatario: e.target.value })}
                  placeholder="5511999999999"
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Apenas números, com código do país (55) e DDD
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Mensagem de Teste
                </label>
                <textarea
                  value={testData.mensagem}
                  onChange={(e) => setTestData({ ...testData, mensagem: e.target.value })}
                  rows={3}
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
              </div>
            </div>

            <div className="mt-4">
              <button
                onClick={handleTest}
                disabled={testMutation.isPending || !testData.destinatario}
                className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {testMutation.isPending ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Enviando...
                  </>
                ) : (
                  <>
                    <Send className="h-4 w-4" />
                    Enviar Teste
                  </>
                )}
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Sidebar */}
      <aside className="space-y-6">
        {/* Status Details */}
        {status && (
          <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 p-4">
            <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Status da Conexão</h3>
            <div className="mt-4 space-y-3 text-sm">
              <div className="flex items-center justify-between rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3">
                <span className="text-gray-600 dark:text-gray-400">Estado</span>
                <span className={`font-medium ${status.conectado ? 'text-green-600 dark:text-green-400' : 'text-gray-500 dark:text-gray-400'}`}>
                  {status.estado || 'Não disponível'}
                </span>
              </div>
              <div className="flex items-center justify-between rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3">
                <span className="text-gray-600 dark:text-gray-400">Disponível</span>
                <span className={`font-medium ${status.disponivel ? 'text-green-600 dark:text-green-400' : 'text-yellow-600 dark:text-yellow-400'}`}>
                  {status.disponivel ? 'Sim' : 'Não'}
                </span>
              </div>
              {status.erro && (
                <div className="rounded-xl border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/30 p-3">
                  <p className="text-xs font-medium text-red-800 dark:text-red-400">Erro</p>
                  <p className="mt-1 text-xs text-red-700 dark:text-red-300">{status.erro}</p>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Checklist */}
        <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 p-4">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Checklist</h3>
          <ul className="mt-3 space-y-2 text-sm text-gray-700 dark:text-gray-300">
            <li className="flex items-start gap-2">
              <span
                className={`mt-1 h-2 w-2 flex-shrink-0 rounded-full ${
                  config?.evolutionApiUrl ? 'bg-green-500' : 'bg-gray-400 dark:bg-gray-600'
                }`}
              />
              Evolution API configurada
            </li>
            <li className="flex items-start gap-2">
              <span
                className={`mt-1 h-2 w-2 flex-shrink-0 rounded-full ${
                  config?.evolutionInstanceName ? 'bg-green-500' : 'bg-gray-400 dark:bg-gray-600'
                }`}
              />
              Instância criada
            </li>
            <li className="flex items-start gap-2">
              <span
                className={`mt-1 h-2 w-2 flex-shrink-0 rounded-full ${
                  status?.conectado ? 'bg-green-500' : 'bg-gray-400 dark:bg-gray-600'
                }`}
              />
              Sessão ativa (QR code escaneado)
            </li>
            <li className="flex items-start gap-2">
              <span
                className={`mt-1 h-2 w-2 flex-shrink-0 rounded-full ${
                  status?.disponivel ? 'bg-green-500' : 'bg-gray-400 dark:bg-gray-600'
                }`}
              />
              Pronto para envio
            </li>
          </ul>

          <div className="mt-4 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3 text-xs text-gray-600 dark:text-gray-400">
            <strong>Dica:</strong> Use "Criar Instância Automaticamente" para configurar rapidamente. A instância será criada usando as configurações globais da Evolution API.
          </div>
        </div>
      </aside>
    </div>
  );
}
