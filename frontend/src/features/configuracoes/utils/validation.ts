import { z } from 'zod';

/**
 * Oficina form validation schemas
 *
 * Split into 4 schemas for each configuration tab:
 * - oficiaBasicoSchema: Basic info (nome, contato, endereco)
 * - oficinaOperacionalSchema: Operational info (horarios, especialidades, redes sociais)
 * - oficinaFinanceiroSchema: Financial info (dados bancarios)
 * - oficinaFiscalSchema: Fiscal info (inscricoes, regime tributario)
 */

// ===== HELPER REGEX PATTERNS =====

const telefonePattern = /^(\(\d{2}\)\s?)?\d{4,5}-?\d{4}$/;
const cepPattern = /^\d{5}-\d{3}$/;
const ufPattern = /^[A-Z]{2}$/;
const cpfPattern = /^\d{3}\.\d{3}\.\d{3}-\d{2}$/;
const cnpjPattern = /^\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2}$/;
const urlPattern = /^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([\/\w .-]*)*\/?$/;
const agenciaPattern = /^\d{4}(-\d)?$/;
const contaPattern = /^\d{4,12}(-\d)?$/;
const horaPattern = /^([01]?\d|2[0-3]):[0-5]\d$/;

// ===== HELPER FUNCTIONS =====

/**
 * Remove formatting from CPF/CNPJ
 */
const removeFormatacao = (documento: string): string => {
  return documento.replace(/[^\d]/g, '');
};

/**
 * Validate CPF with check digits (Brazilian algorithm)
 */
const isValidCpf = (cpf: string): boolean => {
  const limpo = removeFormatacao(cpf);
  if (limpo.length !== 11) return false;
  if (/^(\d)\1{10}$/.test(limpo)) return false;

  let soma = 0;
  for (let i = 0; i < 9; i++) {
    soma += parseInt(limpo.charAt(i)) * (10 - i);
  }
  let resto = soma % 11;
  const primeiroDigito = resto < 2 ? 0 : 11 - resto;
  if (parseInt(limpo.charAt(9)) !== primeiroDigito) return false;

  soma = 0;
  for (let i = 0; i < 10; i++) {
    soma += parseInt(limpo.charAt(i)) * (11 - i);
  }
  resto = soma % 11;
  const segundoDigito = resto < 2 ? 0 : 11 - resto;
  return parseInt(limpo.charAt(10)) === segundoDigito;
};

/**
 * Validate CNPJ with check digits (Brazilian algorithm)
 */
const isValidCnpj = (cnpj: string): boolean => {
  const limpo = removeFormatacao(cnpj);
  if (limpo.length !== 14) return false;
  if (/^(\d)\1{13}$/.test(limpo)) return false;

  const pesosPrimeiroDigito = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  const pesosSegundoDigito = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

  let soma = 0;
  for (let i = 0; i < 12; i++) {
    soma += parseInt(limpo.charAt(i)) * pesosPrimeiroDigito[i];
  }
  let resto = soma % 11;
  const primeiroDigito = resto < 2 ? 0 : 11 - resto;
  if (parseInt(limpo.charAt(12)) !== primeiroDigito) return false;

  soma = 0;
  for (let i = 0; i < 13; i++) {
    soma += parseInt(limpo.charAt(i)) * pesosSegundoDigito[i];
  }
  resto = soma % 11;
  const segundoDigito = resto < 2 ? 0 : 11 - resto;
  return parseInt(limpo.charAt(13)) === segundoDigito;
};

/**
 * Validate CPF or CNPJ automatically based on length
 */
const isValidCpfCnpj = (documento: string): boolean => {
  const limpo = removeFormatacao(documento);
  if (limpo.length === 11) return isValidCpf(documento);
  if (limpo.length === 14) return isValidCnpj(documento);
  return false;
};

// ===== OFICINA BASICO SCHEMA =====

