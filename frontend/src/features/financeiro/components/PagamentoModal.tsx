/**
 * Modal para criar pagamento com validação Zod e React Hook Form
 */

import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Modal } from '@/shared/components/ui/Modal';
import { TipoPagamento, TipoPagamentoLabels } from '../types/pagamento';
import { pagamentoSchema, type PagamentoFormData } from '../utils/validationSchemas';
import { useCriarPagamento } from '../hooks/usePagamentos';

interface PagamentoModalProps {
  isOpen: boolean;
  onClose: () => void;
  ordemServicoId: string;
  valorDefault?: number;
}

export function PagamentoModal({
  isOpen,
  onClose,
  ordemServicoId,
  valorDefault,
}: PagamentoModalProps) {
  const { mutate: criarPagamento, isPending } = useCriarPagamento();

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
    reset,
  } = useForm<PagamentoFormData>({
    resolver: zodResolver(pagamentoSchema),
    defaultValues: {
      valor: valorDefault,
      parcelas: 1,
      parcelaAtual: 1,
    },
  });

  const onSubmit = async (data: PagamentoFormData) => {
    criarPagamento(
      {
        ordemServicoId,
        tipo: data.tipo,
        valor: data.valor,
        parcelas: data.parcelas,
        parcelaAtual: data.parcelaAtual || 1,
        dataVencimento: data.dataVencimento || undefined,
        observacao: data.observacao || undefined,
      },
      {
        onSuccess: () => {
          reset();
          onClose();
        },
        onError: (error: any) => {
          if (error.response?.status === 409) {
            alert('Conflito: Pagamento já existe');
          } else {
            alert(error.response?.data?.message || 'Erro ao criar pagamento');
          }
        },
      }
    );
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Adicionar Pagamento" size="lg">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {/* Tipo de Pagamento */}
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">
            Tipo de Pagamento <span className="text-red-600">*</span>
          </label>
          <Controller
            name="tipo"
            control={control}
            render={({ field }) => (
              <select
                {...field}
                className={`
                  w-full rounded-lg border px-3 py-2
                  focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                  ${errors.tipo ? 'border-red-500' : 'border-gray-300'}
                `}
              >
                <option value="">Selecione o tipo</option>
                {Object.values(TipoPagamento).map((tipo) => (
                  <option key={tipo} value={tipo}>
                    {TipoPagamentoLabels[tipo]}
                  </option>
                ))}
              </select>
            )}
          />
          {errors.tipo && (
            <p className="mt-1 text-sm text-red-600">{errors.tipo.message}</p>
          )}
        </div>

        {/* Valor e Data de Vencimento */}
        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Valor (R$) <span className="text-red-600">*</span>
            </label>
            <input
              type="number"
              step="0.01"
              placeholder="0.00"
              {...register('valor', { valueAsNumber: true })}
              className={`
                w-full rounded-lg border px-3 py-2
                focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                ${errors.valor ? 'border-red-500' : 'border-gray-300'}
              `}
            />
            {errors.valor && (
              <p className="mt-1 text-sm text-red-600">{errors.valor.message}</p>
            )}
            {valorDefault && (
              <p className="mt-1 text-xs text-green-600">
                ✓ Valor sugerido: R$ {valorDefault.toFixed(2)}
              </p>
            )}
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Data de Vencimento
            </label>
            <input
              type="date"
              {...register('dataVencimento')}
              className={`
                w-full rounded-lg border px-3 py-2
                focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                ${errors.dataVencimento ? 'border-red-500' : 'border-gray-300'}
              `}
            />
            {errors.dataVencimento && (
              <p className="mt-1 text-sm text-red-600">{errors.dataVencimento.message}</p>
            )}
          </div>
        </div>

        {/* Parcelas */}
        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Número de Parcelas <span className="text-red-600">*</span>
            </label>
            <input
              type="number"
              min="1"
              max="12"
              {...register('parcelas', { valueAsNumber: true })}
              className={`
                w-full rounded-lg border px-3 py-2
                focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                ${errors.parcelas ? 'border-red-500' : 'border-gray-300'}
              `}
            />
            {errors.parcelas && (
              <p className="mt-1 text-sm text-red-600">{errors.parcelas.message}</p>
            )}
            <p className="mt-1 text-xs text-gray-500">Máximo de 12 parcelas</p>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Parcela Atual
            </label>
            <input
              type="number"
              min="1"
              {...register('parcelaAtual', { valueAsNumber: true })}
              className={`
                w-full rounded-lg border px-3 py-2
                focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                ${errors.parcelaAtual ? 'border-red-500' : 'border-gray-300'}
              `}
            />
            {errors.parcelaAtual && (
              <p className="mt-1 text-sm text-red-600">{errors.parcelaAtual.message}</p>
            )}
            <p className="mt-1 text-xs text-gray-500">
              Útil para registrar parcelas já pagas
            </p>
          </div>
        </div>

        {/* Observação */}
        <div>
          <label className="mb-1 block text-sm font-medium text-gray-700">
            Observação
          </label>
          <textarea
            placeholder="Adicione observações sobre o pagamento..."
            {...register('observacao')}
            className={`
              w-full rounded-lg border px-3 py-2
              focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
              ${errors.observacao ? 'border-red-500' : 'border-gray-300'}
            `}
            rows={3}
          />
          {errors.observacao && (
            <p className="mt-1 text-sm text-red-600">{errors.observacao.message}</p>
          )}
        </div>

        {/* Botões */}
        <div className="flex justify-end gap-2 border-t border-gray-200 pt-4">
          <button
            type="button"
            onClick={handleClose}
            disabled={isPending}
            className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={isPending}
            className="rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:bg-gray-400"
          >
            {isPending ? 'Salvando...' : 'Criar Pagamento'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
