/**
 * Comunicado Alert Component - Exibido no Dashboard
 */

import { Link } from 'react-router-dom';
import { Bell, AlertTriangle, ChevronRight } from 'lucide-react';
import { useComunicadoAlerta } from '../hooks/useComunicados';
import { prioridadeColors, tipoIcons } from '../types';

export function ComunicadoAlert() {
  const { data: alerta, isLoading } = useComunicadoAlerta();

  if (isLoading || !alerta || !alerta.temAlerta) {
    return null;
  }

  return (
    <div className="mb-6 overflow-hidden rounded-lg border border-blue-200 bg-gradient-to-r from-blue-50 to-blue-100 shadow-lg dark:border-blue-800 dark:from-blue-900/30 dark:to-blue-800/30">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-blue-200 bg-blue-100/50 px-4 py-3 dark:border-blue-800 dark:bg-blue-900/50">
        <div className="flex items-center gap-3">
          <div className="relative">
            <Bell className="h-6 w-6 text-blue-600 dark:text-blue-400" />
            {alerta.totalNaoLidos > 0 && (
              <span className="absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-xs font-bold text-white">
                {alerta.totalNaoLidos > 9 ? '9+' : alerta.totalNaoLidos}
              </span>
            )}
          </div>
          <div>
            <h3 className="font-semibold text-blue-900 dark:text-blue-100">
              Voce tem comunicados importantes!
            </h3>
            <p className="text-sm text-blue-700 dark:text-blue-300">
              {alerta.totalNaoLidos} nao lido{alerta.totalNaoLidos !== 1 ? 's' : ''}
              {alerta.pendentesConfirmacao > 0 && (
                <span className="ml-2 text-orange-600 dark:text-orange-400">
                  • {alerta.pendentesConfirmacao} aguardando confirmacao
                </span>
              )}
            </p>
          </div>
        </div>
        <Link
          to="/comunicados"
          className="flex items-center gap-1 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          Ver todos
          <ChevronRight className="h-4 w-4" />
        </Link>
      </div>

      {/* Comunicados urgentes */}
      {alerta.urgentes.length > 0 && (
        <div className="p-4">
          <div className="mb-2 flex items-center gap-2 text-sm font-medium text-red-600 dark:text-red-400">
            <AlertTriangle className="h-4 w-4" />
            Comunicados urgentes/alta prioridade
          </div>
          <div className="space-y-2">
            {alerta.urgentes.map((comunicado) => (
              <Link
                key={comunicado.id}
                to="/comunicados"
                className="flex items-center gap-3 rounded-lg bg-white/80 p-3 transition-colors hover:bg-white dark:bg-gray-800/80 dark:hover:bg-gray-800"
              >
                <span className="text-lg">{tipoIcons[comunicado.tipo] || '📢'}</span>
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium text-gray-900 dark:text-white">
                    {comunicado.titulo}
                  </p>
                  {comunicado.resumo && (
                    <p className="truncate text-sm text-gray-500 dark:text-gray-400">
                      {comunicado.resumo}
                    </p>
                  )}
                </div>
                <span
                  className={`rounded-full px-2 py-1 text-xs font-medium ${prioridadeColors[comunicado.prioridade]}`}
                >
                  {comunicado.prioridadeDescricao}
                </span>
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

/**
 * Badge para o menu lateral
 */
export function ComunicadoBadge() {
  const { data: alerta } = useComunicadoAlerta();

  if (!alerta || alerta.totalNaoLidos === 0) {
    return null;
  }

  return (
    <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-red-500 px-1.5 text-xs font-bold text-white">
      {alerta.totalNaoLidos > 99 ? '99+' : alerta.totalNaoLidos}
    </span>
  );
}