export const oficinaBasicoSchema = z.object({
  nome: z
    .string()
    .min(3, 'Nome deve ter pelo menos 3 caracteres')
    .max(150, 'Nome deve ter no maximo 150 caracteres'),
  nomeFantasia: z
    .string()
    .max(150, 'Nome fantasia deve ter no maximo 150 caracteres')
    .optional()
    .or(z.literal('')),
  tipoPessoa: z.enum(['PESSOA_FISICA', 'PESSOA_JURIDICA'], {
    message: 'Tipo de pessoa e obrigatorio',
  }),
  nomeResponsavel: z
    .string()
    .max(150, 'Nome do responsavel deve ter no maximo 150 caracteres')
    .optional()
    .or(z.literal('')),
  email: z
    .string()
    .email('Email invalido')
    .max(100, 'Email deve ter no maximo 100 caracteres')
    .optional()
    .or(z.literal('')),
  telefone: z
    .string()
    .regex(telefonePattern, 'Telefone invalido. Use formato: (00) 0000-0000')
    .optional()
    .or(z.literal('')),
  celular: z
    .string()
    .regex(telefonePattern, 'Celular invalido. Use formato: (00) 00000-0000')
    .optional()
    .or(z.literal('')),
  cep: z
    .string()
    .regex(cepPattern, 'CEP invalido. Use formato: 00000-000')
    .optional()
    .or(z.literal('')),
  logradouro: z
    .string()
    .max(200, 'Logradouro deve ter no maximo 200 caracteres')
    .optional()
    .or(z.literal('')),
  numero: z
    .string()
    .max(10, 'Numero deve ter no maximo 10 caracteres')
    .optional()
    .or(z.literal('')),
  complemento: z
    .string()
    .max(100, 'Complemento deve ter no maximo 100 caracteres')
    .optional()
    .or(z.literal('')),
  bairro: z
    .string()
    .max(100, 'Bairro deve ter no maximo 100 caracteres')
    .optional()
    .or(z.literal('')),
  cidade: z
    .string()
    .max(100, 'Cidade deve ter no maximo 100 caracteres')
    .optional()
    .or(z.literal('')),
  estado: z
    .string()
    .regex(ufPattern, 'Estado deve ter 2 letras maiusculas (ex: SP)')
    .optional()
    .or(z.literal('')),
});

export type OficinaBasicoFormData = z.infer<typeof oficinaBasicoSchema>;

// ===== OFICINA OPERACIONAL SCHEMA =====

export const oficinaOperacionalSchema = z.object({
  // Horarios
  horarioAbertura: z
    .string()
    .regex(horaPattern, 'Horario invalido. Use formato: HH:MM')
    .optional()
    .or(z.literal('')),
  horarioFechamento: z
    .string()
    .regex(horaPattern, 'Horario invalido. Use formato: HH:MM')
    .optional()
    .or(z.literal('')),
  diasFuncionamento: z
    .array(
      z.enum([
        'DOMINGO',
        'SEGUNDA',
        'TERCA',
        'QUARTA',
        'QUINTA',
        'SEXTA',
        'SABADO',
      ])
    )
    .optional(),
  // Capacidade
  capacidadeAtendimento: z
    .number()
    .int('Capacidade deve ser um numero inteiro')
    .min(0, 'Capacidade deve ser maior ou igual a 0')
    .max(100, 'Capacidade deve ser no maximo 100')
    .optional()
    .or(z.nan()),
  quantidadeElevadores: z
    .number()
    .int('Quantidade deve ser um numero inteiro')
    .min(0, 'Quantidade deve ser maior ou igual a 0')
    .max(50, 'Quantidade deve ser no maximo 50')
    .optional()
    .or(z.nan()),
  // Arrays
  especialidades: z.array(z.string()).optional(),
  marcasAtendidas: z.array(z.string()).optional(),
  servicosOferecidos: z.array(z.string()).optional(),
  // Outros
  aceitaAgendamentoOnline: z.boolean().optional(),
  tempoMedioAtendimento: z
    .number()
    .int('Tempo deve ser um numero inteiro')
    .min(0, 'Tempo deve ser maior ou igual a 0')
    .max(1440, 'Tempo deve ser no maximo 1440 minutos (24 horas)')
    .optional()
    .or(z.nan()),
  observacoes: z
    .string()
    .max(1000, 'Observacoes devem ter no maximo 1000 caracteres')
    .optional()
    .or(z.literal('')),
  // Redes Sociais
  website: z
    .string()
    .regex(urlPattern, 'URL invalida')
    .optional()
    .or(z.literal('')),
  facebook: z
    .string()
    .max(100, 'Facebook deve ter no maximo 100 caracteres')
    .optional()
    .or(z.literal('')),
  instagram: z
    .string()
    .max(100, 'Instagram deve ter no maximo 100 caracteres')
    .optional()
    .or(z.literal('')),
  youtube: z
    .string()
    .max(100, 'YouTube deve ter no maximo 100 caracteres')
    .optional()
    .or(z.literal('')),
  linkedin: z
    .string()
    .max(100, 'LinkedIn deve ter no maximo 100 caracteres')
    .optional()
    .or(z.literal('')),
  twitter: z
    .string()
    .max(100, 'Twitter deve ter no maximo 100 caracteres')
    .optional()
    .or(z.literal('')),
  tiktok: z
    .string()
    .max(100, 'TikTok deve ter no maximo 100 caracteres')
    .optional()
    .or(z.literal('')),
});

