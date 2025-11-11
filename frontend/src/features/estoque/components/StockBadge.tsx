/**
 * Badge de status do estoque
 * Exibe o status visual baseado na quantidade atual vs quantidade mínima
 */

import { getStockStatus } from '../types';

interface StockBadgeProps {
  quantidadeAtual: number;
  quantidadeMinima: number;
  showLabel?: boolean;
}

export const StockBadge = ({
  quantidadeAtual,
  quantidadeMinima,
  showLabel = true,
}: StockBadgeProps) => {
  const statusInfo = getStockStatus(quantidadeAtual, quantidadeMinima);

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${statusInfo.bgColor} ${statusInfo.textColor}`}
    >
      {showLabel && statusInfo.label}
    </span>
  );
};
