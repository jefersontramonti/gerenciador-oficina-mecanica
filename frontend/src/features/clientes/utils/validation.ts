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

/**
 * Remove formatação do CPF/CNPJ (pontos, traços, barras)
 */
const removeFormatacao = (documento: string): string => {
  return documento.replace(/[^\d]/g, '');
};

/**
 * Valida CPF com dígitos verificadores (algoritmo Receita Federal)
 */
const isValidCpf = (cpf: string): boolean => {
  const limpo = removeFormatacao(cpf);

  if (limpo.length !== 11) return false;

  // Rejeita CPFs com todos os dígitos iguais
  if (/^(\d)\1{10}$/.test(limpo)) return false;

  // Calcula primeiro dígito verificador
  let soma = 0;
  for (let i = 0; i < 9; i++) {
    soma += parseInt(limpo.charAt(i)) * (10 - i);
  }
  let resto = soma % 11;
  const primeiroDigito = resto < 2 ? 0 : 11 - resto;

  if (parseInt(limpo.charAt(9)) !== primeiroDigito) return false;

  // Calcula segundo dígito verificador
  soma = 0;
  for (let i = 0; i < 10; i++) {
    soma += parseInt(limpo.charAt(i)) * (11 - i);
  }
  resto = soma % 11;
  const segundoDigito = resto < 2 ? 0 : 11 - resto;

  return parseInt(limpo.charAt(10)) === segundoDigito;
};

/**
 * Valida CNPJ com dígitos verificadores (algoritmo Receita Federal)
 */
const isValidCnpj = (cnpj: string): boolean => {
  const limpo = removeFormatacao(cnpj);

  if (limpo.length !== 14) return false;

  // Rejeita CNPJs com todos os dígitos iguais
  if (/^(\d)\1{13}$/.test(limpo)) return false;

  const pesosPrimeiroDigito = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  const pesosSegundoDigito = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

  // Calcula primeiro dígito verificador
  let soma = 0;
  for (let i = 0; i < 12; i++) {
    soma += parseInt(limpo.charAt(i)) * pesosPrimeiroDigito[i];
  }
  let resto = soma % 11;
  const primeiroDigito = resto < 2 ? 0 : 11 - resto;

  if (parseInt(limpo.charAt(12)) !== primeiroDigito) return false;

  // Calcula segundo dígito verificador
  soma = 0;
  for (let i = 0; i < 13; i++) {
    soma += parseInt(limpo.charAt(i)) * pesosSegundoDigito[i];
  }
  resto = soma % 11;
  const segundoDigito = resto < 2 ? 0 : 11 - resto;

  return parseInt(limpo.charAt(13)) === segundoDigito;
};

/**
 * Valida CPF ou CNPJ automaticamente baseado no tamanho
 */
const isValidCpfCnpj = (documento: string): boolean => {
  const limpo = removeFormatacao(documento);

  if (limpo.length === 11) return isValidCpf(documento);
  if (limpo.length === 14) return isValidCnpj(documento);

  return false;
};

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
      'Formato inválido. Use: 000.000.000-00 (CPF) ou 00.000.000/0000-00 (CNPJ)'
    )
    .refine(
      (val) => isValidCpfCnpj(val),
      'CPF/CNPJ inválido - os dígitos verificadores não conferem'
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
