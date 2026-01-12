/**
 * Card compacto de Ordem de Serviço para exibição em listas
 * Memoized to prevent unnecessary re-renders in list views
 */

import { memo } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, User, Car } from 'lucide-react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import type { OrdemServico } from '../types';
import { StatusBadge } from './StatusBadge';

interface OrdemServicoCardProps {
  ordemServico: OrdemServico;
}

/**
 * Converte array de números ou string ISO para objeto Date
 */
const parseDate = (date?: string | number[]): Date | null => {
  if (!date) return null;

  if (Array.isArray(date)) {
    const [year, month, day] = date;
    return new Date(year, month - 1, day);
  }

  return new Date(date);
};

/**
 * Formata data para exibição curta
 */
const formatDate = (date?: string | number[]): string => {
  const parsed = parseDate(date);
  if (!parsed) return '-';

  return format(parsed, 'dd/MM/yyyy', { locale: ptBR });
};

/**
 * Formata valor monetário
 */
const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

export const OrdemServicoCard: React.FC<OrdemServicoCardProps> = memo(({ ordemServico }) => {
  return (
    <Link
      to={`/ordens-servico/${ordemServico.id}`}
      className="block rounded-lg border border-gray-200 bg-white p-4 shadow-sm transition-shadow hover:shadow-md dark:border-gray-700 dark:bg-gray-800 dark:hover:shadow-lg"
    >
      {/* Header */}
      <div className="mb-3 flex items-start justify-between">
        <div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">OS #{ordemServico.numero}</h3>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            {ordemServico.veiculo?.placa || 'Veículo não informado'}
          </p>
        </div>
        <StatusBadge status={ordemServico.status} />
      </div>

      {/* Info Grid */}
      <div className="space-y-2">
        {/* Cliente */}
        {ordemServico.cliente && (
          <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
            <User className="h-4 w-4" />
            <span>{ordemServico.cliente.nome}</span>
          </div>
        )}

        {/* Veículo */}
        {ordemServico.veiculo && (
          <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
            <Car className="h-4 w-4" />
            <span>
              {ordemServico.veiculo.marca} {ordemServico.veiculo.modelo} (
              {ordemServico.veiculo.ano})
            </span>
          </div>
        )}

        {/* Data de abertura */}
        <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
          <Calendar className="h-4 w-4" />
          <span>Aberta em {formatDate(ordemServico.dataAbertura)}</span>
        </div>
      </div>

      {/* Footer com valor */}
      <div className="mt-4 flex items-center justify-between border-t border-gray-200 pt-3 dark:border-gray-700">
        <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Valor Total:</span>
        <span className="text-lg font-bold text-gray-900 dark:text-white">
          {formatCurrency(ordemServico.valorFinal)}
        </span>
      </div>
    </Link>
  );
});

OrdemServicoCard.displayName = 'OrdemServicoCard';
