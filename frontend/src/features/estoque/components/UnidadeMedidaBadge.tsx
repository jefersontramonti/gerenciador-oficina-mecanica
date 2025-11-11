/**
 * Badge de Unidade de Medida
 * Exibe a sigla da unidade com cor diferenciada
 */

import { UnidadeMedida, UnidadeMedidaSigla } from '../types';

interface UnidadeMedidaBadgeProps {
  unidade: UnidadeMedida;
}

const unidadeColors: Record<UnidadeMedida, { bg: string; text: string }> = {
  [UnidadeMedida.UNIDADE]: { bg: 'bg-blue-100', text: 'text-blue-800' },
  [UnidadeMedida.LITRO]: { bg: 'bg-green-100', text: 'text-green-800' },
  [UnidadeMedida.METRO]: { bg: 'bg-purple-100', text: 'text-purple-800' },
  [UnidadeMedida.QUILO]: { bg: 'bg-orange-100', text: 'text-orange-800' },
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
