/**
 * Componente para listar pagamentos
 */

import { useState } from 'react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { CheckCircle2, XCircle } from 'lucide-react';
import { usePagamentosPorOS, useConfirmarPagamento, useCancelarPagamento } from '../hooks/usePagamentos';
import {
  StatusPagamento,
  StatusPagamentoLabels,
  TipoPagamentoLabels,
  type Pagamento
} from '../types/pagamento';

interface ListaPagamentosProps {
  ordemServicoId: string;
}

const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(value);
};

// Cores dos status com suporte a dark mode
const getStatusColors = (status: StatusPagamento): string => {
  switch (status) {
    case StatusPagamento.PAGO:
      return 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-400';
    case StatusPagamento.PENDENTE:
      return 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-400';
    case StatusPagamento.CANCELADO:
      return 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-400';
    case StatusPagamento.ESTORNADO:
      return 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300';
    default:
      return 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300';
  }
};

export function ListaPagamentos({ ordemServicoId }: ListaPagamentosProps) {
  const { data: pagamentos, isLoading } = usePagamentosPorOS(ordemServicoId);
  const { mutate: confirmarPagamento } = useConfirmarPagamento();
  const { mutate: cancelarPagamento } = useCancelarPagamento();

  const [confirmarDialog, setConfirmarDialog] = useState<{
    open: boolean;
    pagamento?: Pagamento;
  }>({ open: false });
  const [dataPagamento, setDataPagamento] = useState(
    format(new Date(), 'yyyy-MM-dd')
  );

  const handleConfirmar = () => {
    if (!confirmarDialog.pagamento) return;

    confirmarPagamento(
      {
        id: confirmarDialog.pagamento.id,
        data: { dataPagamento }
      },
      {
        onSuccess: () => {
          setConfirmarDialog({ open: false });
          setDataPagamento(format(new Date(), 'yyyy-MM-dd'));
        }
      }
    );
  };

  const handleCancelar = (id: string) => {
    if (confirm('Deseja realmente cancelar este pagamento?')) {
      cancelarPagamento(id);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-2">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-16 w-full animate-pulse rounded bg-gray-200 dark:bg-gray-700" />
        ))}
      </div>
    );
  }

  if (!pagamentos || pagamentos.length === 0) {
    return (
      <div className="py-8 text-center text-gray-500 dark:text-gray-400">
        Nenhum pagamento registrado para esta ordem de serviço.
      </div>
    );
  }

  return (
    <>
      <div className="overflow-hidden rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 dark:bg-gray-900">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Tipo
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Valor
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Parcelas
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Vencimento
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Status
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {pagamentos.map((pagamento) => (
                <tr key={pagamento.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                  <td className="px-6 py-4 text-sm font-medium text-gray-900 dark:text-white">
                    {TipoPagamentoLabels[pagamento.tipo]}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-900 dark:text-white">
                    {formatCurrency(pagamento.valor)}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-900 dark:text-white">
                    {pagamento.parcelaAtual}/{pagamento.parcelas}
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-900 dark:text-white">
                    {pagamento.dataVencimento
                      ? format(new Date(pagamento.dataVencimento), 'dd/MM/yyyy', {
                          locale: ptBR
                        })
                      : '-'}
                  </td>
                  <td className="px-6 py-4 text-sm">
                    <span
                      className={`inline-flex rounded-full px-2 py-1 text-xs font-semibold ${getStatusColors(pagamento.status)}`}
                    >
                      {StatusPagamentoLabels[pagamento.status]}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right text-sm">
                    {pagamento.status === StatusPagamento.PENDENTE && (
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() =>
                            setConfirmarDialog({
                              open: true,
                              pagamento
                            })
                          }
                          className="text-green-600 dark:text-green-400 hover:text-green-700 dark:hover:text-green-300 transition-colors"
                          title="Confirmar pagamento"
                        >
                          <CheckCircle2 className="h-5 w-5" />
                        </button>
                        <button
                          onClick={() => handleCancelar(pagamento.id)}
                          className="text-red-600 dark:text-red-400 hover:text-red-700 dark:hover:text-red-300 transition-colors"
                          title="Cancelar pagamento"
                        >
                          <XCircle className="h-5 w-5" />
                        </button>
                      </div>
                    )}
                    {pagamento.dataPagamento && (
                      <span className="text-xs text-gray-500 dark:text-gray-400">
                        Pago em{' '}
                        {format(new Date(pagamento.dataPagamento), 'dd/MM/yyyy')}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Dialog de Confirmação */}
      {confirmarDialog.open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-md rounded-lg bg-white dark:bg-gray-800 p-6 shadow-xl border border-gray-200 dark:border-gray-700">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              Confirmar Pagamento
            </h3>
            <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
              Confirme a data de recebimento do pagamento
            </p>

            {confirmarDialog.pagamento && (
              <div className="mt-4 space-y-2 rounded-lg bg-gray-50 dark:bg-gray-700/50 p-4">
                <div className="grid grid-cols-2 gap-2 text-sm">
                  <span className="text-gray-600 dark:text-gray-400">Tipo:</span>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {TipoPagamentoLabels[confirmarDialog.pagamento.tipo]}
                  </span>
                  <span className="text-gray-600 dark:text-gray-400">Valor:</span>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {formatCurrency(confirmarDialog.pagamento.valor)}
                  </span>
                </div>
              </div>
            )}

            <div className="mt-4">
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Data do Pagamento
              </label>
              <input
                type="date"
                value={dataPagamento}
                onChange={(e) => setDataPagamento(e.target.value)}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white px-3 py-2 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button
                onClick={() => setConfirmarDialog({ open: false })}
                className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={handleConfirmar}
                className="rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 transition-colors"
              >
                Confirmar
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
