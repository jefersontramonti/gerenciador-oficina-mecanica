/**
 * Timeline de histórico de status da Ordem de Serviço
 * Exibe todas as mudanças de status em ordem cronológica
 */

import { CheckCircle, Clock, Play, PauseCircle, TruckIcon, XCircle, FileText, User } from 'lucide-react';
import { useHistoricoStatus } from '../hooks/useOrdensServico';
import { StatusOS } from '../types';
import type { HistoricoStatusOS } from '../types';

interface OSTimelineProps {
  osId: string;
}

/**
 * Formata data/hora de forma legível
 */
const formatDateTime = (date: string | number[] | undefined | null): string => {
  if (!date) return '-';

  try {
    if (Array.isArray(date)) {
      // Formato [year, month, day, hour, minute, second, nano]
      const [year, month, day, hour = 0, minute = 0] = date;
      const d = new Date(year, month - 1, day, hour, minute);
      return d.toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    }

    const d = new Date(date);
    return d.toLocaleString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return String(date);
  }
};

/**
 * Retorna o ícone apropriado para cada status
 */
const getStatusIcon = (status: StatusOS) => {
  const iconProps = { className: 'h-4 w-4' };

  switch (status) {
    case StatusOS.ORCAMENTO:
      return <FileText {...iconProps} />;
    case StatusOS.APROVADO:
      return <CheckCircle {...iconProps} />;
    case StatusOS.EM_ANDAMENTO:
      return <Play {...iconProps} />;
    case StatusOS.AGUARDANDO_PECA:
      return <PauseCircle {...iconProps} />;
    case StatusOS.FINALIZADO:
      return <CheckCircle {...iconProps} />;
    case StatusOS.ENTREGUE:
      return <TruckIcon {...iconProps} />;
    case StatusOS.CANCELADO:
      return <XCircle {...iconProps} />;
    default:
      return <Clock {...iconProps} />;
  }
};

/**
 * Retorna as cores apropriadas para cada status
 */
const getStatusColors = (status: StatusOS) => {
  switch (status) {
    case StatusOS.ORCAMENTO:
      return {
        bg: 'bg-blue-100 dark:bg-blue-900/30',
        border: 'border-blue-500 dark:border-blue-400',
        text: 'text-blue-700 dark:text-blue-300',
        line: 'bg-blue-200 dark:bg-blue-800',
      };
    case StatusOS.APROVADO:
      return {
        bg: 'bg-green-100 dark:bg-green-900/30',
        border: 'border-green-500 dark:border-green-400',
        text: 'text-green-700 dark:text-green-300',
        line: 'bg-green-200 dark:bg-green-800',
      };
    case StatusOS.EM_ANDAMENTO:
      return {
        bg: 'bg-yellow-100 dark:bg-yellow-900/30',
        border: 'border-yellow-500 dark:border-yellow-400',
        text: 'text-yellow-700 dark:text-yellow-300',
        line: 'bg-yellow-200 dark:bg-yellow-800',
      };
    case StatusOS.AGUARDANDO_PECA:
      return {
        bg: 'bg-orange-100 dark:bg-orange-900/30',
        border: 'border-orange-500 dark:border-orange-400',
        text: 'text-orange-700 dark:text-orange-300',
        line: 'bg-orange-200 dark:bg-orange-800',
      };
    case StatusOS.FINALIZADO:
      return {
        bg: 'bg-purple-100 dark:bg-purple-900/30',
        border: 'border-purple-500 dark:border-purple-400',
        text: 'text-purple-700 dark:text-purple-300',
        line: 'bg-purple-200 dark:bg-purple-800',
      };
    case StatusOS.ENTREGUE:
      return {
        bg: 'bg-gray-100 dark:bg-gray-700',
        border: 'border-gray-500 dark:border-gray-400',
        text: 'text-gray-700 dark:text-gray-300',
        line: 'bg-gray-200 dark:bg-gray-700',
      };
    case StatusOS.CANCELADO:
      return {
        bg: 'bg-red-100 dark:bg-red-900/30',
        border: 'border-red-500 dark:border-red-400',
        text: 'text-red-700 dark:text-red-300',
        line: 'bg-red-200 dark:bg-red-800',
      };
    default:
      return {
        bg: 'bg-gray-100 dark:bg-gray-800',
        border: 'border-gray-400',
        text: 'text-gray-600 dark:text-gray-400',
        line: 'bg-gray-200 dark:bg-gray-700',
      };
  }
};

/**
 * Componente de item individual da timeline
 */
const TimelineItem: React.FC<{
  item: HistoricoStatusOS;
  isLast: boolean;
}> = ({ item, isLast }) => {
  const colors = getStatusColors(item.statusNovo);

  return (
    <div className="relative flex gap-4">
      {/* Linha vertical conectora */}
      {!isLast && (
        <div
          className={`absolute left-[15px] top-8 h-full w-0.5 ${colors.line}`}
        />
      )}

      {/* Círculo com ícone */}
      <div
        className={`relative z-10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full border-2 ${colors.bg} ${colors.border} ${colors.text}`}
      >
        {getStatusIcon(item.statusNovo)}
      </div>

      {/* Conteúdo */}
      <div className="flex-1 pb-6">
        <div className="flex flex-wrap items-center gap-2">
          <span className={`font-semibold ${colors.text}`}>
            {item.statusNovoLabel}
          </span>

          {item.statusAnteriorLabel && (
            <span className="text-xs text-gray-500 dark:text-gray-400">
              (de {item.statusAnteriorLabel})
            </span>
          )}
        </div>

        <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-gray-500 dark:text-gray-400">
          <span className="flex items-center gap-1">
            <Clock className="h-3 w-3" />
            {formatDateTime(item.dataAlteracao)}
          </span>

          {item.usuarioNome && (
            <span className="flex items-center gap-1">
              <User className="h-3 w-3" />
              {item.usuarioNome}
            </span>
          )}
        </div>

        {item.observacao && (
          <p className="mt-2 rounded-lg bg-gray-50 p-2 text-sm text-gray-600 dark:bg-gray-800 dark:text-gray-300">
            {item.observacao}
          </p>
        )}
      </div>
    </div>
  );
};

/**
 * Componente principal da timeline
 */
export const OSTimeline: React.FC<OSTimelineProps> = ({ osId }) => {
  const { data: historico, isLoading, error } = useHistoricoStatus(osId);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
        <span className="ml-2 text-sm text-gray-500 dark:text-gray-400">Carregando histórico...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
        Erro ao carregar histórico de status
      </div>
    );
  }

  if (!historico || historico.length === 0) {
    return (
      <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 text-center text-sm text-gray-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400">
        Nenhum histórico de status disponível
      </div>
    );
  }

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-900">
      <h3 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900 dark:text-white">
        <Clock className="h-5 w-5" />
        Histórico de Status
      </h3>

      <div className="space-y-0">
        {historico.map((item, index) => (
          <TimelineItem
            key={item.id}
            item={item}
            isLast={index === historico.length - 1}
          />
        ))}
      </div>
    </div>
  );
};
