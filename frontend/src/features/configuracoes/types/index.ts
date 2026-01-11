/**
 * Types for Oficina Configuration (ADMIN/GERENTE)
 *
 * These types are used for the workshop configuration pages,
 * allowing ADMIN/GERENTE to update their own workshop data.
 */

// ===== ENUMS =====

export const TipoPessoa = {
  PESSOA_FISICA: 'PESSOA_FISICA',
  PESSOA_JURIDICA: 'PESSOA_JURIDICA',
} as const;

export type TipoPessoa = (typeof TipoPessoa)[keyof typeof TipoPessoa];

export const tipoPessoaLabels: Record<TipoPessoa, string> = {
  PESSOA_FISICA: 'Pessoa Fisica',
  PESSOA_JURIDICA: 'Pessoa Juridica',
};

export const RegimeTributario = {
  SIMPLES_NACIONAL: 'SIMPLES_NACIONAL',
  LUCRO_PRESUMIDO: 'LUCRO_PRESUMIDO',
  LUCRO_REAL: 'LUCRO_REAL',
  MEI: 'MEI',
} as const;

export type RegimeTributario = (typeof RegimeTributario)[keyof typeof RegimeTributario];

export const regimeTributarioLabels: Record<RegimeTributario, string> = {
  SIMPLES_NACIONAL: 'Simples Nacional',
  LUCRO_PRESUMIDO: 'Lucro Presumido',
  LUCRO_REAL: 'Lucro Real',
  MEI: 'MEI',
};

export const StatusOficina = {
  TRIAL: 'TRIAL',
  ATIVA: 'ATIVA',
  INATIVA: 'INATIVA',
  SUSPENSA: 'SUSPENSA',
  CANCELADA: 'CANCELADA',
} as const;

export type StatusOficina = (typeof StatusOficina)[keyof typeof StatusOficina];

export const statusOficinaLabels: Record<StatusOficina, string> = {
  TRIAL: 'Trial',
  ATIVA: 'Ativa',
  INATIVA: 'Inativa',
  SUSPENSA: 'Suspensa',
  CANCELADA: 'Cancelada',
};

export const PlanoAssinatura = {
  ECONOMICO: 'ECONOMICO',
  PROFISSIONAL: 'PROFISSIONAL',
  TURBINADO: 'TURBINADO',
} as const;

export type PlanoAssinatura = (typeof PlanoAssinatura)[keyof typeof PlanoAssinatura];

export const planoLabels: Record<PlanoAssinatura, string> = {
  ECONOMICO: 'Economico',
  PROFISSIONAL: 'Profissional',
  TURBINADO: 'Turbinado',
};

export const DiaSemana = {
  DOMINGO: 'DOMINGO',
  SEGUNDA: 'SEGUNDA',
  TERCA: 'TERCA',
  QUARTA: 'QUARTA',
  QUINTA: 'QUINTA',
  SEXTA: 'SEXTA',
  SABADO: 'SABADO',
} as const;

export type DiaSemana = (typeof DiaSemana)[keyof typeof DiaSemana];

export const diaSemanaLabels: Record<DiaSemana, string> = {
  DOMINGO: 'Domingo',
  SEGUNDA: 'Segunda-feira',
  TERCA: 'Terca-feira',
  QUARTA: 'Quarta-feira',
  QUINTA: 'Quinta-feira',
  SEXTA: 'Sexta-feira',
  SABADO: 'Sabado',
};

// ===== EMBEDDED OBJECTS =====

export interface Contato {
  email?: string;
  telefone?: string;
  celular?: string;
  whatsapp?: string;
}

export interface Endereco {
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  estado?: string; // UF - 2 caracteres
  cep?: string;
  enderecoFormatado?: string;
}

export interface InformacoesOperacionais {
  // Horario de funcionamento
  horarioAbertura?: string; // HH:mm
  horarioFechamento?: string; // HH:mm
  diasFuncionamento?: DiaSemana[];

  // Capacidade
  capacidadeAtendimento?: number;
  quantidadeElevadores?: number;

  // Especialidades
  especialidades?: string[];
  marcasAtendidas?: string[];

  // Servicos
  servicosOferecidos?: string[];
  aceitaAgendamentoOnline?: boolean;
  tempoMedioAtendimento?: number; // em minutos

  // Observacoes
  observacoes?: string;
}

export interface RedesSociais {
  website?: string;
  facebook?: string;
  instagram?: string;
  youtube?: string;
  linkedin?: string;
  twitter?: string;
  tiktok?: string;
}

