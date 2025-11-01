/**
 * Esquemas de validação Zod para veículos
 */

import { z } from 'zod';

/**
 * Padrões de validação
 */
const placaPattern = /^[A-Z]{3}-?\d{4}$|^[A-Z]{3}\d[A-Z]\d{2}$/i; // ABC-1234 ou ABC1D23 (Mercosul)
const chassiPattern = /^[A-HJ-NPR-Z0-9]{17}$/i; // VIN - 17 caracteres
const currentYear = new Date().getFullYear();

/**
 * Schema para criar veículo
 */
export const createVeiculoSchema = z.object({
  clienteId: z.string().uuid('Cliente é obrigatório'),
  placa: z
    .string()
    .min(1, 'Placa é obrigatória')
    .regex(
      placaPattern,
      'Placa inválida. Use formato: ABC-1234 ou ABC1D23 (Mercosul)'
    )
    .transform((val) => val.toUpperCase().replace('-', '')), // Normaliza para ABC1234
  marca: z.string().min(2, 'Marca deve ter pelo menos 2 caracteres').max(50, 'Marca muito longa'),
  modelo: z
    .string()
    .min(2, 'Modelo deve ter pelo menos 2 caracteres')
    .max(100, 'Modelo muito longo'),
  ano: z
    .number({ message: 'Ano é obrigatório' })
    .int('Ano deve ser um número inteiro')
    .min(1900, 'Ano deve ser maior que 1900')
    .max(currentYear + 1, `Ano não pode ser maior que ${currentYear + 1}`),
  cor: z.string().max(30, 'Cor muito longa').optional().or(z.literal('')),
  chassi: z
    .string()
    .length(17, 'Chassi deve ter exatamente 17 caracteres')
    .regex(chassiPattern, 'Chassi inválido. Use apenas letras e números (sem I, O, Q)')
    .transform((val) => val.toUpperCase())
    .optional()
    .or(z.literal('')),
  quilometragem: z
    .number({ message: 'Quilometragem deve ser um número' })
    .int('Quilometragem deve ser um número inteiro')
    .min(0, 'Quilometragem não pode ser negativa')
    .optional(),
});

/**
 * Schema para atualizar veículo
 * Não permite alterar placa e clienteId
 */
export const updateVeiculoSchema = z.object({
  marca: z.string().min(2, 'Marca deve ter pelo menos 2 caracteres').max(50, 'Marca muito longa'),
  modelo: z
    .string()
    .min(2, 'Modelo deve ter pelo menos 2 caracteres')
    .max(100, 'Modelo muito longo'),
  ano: z
    .number({ message: 'Ano é obrigatório' })
    .int('Ano deve ser um número inteiro')
    .min(1900, 'Ano deve ser maior que 1900')
    .max(currentYear + 1, `Ano não pode ser maior que ${currentYear + 1}`),
  cor: z.string().max(30, 'Cor muito longa').optional().or(z.literal('')),
  chassi: z
    .string()
    .length(17, 'Chassi deve ter exatamente 17 caracteres')
    .regex(chassiPattern, 'Chassi inválido. Use apenas letras e números (sem I, O, Q)')
    .transform((val) => val.toUpperCase())
    .optional()
    .or(z.literal('')),
  quilometragem: z
    .number({ message: 'Quilometragem deve ser um número' })
    .int('Quilometragem deve ser um número inteiro')
    .min(0, 'Quilometragem não pode ser negativa')
    .optional(),
});

/**
 * Schema para atualizar apenas quilometragem
 */
export const updateQuilometragemSchema = z.object({
  quilometragem: z
    .number({ message: 'Quilometragem é obrigatória' })
    .int('Quilometragem deve ser um número inteiro')
    .min(0, 'Quilometragem não pode ser negativa'),
});

/**
 * Tipos inferidos dos schemas
 */
export type CreateVeiculoFormData = z.infer<typeof createVeiculoSchema>;
export type UpdateVeiculoFormData = z.infer<typeof updateVeiculoSchema>;
export type UpdateQuilometragemFormData = z.infer<typeof updateQuilometragemSchema>;
