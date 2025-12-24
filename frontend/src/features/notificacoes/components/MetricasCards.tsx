import { Send, XCircle, TrendingUp, Mail, MessageSquare, Smartphone, Bell } from 'lucide-react';
import { useNotificacaoMetricas } from '../hooks/useNotificacoes';
import { TipoNotificacao } from '../types';

export function MetricasCards() {
  const { data: metricas, isLoading, error } = useNotificacaoMetricas();

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="animate-pulse rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-6">
            <div className="h-12 w-12 rounded-lg bg-gray-200 dark:bg-gray-700" />
            <div className="mt-4 h-8 w-20 rounded bg-gray-200 dark:bg-gray-700" />
            <div className="mt-2 h-4 w-32 rounded bg-gray-200 dark:bg-gray-700" />
          </div>
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20 p-4 text-red-800 dark:text-red-400">
        Erro ao carregar métricas. Tente novamente.
      </div>
    );
  }

  if (!metricas) {
    return null;
  }

  const total = metricas.totalEnviadas + metricas.totalFalhas + metricas.totalPendentes;

  const cards = [
    {
      title: 'Total',
      value: total,
      icon: Send,
      color: 'text-blue-600 dark:text-blue-400',
      bgColor: 'bg-blue-100 dark:bg-blue-900/50',
      description: 'Notificações totais',
    },
    {
      title: 'Enviadas',
      value: metricas.totalEnviadas,
      icon: Send,
      color: 'text-green-600 dark:text-green-400',
      bgColor: 'bg-green-100 dark:bg-green-900/50',
      description: 'Notificações enviadas',
    },
    {
      title: 'Falhas',
      value: metricas.totalFalhas,
      icon: XCircle,
      color: 'text-red-600 dark:text-red-400',
      bgColor: 'bg-red-100 dark:bg-red-900/50',
      description: 'Falhas no envio',
    },
    {
      title: 'Taxa de Entrega',
      value: `${metricas.taxaEntrega.toFixed(1)}%`,
      icon: TrendingUp,
      color: 'text-green-600 dark:text-green-400',
      bgColor: 'bg-green-100 dark:bg-green-900/50',
      description: 'Entregas bem-sucedidas',
    },
  ];

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

  const getTipoLabel = (tipo: string) => {
    switch (tipo) {
      case 'EMAIL':
        return 'Email';
      case 'WHATSAPP':
        return 'WhatsApp';
      case 'SMS':
        return 'SMS';
      case 'TELEGRAM':
        return 'Telegram';
      default:
        return tipo;
    }
  };

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {cards.map((card) => {
          const Icon = card.icon;
          return (
            <div
              key={card.title}
              className="rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm transition-shadow hover:shadow-md"
            >
              <div className="flex items-start justify-between">
                <div className={`rounded-lg ${card.bgColor} p-3`}>
                  <Icon className={`h-6 w-6 ${card.color}`} />
                </div>
              </div>
              <div className="mt-4">
                <p className="text-3xl font-bold text-gray-900 dark:text-white">{card.value}</p>
                <p className="mt-1 text-sm font-medium text-gray-600 dark:text-gray-400">{card.title}</p>
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-500">{card.description}</p>
              </div>
            </div>
          );
        })}
      </div>

      <div className="rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">Por Canal</h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
          {(['WHATSAPP', 'TELEGRAM', 'EMAIL', 'SMS'] as TipoNotificacao[]).map((tipo) => {
            const Icon = getTipoIcon(tipo);
            const count = metricas.enviadasPorCanal[tipo] || 0;
            return (
              <div key={tipo} className="flex items-center gap-3 rounded-lg bg-gray-50 dark:bg-gray-900 p-4">
                <Icon className={`h-5 w-5 ${count > 0 ? 'text-blue-600 dark:text-blue-400' : 'text-gray-400 dark:text-gray-600'}`} />
                <div className="flex-1">
                  <p className={`text-sm font-medium ${count > 0 ? 'text-gray-900 dark:text-white' : 'text-gray-500 dark:text-gray-400'}`}>
                    {getTipoLabel(tipo)}
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">{count} notificações</p>
                </div>
                <p className={`text-xl font-bold ${count > 0 ? 'text-gray-900 dark:text-white' : 'text-gray-400 dark:text-gray-600'}`}>{count}</p>
              </div>
            );
          })}
        </div>
      </div>

      {Object.keys(metricas.enviadasPorEvento).length > 0 && (
        <div className="rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-6 shadow-sm">
          <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">Por Evento</h3>
          <div className="space-y-2">
            {Object.entries(metricas.enviadasPorEvento)
              .sort(([, a], [, b]) => b - a)
              .map(([evento, count]) => {
                const totalEventos = Object.values(metricas.enviadasPorEvento).reduce((a, b) => a + b, 0);
                const percentage = totalEventos > 0 ? (count / totalEventos) * 100 : 0;

                return (
                  <div key={evento} className="flex items-center gap-3">
                    <div className="flex-1">
                      <div className="mb-1 flex items-center justify-between text-sm">
                        <span className="font-medium text-gray-700 dark:text-gray-300">
                          {evento.replace(/_/g, ' ')}
                        </span>
                        <span className="text-gray-900 dark:text-white">{count}</span>
                      </div>
                      <div className="h-2 w-full overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
                        <div
                          className="h-full rounded-full bg-blue-600 dark:bg-blue-500 transition-all"
                          style={{ width: `${percentage}%` }}
                        />
                      </div>
                    </div>
                  </div>
                );
              })}
          </div>
        </div>
      )}
    </div>
  );
}
