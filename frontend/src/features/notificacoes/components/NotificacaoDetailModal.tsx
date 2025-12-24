import { Mail, MessageSquare, Smartphone, Send, Clock, CheckCircle, XCircle, AlertCircle, RotateCcw, Ban } from 'lucide-react';
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
      default:
        return Send;
    }
  };

  const getStatusConfig = (status: StatusNotificacao) => {
    switch (status) {
      case 'ENVIADO':
        return {
          icon: CheckCircle,
          color: 'text-green-600',
          bgColor: 'bg-green-100',
          label: 'Enviado',
        };
      case 'ENTREGUE':
        return {
          icon: CheckCircle,
          color: 'text-green-700',
          bgColor: 'bg-green-100',
          label: 'Entregue',
        };
      case 'LIDO':
        return {
          icon: CheckCircle,
          color: 'text-blue-600',
          bgColor: 'bg-blue-100',
          label: 'Lido',
        };
      case 'PENDENTE':
        return {
          icon: Clock,
          color: 'text-yellow-600',
          bgColor: 'bg-yellow-100',
          label: 'Pendente',
        };
      case 'AGENDADO':
        return {
          icon: Clock,
          color: 'text-purple-600',
          bgColor: 'bg-purple-100',
          label: 'Agendado',
        };
      case 'FALHA':
        return {
          icon: XCircle,
          color: 'text-red-600',
          bgColor: 'bg-red-100',
          label: 'Falha',
        };
      case 'CANCELADO':
        return {
          icon: Ban,
          color: 'text-gray-600',
          bgColor: 'bg-gray-100',
          label: 'Cancelado',
        };
      default:
        return {
          icon: AlertCircle,
          color: 'text-gray-600',
          bgColor: 'bg-gray-100',
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
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 border-t-blue-600" />
        </div>
      ) : notificacao ? (
        <div className="space-y-6">
          <div className="flex items-start justify-between">
            <div className="flex items-start gap-3">
              {(() => {
                const TipoIcon = getTipoIcon(notificacao.tipo);
                return (
                  <div className="rounded-lg bg-gray-100 p-2">
                    <TipoIcon className="h-5 w-5 text-gray-600" />
                  </div>
                );
              })()}
              <div>
                <h3 className="font-semibold text-gray-900">
                  {notificacao.tipo === 'EMAIL'
                    ? 'Email'
                    : notificacao.tipo === 'WHATSAPP'
                    ? 'WhatsApp'
                    : notificacao.tipo === 'SMS'
                    ? 'SMS'
                    : notificacao.tipo}
                </h3>
                <p className="text-sm text-gray-500">ID: {notificacao.id}</p>
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

          <div className="grid grid-cols-2 gap-4 rounded-lg bg-gray-50 p-4">
            <div>
              <p className="text-sm font-medium text-gray-500">Destinatário</p>
              <p className="mt-1 font-mono text-sm text-gray-900">{notificacao.destinatario}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Tentativas</p>
              <p className="mt-1 text-sm text-gray-900">{notificacao.tentativas}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Data de Criação</p>
              <p className="mt-1 text-sm text-gray-900">{formatDate(notificacao.createdAt)}</p>
            </div>
            {notificacao.dataEnvio && (
              <div>
                <p className="text-sm font-medium text-gray-500">Data de Envio</p>
                <p className="mt-1 text-sm text-gray-900">{formatDate(notificacao.dataEnvio)}</p>
              </div>
            )}
            {notificacao.dataLeitura && (
              <div>
                <p className="text-sm font-medium text-gray-500">Data de Leitura</p>
                <p className="mt-1 text-sm text-gray-900">{formatDate(notificacao.dataLeitura)}</p>
              </div>
            )}
            {notificacao.evento && (
              <div>
                <p className="text-sm font-medium text-gray-500">Evento</p>
                <p className="mt-1 text-sm text-gray-900">
                  {notificacao.evento.replace(/_/g, ' ')}
                </p>
              </div>
            )}
          </div>

          {notificacao.assunto && (
            <div>
              <p className="text-sm font-medium text-gray-700">Assunto</p>
              <p className="mt-1 text-sm text-gray-900">{notificacao.assunto}</p>
            </div>
          )}

          <div>
            <p className="text-sm font-medium text-gray-700">Mensagem</p>
            <div className="mt-2 rounded-lg border border-gray-200 bg-white p-4">
              <p className="whitespace-pre-wrap text-sm text-gray-900">{notificacao.mensagem}</p>
            </div>
          </div>

          {notificacao.erroMensagem && (
            <div className="rounded-lg border border-red-200 bg-red-50 p-4">
              <p className="text-sm font-medium text-red-800">Mensagem de Erro</p>
              <p className="mt-1 text-sm text-red-700">{notificacao.erroMensagem}</p>
            </div>
          )}

          <div className="flex justify-end gap-3 border-t border-gray-200 pt-4">
            {notificacao.status === 'PENDENTE' && (
              <button
                onClick={handleCancel}
                disabled={cancelMutation.isPending}
                className="flex items-center gap-2 rounded-lg border border-red-600 px-4 py-2 text-sm font-medium text-red-600 transition-colors hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
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
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              Fechar
            </button>
          </div>
        </div>
      ) : (
        <div className="py-12 text-center text-gray-500">
          Notificação não encontrada
        </div>
      )}
    </Modal>
  );
}
