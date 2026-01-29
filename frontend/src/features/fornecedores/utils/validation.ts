import { z } from 'zod';

export const createFornecedorSchema = z.object({
  tipo: z.string().min(1, 'Tipo é obrigatório'),
  nomeFantasia: z.string().min(2, 'Nome fantasia deve ter no mínimo 2 caracteres').max(200),
  razaoSocial: z.string().max(200).optional().or(z.literal('')),
  cpfCnpj: z.string().optional().or(z.literal('')),
  inscricaoEstadual: z.string().max(20).optional().or(z.literal('')),
  email: z.string().email('E-mail inválido').optional().or(z.literal('')),
  telefone: z.string().optional().or(z.literal('')),
  celular: z.string().optional().or(z.literal('')),
  website: z.string().max(200).optional().or(z.literal('')),
  contatoNome: z.string().max(150).optional().or(z.literal('')),
  logradouro: z.string().max(200).optional().or(z.literal('')),
  numero: z.string().max(10).optional().or(z.literal('')),
  complemento: z.string().max(100).optional().or(z.literal('')),
  bairro: z.string().max(100).optional().or(z.literal('')),
  cidade: z.string().max(100).optional().or(z.literal('')),
  estado: z.string().max(2).optional().or(z.literal('')),
  cep: z.string().optional().or(z.literal('')),
  prazoEntrega: z.string().max(100).optional().or(z.literal('')),
  condicoesPagamento: z.string().max(200).optional().or(z.literal('')),
  descontoPadrao: z.string().optional().or(z.literal('')).transform((val) => {
    if (!val || val === '') return undefined;
    const num = parseFloat(val);
    return isNaN(num) ? undefined : num;
  }),
  observacoes: z.string().optional().or(z.literal('')),
});

export type CreateFornecedorFormData = z.infer<typeof createFornecedorSchema>;
