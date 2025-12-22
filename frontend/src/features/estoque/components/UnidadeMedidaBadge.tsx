/**
 * Badge de Unidade de Medida
 * Exibe a sigla da unidade com cor diferenciada
 */

import { UnidadeMedida, UnidadeMedidaSigla } from '../types';

interface UnidadeMedidaBadgeProps {
  unidade: UnidadeMedida;
}

const unidadeColors: Record<UnidadeMedida, { bg: string; text: string }> = {
  [UnidadeMedida.UNIDADE]: { bg: 'bg-blue-100 dark:bg-blue-950/40', text: 'text-blue-800 dark:text-blue-300' },
  [UnidadeMedida.LITRO]: { bg: 'bg-green-100 dark:bg-green-950/40', text: 'text-green-800 dark:text-green-300' },
  [UnidadeMedida.METRO]: { bg: 'bg-purple-100 dark:bg-purple-950/40', text: 'text-purple-800 dark:text-purple-300' },
  [UnidadeMedida.QUILO]: { bg: 'bg-orange-100 dark:bg-orange-950/40', text: 'text-orange-800 dark:text-orange-300' },
};

export const UnidadeMedidaBadge = ({ unidade }: UnidadeMedidaBadgeProps) => {
  const colors = unidadeColors[unidade];
  const sigla = UnidadeMedidaSigla[unidade];

  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${colors.bg} ${colors.text}`}
    >
      {sigla}
    </span>
  );
};
