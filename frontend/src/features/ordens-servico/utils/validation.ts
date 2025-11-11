/**
 * Esquemas de validação Zod para Ordens de Serviço
 */

import { z } from 'zod';
import { TipoItem } from '../types';

/**
 * Schema para Item da OS
 */
export const itemOSSchema = z.object({
  tipo: z.enum([TipoItem.PECA, TipoItem.SERVICO], {
    message: 'Tipo é obrigatório',
  }),
  pecaId: z.string().uuid().optional(),
  descricao: z
    .string()
    .min(3, 'Descrição deve ter pelo menos 3 caracteres')
    .max(255, 'Descrição deve ter no máximo 255 caracteres'),
  quantidade: z
    .number()
    .int('Quantidade deve ser um número inteiro')
    .min(1, 'Quantidade deve ser pelo menos 1'),
  valorUnitario: z.number().min(0, 'Valor unitário deve ser positivo'),
  desconto: z
    .number()
    .min(0, 'Desconto deve ser positivo')
    .default(0),
  valorTotal: z.number().min(0).default(0), // Campo calculado para o formulário
});

/**
 * Schema para criar Ordem de Serviço
 */
export const createOrdemServicoSchema = z.object({
  veiculoId: z.string().uuid('Veículo inválido'),
  usuarioId: z.string().uuid('Mecânico inválido'),
  problemasRelatados: z
    .string()
    .min(10, 'Problemas relatados devem ter pelo menos 10 caracteres')
    .max(1000, 'Problemas relatados devem ter no máximo 1000 caracteres'),
  diagnostico: z
    .string()
    .max(1000, 'Diagnóstico deve ter no máximo 1000 caracteres')
    .optional()
    .or(z.literal('')),
  observacoes: z
    .string()
    .max(1000, 'Observações devem ter no máximo 1000 caracteres')
    .optional()
    .or(z.literal('')),
  dataPrevisao: z
    .string()
    .optional()
    .refine(
      (date) => {
        if (!date) return true;
        const previsao = new Date(date);
        const hoje = new Date();
        hoje.setHours(0, 0, 0, 0);
        return previsao >= hoje;
      },
      { message: 'Data de previsão não pode ser no passado' }
    ),
  valorMaoObra: z.number().min(0, 'Valor da mão de obra deve ser positivo'),
  descontoPercentual: z
    .number()
    .min(0, 'Desconto percentual deve ser entre 0 e 100')
    .max(100, 'Desconto percentual deve ser entre 0 e 100')
    .optional()
    .default(0),
  descontoValor: z
    .number()
    .min(0, 'Desconto em valor deve ser positivo')
    .optional()
    .default(0),
  itens: z
    .array(itemOSSchema)
    .min(1, 'Adicione pelo menos 1 item (peça ou serviço)'),
});

/**
 * Schema para atualizar Ordem de Serviço
 * Mesmos campos que criar, mas todos opcionais
 */
export const updateOrdemServicoSchema = createOrdemServicoSchema.partial();

/**
 * Schema para cancelar Ordem de Serviço
 */
export const cancelarOrdemServicoSchema = z.object({
  motivo: z
    .string()
    .min(10, 'Motivo do cancelamento deve ter pelo menos 10 caracteres')
    .max(500, 'Motivo do cancelamento deve ter no máximo 500 caracteres'),
});

/**
 * Schema para formulário (com campos calculados)
 */
export const ordemServicoFormSchema = z.object({
  veiculoId: z.string().uuid('Veículo inválido'),
  usuarioId: z.string().uuid('Mecânico inválido'),
  problemasRelatados: z
    .string()
    .min(10, 'Problemas relatados devem ter pelo menos 10 caracteres')
    .max(1000, 'Problemas relatados devem ter no máximo 1000 caracteres'),
  diagnostico: z.string().max(1000, 'Diagnóstico deve ter no máximo 1000 caracteres').default(''),
  observacoes: z.string().max(1000, 'Observações devem ter no máximo 1000 caracteres').default(''),
  dataAbertura: z.string(),
  dataPrevisao: z.string().default(''),
  valorMaoObra: z.number().min(0, 'Valor da mão de obra deve ser positivo').default(0),
  valorPecas: z.number().min(0).default(0), // Calculado
  valorTotal: z.number().min(0).default(0), // Calculado
  descontoPercentual: z.number().min(0).max(100).default(0),
  descontoValor: z.number().min(0).default(0),
  valorFinal: z.number().min(0).default(0), // Calculado
  itens: z.array(itemOSSchema).min(1, 'Adicione pelo menos 1 item'),
});

/**
 * Tipos inferidos dos schemas
 */
export type ItemOSFormData = z.infer<typeof itemOSSchema>;
export type CreateOrdemServicoFormData = z.infer<typeof createOrdemServicoSchema>;
export type UpdateOrdemServicoFormData = z.infer<typeof updateOrdemServicoSchema>;
export type CancelarOrdemServicoFormData = z.infer<typeof cancelarOrdemServicoSchema>;
export type OrdemServicoFormData = z.infer<typeof ordemServicoFormSchema>;
