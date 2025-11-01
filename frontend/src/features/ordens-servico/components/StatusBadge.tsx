/**
 * Badge de status da Ordem de Serviço
 * Exibe o status com cores apropriadas
 */

import { StatusOS } from '../types';
import { STATUS_COLORS, STATUS_LABELS } from '../utils/statusTransitions';

interface StatusBadgeProps {
  status: StatusOS;
  className?: string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, className = '' }) => {
  const colors = STATUS_COLORS[status];
  const label = STATUS_LABELS[status];

  return (
    <span
      className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-medium ${colors.bg} ${colors.text} ${colors.border} ${className}`}
    >
      {label}
    </span>
  );
};
