/**
 * Badge de status de pagamento
 */

import {
  StatusPagamentoLabels,
  StatusPagamentoColors,
  type StatusPagamento as StatusPagamentoType,
} from '../types/pagamento';

interface PaymentStatusBadgeProps {
  status: StatusPagamentoType;
  className?: string;
}

export function PaymentStatusBadge({ status, className = '' }: PaymentStatusBadgeProps) {
  const colorClass = StatusPagamentoColors[status] || 'bg-gray-100 text-gray-800';
  const label = StatusPagamentoLabels[status] || status;

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
