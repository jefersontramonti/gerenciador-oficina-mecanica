/**
 * Widget de Uso do Plano para o Dashboard
 * Mostra uso atual vs limites do plano com detalhes expansíveis
 */

import { memo } from 'react';
import { Link } from 'react-router-dom';
import { Users, FileText, AlertTriangle, TrendingUp, ArrowUpRight } from 'lucide-react';
import { MiniWidget } from '@/features/dashboard/components/MiniWidget';
import { usePlanoLimites } from '../hooks/usePlanoLimites';

const MESES = [
  '', 'Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun',
  'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'
];

interface ProgressBarProps {
  percentual: number;
  ilimitado: boolean;
  atual: number;
  limite: number;
}

function ProgressBar({ percentual, ilimitado, atual, limite }: ProgressBarProps) {
  if (ilimitado) {
    return (
      <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
        <span className="font-medium">{atual}</span>
        <span>/ Ilimitado</span>
      </div>
    );
  }

  const getColor = () => {
    if (percentual >= 90) return 'bg-red-500';
    if (percentual >= 80) return 'bg-yellow-500';
    return 'bg-blue-500';
  };

  return (
    <div className="space-y-1">
      <div className="flex justify-between text-xs sm:text-sm">
        <span className="text-gray-600 dark:text-gray-400">{atual} de {limite}</span>
        <span className={`font-medium ${percentual >= 80 ? 'text-yellow-600 dark:text-yellow-400' : 'text-gray-600 dark:text-gray-400'}`}>
          {percentual}%
        </span>
      </div>
      <div className="h-1.5 sm:h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
        <div
          className={`h-full ${getColor()} transition-all duration-300 rounded-full`}
          style={{ width: `${Math.min(percentual, 100)}%` }}
        />
      </div>
    </div>
  );
}

