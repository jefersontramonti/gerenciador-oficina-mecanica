import { useState, useMemo } from 'react';
import { X, Calculator } from 'lucide-react';
import { useCriarAcordo } from '../hooks/useSaas';
import type { OficinaInadimplente, CriarAcordoRequest } from '../types';

interface Props {
  oficina: OficinaInadimplente;
  onClose: () => void;
  onSuccess: () => void;
}

export function CriarAcordoModal({ oficina, onClose, onSuccess }: Props) {
  const [selectedFaturas, setSelectedFaturas] = useState<string[]>(
    oficina.faturasVencidasList.map((f) => f.faturaId)
  );
  const [numeroParcelas, setNumeroParcelas] = useState(1);
  const [percentualDesconto, setPercentualDesconto] = useState(0);
  const [primeiroVencimento, setPrimeiroVencimento] = useState(
    new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  );
  const [observacoes, setObservacoes] = useState('');

  const criarAcordo = useCriarAcordo();

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  };

  const valorOriginal = useMemo(() => {
    return oficina.faturasVencidasList
      .filter((f) => selectedFaturas.includes(f.faturaId))
      .reduce((sum, f) => sum + f.valor, 0);
  }, [oficina.faturasVencidasList, selectedFaturas]);

  const valorDesconto = useMemo(() => {
    return (valorOriginal * percentualDesconto) / 100;
  }, [valorOriginal, percentualDesconto]);

  const valorFinal = useMemo(() => {
    return valorOriginal - valorDesconto;
  }, [valorOriginal, valorDesconto]);

  const valorParcela = useMemo(() => {
    if (numeroParcelas <= 0) return 0;
    return valorFinal / numeroParcelas;
  }, [valorFinal, numeroParcelas]);

  const handleSelectFatura = (id: string, checked: boolean) => {
    if (checked) {
      setSelectedFaturas([...selectedFaturas, id]);
    } else {
      setSelectedFaturas(selectedFaturas.filter((f) => f !== id));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (selectedFaturas.length === 0) {
      alert('Selecione pelo menos uma fatura');
      return;
    }

    const data: CriarAcordoRequest = {
      faturaIds: selectedFaturas,
      valorTotalAcordado: valorFinal,
      numeroParcelas,
      primeiroVencimento,
      percentualDesconto: percentualDesconto > 0 ? percentualDesconto : undefined,
      observacoes: observacoes || undefined,
    };

    try {
      await criarAcordo.mutateAsync({
        oficinaId: oficina.oficinaId,
        data,
      });
      onSuccess();
    } catch (error) {
      console.error('Erro ao criar acordo:', error);
      alert('Erro ao criar acordo');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-lg bg-white p-6 shadow-xl dark:bg-gray-800">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Criar Acordo de Pagamento</h2>
          <button
            onClick={onClose}
            className="rounded p-1 text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-700"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Workshop Info */}
        <div className="mb-4 rounded-lg bg-gray-50 p-3 dark:bg-gray-700">
          <p className="font-medium text-gray-900 dark:text-white">{oficina.nomeFantasia}</p>
          <p className="text-sm text-gray-600 dark:text-gray-400">{oficina.cnpj} • {oficina.email}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Select Invoices */}
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Faturas a Incluir no Acordo
            </label>
            <div className="max-h-40 overflow-y-auto rounded-lg border border-gray-300 dark:border-gray-600">
              {oficina.faturasVencidasList.map((fatura) => (
                <label
                  key={fatura.faturaId}
                  className="flex cursor-pointer items-center justify-between border-b border-gray-200 p-3 hover:bg-gray-50 last:border-b-0 dark:border-gray-700 dark:hover:bg-gray-700/50"
                >
                  <div className="flex items-center gap-3">
                    <input
                      type="checkbox"
                      checked={selectedFaturas.includes(fatura.faturaId)}
                      onChange={(e) => handleSelectFatura(fatura.faturaId, e.target.checked)}
                      className="rounded border-gray-300 dark:border-gray-600"
                    />
                    <div>
                      <p className="text-sm font-medium text-gray-900 dark:text-white">{fatura.numero}</p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        {fatura.mesReferencia} • {fatura.diasAtraso} dias de atraso
                      </p>
                    </div>
                  </div>
                  <span className="font-medium text-gray-900 dark:text-white">{formatCurrency(fatura.valor)}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Discount and Installments */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Desconto (%)
              </label>
              <input
                type="number"
                min={0}
                max={50}
                value={percentualDesconto}
                onChange={(e) => setPercentualDesconto(Number(e.target.value))}
                className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Número de Parcelas *
              </label>
              <select
                required
                value={numeroParcelas}
                onChange={(e) => setNumeroParcelas(Number(e.target.value))}
                className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              >
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((n) => (
                  <option key={n} value={n}>
                    {n}x de {formatCurrency(valorFinal / n)}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Primeiro Vencimento *
            </label>
            <input
              type="date"
              required
              value={primeiroVencimento}
              onChange={(e) => setPrimeiroVencimento(e.target.value)}
              min={new Date().toISOString().split('T')[0]}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Observações
            </label>
            <textarea
              value={observacoes}
              onChange={(e) => setObservacoes(e.target.value)}
              rows={2}
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              placeholder="Condições especiais, observações..."
            />
          </div>

          {/* Summary */}
          <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-700/50">
            <div className="flex items-center gap-2 text-gray-700 dark:text-gray-300">
              <Calculator className="h-5 w-5" />
              <span className="font-medium">Resumo do Acordo</span>
            </div>
            <div className="mt-2 space-y-1 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600 dark:text-gray-400">Valor Original:</span>
                <span className="text-gray-900 dark:text-white">{formatCurrency(valorOriginal)}</span>
              </div>
              {valorDesconto > 0 && (
                <div className="flex justify-between text-green-600 dark:text-green-400">
                  <span>Desconto ({percentualDesconto}%):</span>
                  <span>-{formatCurrency(valorDesconto)}</span>
                </div>
              )}
              <div className="flex justify-between border-t border-gray-300 pt-1 dark:border-gray-600">
                <span className="font-medium text-gray-900 dark:text-white">Valor Final:</span>
                <span className="text-lg font-bold text-gray-900 dark:text-white">{formatCurrency(valorFinal)}</span>
              </div>
              <div className="flex justify-between text-blue-600 dark:text-blue-400">
                <span>Parcelas:</span>
                <span>{numeroParcelas}x de {formatCurrency(valorParcela)}</span>
              </div>
            </div>
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
              disabled={criarAcordo.isPending || selectedFaturas.length === 0}
              className="rounded-lg bg-green-600 px-4 py-2 text-white hover:bg-green-700 disabled:opacity-50 dark:bg-green-500 dark:hover:bg-green-600"
            >
              {criarAcordo.isPending ? 'Criando...' : 'Criar Acordo'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
