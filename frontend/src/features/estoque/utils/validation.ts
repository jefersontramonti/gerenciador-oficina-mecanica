/**
 * Schemas de validação Zod para Estoque/Peças
 */

import { z } from 'zod';
import { UnidadeMedida } from '../types';

// ==================== PEÇA ====================

export const createPecaSchema = z.object({
  codigo: z
    .string()
    .min(3, 'Código deve ter no mínimo 3 caracteres')
    .max(50, 'Código deve ter no máximo 50 caracteres')
    .trim(),
  descricao: z
    .string()
    .min(3, 'Descrição deve ter no mínimo 3 caracteres')
    .max(500, 'Descrição deve ter no máximo 500 caracteres')
    .trim(),
  marca: z
    .string()
    .max(100, 'Marca deve ter no máximo 100 caracteres')
    .trim()
    .optional()
    .or(z.literal('')),
  aplicacao: z
    .string()
    .max(500, 'Aplicação deve ter no máximo 500 caracteres')
    .trim()
    .optional()
    .or(z.literal('')),
  localizacao: z
    .string()
    .max(100, 'Localização deve ter no máximo 100 caracteres')
    .trim()
    .optional()
    .or(z.literal('')),
  localArmazenamentoId: z
    .string()
    .uuid('ID do local de armazenamento inválido')
    .optional()
    .or(z.literal('')),
  unidadeMedida: z.enum([
    UnidadeMedida.UNIDADE,
    UnidadeMedida.LITRO,
    UnidadeMedida.METRO,
    UnidadeMedida.QUILO,
  ], {
    message: 'Selecione uma unidade de medida válida',
  }),
  quantidadeMinima: z
    .number()
    .min(0, 'Quantidade mínima não pode ser negativa')
    .int('Quantidade mínima deve ser um número inteiro'),
  valorCusto: z
    .number()
    .min(0, 'Valor de custo não pode ser negativo'),
  valorVenda: z
    .number()
    .min(0, 'Valor de venda não pode ser negativo'),
}).refine(
  (data) => data.valorVenda >= data.valorCusto,
  {
    message: 'Valor de venda deve ser maior ou igual ao valor de custo',
    path: ['valorVenda'],
  }
);

export type CreatePecaFormData = z.infer<typeof createPecaSchema>;

export const updatePecaSchema = createPecaSchema.partial();

export type UpdatePecaFormData = z.infer<typeof updatePecaSchema>;

// ==================== MOVIMENTAÇÃO - ENTRADA ====================

export const createEntradaSchema = z.object({
  pecaId: z
    .string()
    .uuid('ID da peça inválido'),
  quantidade: z
    .number()
    .min(1, 'Quantidade deve ser maior que zero')
    .int('Quantidade deve ser um número inteiro'),
  valorUnitario: z
    .number()
    .min(0, 'Valor unitário não pode ser negativo'),
  motivo: z
    .string()
    .max(500, 'Motivo deve ter no máximo 500 caracteres')
    .trim()
    .optional()
    .or(z.literal('')),
  observacao: z
    .string()
    .max(1000, 'Observação deve ter no máximo 1000 caracteres')
    .trim()
    .optional()
    .or(z.literal('')),
});

export type CreateEntradaFormData = z.infer<typeof createEntradaSchema>;

// ==================== MOVIMENTAÇÃO - SAÍDA ====================

export const createSaidaSchema = (quantidadeAtual?: number) =>
  z.object({
    pecaId: z
      .string()
      .uuid('ID da peça inválido'),
    quantidade: z
      .number()
      .min(1, 'Quantidade deve ser maior que zero')
      .int('Quantidade deve ser um número inteiro')
      .refine(
        (val) => !quantidadeAtual || val <= quantidadeAtual,
        {
          message: `Quantidade não pode exceder o estoque atual (${quantidadeAtual || 0})`,
        }
      ),
    valorUnitario: z
      .number()
      .min(0, 'Valor unitário não pode ser negativo'),
    motivo: z
      .string()
      .max(500, 'Motivo deve ter no máximo 500 caracteres')
      .trim()
      .optional()
      .or(z.literal('')),
    observacao: z
      .string()
      .max(1000, 'Observação deve ter no máximo 1000 caracteres')
      .trim()
      .optional()
      .or(z.literal('')),
  });

export type CreateSaidaFormData = z.infer<ReturnType<typeof createSaidaSchema>>;

// ==================== MOVIMENTAÇÃO - AJUSTE ====================

export const createAjusteSchema = z.object({
  pecaId: z
    .string()
    .uuid('ID da peça inválido'),
  quantidadeNova: z
    .number()
    .min(0, 'Quantidade nova não pode ser negativa')
    .int('Quantidade nova deve ser um número inteiro'),
  valorUnitario: z
    .number()
    .min(0, 'Valor unitário não pode ser negativo'),
  motivo: z
    .string()
    .min(3, 'Motivo é obrigatório e deve ter no mínimo 3 caracteres')
    .max(500, 'Motivo deve ter no máximo 500 caracteres')
    .trim(),
  observacao: z
    .string()
    .max(1000, 'Observação deve ter no máximo 1000 caracteres')
    .trim()
    .optional()
    .or(z.literal('')),
});

export type CreateAjusteFormData = z.infer<typeof createAjusteSchema>;
