import { Mail, MessageSquare, Smartphone, Send, Clock, CheckCircle, XCircle, AlertCircle, RotateCcw, Ban, Bell } from 'lucide-react';
import { Modal } from '@/shared/components/ui/Modal';
import { useNotificacao, useRetryNotificacao, useCancelNotificacao } from '../hooks/useNotificacoes';
import { TipoNotificacao, StatusNotificacao } from '../types';

interface NotificacaoDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  notificacaoId: string;
}

export function NotificacaoDetailModal({
  isOpen,
  onClose,
  notificacaoId,
}: NotificacaoDetailModalProps) {
  const { data: notificacao, isLoading } = useNotificacao(notificacaoId);
  const retryMutation = useRetryNotificacao();
  const cancelMutation = useCancelNotificacao();

  const handleRetry = async () => {
    try {
      await retryMutation.mutateAsync(notificacaoId);
      alert('Notificação reenviada com sucesso!');
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao reenviar notificação');
    }
  };

  const handleCancel = async () => {
    if (!confirm('Deseja realmente cancelar esta notificação?')) return;

    try {
      await cancelMutation.mutateAsync(notificacaoId);
      alert('Notificação cancelada com sucesso!');
      onClose();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao cancelar notificação');
    }
  };

  const getTipoIcon = (tipo: TipoNotificacao) => {
    switch (tipo) {
      case 'EMAIL':
        return Mail;
      case 'WHATSAPP':
        return MessageSquare;
      case 'SMS':
        return Smartphone;
      case 'TELEGRAM':
        return Bell;
      default:
        return Send;
    }
  };

  const getTipoLabel = (tipo: TipoNotificacao) => {
    switch (tipo) {
      case 'EMAIL': return 'Email';
      case 'WHATSAPP': return 'WhatsApp';
      case 'SMS': return 'SMS';
      case 'TELEGRAM': return 'Telegram';
      default: return tipo;
    }
  };

  const getStatusConfig = (status: StatusNotificacao) => {
    switch (status) {
      case 'ENVIADO':
        return {
          icon: CheckCircle,
          color: 'text-green-600 dark:text-green-400',
          bgColor: 'bg-green-100 dark:bg-green-900/30',
          label: 'Enviado',
        };
      case 'ENTREGUE':
        return {
          icon: CheckCircle,
          color: 'text-green-700 dark:text-green-400',
          bgColor: 'bg-green-100 dark:bg-green-900/30',
          label: 'Entregue',
        };
      case 'LIDO':
        return {
          icon: CheckCircle,
          color: 'text-blue-600 dark:text-blue-400',
          bgColor: 'bg-blue-100 dark:bg-blue-900/30',
          label: 'Lido',
        };
      case 'PENDENTE':
        return {
          icon: Clock,
          color: 'text-yellow-600 dark:text-yellow-400',
          bgColor: 'bg-yellow-100 dark:bg-yellow-900/30',
          label: 'Pendente',
        };
      case 'AGENDADO':
        return {
          icon: Clock,
          color: 'text-purple-600 dark:text-purple-400',
          bgColor: 'bg-purple-100 dark:bg-purple-900/30',
          label: 'Agendado',
        };
      case 'FALHA':
        return {
          icon: XCircle,
          color: 'text-red-600 dark:text-red-400',
          bgColor: 'bg-red-100 dark:bg-red-900/30',
          label: 'Falha',
        };
      case 'CANCELADO':
        return {
          icon: Ban,
          color: 'text-gray-600 dark:text-gray-400',
          bgColor: 'bg-gray-100 dark:bg-gray-700',
          label: 'Cancelado',
        };
      default:
        return {
          icon: AlertCircle,
          color: 'text-gray-600 dark:text-gray-400',
          bgColor: 'bg-gray-100 dark:bg-gray-700',
          label: status,
        };
    }
  };

  const formatDate = (date: string | number[] | undefined) => {
    if (!date) return '-';

    const dateObj = Array.isArray(date)
      ? new Date(date[0], date[1] - 1, date[2], date[3] || 0, date[4] || 0)
      : new Date(date);

    return dateObj.toLocaleString('pt-BR');
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Detalhes da Notificação" size="lg">
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 dark:border-gray-700 border-t-blue-600 dark:border-t-blue-400" />
        </div>
      ) : notificacao ? (
        <div className="space-y-6">
          {/* Header com tipo e status */}
          <div className="flex items-start justify-between">
            <div className="flex items-start gap-3">
              {(() => {
                const TipoIcon = getTipoIcon(notificacao.tipo);
                return (
                  <div className="rounded-lg bg-gray-100 dark:bg-gray-700 p-2">
                    <TipoIcon className="h-5 w-5 text-gray-600 dark:text-gray-300" />
                  </div>
                );
              })()}
              <div>
                <h3 className="font-semibold text-gray-900 dark:text-white">
                  {getTipoLabel(notificacao.tipo)}
                </h3>
                <p className="text-sm text-gray-500 dark:text-gray-400">ID: {notificacao.id}</p>
              </div>
            </div>
            {(() => {
              const statusConfig = getStatusConfig(notificacao.status);
              const StatusIcon = statusConfig.icon;
              return (
                <div className={`flex items-center gap-2 rounded-lg ${statusConfig.bgColor} px-3 py-1`}>
                  <StatusIcon className={`h-4 w-4 ${statusConfig.color}`} />
                  <span className={`text-sm font-medium ${statusConfig.color}`}>
                    {statusConfig.label}
                  </span>
                </div>
              );
            })()}
          </div>

          {/* Informações principais */}
          <div className="grid grid-cols-2 gap-4 rounded-lg bg-gray-50 dark:bg-gray-800/50 p-4">
            <div>
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Destinatário</p>
              <p className="mt-1 font-mono text-sm text-gray-900 dark:text-white">{notificacao.destinatario}</p>
            </div>
            {notificacao.nomeDestinatario && (
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Nome</p>
                <p className="mt-1 text-sm text-gray-900 dark:text-white">{notificacao.nomeDestinatario}</p>
              </div>
            )}
            <div>
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Tentativas</p>
              <p className="mt-1 text-sm text-gray-900 dark:text-white">{notificacao.tentativas}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Data de Criação</p>
              <p className="mt-1 text-sm text-gray-900 dark:text-white">{formatDate(notificacao.createdAt)}</p>
            </div>
            {notificacao.dataEnvio && (
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Data de Envio</p>
                <p className="mt-1 text-sm text-gray-900 dark:text-white">{formatDate(notificacao.dataEnvio)}</p>
              </div>
            )}
            {notificacao.dataEntrega && (
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Data de Entrega</p>
                <p className="mt-1 text-sm text-gray-900 dark:text-white">{formatDate(notificacao.dataEntrega)}</p>
              </div>
            )}
            {notificacao.dataLeitura && (
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Data de Leitura</p>
                <p className="mt-1 text-sm text-gray-900 dark:text-white">{formatDate(notificacao.dataLeitura)}</p>
              </div>
            )}
            {notificacao.evento && (
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Evento</p>
                <p className="mt-1 text-sm text-gray-900 dark:text-white">
                  {notificacao.evento.replace(/_/g, ' ')}
                </p>
              </div>
            )}
            {notificacao.idExterno && (
              <div className="col-span-2">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">ID Externo (Message ID)</p>
                <p className="mt-1 font-mono text-sm text-gray-900 dark:text-white">{notificacao.idExterno}</p>
              </div>
            )}
          </div>

          {/* Assunto (para email) */}
          {notificacao.assunto && (
            <div>
              <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Assunto</p>
              <p className="mt-1 text-sm text-gray-900 dark:text-white">{notificacao.assunto}</p>
            </div>
          )}

          {/* Mensagem */}
          <div>
            <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Mensagem</p>
            <div className="mt-2 rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4">
              <p className="whitespace-pre-wrap text-sm text-gray-900 dark:text-gray-100">{notificacao.mensagem}</p>
            </div>
          </div>

          {/* Motivo do agendamento */}
          {notificacao.motivoAgendamento && (
            <div className="rounded-lg border border-purple-200 dark:border-purple-800 bg-purple-50 dark:bg-purple-900/20 p-4">
              <p className="text-sm font-medium text-purple-800 dark:text-purple-300">Motivo do Agendamento</p>
              <p className="mt-1 text-sm text-purple-700 dark:text-purple-400">{notificacao.motivoAgendamento}</p>
            </div>
          )}

          {/* Erro */}
          {notificacao.erroMensagem && (
            <div className="rounded-lg border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20 p-4">
              <p className="text-sm font-medium text-red-800 dark:text-red-300">Mensagem de Erro</p>
              <p className="mt-1 text-sm text-red-700 dark:text-red-400">{notificacao.erroMensagem}</p>
              {notificacao.erroCodigo && (
                <p className="mt-1 font-mono text-xs text-red-600 dark:text-red-500">Código: {notificacao.erroCodigo}</p>
              )}
            </div>
          )}

          {/* Resposta da API (para debug) */}
          {notificacao.respostaApi && Object.keys(notificacao.respostaApi).length > 0 && (
            <details className="rounded-lg border border-gray-200 dark:border-gray-700">
              <summary className="cursor-pointer px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800">
                Resposta da API (detalhes técnicos)
              </summary>
              <div className="border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 p-4">
                <pre className="overflow-x-auto text-xs text-gray-600 dark:text-gray-400">
                  {JSON.stringify(notificacao.respostaApi, null, 2)}
                </pre>
              </div>
            </details>
          )}

          {/* Ações */}
          <div className="flex justify-end gap-3 border-t border-gray-200 dark:border-gray-700 pt-4">
            {notificacao.status === 'PENDENTE' && (
              <button
                onClick={handleCancel}
                disabled={cancelMutation.isPending}
                className="flex items-center gap-2 rounded-lg border border-red-600 dark:border-red-500 px-4 py-2 text-sm font-medium text-red-600 dark:text-red-400 transition-colors hover:bg-red-50 dark:hover:bg-red-900/20 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <Ban className="h-4 w-4" />
                Cancelar
              </button>
            )}
            {notificacao.status === 'FALHA' && (
              <button
                onClick={handleRetry}
                disabled={retryMutation.isPending}
                className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <RotateCcw className="h-4 w-4" />
                Reenviar
              </button>
            )}
            <button
              onClick={onClose}
              className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 transition-colors hover:bg-gray-50 dark:hover:bg-gray-600"
            >
              Fechar
            </button>
          </div>
        </div>
      ) : (
        <div className="py-12 text-center text-gray-500 dark:text-gray-400">
          Notificação não encontrada
        </div>
      )}
    </Modal>
  );
}
