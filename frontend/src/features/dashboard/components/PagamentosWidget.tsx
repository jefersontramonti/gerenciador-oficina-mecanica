/**
 * Widget de Pagamentos para o Dashboard
 * Mostra resumo de pagamentos do mês com detalhes expansíveis
 */

import { memo } from 'react';
import { Link } from 'react-router-dom';
import { CreditCard, AlertTriangle, Clock, CheckCircle } from 'lucide-react';
import { MiniWidget } from './MiniWidget';
import { usePagamentosResumo } from '../hooks/useDashboardWidgets';

const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

export const PagamentosWidget = memo(() => {
  const { data, isLoading } = usePagamentosResumo();

  const summary = (
    <div className="grid grid-cols-2 gap-2 sm:gap-3">
      {/* Total Recebido */}
      <div className="rounded-lg bg-green-50 dark:bg-green-900/20 p-2 sm:p-3">
        <div className="flex items-center gap-2">
          <CheckCircle className="h-4 w-4 text-green-600 dark:text-green-400" />
          <span className="text-xs text-green-700 dark:text-green-400">Recebido</span>
        </div>
        <p className="mt-1 text-sm sm:text-lg font-bold text-green-700 dark:text-green-300">
          {data ? formatCurrency(data.recebidoMes) : '-'}
        </p>
      </div>

      {/* Total Pendente */}
      <div className="rounded-lg bg-amber-50 dark:bg-amber-900/20 p-2 sm:p-3">
        <div className="flex items-center gap-2">
          <Clock className="h-4 w-4 text-amber-600 dark:text-amber-400" />
          <span className="text-xs text-amber-700 dark:text-amber-400">Pendente</span>
        </div>
        <p className="mt-1 text-sm sm:text-lg font-bold text-amber-700 dark:text-amber-300">
          {data ? formatCurrency(data.pendentesValor) : '-'}
        </p>
      </div>
    </div>
  );

  return (
    <MiniWidget
      title="Pagamentos"
      icon={CreditCard}
      iconColor="text-emerald-600 dark:text-emerald-400"
      iconBgColor="bg-emerald-100 dark:bg-emerald-900/30"
      summary={summary}
      isLoading={isLoading}
      badge={data?.vencidosCount}
      badgeColor="bg-red-500"
    >
      {/* Conteúdo expandido */}
      <div className="space-y-3">
        {/* Detalhes */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <p className="text-xs text-gray-500 dark:text-gray-400">Pendentes</p>
            <p className="text-sm font-semibold text-gray-900 dark:text-white">
              {data?.pendentesCount || 0} pagamentos
            </p>
          </div>
          <div>
            <p className="text-xs text-gray-500 dark:text-gray-400">Vencidos</p>
            <p className="text-sm font-semibold text-red-600 dark:text-red-400">
              {data?.vencidosCount || 0} pagamentos
            </p>
          </div>
        </div>

        {/* Valor Vencido */}
        {data && data.vencidosValor > 0 && (
          <div className="rounded-lg bg-red-50 dark:bg-red-900/20 p-3 border border-red-200 dark:border-red-800">
            <div className="flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 text-red-600 dark:text-red-400" />
              <span className="text-sm font-medium text-red-700 dark:text-red-400">
                Total vencido: {formatCurrency(data.vencidosValor)}
              </span>
            </div>
          </div>
        )}

        {/* Link para ver mais */}
        <Link
          to="/financeiro"
          className="block text-center text-sm font-medium text-blue-600 dark:text-blue-400 hover:underline"
        >
          Ver todos os pagamentos
        </Link>
      </div>
    </MiniWidget>
  );
});

PagamentosWidget.displayName = 'PagamentosWidget';
