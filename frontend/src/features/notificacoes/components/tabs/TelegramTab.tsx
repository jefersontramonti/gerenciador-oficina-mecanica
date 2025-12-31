import { useState, useEffect } from 'react';
import { Send, CheckCircle, XCircle, AlertCircle, Loader2, ExternalLink, Key, ShieldCheck } from 'lucide-react';
import { useConfiguracoes, useConfigurarTelegram, useTelegramStatus, useTestarNotificacao } from '../../hooks/useNotificacoes';

export function TelegramTab() {
  const { data: config, isLoading: configLoading } = useConfiguracoes();
  const { data: status, isLoading: statusLoading } = useTelegramStatus();
  const configurarMutation = useConfigurarTelegram();
  const testMutation = useTestarNotificacao();

  const [formData, setFormData] = useState({
    botToken: '',
    chatId: '',
  });

  const [testData, setTestData] = useState({
    mensagem: 'Mensagem de teste do sistema PitStop',
  });

  // Initialize form data when config loads
  useEffect(() => {
    if (config) {
      setFormData({
        botToken: '', // Never show token
        chatId: config.telegramChatId || '',
      });
    }
  }, [config]);

  const handleSave = async () => {
    if (!formData.botToken && !config?.temTelegramConfigurado) {
      alert('Informe o Bot Token');
      return;
    }
    if (!formData.chatId) {
      alert('Informe o Chat ID');
      return;
    }

    try {
      await configurarMutation.mutateAsync({
        botToken: formData.botToken || undefined!,
        chatId: formData.chatId,
      });
      setFormData(prev => ({ ...prev, botToken: '' })); // Clear token after save
      alert('Configuracao salva com sucesso!');
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao salvar configuracao');
    }
  };

  const handleTest = async () => {
    try {
      const result = await testMutation.mutateAsync({
        tipo: 'TELEGRAM',
        destinatario: formData.chatId || config?.telegramChatId || '',
        mensagem: testData.mensagem,
      });
      if (result.sucesso) {
        alert('Teste enviado com sucesso! Verifique o Telegram.');
      } else {
        alert(`Erro no teste: ${result.mensagem}`);
      }
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao enviar teste');
    }
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

    if (!status || !config?.temTelegramConfigurado) {
      return {
        icon: AlertCircle,
        color: 'text-yellow-700 dark:text-yellow-400',
        bgColor: 'bg-yellow-50 dark:bg-yellow-900/30',
        borderColor: 'border-yellow-200 dark:border-yellow-800',
        label: 'Nao configurado',
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
            <h2 className="text-base font-semibold text-gray-900 dark:text-white">Telegram Bot</h2>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              Configure seu bot do Telegram para enviar notificacoes.
            </p>
          </div>

          <span
            className={`inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-medium ${statusConfig.bgColor} ${statusConfig.color} ${statusConfig.borderColor}`}
          >
            <StatusIcon className={`h-3.5 w-3.5 ${statusConfig.animate ? 'animate-spin' : ''}`} />
            {statusConfig.label}
          </span>
        </div>

        {/* Configuration Form */}
        <div className="mt-6 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-5">
          <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white">
            <Send className="h-4 w-4" />
            Configuracao do Bot
          </h3>

          <div className="mt-4 grid grid-cols-1 gap-4">
            <div>
              <div className="flex items-center justify-between">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Bot Token
                </label>
                {config?.temTelegramConfigurado && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-green-100 dark:bg-green-900/30 px-2 py-0.5 text-xs font-medium text-green-700 dark:text-green-400">
                    <ShieldCheck className="h-3 w-3" />
                    Configurado
                  </span>
                )}
              </div>

              {config?.temTelegramConfigurado ? (
                <div className="mt-1 space-y-2">
                  <div className="flex items-center gap-2 rounded-lg border border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-900/20 px-3 py-2">
                    <Key className="h-4 w-4 text-green-600 dark:text-green-400" />
                    <span className="text-sm text-green-700 dark:text-green-300">
                      Token salvo com segurança
                    </span>
                  </div>
                  <input
                    type="password"
                    value={formData.botToken}
                    onChange={(e) => setFormData({ ...formData, botToken: e.target.value })}
                    placeholder="Cole um novo token para substituir"
                    className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  />
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    Deixe em branco para manter o token atual, ou cole um novo para substituir
                  </p>
                </div>
              ) : (
                <>
                  <input
                    type="password"
                    value={formData.botToken}
                    onChange={(e) => setFormData({ ...formData, botToken: e.target.value })}
                    placeholder="Cole o token do seu bot aqui"
                    className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  />
                  <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                    Obtenha o token criando um bot no{' '}
                    <a
                      href="https://t.me/BotFather"
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-blue-600 dark:text-blue-400 hover:underline"
                    >
                      @BotFather
                    </a>
                  </p>
                </>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Chat ID
              </label>
              <input
                type="text"
                value={formData.chatId}
                onChange={(e) => setFormData({ ...formData, chatId: e.target.value })}
                placeholder="-1001234567890"
                className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                ID do grupo ou canal onde o bot enviara notificacoes
              </p>
            </div>
          </div>

          <div className="mt-6 flex flex-wrap gap-3">
            <button
              onClick={handleSave}
              disabled={configurarMutation.isPending}
              className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {configurarMutation.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Salvando...
                </>
              ) : (
                'Salvar'
              )}
            </button>
          </div>
        </div>

        {/* Test Section */}
        <div className="mt-6 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-5">
          <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white">
            <Send className="h-4 w-4" />
            Testar Envio
          </h3>

          <div className="mt-4 grid grid-cols-1 gap-4">
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
              disabled={testMutation.isPending || !config?.temTelegramConfigurado}
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
            {!config?.temTelegramConfigurado && (
              <p className="mt-2 text-xs text-yellow-600 dark:text-yellow-400">
                Configure o bot antes de enviar testes
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Sidebar */}
      <aside className="space-y-6">
        {/* Status Details */}
        {status?.conectado && (
          <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 p-4">
            <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Informacoes do Bot</h3>
            <div className="mt-4 space-y-3 text-sm">
              {status.botNome && (
                <div className="flex items-center justify-between rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3">
                  <span className="text-gray-600 dark:text-gray-400">Nome</span>
                  <span className="font-medium text-gray-900 dark:text-white">{status.botNome}</span>
                </div>
              )}
              {status.botUsername && (
                <div className="flex items-center justify-between rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3">
                  <span className="text-gray-600 dark:text-gray-400">Username</span>
                  <a
                    href={`https://t.me/${status.botUsername.replace('@', '')}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-1 font-medium text-blue-600 dark:text-blue-400 hover:underline"
                  >
                    {status.botUsername}
                    <ExternalLink className="h-3 w-3" />
                  </a>
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
                  config?.temTelegramConfigurado ? 'bg-green-500' : 'bg-gray-400 dark:bg-gray-600'
                }`}
              />
              Bot Token configurado
            </li>
            <li className="flex items-start gap-2">
              <span
                className={`mt-1 h-2 w-2 flex-shrink-0 rounded-full ${
                  config?.telegramChatId ? 'bg-green-500' : 'bg-gray-400 dark:bg-gray-600'
                }`}
              />
              Chat ID configurado
            </li>
            <li className="flex items-start gap-2">
              <span
                className={`mt-1 h-2 w-2 flex-shrink-0 rounded-full ${
                  status?.conectado ? 'bg-green-500' : 'bg-gray-400 dark:bg-gray-600'
                }`}
              />
              Bot respondendo
            </li>
          </ul>
        </div>

        {/* Instructions */}
        <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 p-4">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Como configurar</h3>
          <ol className="mt-3 space-y-3 text-sm text-gray-700 dark:text-gray-300">
            <li className="flex items-start gap-2">
              <span className="flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/50 text-xs font-medium text-blue-600 dark:text-blue-400">
                1
              </span>
              <span>
                Acesse{' '}
                <a
                  href="https://t.me/BotFather"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-600 dark:text-blue-400 hover:underline"
                >
                  @BotFather
                </a>{' '}
                no Telegram
              </span>
            </li>
            <li className="flex items-start gap-2">
              <span className="flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/50 text-xs font-medium text-blue-600 dark:text-blue-400">
                2
              </span>
              <span>Envie /newbot e siga as instrucoes</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/50 text-xs font-medium text-blue-600 dark:text-blue-400">
                3
              </span>
              <span>Copie o Token fornecido e cole acima</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/50 text-xs font-medium text-blue-600 dark:text-blue-400">
                4
              </span>
              <span>Adicione o bot ao grupo/canal desejado</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/50 text-xs font-medium text-blue-600 dark:text-blue-400">
                5
              </span>
              <span>
                Use{' '}
                <a
                  href="https://t.me/userinfobot"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-600 dark:text-blue-400 hover:underline"
                >
                  @userinfobot
                </a>{' '}
                para descobrir o Chat ID
              </span>
            </li>
          </ol>
        </div>
      </aside>
    </div>
  );
}