export const PlanoLimitesCard = memo(() => {
  const { data, isLoading } = usePlanoLimites();

  const usuariosEmAlerta = data && !data.usuariosIlimitados && data.percentualUsuarios >= 80;
  const osEmAlerta = data && !data.osIlimitadas && data.percentualOsMes >= 80;
  const temAlerta = usuariosEmAlerta || osEmAlerta;

  const summary = (
    <div className="grid grid-cols-2 gap-2 sm:gap-3">
      {/* Usuários */}
      <div className={`rounded-lg p-2 sm:p-3 ${usuariosEmAlerta ? 'bg-yellow-50 dark:bg-yellow-900/20' : 'bg-blue-50 dark:bg-blue-900/20'}`}>
        <div className="flex items-center gap-1.5 sm:gap-2">
          <Users className={`h-3.5 w-3.5 sm:h-4 sm:w-4 ${usuariosEmAlerta ? 'text-yellow-600 dark:text-yellow-400' : 'text-blue-600 dark:text-blue-400'}`} />
          <span className={`text-xs ${usuariosEmAlerta ? 'text-yellow-700 dark:text-yellow-400' : 'text-blue-700 dark:text-blue-400'}`}>
            Usuários
          </span>
        </div>
        <p className={`mt-1 text-sm sm:text-lg font-bold ${usuariosEmAlerta ? 'text-yellow-700 dark:text-yellow-300' : 'text-blue-700 dark:text-blue-300'}`}>
          {data ? (
            data.usuariosIlimitados
              ? `${data.usuariosAtivos} / ∞`
              : `${data.usuariosAtivos} / ${data.limiteUsuarios}`
          ) : '-'}
        </p>
      </div>

      {/* OS do Mês */}
      <div className={`rounded-lg p-2 sm:p-3 ${osEmAlerta ? 'bg-yellow-50 dark:bg-yellow-900/20' : 'bg-green-50 dark:bg-green-900/20'}`}>
        <div className="flex items-center gap-1.5 sm:gap-2">
          <FileText className={`h-3.5 w-3.5 sm:h-4 sm:w-4 ${osEmAlerta ? 'text-yellow-600 dark:text-yellow-400' : 'text-green-600 dark:text-green-400'}`} />
          <span className={`text-xs ${osEmAlerta ? 'text-yellow-700 dark:text-yellow-400' : 'text-green-700 dark:text-green-400'}`}>
            OS/{MESES[data?.mesReferencia ?? 1]}
          </span>
        </div>
        <p className={`mt-1 text-sm sm:text-lg font-bold ${osEmAlerta ? 'text-yellow-700 dark:text-yellow-300' : 'text-green-700 dark:text-green-300'}`}>
          {data ? (
            data.osIlimitadas
              ? `${data.osNoMes} / ∞`
              : `${data.osNoMes} / ${data.limiteOsMes}`
          ) : '-'}
        </p>
      </div>
    </div>
  );

  // Calcular badge (número de alertas)
  const badgeCount = (usuariosEmAlerta ? 1 : 0) + (osEmAlerta ? 1 : 0);

  return (
    <MiniWidget
      title={`Plano ${data?.planoNome || ''}`}
      icon={TrendingUp}
      iconColor="text-purple-600 dark:text-purple-400"
      iconBgColor="bg-purple-100 dark:bg-purple-900/30"
      summary={summary}
      isLoading={isLoading}
      badge={badgeCount > 0 ? badgeCount : undefined}
      badgeColor="bg-yellow-500"
    >
      {/* Conteúdo expandido */}
      <div className="space-y-3 sm:space-y-4">
        {/* Alerta se próximo do limite */}
        {temAlerta && (
          <div className="flex items-start gap-2 p-2 sm:p-3 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg">
            <AlertTriangle className="h-4 w-4 text-yellow-600 dark:text-yellow-400 flex-shrink-0 mt-0.5" />
            <div className="text-xs sm:text-sm text-yellow-700 dark:text-yellow-300">
              <p className="font-medium">Próximo do limite!</p>
              <p className="mt-0.5">
                Considere fazer upgrade do plano.
              </p>
            </div>
          </div>
        )}

        {/* Detalhes com barras de progresso */}
        <div className="space-y-3">
          {/* Usuários */}
          <div>
            <div className="flex items-center gap-2 mb-1.5">
              <Users className="h-3.5 w-3.5 sm:h-4 sm:w-4 text-gray-500 dark:text-gray-400" />
              <span className="text-xs sm:text-sm font-medium text-gray-700 dark:text-gray-300">
                Usuários Ativos
              </span>
            </div>
            {data && (
              <ProgressBar
                percentual={data.percentualUsuarios}
                ilimitado={data.usuariosIlimitados}
                atual={data.usuariosAtivos}
                limite={data.limiteUsuarios}
              />
            )}
          </div>

          {/* OS do Mês */}
          <div>
            <div className="flex items-center gap-2 mb-1.5">
              <FileText className="h-3.5 w-3.5 sm:h-4 sm:w-4 text-gray-500 dark:text-gray-400" />
              <span className="text-xs sm:text-sm font-medium text-gray-700 dark:text-gray-300">
                Ordens de Serviço - {MESES[data?.mesReferencia ?? 1]}/{data?.anoReferencia}
              </span>
            </div>
            {data && (
              <ProgressBar
                percentual={data.percentualOsMes}
                ilimitado={data.osIlimitadas}
                atual={data.osNoMes}
                limite={data.limiteOsMes}
              />
            )}
          </div>
        </div>

        {/* Info */}
        <p className="text-[10px] sm:text-xs text-gray-500 dark:text-gray-400 text-center">
          Limites de OS renovam todo dia 1º
        </p>

        {/* Link para Meu Plano */}
        <Link
          to="/meu-plano"
          className="flex items-center justify-center gap-1 text-xs sm:text-sm font-medium text-purple-600 dark:text-purple-400 hover:underline"
        >
          Ver detalhes do plano
          <ArrowUpRight className="h-3 w-3 sm:h-4 sm:w-4" />
        </Link>
      </div>
    </MiniWidget>
  );
});

PlanoLimitesCard.displayName = 'PlanoLimitesCard';