export interface DadosBancarios {
  banco?: string;
  agencia?: string;
  conta?: string;
  tipoConta?: 'CORRENTE' | 'POUPANCA';
  titularConta?: string;
  cpfCnpjTitular?: string;

  // PIX
  chavePix?: string;
  tipoChavePix?: 'CPF' | 'CNPJ' | 'EMAIL' | 'TELEFONE' | 'ALEATORIA';
}

// ===== MAIN OFICINA INTERFACE =====

export interface Oficina {
  id: string;

  // Dados basicos
  nome: string;
  nomeFantasia?: string;
  razaoSocial?: string;
  cnpjCpf: string;
  tipoPessoa: TipoPessoa;

  // Inscricoes
  inscricaoEstadual?: string;
  inscricaoMunicipal?: string;

  // Responsavel
  nomeResponsavel?: string;

  // Contato (embedded)
  email?: string;
  telefone?: string;
  celular?: string;

  // Fiscal
  regimeTributario?: RegimeTributario;

  // Plano e Status
  plano: PlanoAssinatura;
  status: StatusOficina;
  dataAssinatura?: string;
  dataVencimentoPlano?: string;
  valorMensalidade?: number;

  // Embedded objects
  endereco?: Endereco;
  informacoesOperacionais?: InformacoesOperacionais;
  redesSociais?: RedesSociais;
  dadosBancarios?: DadosBancarios;

  // Outros
  logoUrl?: string;

  // Configuracoes de mao de obra
  valorHora?: number;

  // Timestamps
  createdAt?: string;
  updatedAt?: string;
}

// ===== REQUEST DTOs =====

/**
 * Request DTO for updating Oficina.
 * All fields are optional - only non-null fields are updated.
 */
export interface UpdateOficinaRequest {
  // Dados basicos
  nome?: string;
  nomeFantasia?: string;
  tipoPessoa?: TipoPessoa;

  // Inscricoes
  inscricaoEstadual?: string;
  inscricaoMunicipal?: string;

  // Fiscal
  regimeTributario?: RegimeTributario;

  // Contato (embedded)
  contato?: Contato;

  // Endereco (embedded)
  endereco?: Endereco;

  // Informacoes Operacionais (embedded)
  informacoesOperacionais?: InformacoesOperacionais;

  // Redes Sociais (embedded)
  redesSociais?: RedesSociais;

  // Dados Bancarios (embedded)
  dadosBancarios?: DadosBancarios;

  // Logo
  logoUrl?: string;

  // Configuracoes de mao de obra
  valorHora?: number;
}

// ===== FORM DATA TYPES =====

/**
 * Form data for basic info tab (Dados Basicos)
 */
export interface OficinaBasicoFormData {
  nome: string;
  nomeFantasia?: string;
  tipoPessoa: TipoPessoa;
  nomeResponsavel?: string;
  email?: string;
  telefone?: string;
  celular?: string;
  // Endereco
  cep?: string;
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  estado?: string;
}

/**
 * Form data for operational info tab (Informacoes Operacionais)
 */
export interface OficinaOperacionalFormData {
  horarioAbertura?: string;
  horarioFechamento?: string;
  diasFuncionamento?: DiaSemana[];
  capacidadeAtendimento?: number;
  quantidadeElevadores?: number;
  especialidades?: string[];
  marcasAtendidas?: string[];
  servicosOferecidos?: string[];
  aceitaAgendamentoOnline?: boolean;
  tempoMedioAtendimento?: number;
  observacoes?: string;
  // Redes Sociais
  website?: string;
  facebook?: string;
  instagram?: string;
  youtube?: string;
  linkedin?: string;
  twitter?: string;
  tiktok?: string;
  // Valor/hora de mao de obra
  valorHora?: number;
}

/**
 * Form data for financial info tab (Dados Financeiros)
 */
export interface OficinaFinanceiroFormData {
  banco?: string;
  agencia?: string;
  conta?: string;
  tipoConta?: 'CORRENTE' | 'POUPANCA';
  titularConta?: string;
  cpfCnpjTitular?: string;
  chavePix?: string;
  tipoChavePix?: 'CPF' | 'CNPJ' | 'EMAIL' | 'TELEFONE' | 'ALEATORIA';
}

/**
 * Form data for fiscal info tab (Dados Fiscais)
 */
export interface OficinaFiscalFormData {
  inscricaoEstadual?: string;
  inscricaoMunicipal?: string;
  regimeTributario?: RegimeTributario;
}

// ===== UTILITY TYPES =====

/**
 * List of Brazilian states (UF)
 */
