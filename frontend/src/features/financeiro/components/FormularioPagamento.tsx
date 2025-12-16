/**
 * Formulário para criar pagamento
 */

import { useState, type FormEvent } from 'react';
import { TipoPagamento, TipoPagamentoLabels } from '../types/pagamento';
import { useCriarPagamento } from '../hooks/usePagamentos';

interface FormularioPagamentoProps {
  ordemServicoId: string;
  valorDefault?: number; // Valor padrão para pré-preencher
  onSuccess?: () => void;
}

export function FormularioPagamento({
  ordemServicoId,
  valorDefault,
  onSuccess
}: FormularioPagamentoProps) {
  const { mutate: criarPagamento, isPending } = useCriarPagamento();

  const [formData, setFormData] = useState({
    tipo: '' as TipoPagamento | '',
    valor: valorDefault ? valorDefault.toFixed(2) : '',
    parcelas: '1',
    parcelaAtual: '1',
    dataVencimento: '',
    observacao: ''
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();

    if (!formData.tipo || !formData.valor) {
      alert('Preencha os campos obrigatórios');
      return;
    }

    criarPagamento(
      {
        ordemServicoId,
        tipo: formData.tipo,
        valor: parseFloat(formData.valor),
        parcelas: parseInt(formData.parcelas) || 1,
        parcelaAtual: parseInt(formData.parcelaAtual) || 1,
        dataVencimento: formData.dataVencimento || undefined,
        observacao: formData.observacao || undefined
      },
      {
        onSuccess: () => {
          setFormData({
            tipo: '',
            valor: '',
            parcelas: '1',
            parcelaAtual: '1',
            dataVencimento: '',
            observacao: ''
          });
          onSuccess?.();
        }
      }
    );
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {/* Tipo de Pagamento */}
      <div>
        <label className="mb-1 block text-sm font-medium text-gray-700">
          Tipo de Pagamento *
        </label>
        <select
          className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
          value={formData.tipo}
          onChange={(e) =>
            setFormData({ ...formData, tipo: e.target.value as TipoPagamento })
          }
          required
        >
          <option value="">Selecione o tipo</option>
          {Object.values(TipoPagamento).map((tipo) => (
            <option key={tipo} value={tipo}>
              {TipoPagamentoLabels[tipo]}
            </option>
          ))}
        </select>
      </div>

      {/* Valor e Data de Vencimento */}
      <div className="grid gap-4 md:grid-cols-2">
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">
            Valor (R$) *
          </label>
          <input
            type="number"
            step="0.01"
            min="0.01"
            placeholder="0.00"
            className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            value={formData.valor}
            onChange={(e) => setFormData({ ...formData, valor: e.target.value })}
            required
          />
          {valorDefault && (
            <p className="mt-1 text-xs text-green-600">
              ✓ Valor pré-preenchido com o total da OS
            </p>
          )}
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">
            Data de Vencimento
          </label>
          <input
            type="date"
            className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            value={formData.dataVencimento}
            onChange={(e) =>
              setFormData({ ...formData, dataVencimento: e.target.value })
            }
          />
        </div>
      </div>

      {/* Parcelas */}
      <div className="grid gap-4 md:grid-cols-2">
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">
            Número de Parcelas *
          </label>
          <input
            type="number"
            min="1"
            max="12"
            placeholder="1"
            className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            value={formData.parcelas}
            onChange={(e) => setFormData({ ...formData, parcelas: e.target.value })}
            required
          />
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">
            Parcela Atual
          </label>
          <input
            type="number"
            min="1"
            placeholder="1"
            className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            value={formData.parcelaAtual}
            onChange={(e) =>
              setFormData({ ...formData, parcelaAtual: e.target.value })
            }
          />
        </div>
      </div>

      {/* Observação */}
      <div>
        <label className="mb-1 block text-sm font-medium text-gray-700">
          Observação
        </label>
        <textarea
          placeholder="Adicione observações sobre o pagamento..."
          className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
          rows={3}
          value={formData.observacao}
          onChange={(e) => setFormData({ ...formData, observacao: e.target.value })}
        />
      </div>

      {/* Botão */}
      <div className="flex justify-end gap-2">
        <button
          type="submit"
          disabled={isPending}
          className="rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:bg-gray-400"
        >
          {isPending ? 'Salvando...' : 'Criar Pagamento'}
        </button>
      </div>
    </form>
  );
}
