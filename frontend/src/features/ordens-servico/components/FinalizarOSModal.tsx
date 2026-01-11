/**
 * Modal para finalizar Ordem de Serviço
 * Usado quando a OS está no modelo POR_HORA para informar horas trabalhadas
 */

import { useState } from 'react';
import { useForm, type Resolver } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Clock, AlertCircle, CheckCircle2 } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/shared/components/ui/dialog';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Label } from '@/shared/components/ui/label';
import { Textarea } from '@/shared/components/ui/textarea';
import { useFinalizarComHoras, useFinalizarOrdemServico } from '../hooks/useOrdensServico';
import { finalizarOSSchema, type FinalizarOSFormData } from '../utils/validation';
import type { OrdemServico } from '../types';

interface FinalizarOSModalProps {
  os: OrdemServico;
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

/**
 * Formatar valor monetário em Reais
 */
const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

export function FinalizarOSModal({ os, isOpen, onClose, onSuccess }: FinalizarOSModalProps) {
  const [error, setError] = useState<string | null>(null);

  const finalizarComHorasMutation = useFinalizarComHoras();
  const finalizarMutation = useFinalizarOrdemServico();

  const isPorHora = os.tipoCobrancaMaoObra === 'POR_HORA';

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<FinalizarOSFormData>({
    resolver: zodResolver(finalizarOSSchema) as Resolver<FinalizarOSFormData>,
    defaultValues: {
      horasTrabalhadas: os.tempoEstimadoHoras || 1,
      observacoesFinais: '',
    },
  });

  const horasTrabalhadas = watch('horasTrabalhadas');

  // Calcular valor da mão de obra baseado nas horas
  const valorMaoObraCalculado = isPorHora && os.valorHoraSnapshot
    ? horasTrabalhadas * os.valorHoraSnapshot
    : os.valorMaoObra;

  // Calcular total final
  const totalFinal = os.valorPecas + valorMaoObraCalculado - (os.descontoValor || 0);

  // Verificar se excede limite aprovado
  const excedeLimit = isPorHora && os.limiteHorasAprovado
    ? horasTrabalhadas > os.limiteHorasAprovado
    : false;

  const handleFinalizar = async (data: FinalizarOSFormData) => {
    setError(null);

    try {
      if (isPorHora) {
        await finalizarComHorasMutation.mutateAsync({
          id: os.id,
          data: {
            horasTrabalhadas: data.horasTrabalhadas,
            observacoesFinais: data.observacoesFinais || undefined,
          },
        });
      } else {
        await finalizarMutation.mutateAsync(os.id);
      }

      onSuccess?.();
      onClose();
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Erro ao finalizar OS';
      setError(errorMessage);
    }
  };

  const isLoading = finalizarComHorasMutation.isPending || finalizarMutation.isPending;

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <CheckCircle2 className="h-5 w-5 text-green-600" />
            Finalizar OS #{os.numero}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(handleFinalizar)} className="space-y-4">
          {/* Informações do modelo de cobrança */}
          {isPorHora ? (
            <div className="space-y-4">
              {/* Badge do modelo */}
              <div className="flex items-center gap-2 rounded-lg bg-blue-50 p-3 dark:bg-blue-900/20">
                <Clock className="h-5 w-5 text-blue-600" />
                <div>
                  <p className="text-sm font-medium text-blue-900 dark:text-blue-100">
                    Cobrança por Hora
                  </p>
                  <p className="text-xs text-blue-700 dark:text-blue-300">
                    Valor/hora: {formatCurrency(os.valorHoraSnapshot || 0)}
                  </p>
                </div>
              </div>

              {/* Campo de horas trabalhadas */}
              <div className="space-y-2">
                <Label htmlFor="horasTrabalhadas">Horas Trabalhadas *</Label>
                <Input
                  id="horasTrabalhadas"
                  type="number"
                  step="0.5"
                  min="0.5"
                  max={100}
                  {...register('horasTrabalhadas', { valueAsNumber: true })}
                  className={excedeLimit ? 'border-red-500' : ''}
                />
                {errors.horasTrabalhadas && (
                  <p className="text-sm text-red-500">{errors.horasTrabalhadas.message}</p>
                )}

                {/* Limite aprovado */}
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-600 dark:text-gray-400">
                    Tempo estimado: {os.tempoEstimadoHoras}h
                  </span>
                  <span className={`font-medium ${excedeLimit ? 'text-red-600' : 'text-gray-600 dark:text-gray-400'}`}>
                    Limite aprovado: {os.limiteHorasAprovado}h
                  </span>
                </div>

                {/* Alerta de limite excedido */}
                {excedeLimit && (
                  <div className="flex items-start gap-2 rounded-lg bg-red-50 p-3 dark:bg-red-900/20">
                    <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
                    <div className="text-sm text-red-800 dark:text-red-200">
                      <p className="font-medium">Limite excedido!</p>
                      <p>
                        As horas trabalhadas excedem o limite aprovado pelo cliente.
                        Será necessária nova aprovação.
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="rounded-lg bg-gray-50 p-3 dark:bg-gray-800">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Cobrança: <span className="font-medium">Valor Fixo</span>
              </p>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Mão de obra: <span className="font-medium">{formatCurrency(os.valorMaoObra)}</span>
              </p>
            </div>
          )}

          {/* Observações finais */}
          <div className="space-y-2">
            <Label htmlFor="observacoesFinais">Observações Finais (opcional)</Label>
            <Textarea
              id="observacoesFinais"
              placeholder="Ex: Serviço concluído conforme solicitado..."
              rows={3}
              {...register('observacoesFinais')}
            />
            {errors.observacoesFinais && (
              <p className="text-sm text-red-500">{errors.observacoesFinais.message}</p>
            )}
          </div>

          {/* Resumo financeiro */}
          <div className="space-y-2 rounded-lg border border-gray-200 p-4 dark:border-gray-700">
            <h4 className="font-medium text-gray-900 dark:text-white">Resumo Financeiro</h4>

            <div className="space-y-1 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600 dark:text-gray-400">Peças:</span>
                <span>{formatCurrency(os.valorPecas)}</span>
              </div>

              <div className="flex justify-between">
                <span className="text-gray-600 dark:text-gray-400">
                  Mão de Obra {isPorHora && `(${horasTrabalhadas}h)`}:
                </span>
                <span>{formatCurrency(valorMaoObraCalculado)}</span>
              </div>

              {(os.descontoValor ?? 0) > 0 && (
                <div className="flex justify-between text-red-600">
                  <span>Desconto:</span>
                  <span>-{formatCurrency(os.descontoValor)}</span>
                </div>
              )}

              <div className="border-t border-gray-200 pt-2 dark:border-gray-700">
                <div className="flex justify-between text-lg font-bold">
                  <span>Total:</span>
                  <span className="text-green-600">{formatCurrency(totalFinal)}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Erro */}
          {error && (
            <div className="flex items-center gap-2 rounded-lg bg-red-50 p-3 text-red-800 dark:bg-red-900/20 dark:text-red-200">
              <AlertCircle className="h-5 w-5" />
              <span className="text-sm">{error}</span>
            </div>
          )}

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={isLoading}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={isLoading || excedeLimit}
            >
              {isLoading ? 'Finalizando...' : 'Finalizar OS'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
