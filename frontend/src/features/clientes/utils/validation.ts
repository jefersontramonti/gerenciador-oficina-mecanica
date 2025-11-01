import { z } from 'zod';

/**
 * Cliente form validation schemas
 */

// Helper regex patterns
const cpfPattern = /^\d{3}\.\d{3}\.\d{3}-\d{2}$/;
const cnpjPattern = /^\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2}$/;
const telefonePattern = /^(\(\d{2}\)\s?)?\d{4,5}-?\d{4}$/;
const cepPattern = /^\d{5}-\d{3}$/;
const ufPattern = /^[A-Z]{2}$/;

// Create cliente validation
export const createClienteSchema = z.object({
  tipo: z.enum(['PESSOA_FISICA', 'PESSOA_JURIDICA'], {
    message: 'Tipo é obrigatório',
  }),
  nome: z.string().min(3, 'Nome deve ter pelo menos 3 caracteres').max(150),
  cpfCnpj: z
    .string()
    .min(1, 'CPF/CNPJ é obrigatório')
    .refine(
      (val) => cpfPattern.test(val) || cnpjPattern.test(val),
      'CPF/CNPJ inválido. Use formato: 000.000.000-00 ou 00.000.000/0000-00'
    ),
  email: z.string().email('Email inválido').max(100).optional().or(z.literal('')),
  telefone: z
    .string()
    .regex(telefonePattern, 'Telefone inválido. Use formato: (00) 0000-0000')
    .optional()
    .or(z.literal('')),
  celular: z
    .string()
    .regex(telefonePattern, 'Celular inválido. Use formato: (00) 00000-0000')
    .optional()
    .or(z.literal('')),
  logradouro: z.string().max(200).optional().or(z.literal('')),
  numero: z.string().max(10).optional().or(z.literal('')),
  complemento: z.string().max(100).optional().or(z.literal('')),
  bairro: z.string().max(100).optional().or(z.literal('')),
  cidade: z.string().max(100).optional().or(z.literal('')),
  estado: z
    .string()
    .regex(ufPattern, 'Estado deve ter 2 letras maiúsculas (ex: SP)')
    .optional()
    .or(z.literal('')),
  cep: z
    .string()
    .regex(cepPattern, 'CEP inválido. Use formato: 00000-000')
    .optional()
    .or(z.literal('')),
});

// Update cliente validation (same as create but without tipo and cpfCnpj)
export const updateClienteSchema = z.object({
  nome: z.string().min(3, 'Nome deve ter pelo menos 3 caracteres').max(150),
  email: z.string().email('Email inválido').max(100).optional().or(z.literal('')),
  telefone: z
    .string()
    .regex(telefonePattern, 'Telefone inválido. Use formato: (00) 0000-0000')
    .optional()
    .or(z.literal('')),
  celular: z
    .string()
    .regex(telefonePattern, 'Celular inválido. Use formato: (00) 00000-0000')
    .optional()
    .or(z.literal('')),
  logradouro: z.string().max(200).optional().or(z.literal('')),
  numero: z.string().max(10).optional().or(z.literal('')),
  complemento: z.string().max(100).optional().or(z.literal('')),
  bairro: z.string().max(100).optional().or(z.literal('')),
  cidade: z.string().max(100).optional().or(z.literal('')),
  estado: z
    .string()
    .regex(ufPattern, 'Estado deve ter 2 letras maiúsculas (ex: SP)')
    .optional()
    .or(z.literal('')),
  cep: z
    .string()
    .regex(cepPattern, 'CEP inválido. Use formato: 00000-000')
    .optional()
    .or(z.literal('')),
});

export type CreateClienteFormData = z.infer<typeof createClienteSchema>;
export type UpdateClienteFormData = z.infer<typeof updateClienteSchema>;