export type OficinaOperacionalFormData = z.infer<typeof oficinaOperacionalSchema>;

// ===== OFICINA FINANCEIRO SCHEMA =====

export const oficinaFinanceiroSchema = z.object({
  banco: z
    .string()
    .max(100, 'Banco deve ter no maximo 100 caracteres')
    .optional()
    .or(z.literal('')),
  agencia: z
    .string()
    .regex(agenciaPattern, 'Agencia invalida. Use formato: 0000 ou 0000-0')
    .optional()
    .or(z.literal('')),
  conta: z
    .string()
    .regex(contaPattern, 'Conta invalida. Use formato: 00000-0')
    .optional()
    .or(z.literal('')),
  tipoConta: z.enum(['CORRENTE', 'POUPANCA']).optional(),
  titularConta: z
    .string()
    .max(150, 'Nome do titular deve ter no maximo 150 caracteres')
    .optional()
    .or(z.literal('')),
  cpfCnpjTitular: z
    .string()
    .refine(
      (val) => !val || cpfPattern.test(val) || cnpjPattern.test(val),
      'Formato invalido. Use: 000.000.000-00 (CPF) ou 00.000.000/0000-00 (CNPJ)'
    )
    .refine(
      (val) => !val || isValidCpfCnpj(val),
      'CPF/CNPJ invalido - os digitos verificadores nao conferem'
    )
    .optional()
    .or(z.literal('')),
  chavePix: z
    .string()
    .max(200, 'Chave PIX deve ter no maximo 200 caracteres')
    .optional()
    .or(z.literal('')),
  tipoChavePix: z
    .enum(['CPF', 'CNPJ', 'EMAIL', 'TELEFONE', 'ALEATORIA'])
    .optional(),
});

export type OficinaFinanceiroFormData = z.infer<typeof oficinaFinanceiroSchema>;

// ===== OFICINA FISCAL SCHEMA =====

export const oficinaFiscalSchema = z.object({
  inscricaoEstadual: z
    .string()
    .max(30, 'Inscricao Estadual deve ter no maximo 30 caracteres')
    .optional()
    .or(z.literal('')),
  inscricaoMunicipal: z
    .string()
    .max(30, 'Inscricao Municipal deve ter no maximo 30 caracteres')
    .optional()
    .or(z.literal('')),
  regimeTributario: z
    .enum(['SIMPLES_NACIONAL', 'LUCRO_PRESUMIDO', 'LUCRO_REAL', 'MEI'])
    .optional(),
});

export type OficinaFiscalFormData = z.infer<typeof oficinaFiscalSchema>;
