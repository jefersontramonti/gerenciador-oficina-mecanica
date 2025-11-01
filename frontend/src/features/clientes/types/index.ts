/**
 * Cliente types - Based on API documentation
 */

export const TipoCliente = {
  PESSOA_FISICA: 'PESSOA_FISICA',
  PESSOA_JURIDICA: 'PESSOA_JURIDICA',
} as const;

export type TipoCliente = typeof TipoCliente[keyof typeof TipoCliente];

export interface Endereco {
  logradouro: string;
  numero: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  estado: string; // UF - 2 caracteres
  cep: string; // Formato: 00000-000
  enderecoFormatado?: string;
}

export interface Cliente {
  id: string;
  tipo: TipoCliente;
  nome: string;
  cpfCnpj: string;
  email?: string;
  telefone?: string;
  celular?: string;
  endereco?: Endereco;
  ativo: boolean;
  createdAt: string | number[];
  updatedAt: string | number[];
}

export interface CreateClienteRequest {
  tipo: TipoCliente;
  nome: string;
  cpfCnpj: string; // Formato: 000.000.000-00 ou 00.000.000/0000-00
  email?: string;
  telefone?: string; // Formato: (00) 0000-0000 ou (00) 00000-0000
  celular?: string;
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  estado?: string; // UF - 2 caracteres maiúsculas
  cep?: string; // Formato: 00000-000
}

export interface UpdateClienteRequest {
  nome: string;
  email?: string;
  telefone?: string;
  celular?: string;
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  estado?: string;
  cep?: string;
}

export interface ClienteFilters {
  nome?: string;
  tipo?: TipoCliente;
  cidade?: string;
  estado?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ClienteEstatisticas {
  [key: string]: number;
}
