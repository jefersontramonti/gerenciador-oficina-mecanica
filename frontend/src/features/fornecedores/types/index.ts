/**
 * Enums e tipos do módulo de Fornecedores
 */

// ==================== ENUMS ====================

export const TipoFornecedor = {
  FABRICANTE: 'FABRICANTE',
  DISTRIBUIDOR: 'DISTRIBUIDOR',
  ATACADISTA: 'ATACADISTA',
  VAREJISTA: 'VAREJISTA',
  IMPORTADOR: 'IMPORTADOR',
  OUTRO: 'OUTRO',
} as const;

export type TipoFornecedor = (typeof TipoFornecedor)[keyof typeof TipoFornecedor];

export const TipoFornecedorLabel: Record<TipoFornecedor, string> = {
  [TipoFornecedor.FABRICANTE]: 'Fabricante',
  [TipoFornecedor.DISTRIBUIDOR]: 'Distribuidor',
  [TipoFornecedor.ATACADISTA]: 'Atacadista',
  [TipoFornecedor.VAREJISTA]: 'Varejista',
  [TipoFornecedor.IMPORTADOR]: 'Importador',
  [TipoFornecedor.OUTRO]: 'Outro',
};

// ==================== INTERFACES ====================

export interface Endereco {
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  estado?: string;
  cep?: string;
  enderecoFormatado?: string;
}

export interface Fornecedor {
  id: string;
  tipo: TipoFornecedor;
  nomeFantasia: string;
  razaoSocial?: string;
  cpfCnpj?: string;
  inscricaoEstadual?: string;
  email?: string;
  telefone?: string;
  celular?: string;
  website?: string;
  contatoNome?: string;
  endereco?: Endereco;
  prazoEntrega?: string;
  condicoesPagamento?: string;
  descontoPadrao?: number;
  observacoes?: string;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface FornecedorResumo {
  id: string;
  tipo: TipoFornecedor;
  nomeFantasia: string;
  cpfCnpj?: string;
  telefone?: string;
  celular?: string;
}

// ==================== DTOs ====================

export interface CreateFornecedorRequest {
  tipo: TipoFornecedor;
  nomeFantasia: string;
  razaoSocial?: string;
  cpfCnpj?: string;
  inscricaoEstadual?: string;
  email?: string;
  telefone?: string;
  celular?: string;
  website?: string;
  contatoNome?: string;
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  estado?: string;
  cep?: string;
  prazoEntrega?: string;
  condicoesPagamento?: string;
  descontoPadrao?: number;
  observacoes?: string;
}

export interface UpdateFornecedorRequest {
  tipo?: TipoFornecedor;
  nomeFantasia?: string;
  razaoSocial?: string;
  cpfCnpj?: string;
  inscricaoEstadual?: string;
  email?: string;
  telefone?: string;
  celular?: string;
  website?: string;
  contatoNome?: string;
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  estado?: string;
  cep?: string;
  prazoEntrega?: string;
  condicoesPagamento?: string;
  descontoPadrao?: number;
  observacoes?: string;
}

export interface FornecedorFilters {
  nome?: string;
  tipo?: TipoFornecedor;
  cidade?: string;
  page?: number;
  size?: number;
  sort?: string;
}
