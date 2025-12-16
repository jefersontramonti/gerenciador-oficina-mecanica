/**
 * Componente para exibir resumo financeiro de uma OS
 */

import { CheckCircle2, Clock } from 'lucide-react';
import { useResumoFinanceiro } from '../hooks/usePagamentos';

interface ResumoFinanceiroProps {
  ordemServicoId: string;
}

const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(value);
};

export function ResumoFinanceiro({ ordemServicoId }: ResumoFinanceiroProps) {
  const { data: resumo, isLoading } = useResumoFinanceiro(ordemServicoId);

  if (isLoading) {
    return (
      <div className="grid gap-4 md:grid-cols-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="animate-pulse rounded-lg bg-white p-6 shadow">
            <div className="h-4 w-24 rounded bg-gray-200"></div>
            <div className="mt-2 h-7 w-32 rounded bg-gray-200"></div>
          </div>
        ))}
      </div>
    );
  }

  if (!resumo) return null;

  const totalGeral = resumo.totalPago + resumo.totalPendente;

  return (
    <div className="grid gap-4 md:grid-cols-3">
      {/* Total Pago */}
      <div className="rounded-lg bg-white p-6 shadow">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">Total Pago</p>
            <p className="mt-2 text-2xl font-bold text-green-600">
              {formatCurrency(resumo.totalPago)}
            </p>
            {totalGeral > 0 && (
              <p className="mt-1 text-xs text-gray-500">
                {((resumo.totalPago / totalGeral) * 100).toFixed(1)}% do total
              </p>
            )}
          </div>
          <div className="rounded-full bg-green-100 p-3">
            <CheckCircle2 className="h-6 w-6 text-green-600" />
          </div>
        </div>
      </div>

      {/* Total Pendente */}
      <div className="rounded-lg bg-white p-6 shadow">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">Total Pendente</p>
            <p className="mt-2 text-2xl font-bold text-yellow-600">
              {formatCurrency(resumo.totalPendente)}
            </p>
            {totalGeral > 0 && (
              <p className="mt-1 text-xs text-gray-500">
                {((resumo.totalPendente / totalGeral) * 100).toFixed(1)}% do total
              </p>
            )}
          </div>
          <div className="rounded-full bg-yellow-100 p-3">
            <Clock className="h-6 w-6 text-yellow-600" />
          </div>
        </div>
      </div>

      {/* Status */}
      <div className="rounded-lg bg-white p-6 shadow">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-gray-600">Status</p>
            <p
              className={`mt-2 text-2xl font-bold ${
                resumo.quitada ? 'text-green-600' : 'text-yellow-600'
              }`}
            >
              {resumo.quitada ? 'Quitada' : 'Pendente'}
            </p>
            <p className="mt-1 text-xs text-gray-500">
              Total: {formatCurrency(totalGeral)}
            </p>
          </div>
          <div
            className={`rounded-full p-3 ${
              resumo.quitada ? 'bg-green-100' : 'bg-yellow-100'
            }`}
          >
            {resumo.quitada ? (
              <CheckCircle2 className="h-6 w-6 text-green-600" />
            ) : (
              <Clock className="h-6 w-6 text-yellow-600" />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
