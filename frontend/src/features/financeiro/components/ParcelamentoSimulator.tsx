import { useState, useEffect } from 'react';
import { Calculator, CreditCard, Check, AlertCircle } from 'lucide-react';
import { useSimulacaoParcelamento } from '../hooks/useParcelamento';
import type { OpcaoParcelamento } from '../types/parcelamento';

interface ParcelamentoSimulatorProps {
  valor: number;
  onSelect?: (opcao: OpcaoParcelamento) => void;
  selectedParcelas?: number;
  className?: string;
}

export function ParcelamentoSimulator({
  valor,
  onSelect,
  selectedParcelas,
  className = '',
}: ParcelamentoSimulatorProps) {
  const { data: simulacao, isLoading, error } = useSimulacaoParcelamento(valor);
  const [selected, setSelected] = useState<number | null>(selectedParcelas || null);

  useEffect(() => {
    if (selectedParcelas) {
      setSelected(selectedParcelas);
    }
  }, [selectedParcelas]);

  const handleSelect = (opcao: OpcaoParcelamento) => {
    if (!opcao.disponivel) return;
    setSelected(opcao.parcelas);
    onSelect?.(opcao);
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  if (error) {
    return (
      <div className={`rounded-lg border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20 p-4 ${className}`}>
        <div className="flex items-center gap-2 text-red-600 dark:text-red-400">
          <AlertCircle className="h-5 w-5" />
          <span>Erro ao carregar opções de parcelamento</span>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className={`rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 ${className}`}>
        <div className="flex items-center justify-center gap-2 text-gray-500">
          <div className="h-5 w-5 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
          <span>Calculando parcelas...</span>
        </div>
      </div>
    );
  }

  if (!simulacao || simulacao.opcoes.length === 0) {
    return (
      <div className={`rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 ${className}`}>
        <div className="flex items-center gap-2 text-gray-500 dark:text-gray-400">
          <CreditCard className="h-5 w-5" />
          <span>Parcelamento não disponível para este valor</span>
        </div>
      </div>
    );
  }

  return (
    <div className={`rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 ${className}`}>
      <div className="flex items-center gap-2 p-4 border-b border-gray-200 dark:border-gray-700">
        <Calculator className="h-5 w-5 text-blue-600 dark:text-blue-400" />
        <h3 className="font-medium text-gray-900 dark:text-white">
          Opções de Parcelamento
        </h3>
        <span className="ml-auto text-sm text-gray-500 dark:text-gray-400">
          Valor: {formatCurrency(simulacao.valorOriginal)}
        </span>
      </div>

      <div className="p-4 space-y-2 max-h-80 overflow-y-auto">
        {simulacao.opcoes.map((opcao) => (
          <button
            key={opcao.parcelas}
            onClick={() => handleSelect(opcao)}
            disabled={!opcao.disponivel}
            className={`w-full flex items-center justify-between p-3 rounded-lg border transition-all ${
              selected === opcao.parcelas
                ? 'border-blue-600 bg-blue-50 dark:bg-blue-900/20 ring-2 ring-blue-600'
                : opcao.disponivel
                ? 'border-gray-200 dark:border-gray-600 hover:border-blue-300 dark:hover:border-blue-600 hover:bg-gray-50 dark:hover:bg-gray-700/50'
                : 'border-gray-200 dark:border-gray-700 bg-gray-100 dark:bg-gray-900 opacity-50 cursor-not-allowed'
            }`}
          >
            <div className="flex items-center gap-3">
              {selected === opcao.parcelas && (
                <div className="flex-shrink-0 h-5 w-5 rounded-full bg-blue-600 flex items-center justify-center">
                  <Check className="h-3 w-3 text-white" />
                </div>
              )}
              <div className="text-left">
                <p className="font-medium text-gray-900 dark:text-white">
                  {opcao.textoExibicao}
                </p>
                {!opcao.semJuros && opcao.valorJuros > 0 && (
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    Juros: {formatCurrency(opcao.valorJuros)} (CET: {opcao.cetAnual.toFixed(2)}% a.a.)
                  </p>
                )}
                {!opcao.disponivel && opcao.mensagemIndisponivel && (
                  <p className="text-xs text-red-500">{opcao.mensagemIndisponivel}</p>
                )}
              </div>
            </div>

            <div className="text-right">
              <p className="font-semibold text-gray-900 dark:text-white">
                {formatCurrency(opcao.valorTotal)}
              </p>
              {opcao.semJuros && opcao.parcelas > 1 && (
                <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-400">
                  Sem juros
                </span>
              )}
            </div>
          </button>
        ))}
      </div>

      {selected && (
        <div className="p-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700/50">
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600 dark:text-gray-400">
              Parcelas selecionadas:
            </span>
            <span className="font-semibold text-blue-600 dark:text-blue-400">
              {selected}x
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
