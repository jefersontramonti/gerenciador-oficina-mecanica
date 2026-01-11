/**
 * Esquemas de validação Zod para Ordens de Serviço
 */

import { z } from 'zod';
import { TipoItem, TipoCobrancaMaoObra, OrigemPeca } from '../types';

/**
 * Schema para Item da OS
 */
export const itemOSSchema = z.object({
  tipo: z.enum([TipoItem.PECA, TipoItem.SERVICO], {
    message: 'Tipo é obrigatório',
  }),
  origemPeca: z.enum([OrigemPeca.ESTOQUE, OrigemPeca.AVULSA, OrigemPeca.CLIENTE]).optional().nullable(),
  pecaId: z.string()
    .regex(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i, 'Peça inválida')
    .optional()
    .nullable(),
  descricao: z
    .string()
    .min(3, 'Descrição deve ter pelo menos 3 caracteres')
    .max(500, 'Descrição deve ter no máximo 500 caracteres'),
  quantidade: z.coerce.number().int('Quantidade deve ser um número inteiro').min(1, 'Quantidade deve ser pelo menos 1').default(1),
  valorUnitario: z.coerce.number().min(0, 'Valor unitário deve ser positivo').default(0),
  desconto: z.coerce.number().min(0, 'Desconto deve ser positivo').default(0),
  valorTotal: z.coerce.number().min(0).default(0), // Campo calculado para o formulário
}).refine(
  (item) => {
    // Se PECA, origemPeca é obrigatório
    if (item.tipo === TipoItem.PECA && !item.origemPeca) {
      return false;
    }
    return true;
  },
  { message: 'Peça requer origem definida (estoque, avulsa ou cliente)', path: ['origemPeca'] }
).refine(
  (item) => {
    // Se ESTOQUE, pecaId é obrigatório
    if (item.tipo === TipoItem.PECA && item.origemPeca === OrigemPeca.ESTOQUE && !item.pecaId) {
      return false;
    }
    return true;
  },
  { message: 'Peça do estoque requer seleção do item', path: ['pecaId'] }
).refine(
  (item) => {
    // Se AVULSA ou CLIENTE, descrição min 10 chars
    if (item.tipo === TipoItem.PECA && item.origemPeca && item.origemPeca !== OrigemPeca.ESTOQUE) {
      return item.descricao && item.descricao.length >= 10;
    }
    return true;
  },
  { message: 'Peça avulsa/cliente requer descrição detalhada (min 10 caracteres)', path: ['descricao'] }
);

/**
 * Schema para criar Ordem de Serviço
 */
export const createOrdemServicoSchema = z.object({
  veiculoId: z.string()
    .regex(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i, 'Veículo inválido')
    .min(1, 'Selecione um veículo'),
  usuarioId: z.string()
    .regex(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i, 'Mecânico inválido')
    .min(1, 'Selecione um mecânico'),
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
  // Modelo híbrido de mão de obra
  tipoCobrancaMaoObra: z.enum([TipoCobrancaMaoObra.VALOR_FIXO, TipoCobrancaMaoObra.POR_HORA], {
    message: 'Selecione o tipo de cobrança de mão de obra',
  }),
  valorMaoObra: z.coerce.number().min(0, 'Valor da mão de obra deve ser positivo').optional(),
  tempoEstimadoHoras: z.coerce.number().min(0.5, 'Mínimo 30 minutos').max(100, 'Máximo 100 horas').optional(),
  limiteHorasAprovado: z.coerce.number().min(0.5, 'Mínimo 30 minutos').max(100, 'Máximo 100 horas').optional(),
  descontoPercentual: z.coerce.number().min(0, 'Desconto percentual deve ser entre 0 e 100').max(100, 'Desconto percentual deve ser entre 0 e 100').default(0),
  descontoValor: z.coerce.number().min(0, 'Desconto em valor deve ser positivo').default(0),
  itens: z
    .array(itemOSSchema)
    .min(1, 'Adicione pelo menos 1 item (peça ou serviço)'),
}).refine(
  (data) => {
    // Se VALOR_FIXO, valorMaoObra é obrigatório
    if (data.tipoCobrancaMaoObra === TipoCobrancaMaoObra.VALOR_FIXO) {
      return data.valorMaoObra !== undefined && data.valorMaoObra >= 0;
    }
    return true;
  },
  { message: 'Valor da mão de obra é obrigatório para cobrança fixa', path: ['valorMaoObra'] }
).refine(
  (data) => {
    // Se POR_HORA, tempoEstimadoHoras e limiteHorasAprovado são obrigatórios
    if (data.tipoCobrancaMaoObra === TipoCobrancaMaoObra.POR_HORA) {
      return data.tempoEstimadoHoras !== undefined && data.tempoEstimadoHoras >= 0.5;
    }
    return true;
  },
  { message: 'Tempo estimado é obrigatório para cobrança por hora', path: ['tempoEstimadoHoras'] }
).refine(
  (data) => {
    // Se POR_HORA, limiteHorasAprovado é obrigatório
    if (data.tipoCobrancaMaoObra === TipoCobrancaMaoObra.POR_HORA) {
      return data.limiteHorasAprovado !== undefined && data.limiteHorasAprovado >= 0.5;
    }
    return true;
  },
  { message: 'Limite de horas é obrigatório para cobrança por hora', path: ['limiteHorasAprovado'] }
).refine(
  (data) => {
    // limiteHorasAprovado deve ser >= tempoEstimadoHoras
    if (data.tipoCobrancaMaoObra === TipoCobrancaMaoObra.POR_HORA && data.tempoEstimadoHoras && data.limiteHorasAprovado) {
      return data.limiteHorasAprovado >= data.tempoEstimadoHoras;
    }
    return true;
  },
  { message: 'Limite de horas deve ser maior ou igual ao tempo estimado', path: ['limiteHorasAprovado'] }
);

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
 * Schema para finalizar Ordem de Serviço (modelo POR_HORA)
 */
