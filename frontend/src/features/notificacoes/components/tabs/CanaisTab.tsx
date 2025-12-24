import { Mail, MessageSquare, Smartphone, Bell, ToggleLeft, ToggleRight, CheckCircle, XCircle, AlertCircle } from 'lucide-react';
import { useConfiguracoes, useUpdateConfiguracao } from '../../hooks/useNotificacoes';
import type { UpdateConfiguracaoNotificacaoRequest } from '../../types';

interface CanalConfig {
  key: 'emailHabilitado' | 'whatsappHabilitado' | 'smsHabilitado' | 'telegramHabilitado';
  label: string;
  description: string;
  icon: typeof Mail;
  getStatus: (config: any) => { configurado: boolean; texto: string };
}

const canaisConfig: CanalConfig[] = [
  {
    key: 'emailHabilitado',
    label: 'E-mail automático',
    description: 'Envio via SMTP (confirmações, aprovações e status)',
    icon: Mail,
    getStatus: (config) => ({
      configurado: true,
      texto: config?.temSmtpProprio ? `SMTP: ${config.smtpHost}` : 'Usando servidor padrão',
    }),
  },
  {
    key: 'whatsappHabilitado',
    label: 'WhatsApp automático',
    description: 'Envio via Evolution API (mensagens transacionais e lembretes)',
    icon: MessageSquare,
    getStatus: (config) => ({
      configurado: config?.temEvolutionApiConfigurada ?? false,
      texto: config?.temEvolutionApiConfigurada
        ? `Instância: ${config.evolutionInstanceName}`
        : 'Evolution API não configurada',
    }),
  },
  {
    key: 'smsHabilitado',
    label: 'SMS',
    description: 'Envio de mensagens SMS (em breve)',
    icon: Smartphone,
    getStatus: () => ({
      configurado: false,
      texto: 'Não implementado',
    }),
  },
  {
    key: 'telegramHabilitado',
    label: 'Telegram',
    description: 'Notificações via bot do Telegram',
    icon: Bell,
    getStatus: (config) => ({
      configurado: config?.temTelegramConfigurado ?? false,
      texto: config?.temTelegramConfigurado ? 'Bot configurado' : 'Bot não configurado',
    }),
  },
];

export function CanaisTab() {
  const { data: config, isLoading } = useConfiguracoes();
  const updateMutation = useUpdateConfiguracao();

  const handleToggleCanal = async (
    canal: CanalConfig['key'],
    valor: boolean
  ) => {
    try {
      const request: UpdateConfiguracaoNotificacaoRequest = {
        [canal]: valor,
      };
      await updateMutation.mutateAsync({ data: request });
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao atualizar configuração');
    }
  };

  if (isLoading) {
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
        <h2 className="text-base font-semibold text-gray-900 dark:text-white">Canais de comunicação</h2>
        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
          Defina quais canais esta oficina utilizará para disparos automáticos.
        </p>

        <div className="mt-6 space-y-4">
          {canaisConfig.map((canal) => {
            const Icon = canal.icon;
            const habilitado = config?.[canal.key] ?? false;
            const status = canal.getStatus(config);
            const disabled = !status.configurado && canal.key !== 'emailHabilitado';

            return (
              <div
                key={canal.key}
                className={`flex items-start justify-between gap-4 rounded-xl border p-4 transition-colors ${
                  disabled ? 'opacity-60' : ''
                } ${
                  habilitado && status.configurado
                    ? 'border-blue-200 dark:border-blue-800 bg-blue-50/50 dark:bg-blue-900/20'
                    : habilitado && !status.configurado
                    ? 'border-yellow-200 dark:border-yellow-800 bg-yellow-50/50 dark:bg-yellow-900/20'
                    : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800'
                }`}
              >
                <div className="flex items-start gap-3">
                  <div
                    className={`rounded-lg p-2 ${
                      habilitado ? 'bg-blue-100 dark:bg-blue-900/50' : 'bg-gray-100 dark:bg-gray-700'
                    }`}
                  >
                    <Icon
                      className={`h-5 w-5 ${
                        habilitado ? 'text-blue-600 dark:text-blue-400' : 'text-gray-500 dark:text-gray-400'
                      }`}
                    />
                  </div>
                  <div>
                    <p className="font-medium text-gray-900 dark:text-white">{canal.label}</p>
                    <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">{canal.description}</p>
                    <p className="mt-1 text-xs text-gray-500 dark:text-gray-500">{status.texto}</p>
                  </div>
                </div>

                <button
                  onClick={() => handleToggleCanal(canal.key, !habilitado)}
                  disabled={disabled || updateMutation.isPending}
                  className="flex-shrink-0 disabled:cursor-not-allowed"
                  title={
                    disabled
                      ? 'Configure primeiro'
                      : habilitado
                      ? 'Desativar'
                      : 'Ativar'
                  }
                >
                  {habilitado ? (
                    <ToggleRight className="h-8 w-8 text-blue-600 dark:text-blue-400" />
                  ) : (
                    <ToggleLeft className="h-8 w-8 text-gray-400 dark:text-gray-500" />
                  )}
                </button>
              </div>
            );
          })}

          <div className="rounded-xl bg-gray-50 dark:bg-gray-900 p-4 text-sm text-gray-700 dark:text-gray-300">
            <p className="font-medium">Observação</p>
            <p className="mt-1">
              As mensagens só serão enviadas pelos canais habilitados. Configure cada
              canal na aba correspondente antes de ativar.
            </p>
          </div>
        </div>
      </div>

      {/* Status Sidebar */}
      <aside className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 p-4">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Status rápido</h3>
        <div className="mt-4 space-y-3 text-sm">
          {canaisConfig.map((canal) => {
            const habilitado = config?.[canal.key] ?? false;
            const status = canal.getStatus(config);

            let statusColor = 'text-gray-500 dark:text-gray-400';
            let StatusIcon = XCircle;
            let statusText = 'Desativado';
            let bgColor = 'bg-gray-100 dark:bg-gray-800';

            if (habilitado && status.configurado) {
              statusColor = 'text-green-700 dark:text-green-400';
              StatusIcon = CheckCircle;
              statusText = 'Ativo';
              bgColor = 'bg-green-50 dark:bg-green-900/30';
            } else if (habilitado && !status.configurado) {
              statusColor = 'text-yellow-700 dark:text-yellow-400';
              StatusIcon = AlertCircle;
              statusText = 'Pendente';
              bgColor = 'bg-yellow-50 dark:bg-yellow-900/30';
            } else if (!status.configurado) {
              statusColor = 'text-gray-500 dark:text-gray-400';
              StatusIcon = XCircle;
              statusText = 'Não configurado';
              bgColor = 'bg-gray-50 dark:bg-gray-800';
            }

            return (
              <div
                key={canal.key}
                className="flex items-center justify-between rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3"
              >
                <span className="text-gray-700 dark:text-gray-300">{canal.label.split(' ')[0]}</span>
                <span
                  className={`inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-xs font-medium ${bgColor} ${statusColor}`}
                >
                  <StatusIcon className="h-3 w-3" />
                  {statusText}
                </span>
              </div>
            );
          })}
        </div>

        <div className="mt-4 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3 text-xs text-gray-600 dark:text-gray-400">
          <strong>Dica:</strong> Mantenha os disparos transacionais sempre habilitados
          para reduzir ligações e retrabalho.
        </div>
      </aside>
    </div>
  );
}
