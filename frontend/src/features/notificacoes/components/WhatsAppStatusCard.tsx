import { Smartphone, AlertCircle, CheckCircle, XCircle, Loader2 } from 'lucide-react';
import { useWhatsAppStatus, useConnectWhatsApp, useDisconnectWhatsApp } from '../hooks/useNotificacoes';

export function WhatsAppStatusCard() {
  const { data: status, isLoading, error } = useWhatsAppStatus();
  const connectMutation = useConnectWhatsApp();
  const disconnectMutation = useDisconnectWhatsApp();

  const handleConnect = async () => {
    try {
      await connectMutation.mutateAsync();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao conectar WhatsApp');
    }
  };

  const handleDisconnect = async () => {
    if (!confirm('Deseja realmente desconectar o WhatsApp?')) return;

    try {
      await disconnectMutation.mutateAsync();
      alert('WhatsApp desconectado com sucesso');
    } catch (error: any) {
      alert(error.response?.data?.message || 'Erro ao desconectar WhatsApp');
    }
  };

  const getStatusConfig = () => {
    if (isLoading) {
      return {
        icon: Loader2,
        color: 'text-gray-500',
        bgColor: 'bg-gray-100',
        label: 'Verificando...',
      };
    }

    if (error || !status) {
      return {
        icon: AlertCircle,
        color: 'text-red-600',
        bgColor: 'bg-red-100',
        label: 'Erro',
      };
    }

    if (status.conectado) {
      return {
        icon: CheckCircle,
        color: 'text-green-600',
        bgColor: 'bg-green-100',
        label: 'Conectado',
      };
    }

    return {
      icon: XCircle,
      color: 'text-yellow-600',
      bgColor: 'bg-yellow-100',
      label: 'Desconectado',
    };
  };

  const statusConfig = getStatusConfig();
  const StatusIcon = statusConfig.icon;

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <div className="mb-4 flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className={`rounded-lg ${statusConfig.bgColor} p-3`}>
            <Smartphone className={`h-6 w-6 ${statusConfig.color}`} />
          </div>
          <div>
            <h3 className="font-semibold text-gray-900">WhatsApp Business</h3>
            <p className="text-sm text-gray-500">Status da integração</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <StatusIcon className={`h-5 w-5 ${statusConfig.color}`} />
          <span className={`text-sm font-medium ${statusConfig.color}`}>
            {statusConfig.label}
          </span>
        </div>
      </div>

      {status && status.conectado && (
        <div className="mb-4 space-y-2 rounded-lg bg-gray-50 p-3">
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">Estado:</span>
            <span className="font-medium text-green-600">{status.estado}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">Disponível:</span>
            <span className={`font-medium ${status.disponivel ? 'text-green-600' : 'text-yellow-600'}`}>
              {status.disponivel ? 'Sim' : 'Não'}
            </span>
          </div>
        </div>
      )}

      {status && status.erro && (
        <div className="mb-4 rounded-lg bg-red-50 p-3">
          <p className="text-sm text-red-800">{status.erro}</p>
        </div>
      )}

      <div className="flex gap-3">
        {!status?.conectado ? (
          <button
            onClick={handleConnect}
            disabled={connectMutation.isPending || isLoading}
            className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-green-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {connectMutation.isPending ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Conectando...
              </>
            ) : (
              <>
                <Smartphone className="h-4 w-4" />
                Conectar WhatsApp
              </>
            )}
          </button>
        ) : (
          <button
            onClick={handleDisconnect}
            disabled={disconnectMutation.isPending}
            className="flex flex-1 items-center justify-center gap-2 rounded-lg border border-red-600 px-4 py-2 text-sm font-medium text-red-600 transition-colors hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {disconnectMutation.isPending ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Desconectando...
              </>
            ) : (
              <>
                <XCircle className="h-4 w-4" />
                Desconectar
              </>
            )}
          </button>
        )}
      </div>
    </div>
  );
}
