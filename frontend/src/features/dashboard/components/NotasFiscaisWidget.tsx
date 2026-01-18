/**
 * Widget de Notas Fiscais para o Dashboard
 * Mostra resumo de notas fiscais do mês
 */

import { memo } from 'react';
import { Link } from 'react-router-dom';
import { FileText, CheckCircle, FileEdit, XCircle } from 'lucide-react';
import { MiniWidget } from './MiniWidget';
import { useNotasFiscaisResumo } from '../hooks/useDashboardWidgets';

export const NotasFiscaisWidget = memo(() => {
  const { data, isLoading } = useNotasFiscaisResumo();

  const summary = (
    <div className="grid grid-cols-3 gap-2 sm:gap-3">
      {/* Emitidas */}
      <div className="rounded-lg bg-green-50 dark:bg-green-900/20 p-2 sm:p-3 text-center">
        <CheckCircle className="h-4 w-4 text-green-600 dark:text-green-400 mx-auto" />
        <p className="mt-1 text-sm sm:text-lg font-bold text-green-700 dark:text-green-300">
          {data?.emitidasMes || 0}
        </p>
        <p className="text-[10px] sm:text-xs text-green-600 dark:text-green-400">
          Emitidas
        </p>
      </div>

      {/* Rascunhos */}
      <div className="rounded-lg bg-amber-50 dark:bg-amber-900/20 p-2 sm:p-3 text-center">
        <FileEdit className="h-4 w-4 text-amber-600 dark:text-amber-400 mx-auto" />
        <p className="mt-1 text-sm sm:text-lg font-bold text-amber-700 dark:text-amber-300">
          {data?.rascunhos || 0}
        </p>
        <p className="text-[10px] sm:text-xs text-amber-600 dark:text-amber-400">
          Rascunhos
        </p>
      </div>

      {/* Canceladas */}
      <div className="rounded-lg bg-red-50 dark:bg-red-900/20 p-2 sm:p-3 text-center">
        <XCircle className="h-4 w-4 text-red-600 dark:text-red-400 mx-auto" />
        <p className="mt-1 text-sm sm:text-lg font-bold text-red-700 dark:text-red-300">
          {data?.canceladasMes || 0}
        </p>
        <p className="text-[10px] sm:text-xs text-red-600 dark:text-red-400">
          Canceladas
        </p>
      </div>
    </div>
  );

  return (
    <MiniWidget
      title="Notas Fiscais"
      icon={FileText}
      iconColor="text-purple-600 dark:text-purple-400"
      iconBgColor="bg-purple-100 dark:bg-purple-900/30"
      summary={summary}
      isLoading={isLoading}
      badge={data?.rascunhos}
      badgeColor="bg-amber-500"
    >
      {/* Conteúdo expandido */}
      <div className="space-y-3">
        {/* Informações adicionais */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-3 border border-gray-200 dark:border-gray-600">
          <p className="text-xs text-gray-500 dark:text-gray-400 mb-2">
            Resumo do mês atual
          </p>
          <div className="flex justify-between items-center">
            <span className="text-sm text-gray-600 dark:text-gray-300">
              Total emitidas
            </span>
            <span className="text-sm font-semibold text-gray-900 dark:text-white">
              {data?.emitidasMes || 0} notas
            </span>
          </div>
          {data && data.rascunhos > 0 && (
            <div className="mt-2 pt-2 border-t border-gray-200 dark:border-gray-600 flex justify-between items-center">
              <span className="text-sm text-amber-600 dark:text-amber-400">
                Aguardando emissão
              </span>
              <span className="text-sm font-semibold text-amber-600 dark:text-amber-400">
                {data.rascunhos} rascunhos
              </span>
            </div>
          )}
        </div>

        {/* Link para ver mais */}
        <Link
          to="/financeiro/notas-fiscais"
          className="block text-center text-sm font-medium text-blue-600 dark:text-blue-400 hover:underline"
        >
          Ver todas as notas fiscais
        </Link>
      </div>
    </MiniWidget>
  );
});

NotasFiscaisWidget.displayName = 'NotasFiscaisWidget';
