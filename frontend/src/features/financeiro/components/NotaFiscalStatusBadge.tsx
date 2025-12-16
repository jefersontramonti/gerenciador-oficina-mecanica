/**
 * Badge de status de nota fiscal
 */

import {
  StatusNotaFiscalLabels,
  StatusNotaFiscalColors,
  type StatusNotaFiscal,
} from '../types/notaFiscal';

interface NotaFiscalStatusBadgeProps {
  status: StatusNotaFiscal;
  className?: string;
}

export function NotaFiscalStatusBadge({
  status,
  className = '',
}: NotaFiscalStatusBadgeProps) {
  const colorClass = StatusNotaFiscalColors[status] || 'bg-gray-100 text-gray-800';
  const label = StatusNotaFiscalLabels[status] || status;

  return (
    <span
      className={`
        inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium
        ${colorClass}
        ${className}
      `}
    >
      {label}
    </span>
  );
}
