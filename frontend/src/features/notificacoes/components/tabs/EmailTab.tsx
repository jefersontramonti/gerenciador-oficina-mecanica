import { useState, useEffect } from 'react';
import { Mail, Send, Loader2, CheckCircle, Server, Lock } from 'lucide-react';
import { useConfiguracoes, useUpdateConfiguracao, useTestarNotificacao } from '../../hooks/useNotificacoes';
import type { UpdateConfiguracaoNotificacaoRequest } from '../../types';
import { useFeatureFlag } from '@/shared/hooks/useFeatureFlag';

export function EmailTab() {
  const { data: config, isLoading: configLoading } = useConfiguracoes();
  const updateMutation = useUpdateConfiguracao();
  const testMutation = useTestarNotificacao();
  const canUseCustomSmtp = useFeatureFlag('SMTP_CUSTOMIZADO');

  const [useCustomSmtp, setUseCustomSmtp] = useState(false);

  const [formData, setFormData] = useState({
    smtpHost: '',
    smtpPort: 587,
    smtpUsername: '',
    smtpPassword: '',
    smtpUsarTls: true,
    emailRemetente: '',
    emailRemetenteNome: '',
  });

  const [testData, setTestData] = useState({
    destinatario: '',
    mensagem: 'Mensagem de teste do sistema PitStop',
  });

  useEffect(() => {
    if (config) {
      setUseCustomSmtp(config.temSmtpProprio || false);
      setFormData({
        smtpHost: config.smtpHost || '',
        smtpPort: config.smtpPort || 587,
        smtpUsername: config.smtpUsername || '',
        smtpPassword: '',
        smtpUsarTls: config.smtpUsarTls ?? true,
        emailRemetente: config.emailRemetente || '',
        emailRemetenteNome: config.emailRemetenteNome || '',
      });
    }
  }, [config]);

  const handleSave = async () => {
    try {
      const request: UpdateConfiguracaoNotificacaoRequest = useCustomSmtp
        ? {
            smtpHost: formData.smtpHost || undefined,
            smtpPort: formData.smtpPort || undefined,
            smtpUsername: formData.smtpUsername || undefined,
            smtpPassword: formData.smtpPassword || undefined,
            smtpUsarTls: formData.smtpUsarTls,
            emailRemetente: formData.emailRemetente || undefined,
            emailRemetenteNome: formData.emailRemetenteNome || undefined,
          }
        : {
            smtpHost: undefined,
            smtpPort: undefined,
            smtpUsername: undefined,
            smtpPassword: undefined,
            emailRemetente: formData.emailRemetente || undefined,
            emailRemetenteNome: formData.emailRemetenteNome || undefined,
          };

      await updateMutation.mutateAsync({ data: request });
      alert('Configuração salva com sucesso!');
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao salvar configuração');
    }
  };

  const handleTest = async () => {
    if (!testData.destinatario) {
      alert('Informe o e-mail de destino para o teste');
      return;
    }

    try {
      const result = await testMutation.mutateAsync({
        tipo: 'EMAIL',
        destinatario: testData.destinatario,
        mensagem: testData.mensagem,
      });
      if (result.sucesso) {
        alert('E-mail de teste enviado! Verifique a caixa de entrada.');
      } else {
        alert(`Erro no teste: ${result.mensagem}`);
      }
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao enviar teste');
    }
  };

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
        <h2 className="text-base font-semibold text-gray-900 dark:text-white">E-mail (SMTP)</h2>
        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
          Configure o servidor SMTP para disparos automáticos de e-mail.
        </p>

        {/* SMTP Mode Selection */}
        <div className="mt-6 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-5">
          <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white">
            <Server className="h-4 w-4" />
            Servidor de E-mail
          </h3>

          <div className="mt-4 space-y-3">
            <label className="flex items-start gap-3 rounded-lg border border-gray-200 dark:border-gray-700 p-4 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700">
              <input
                type="radio"
                name="smtpMode"
                checked={!useCustomSmtp}
                onChange={() => setUseCustomSmtp(false)}
                className="mt-0.5 h-4 w-4 text-blue-600 focus:ring-blue-500"
              />
              <div>
                <p className="font-medium text-gray-900 dark:text-white">Servidor Padrão</p>
                <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
                  Usar o servidor SMTP padrão do sistema (recomendado para a maioria dos casos)
                </p>
              </div>
            </label>

            <label
              className={`flex items-start gap-3 rounded-lg border p-4 ${
                canUseCustomSmtp
                  ? 'border-gray-200 dark:border-gray-700 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700'
                  : 'border-gray-200 dark:border-gray-700 cursor-not-allowed opacity-60'
              }`}
            >
              <input
                type="radio"
                name="smtpMode"
                checked={useCustomSmtp}
                onChange={() => canUseCustomSmtp && setUseCustomSmtp(true)}
                disabled={!canUseCustomSmtp}
                className="mt-0.5 h-4 w-4 text-blue-600 focus:ring-blue-500 disabled:cursor-not-allowed"
              />
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <p className="font-medium text-gray-900 dark:text-white">SMTP Personalizado</p>
                  {!canUseCustomSmtp && (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-400 border border-purple-200 dark:border-purple-800">
                      <Lock className="h-3 w-3" />
                      Profissional
                    </span>
                  )}
                </div>
                <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
                  Configurar servidor SMTP próprio (Gmail, Outlook, domínio próprio, etc.)
                </p>
              </div>
            </label>
          </div>
        </div>

        {/* Custom SMTP Configuration */}
        {useCustomSmtp && (
          <div className="mt-6 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-5">
            <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white">
              <Mail className="h-4 w-4" />
              Configuração SMTP
            </h3>

            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Servidor SMTP
                </label>
                <input
                  type="text"
                  value={formData.smtpHost}
                  onChange={(e) => setFormData({ ...formData, smtpHost: e.target.value })}
                  placeholder="smtp.gmail.com"
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Porta
                </label>
                <input
                  type="number"
                  value={formData.smtpPort}
                  onChange={(e) => setFormData({ ...formData, smtpPort: parseInt(e.target.value) || 587 })}
                  placeholder="587"
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Usuário
                </label>
                <input
                  type="text"
                  value={formData.smtpUsername}
                  onChange={(e) => setFormData({ ...formData, smtpUsername: e.target.value })}
                  placeholder="seu-email@gmail.com"
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Senha
                </label>
                <input
                  type="password"
                  value={formData.smtpPassword}
                  onChange={(e) => setFormData({ ...formData, smtpPassword: e.target.value })}
                  placeholder="••••••••••••"
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Para Gmail, use uma senha de app
                </p>
              </div>

              <div className="sm:col-span-2">
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={formData.smtpUsarTls}
                    onChange={(e) => setFormData({ ...formData, smtpUsarTls: e.target.checked })}
                    className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                  <span className="text-sm text-gray-700 dark:text-gray-300">Usar TLS (STARTTLS)</span>
                </label>
              </div>
            </div>
          </div>
        )}

        {/* Sender Configuration */}
        <div className="mt-6 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-5">
          <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white">
            <Mail className="h-4 w-4" />
            Remetente
          </h3>

          <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                E-mail Remetente
              </label>
              <input
                type="email"
                value={formData.emailRemetente}
                onChange={(e) => setFormData({ ...formData, emailRemetente: e.target.value })}
                placeholder="contato@suaoficina.com.br"
                className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Nome do Remetente
              </label>
              <input
                type="text"
                value={formData.emailRemetenteNome}
                onChange={(e) => setFormData({ ...formData, emailRemetenteNome: e.target.value })}
                placeholder="Oficina Auto Center"
                className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
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
                'Salvar'
              )}
            </button>
          </div>
        </div>

        {/* Test Section */}
        <div className="mt-6 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-5">
          <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white">
            <Send className="h-4 w-4" />
            Testar E-mail
          </h3>

          <div className="mt-4 grid grid-cols-1 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                E-mail de Destino
              </label>
              <input
                type="email"
                value={testData.destinatario}
                onChange={(e) => setTestData({ ...testData, destinatario: e.target.value })}
                placeholder="teste@exemplo.com"
                className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
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
      </div>

      {/* Sidebar */}
      <aside className="space-y-6">
        {/* Status */}
        <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 p-4">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Status</h3>
          <div className="mt-4 space-y-3 text-sm">
            <div className="flex items-center justify-between rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3">
              <span className="text-gray-600 dark:text-gray-400">Modo</span>
              <span className="font-medium text-gray-900 dark:text-white">
                {useCustomSmtp ? 'SMTP Próprio' : 'Padrão'}
              </span>
            </div>
            {config?.emailHabilitado && (
              <div className="flex items-center justify-between rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3">
                <span className="text-gray-600 dark:text-gray-400">E-mail</span>
                <span className="inline-flex items-center gap-1.5 text-green-700 dark:text-green-400">
                  <CheckCircle className="h-3.5 w-3.5" />
                  Ativo
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Best Practices */}
        <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 p-4">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Boas Práticas</h3>
          <ul className="mt-3 space-y-2 text-sm text-gray-700 dark:text-gray-300">
            <li className="flex items-start gap-2">
              <span className="mt-1 h-2 w-2 flex-shrink-0 rounded-full bg-gray-400 dark:bg-gray-600" />
              Usar TLS (STARTTLS) quando possível
            </li>
            <li className="flex items-start gap-2">
              <span className="mt-1 h-2 w-2 flex-shrink-0 rounded-full bg-gray-400 dark:bg-gray-600" />
              Configurar SPF/DKIM no domínio
            </li>
            <li className="flex items-start gap-2">
              <span className="mt-1 h-2 w-2 flex-shrink-0 rounded-full bg-gray-400 dark:bg-gray-600" />
              Para Gmail, usar senha de app
            </li>
            <li className="flex items-start gap-2">
              <span className="mt-1 h-2 w-2 flex-shrink-0 rounded-full bg-gray-400 dark:bg-gray-600" />
              Implementar fila e retentativas
            </li>
          </ul>

          <div className="mt-4 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3 text-xs text-gray-600 dark:text-gray-400">
            <strong>Dica:</strong> Use um e-mail de domínio próprio para melhor
            entregabilidade e profissionalismo.
          </div>
        </div>
      </aside>
    </div>
  );
}
