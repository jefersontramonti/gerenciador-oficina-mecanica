/**
 * Schemas de validação Zod para o módulo financeiro
 */

import { z } from 'zod';
import { TipoPagamento } from '../types/pagamento';

export const pagamentoSchema = z.object({
  tipo: z.nativeEnum(TipoPagamento, {
    message: 'Tipo de pagamento é obrigatório',
  }),
  valor: z
    .number({
      message: 'Valor deve ser um número',
    })
    .positive('Valor deve ser maior que zero')
    .min(0.01, 'Valor mínimo é R$ 0,01')
    .max(999999.99, 'Valor máximo é R$ 999.999,99'),
  parcelas: z
    .number({
      message: 'Parcelas deve ser um número',
    })
    .int('Parcelas deve ser um número inteiro')
    .min(1, 'Mínimo de 1 parcela')
    .max(12, 'Máximo de 12 parcelas'),
  parcelaAtual: z
    .number()
    .int('Parcela atual deve ser um número inteiro')
    .min(1, 'Parcela atual deve ser maior que zero')
    .optional(),
  dataVencimento: z.string().optional(),
  observacao: z
    .string()
    .max(1000, 'Observação deve ter no máximo 1000 caracteres')
    .optional(),
});

export type PagamentoFormData = z.infer<typeof pagamentoSchema>;

export const confirmarPagamentoSchema = z.object({
  dataPagamento: z.string({
    message: 'Data de pagamento é obrigatória',
  }),
  comprovante: z.string().optional(),
});

export type ConfirmarPagamentoFormData = z.infer<typeof confirmarPagamentoSchema>;
