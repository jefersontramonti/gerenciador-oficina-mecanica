/**
 * Widget de Manutenção Preventiva para o Dashboard
 * Mostra resumo de planos de manutenção e alertas
 */

import { memo } from 'react';
import { Link } from 'react-router-dom';
import { Wrench, Calendar, AlertTriangle, CheckCircle } from 'lucide-react';
import { MiniWidget } from './MiniWidget';
import { useManutencaoResumo, useProximasManutencoes } from '../hooks/useDashboardWidgets';

const formatDate = (dateStr: string): string => {
  const date = new Date(dateStr);
  return date.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'short',
  });
};

export const ManutencaoWidget = memo(() => {
  const { data: resumo, isLoading: isLoadingResumo } = useManutencaoResumo();
  const { data: proximas, isLoading: isLoadingProximas } = useProximasManutencoes(7, 3);

  const isLoading = isLoadingResumo || isLoadingProximas;

  const summary = (
    <div className="grid grid-cols-2 gap-2 sm:gap-3">
      {/* Planos Ativos */}
      <div className="rounded-lg bg-blue-50 dark:bg-blue-900/20 p-2 sm:p-3">
        <div className="flex items-center gap-2">
          <CheckCircle className="h-4 w-4 text-blue-600 dark:text-blue-400" />
          <span className="text-xs text-blue-700 dark:text-blue-400">Planos Ativos</span>
        </div>
        <p className="mt-1 text-sm sm:text-lg font-bold text-blue-700 dark:text-blue-300">
          {resumo?.planosAtivos || 0}
        </p>
      </div>

      {/* Alertas */}
      <div className="rounded-lg bg-amber-50 dark:bg-amber-900/20 p-2 sm:p-3">
        <div className="flex items-center gap-2">
          <AlertTriangle className="h-4 w-4 text-amber-600 dark:text-amber-400" />
          <span className="text-xs text-amber-700 dark:text-amber-400">Alertas</span>
        </div>
        <p className="mt-1 text-sm sm:text-lg font-bold text-amber-700 dark:text-amber-300">
          {resumo?.alertasPendentes || 0}
        </p>
      </div>
    </div>
  );

  return (
    <MiniWidget
      title="Manutenção Preventiva"
      icon={Wrench}
      iconColor="text-orange-600 dark:text-orange-400"
      iconBgColor="bg-orange-100 dark:bg-orange-900/30"
      summary={summary}
      isLoading={isLoading}
      badge={resumo?.planosVencidos}
      badgeColor="bg-red-500"
    >
      {/* Conteúdo expandido */}
      <div className="space-y-3">
        {/* Stats rápidos */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <p className="text-xs text-gray-500 dark:text-gray-400">Vencidas</p>
            <p className="text-sm font-semibold text-red-600 dark:text-red-400">
              {resumo?.planosVencidos || 0}
            </p>
          </div>
          <div>
            <p className="text-xs text-gray-500 dark:text-gray-400">Próximos 7 dias</p>
            <p className="text-sm font-semibold text-gray-900 dark:text-white">
              {proximas?.length || 0}
            </p>
          </div>
        </div>

        {/* Lista de próximas manutenções */}
        {proximas && proximas.length > 0 && (
          <div className="space-y-2">
            <p className="text-xs font-medium text-gray-600 dark:text-gray-400 uppercase">
              Próximas Manutenções
            </p>
            <div className="space-y-2">
              {proximas.map((m) => (
                <div
                  key={m.id}
                  className="flex items-center justify-between rounded-lg bg-white dark:bg-gray-800 p-2 border border-gray-200 dark:border-gray-600"
                >
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-gray-900 dark:text-white truncate">
                      {m.veiculoPlaca}
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
                      {m.tipoManutencao}
                    </p>
                  </div>
                  <div className="text-right ml-2">
                    <div className="flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400">
                      <Calendar className="h-3 w-3" />
                      <span>{formatDate(m.dataPrevisao)}</span>
                    </div>
                    <p className={`text-xs font-medium ${
                      m.diasRestantes <= 0 ? 'text-red-600 dark:text-red-400' :
                      m.diasRestantes <= 3 ? 'text-amber-600 dark:text-amber-400' :
                      'text-green-600 dark:text-green-400'
                    }`}>
                      {m.diasRestantes <= 0 ? 'Vencida' :
                       m.diasRestantes === 1 ? 'Amanhã' :
                       `${m.diasRestantes} dias`}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Link para ver mais */}
        <Link
          to="/manutencao-preventiva/planos"
          className="block text-center text-sm font-medium text-blue-600 dark:text-blue-400 hover:underline"
        >
          Ver planos de manutenção
        </Link>
      </div>
    </MiniWidget>
  );
});

ManutencaoWidget.displayName = 'ManutencaoWidget';