export const ESTADOS_BRASIL = [
  { value: 'AC', label: 'Acre' },
  { value: 'AL', label: 'Alagoas' },
  { value: 'AP', label: 'Amapa' },
  { value: 'AM', label: 'Amazonas' },
  { value: 'BA', label: 'Bahia' },
  { value: 'CE', label: 'Ceara' },
  { value: 'DF', label: 'Distrito Federal' },
  { value: 'ES', label: 'Espirito Santo' },
  { value: 'GO', label: 'Goias' },
  { value: 'MA', label: 'Maranhao' },
  { value: 'MT', label: 'Mato Grosso' },
  { value: 'MS', label: 'Mato Grosso do Sul' },
  { value: 'MG', label: 'Minas Gerais' },
  { value: 'PA', label: 'Para' },
  { value: 'PB', label: 'Paraiba' },
  { value: 'PR', label: 'Parana' },
  { value: 'PE', label: 'Pernambuco' },
  { value: 'PI', label: 'Piaui' },
  { value: 'RJ', label: 'Rio de Janeiro' },
  { value: 'RN', label: 'Rio Grande do Norte' },
  { value: 'RS', label: 'Rio Grande do Sul' },
  { value: 'RO', label: 'Rondonia' },
  { value: 'RR', label: 'Roraima' },
  { value: 'SC', label: 'Santa Catarina' },
  { value: 'SP', label: 'Sao Paulo' },
  { value: 'SE', label: 'Sergipe' },
  { value: 'TO', label: 'Tocantins' },
] as const;

/**
 * List of common banks in Brazil
 */
export const BANCOS_BRASIL = [
  { value: '001', label: 'Banco do Brasil' },
  { value: '033', label: 'Santander' },
  { value: '104', label: 'Caixa Economica Federal' },
  { value: '237', label: 'Bradesco' },
  { value: '341', label: 'Itau Unibanco' },
  { value: '077', label: 'Inter' },
  { value: '260', label: 'Nubank' },
  { value: '212', label: 'Banco Original' },
  { value: '336', label: 'C6 Bank' },
  { value: '290', label: 'PagBank' },
  { value: '380', label: 'PicPay' },
  { value: '748', label: 'Sicredi' },
  { value: '756', label: 'Sicoob' },
  { value: '422', label: 'Safra' },
  { value: '655', label: 'Neon' },
] as const;

/**
 * Common automotive specialties
 */
export const ESPECIALIDADES = [
  'Mecanica Geral',
  'Eletrica Automotiva',
  'Funilaria e Pintura',
  'Ar Condicionado',
  'Suspensao e Direcao',
  'Freios',
  'Injecao Eletronica',
  'Cambio Automatico',
  'Cambio Manual',
  'Diesel',
  'GNV',
  'Hibridos e Eletricos',
  'Pneus e Alinhamento',
  'Vidros e Para-brisas',
  'Estofamento',
  'Som Automotivo',
  'Blindagem',
  'Performance',
  'Preparacao',
  'Restauracao',
] as const;

/**
 * Common automotive brands
 */
export const MARCAS_AUTOMOTIVAS = [
  'Chevrolet',
  'Fiat',
  'Ford',
  'Volkswagen',
  'Toyota',
  'Honda',
  'Hyundai',
  'Jeep',
  'Renault',
  'Nissan',
  'Peugeot',
  'Citroen',
  'Mitsubishi',
  'Kia',
  'Mercedes-Benz',
  'BMW',
  'Audi',
  'Volvo',
  'Land Rover',
  'Porsche',
  'Todas as Marcas',
] as const;

/**
 * Common automotive services
 */
export const SERVICOS_AUTOMOTIVOS = [
  'Troca de Oleo',
  'Revisao Completa',
  'Alinhamento e Balanceamento',
  'Troca de Pastilhas de Freio',
  'Troca de Discos de Freio',
  'Troca de Amortecedores',
  'Troca de Correia Dentada',
  'Troca de Embreagem',
  'Limpeza de Bicos Injetores',
  'Diagnostico Eletronico',
  'Troca de Bateria',
  'Recarga de Ar Condicionado',
  'Higienizacao de Ar Condicionado',
  'Troca de Filtros',
  'Troca de Velas',
  'Retifica de Motor',
  'Troca de Junta do Cabecote',
  'Reparo de Cambio',
  'Geometria',
  'Lavagem Tecnica',
  'Polimento',
  'Vitrificacao',
  'Martelinho de Ouro',
  'Guincho 24h',
] as const;
