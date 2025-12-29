import { useState } from 'react';
import { X } from 'lucide-react';
import { useRegistrarPagamentoFatura } from '../hooks/useSaas';
import type { FaturaResumo, RegistrarPagamentoFaturaRequest } from '../types';

interface Props {
  fatura: FaturaResumo;
  onClose: () => void;
  onSuccess: () => void;
}

const metodosPagamento = [
  { value: 'PIX', label: 'PIX' },
  { value: 'BOLETO', label: 'Boleto Bancário' },
  { value: 'TRANSFERENCIA', label: 'Transferência Bancária' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de Crédito' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de Débito' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
];

export function RegistrarPagamentoModal({ fatura, onClose, onSuccess }: Props) {
  const [formData, setFormData] = useState<RegistrarPagamentoFaturaRequest>({
    dataPagamento: new Date().toISOString().split('T')[0],
    metodoPagamento: 'PIX',
    transacaoId: '',
    observacao: '',
  });

  const registrarPagamento = useRegistrarPagamentoFatura();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      await registrarPagamento.mutateAsync({
        id: fatura.id,
        data: formData,
      });
      onSuccess();
    } catch (error) {
      console.error('Erro ao registrar pagamento:', error);
      alert('Erro ao registrar pagamento');
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl dark:bg-gray-800">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Registrar Pagamento</h2>
          <button
            onClick={onClose}
            className="rounded p-1 text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Invoice Info */}
        <div className="mb-4 rounded-lg bg-gray-50 p-3 dark:bg-gray-700">
          <p className="text-sm text-gray-600 dark:text-gray-400">Fatura: <span className="font-mono font-medium text-gray-900 dark:text-white">{fatura.numero}</span></p>
          <p className="text-sm text-gray-600 dark:text-gray-400">Oficina: <span className="font-medium text-gray-900 dark:text-white">{fatura.oficinaNome}</span></p>
          <p className="text-sm text-gray-600 dark:text-gray-400">Referência: <span className="font-medium text-gray-900 dark:text-white">{fatura.mesReferenciaFormatado}</span></p>
          <p className="mt-2 text-lg font-bold text-gray-900 dark:text-white">{formatCurrency(fatura.valorTotal)}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Data do Pagamento *
            </label>
            <input
              type="date"
              required
              value={formData.dataPagamento}
              onChange={(e) => setFormData({ ...formData, dataPagamento: e.target.value })}
              max={new Date().toISOString().split('T')[0]}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Método de Pagamento *
            </label>
            <select
              required
              value={formData.metodoPagamento}
              onChange={(e) => setFormData({ ...formData, metodoPagamento: e.target.value })}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            >
              {metodosPagamento.map((metodo) => (
                <option key={metodo.value} value={metodo.value}>
                  {metodo.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              ID da Transação
            </label>
            <input
              type="text"
              value={formData.transacaoId}
              onChange={(e) => setFormData({ ...formData, transacaoId: e.target.value })}
              placeholder="Ex: PIX ou código do boleto"
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white dark:placeholder-gray-400"
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Observação
            </label>
            <textarea
              value={formData.observacao}
              onChange={(e) => setFormData({ ...formData, observacao: e.target.value })}
              rows={2}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={registrarPagamento.isPending}
              className="rounded-lg bg-green-600 px-4 py-2 text-white hover:bg-green-700 disabled:opacity-50 dark:bg-green-500 dark:hover:bg-green-600"
            >
              {registrarPagamento.isPending ? 'Registrando...' : 'Confirmar Pagamento'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
