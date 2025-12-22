/**
 * Modal para registrar movimentações de estoque (Entrada/Saída/Ajuste)
 */

import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowDownCircle, ArrowUpCircle, Settings } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/shared/components/ui/dialog';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Label } from '@/shared/components/ui/label';
import { Textarea } from '@/shared/components/ui/textarea';
import { formatCurrency } from '@/shared/utils/formatters';
import {
  useRegistrarEntrada,
  useRegistrarSaida,
  useRegistrarAjuste,
} from '../hooks/useMovimentacoes';
import {
  createEntradaSchema,
  createSaidaSchema,
  createAjusteSchema,
  type CreateEntradaFormData,
  type CreateSaidaFormData,
  type CreateAjusteFormData,
} from '../utils/validation';
import type { PecaResumo } from '../types';
import { UnidadeMedidaBadge } from './UnidadeMedidaBadge';

interface MovimentacaoModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
  tipo: 'ENTRADA' | 'SAIDA' | 'AJUSTE';
  peca: PecaResumo & { quantidadeAtual: number };
}

type FormData = CreateEntradaFormData | CreateSaidaFormData | CreateAjusteFormData;

export const MovimentacaoModal = ({
  isOpen,
  onClose,
  onSuccess,
  tipo,
  peca,
}: MovimentacaoModalProps) => {
  const [submitError, setSubmitError] = useState<string | null>(null);

  const registrarEntrada = useRegistrarEntrada();
  const registrarSaida = useRegistrarSaida();
  const registrarAjuste = useRegistrarAjuste();

  // Escolher schema e mutation baseado no tipo
  const { schema, mutation, title, icon, description } = useMemo(() => {
    switch (tipo) {
      case 'ENTRADA':
        return {
          schema: createEntradaSchema,
          mutation: registrarEntrada,
          title: 'Registrar Entrada de Estoque',
          icon: <ArrowDownCircle className="h-5 w-5 text-green-600 dark:text-green-400" />,
          description: 'Adicione itens ao estoque',
        };
      case 'SAIDA':
        return {
          schema: createSaidaSchema(peca.quantidadeAtual),
          mutation: registrarSaida,
          title: 'Registrar Saída de Estoque',
          icon: <ArrowUpCircle className="h-5 w-5 text-red-600 dark:text-red-400" />,
          description: 'Remova itens do estoque',
        };
      case 'AJUSTE':
        return {
          schema: createAjusteSchema,
          mutation: registrarAjuste,
          title: 'Ajustar Inventário',
          icon: <Settings className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />,
          description: 'Corrija a quantidade em estoque',
        };
    }
  }, [tipo, peca.quantidadeAtual, registrarEntrada, registrarSaida, registrarAjuste]);

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      pecaId: peca.id,
      quantidade: tipo !== 'AJUSTE' ? 1 : undefined,
      quantidadeNova: tipo === 'AJUSTE' ? peca.quantidadeAtual : undefined,
      valorUnitario: 0,
      motivo: '',
      observacao: '',
    },
  });

  // Resetar form quando modal abre
  useEffect(() => {
    if (isOpen) {
      reset({
        pecaId: peca.id,
        quantidade: tipo !== 'AJUSTE' ? 1 : undefined,
        quantidadeNova: tipo === 'AJUSTE' ? peca.quantidadeAtual : undefined,
        valorUnitario: 0,
        motivo: '',
        observacao: '',
      });
    }
  }, [isOpen, peca, tipo, reset]);

  const quantidade = watch('quantidade') as number | undefined;
  const quantidadeNova = watch('quantidadeNova') as number | undefined;
  const valorUnitario = watch('valorUnitario') as number;

  // Cálculos
  const valorTotal = tipo !== 'AJUSTE' && quantidade ? quantidade * valorUnitario : 0;
  const diferencaAjuste =
    tipo === 'AJUSTE' && quantidadeNova !== undefined
      ? quantidadeNova - peca.quantidadeAtual
      : 0;

  const onSubmit = async (data: FormData) => {
    try {
      setSubmitError(null); // Clear previous errors

      // Clean up empty strings for optional fields
      const cleanedData = {
        ...data,
        motivo: data.motivo?.trim() || undefined,
        observacao: data.observacao?.trim() || undefined,
      };

      await mutation.mutateAsync(cleanedData as any);
      onClose();
      onSuccess?.(); // Trigger refresh after closing
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message ||
        error.message ||
        'Erro ao processar operação. Tente novamente.';
      setSubmitError(errorMessage);
      // Don't close modal - keep it open to show error
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <div className="flex items-center gap-2">
            {icon}
            <DialogTitle>{title}</DialogTitle>
          </div>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>

        {/* Info da Peça */}
        <div className="bg-muted p-3 rounded-md">
          <div className="flex items-start justify-between">
            <div>
              <p className="font-medium">{peca.codigo}</p>
              <p className="text-sm text-muted-foreground">{peca.descricao}</p>
            </div>
            <UnidadeMedidaBadge unidade={peca.unidadeMedida} />
          </div>
          <p className="text-sm mt-2">
            Quantidade atual: <span className="font-medium">{peca.quantidadeAtual}</span>
          </p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* Campo de Quantidade ou Quantidade Nova */}
          {tipo !== 'AJUSTE' ? (
            <div className="space-y-2">
              <Label htmlFor="quantidade">Quantidade *</Label>
              <Input
                id="quantidade"
                type="number"
                min="1"
                step="1"
                {...register('quantidade', { valueAsNumber: true })}
              />
              {'quantidade' in errors && errors.quantidade && (
                <p className="text-sm text-destructive">{errors.quantidade?.message}</p>
              )}
              {tipo === 'SAIDA' && quantidade && quantidade > peca.quantidadeAtual && (
                <div className="bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800 rounded-md p-2">
                  <p className="text-xs text-red-800 dark:text-red-300 font-medium">
                    ⚠️ Estoque insuficiente! Disponível: {peca.quantidadeAtual}
                  </p>
                </div>
              )}
            </div>
          ) : (
            <div className="space-y-2">
              <Label htmlFor="quantidadeNova">Quantidade Nova *</Label>
              <Input
                id="quantidadeNova"
                type="number"
                min="0"
                step="1"
                {...register('quantidadeNova', { valueAsNumber: true })}
              />
              {'quantidadeNova' in errors && errors.quantidadeNova && (
                <p className="text-sm text-destructive">{errors.quantidadeNova?.message}</p>
              )}
              {quantidadeNova !== undefined && (
                <p className="text-sm text-muted-foreground">
                  Diferença:{' '}
                  <span
                    className={
                      diferencaAjuste > 0
                        ? 'text-green-600 dark:text-green-400 font-medium'
                        : diferencaAjuste < 0
                        ? 'text-red-600 dark:text-red-400 font-medium'
                        : 'font-medium'
                    }
                  >
                    {diferencaAjuste > 0 ? '+' : ''}
                    {diferencaAjuste}
                  </span>
                </p>
              )}
            </div>
          )}

          {/* Valor Unitário */}
          <div className="space-y-2">
            <Label htmlFor="valorUnitario">Valor Unitário *</Label>
            <Input
              id="valorUnitario"
              type="number"
              min="0"
              step="0.01"
              placeholder="0.00"
              {...register('valorUnitario', { valueAsNumber: true })}
            />
            {errors.valorUnitario && (
              <p className="text-sm text-destructive">{errors.valorUnitario.message}</p>
            )}
          </div>

          {/* Motivo */}
          <div className="space-y-2">
            <Label htmlFor="motivo">
              Motivo {tipo === 'AJUSTE' ? '*' : '(opcional)'}
            </Label>
            <Input
              id="motivo"
              type="text"
              placeholder={
                tipo === 'ENTRADA'
                  ? 'Ex: Compra de fornecedor'
                  : tipo === 'SAIDA'
                  ? 'Ex: Uso interno'
                  : 'Ex: Contagem física divergente'
              }
              {...register('motivo')}
            />
            {errors.motivo && (
              <p className="text-sm text-destructive">{errors.motivo.message}</p>
            )}
          </div>

          {/* Observação */}
          <div className="space-y-2">
            <Label htmlFor="observacao">Observação (opcional)</Label>
            <Textarea
              id="observacao"
              rows={2}
              placeholder="Informações adicionais"
              {...register('observacao')}
            />
            {errors.observacao && (
              <p className="text-sm text-destructive">{errors.observacao.message}</p>
            )}
          </div>

          {/* Resumo */}
          {tipo !== 'AJUSTE' && valorTotal > 0 && (
            <div className="bg-primary/5 p-3 rounded-md">
              <p className="text-sm font-medium">Valor Total</p>
              <p className="text-lg font-bold text-primary">{formatCurrency(valorTotal)}</p>
            </div>
          )}

          {/* Error Alert */}
          {submitError && (
            <div className="bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800 rounded-md p-3">
              <p className="text-sm text-red-800 dark:text-red-300 font-medium">{submitError}</p>
            </div>
          )}

          {/* Botões */}
          <div className="flex gap-3 justify-end pt-4">
            <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting}
              className={
                tipo === 'ENTRADA'
                  ? 'bg-green-600 hover:bg-green-700 dark:bg-green-700 dark:hover:bg-green-600'
                  : tipo === 'SAIDA'
                  ? 'bg-red-600 hover:bg-red-700 dark:bg-red-700 dark:hover:bg-red-600'
                  : 'bg-yellow-600 hover:bg-yellow-700 dark:bg-yellow-700 dark:hover:bg-yellow-600'
              }
            >
              {isSubmitting ? 'Salvando...' : 'Confirmar'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
