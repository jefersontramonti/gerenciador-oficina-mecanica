/**
 * Barra de alertas dinâmicos do dashboard
 * Exibe alertas sobre pagamentos vencidos, manutenções pendentes e peças críticas
 * Estilo: Banners coloridos com animação slide-down
 */

import { memo } from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, AlertTriangle, Clock, Package, Wrench } from 'lucide-react';
import { useDashboardAlertas } from '../hooks/useDashboardWidgets';
import type { DashboardAlertas, AlertaItem, AlertaType } from '../types';

interface AlertBannerProps {
  alert: AlertaItem;
}

const alertStyles: Record<AlertaType, {
  bg: string;
  border: string;
  titleColor: string;
  subtitleColor: string;
  iconColor: string;
  buttonBg: string;
  buttonHover: string;
}> = {
  danger: {
    bg: 'bg-red-500/10 dark:bg-red-500/10',
    border: 'border-red-500/30 dark:border-red-500/30',
    titleColor: 'text-red-600 dark:text-red-400',
    subtitleColor: 'text-red-500/70 dark:text-gray-400',
    iconColor: 'text-red-500 dark:text-red-500',
    buttonBg: 'bg-red-500 dark:bg-red-600',
    buttonHover: 'hover:bg-red-600 dark:hover:bg-red-700',
  },
  warning: {
    bg: 'bg-yellow-500/10 dark:bg-yellow-500/10',
    border: 'border-yellow-500/30 dark:border-yellow-500/30',
    titleColor: 'text-yellow-600 dark:text-yellow-400',
    subtitleColor: 'text-yellow-500/70 dark:text-gray-400',
    iconColor: 'text-yellow-500 dark:text-yellow-500',
    buttonBg: 'bg-yellow-500 dark:bg-yellow-600',
    buttonHover: 'hover:bg-yellow-600 dark:hover:bg-yellow-700',
  },
  info: {
    bg: 'bg-blue-500/10 dark:bg-blue-500/10',
    border: 'border-blue-500/30 dark:border-blue-500/30',
    titleColor: 'text-blue-600 dark:text-blue-400',
    subtitleColor: 'text-blue-500/70 dark:text-gray-400',
    iconColor: 'text-blue-500 dark:text-blue-500',
    buttonBg: 'bg-blue-500 dark:bg-blue-600',
    buttonHover: 'hover:bg-blue-600 dark:hover:bg-blue-700',
  },
};

const getAlertIcon = (iconName: string) => {
  switch (iconName) {
    case 'AlertCircle':
      return AlertCircle;
    case 'AlertTriangle':
      return AlertTriangle;
    case 'Clock':
      return Clock;
    case 'Package':
      return Package;
    case 'Wrench':
      return Wrench;
    default:
      return AlertTriangle;
  }
};

const AlertBanner = memo(({ alert }: AlertBannerProps) => {
  const styles = alertStyles[alert.type];
  const IconComponent = getAlertIcon(alert.icon);

  return (
    <div
      className={`${styles.bg} ${styles.border} border rounded-lg p-3 sm:p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 animate-[slideDown_0.5s_ease-out]`}
    >
      <div className="flex items-center gap-3">
        <IconComponent className={`h-5 w-5 ${styles.iconColor} flex-shrink-0`} />
        <div>
          <p className={`font-semibold ${styles.titleColor}`}>
            {alert.emoji} {alert.count} {alert.message}
          </p>
          {alert.subtitle && (
            <p className={`text-sm ${styles.subtitleColor}`}>
              {alert.subtitle}
            </p>
          )}
        </div>
      </div>
      <Link
        to={alert.link}
        className={`${styles.buttonBg} ${styles.buttonHover} text-white px-4 py-2 rounded-lg transition-all duration-200 text-sm font-medium whitespace-nowrap hover:shadow-md active:scale-95`}
      >
        {alert.buttonText}
      </Link>
    </div>
  );
});

AlertBanner.displayName = 'AlertBanner';

const buildAlertsFromData = (data: DashboardAlertas): AlertaItem[] => {
  const alerts: AlertaItem[] = [];

  if (data.pagamentosVencidos > 0) {
    alerts.push({
      id: 'pagamentos-vencidos',
      type: 'danger',
      icon: 'AlertCircle',
      emoji: '💰',
      message: data.pagamentosVencidos === 1 ? 'pagamento vencido' : 'pagamentos vencidos',
      subtitle: 'Clientes aguardando cobrança',
      count: data.pagamentosVencidos,
      link: '/financeiro?status=VENCIDO',
      buttonText: 'Ver clientes',
    });
  }

  if (data.manutencoesAtrasadas > 0) {
    alerts.push({
      id: 'manutencoes-atrasadas',
      type: 'warning',
      icon: 'AlertTriangle',
      emoji: '🔔',
      message: data.manutencoesAtrasadas === 1 ? 'manutenção preventiva pendente' : 'manutenções preventivas pendentes',
      subtitle: 'Clientes aguardando contato',
      count: data.manutencoesAtrasadas,
      link: '/manutencao-preventiva/vencidos',
      buttonText: 'Acessar',
    });
  }

  if (data.pecasCriticas > 0) {
    alerts.push({
      id: 'pecas-criticas',
      type: 'warning',
      icon: 'Package',
      emoji: '📦',
      message: data.pecasCriticas === 1 ? 'peça em nível crítico' : 'peças em nível crítico',
      subtitle: 'Estoque zerado ou abaixo do mínimo',
      count: data.pecasCriticas,
      link: '/estoque/alertas',
      buttonText: 'Ver estoque',
    });
  }

  if (data.planosManutencaoAtivos > 0) {
    alerts.push({
      id: 'planos-ativos',
      type: 'info',
      icon: 'Wrench',
      emoji: '🔧',
      message: data.planosManutencaoAtivos === 1 ? 'plano de manutenção ativo' : 'planos de manutenção ativos',
      subtitle: 'Planos sendo monitorados',
      count: data.planosManutencaoAtivos,
      link: '/manutencao-preventiva/planos',
      buttonText: 'Ver planos',
    });
  }

  return alerts;
};

export const AlertsBar = memo(() => {
  const { data, isLoading, error } = useDashboardAlertas();

  if (isLoading) {
    return (
      <div className="mb-4 sm:mb-6 space-y-2">
        {[1, 2].map((i) => (
          <div
            key={i}
            className="h-16 sm:h-14 animate-pulse rounded-lg bg-gray-200 dark:bg-gray-700"
          />
        ))}
      </div>
    );
  }

  if (error || !data) {
    return null;
  }

  const alerts = buildAlertsFromData(data);

  if (alerts.length === 0) {
    return null;
  }

  return (
    <div className="mb-4 sm:mb-6 space-y-2">
      {alerts.map((alert) => (
        <AlertBanner key={alert.id} alert={alert} />
      ))}
    </div>
  );
});

AlertsBar.displayName = 'AlertsBar';