export const finalizarOSSchema = z.object({
  horasTrabalhadas: z.coerce.number().min(0.5, 'Mínimo 30 minutos (0.5 horas)').max(100, 'Máximo 100 horas'),
  observacoesFinais: z
    .string()
    .max(1000, 'Observações finais devem ter no máximo 1000 caracteres')
    .optional()
    .or(z.literal('')),
});

/**
 * Schema para formulário (com campos calculados)
 */
export const ordemServicoFormSchema = z.object({
  veiculoId: z.string()
    .regex(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i, 'Veículo inválido')
    .min(1, 'Selecione um veículo'),
  usuarioId: z.string()
    .regex(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i, 'Mecânico inválido')
    .min(1, 'Selecione um mecânico'),
  problemasRelatados: z
    .string()
    .min(10, 'Problemas relatados devem ter pelo menos 10 caracteres')
    .max(1000, 'Problemas relatados devem ter no máximo 1000 caracteres'),
  diagnostico: z.string().max(1000, 'Diagnóstico deve ter no máximo 1000 caracteres').default(''),
  observacoes: z.string().max(1000, 'Observações devem ter no máximo 1000 caracteres').default(''),
  dataAbertura: z.string(),
  dataPrevisao: z.string().default(''),
  // Modelo híbrido de mão de obra
  tipoCobrancaMaoObra: z.enum([TipoCobrancaMaoObra.VALOR_FIXO, TipoCobrancaMaoObra.POR_HORA]).default(TipoCobrancaMaoObra.VALOR_FIXO),
  valorMaoObra: z.coerce.number().min(0, 'Valor da mão de obra deve ser positivo').default(0),
  tempoEstimadoHoras: z.coerce.number().min(0).max(100).optional(),
  limiteHorasAprovado: z.coerce.number().min(0).max(100).optional(),
  horasTrabalhadas: z.coerce.number().min(0).max(100).optional(),
  valorHoraSnapshot: z.coerce.number().min(0).optional(),
  valorPecas: z.coerce.number().min(0).default(0), // Calculado
  valorTotal: z.coerce.number().min(0).default(0), // Calculado
  descontoPercentual: z.coerce.number().min(0).max(100).default(0),
  descontoValor: z.coerce.number().min(0).default(0),
  valorFinal: z.coerce.number().min(0).default(0), // Calculado
  itens: z.array(itemOSSchema).min(1, 'Adicione pelo menos 1 item'),
});

/**
 * Tipos inferidos dos schemas
 */
export type ItemOSFormData = z.infer<typeof itemOSSchema>;
export type CreateOrdemServicoFormData = z.infer<typeof createOrdemServicoSchema>;
export type UpdateOrdemServicoFormData = z.infer<typeof updateOrdemServicoSchema>;
export type CancelarOrdemServicoFormData = z.infer<typeof cancelarOrdemServicoSchema>;
export type FinalizarOSFormData = z.infer<typeof finalizarOSSchema>;
export type OrdemServicoFormData = z.infer<typeof ordemServicoFormSchema>;
