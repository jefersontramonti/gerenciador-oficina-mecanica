/**
 * Tipos e interfaces para o módulo de Veículos
 * Baseado na API: /api/veiculos
 */

/**
 * Cliente resumido (retornado nas respostas de veículo)
 */
export interface ClienteResumo {
  id: string;
  nome: string;
  cpfCnpj: string;
  telefone?: string;
}

/**
 * Veículo (entidade completa)
 */
export interface Veiculo {
  id: string;
  clienteId: string;
  cliente?: ClienteResumo;
  placa: string;
  marca: string;
  modelo: string;
  ano: number;
  cor?: string;
  chassi?: string;
  quilometragem?: number;
  createdAt: string | number[];
  updatedAt: string | number[];
}

/**
 * Request para criar veículo
 */
export interface CreateVeiculoRequest {
  clienteId: string;
  placa: string;
  marca: string;
  modelo: string;
  ano: number;
  cor?: string;
  chassi?: string;
  quilometragem?: number;
}

/**
 * Request para atualizar veículo
 * Não permite alterar placa e cliente
 */
export interface UpdateVeiculoRequest {
  marca: string;
  modelo: string;
  ano: number;
  cor?: string;
  chassi?: string;
  quilometragem?: number;
}

/**
 * Request para atualizar apenas quilometragem
 */
export interface UpdateQuilometragemRequest {
  quilometragem: number;
}

/**
 * Filtros para listagem de veículos
 */
export interface VeiculoFilters {
  clienteId?: string;
  placa?: string;
  marca?: string;
  modelo?: string;
  ano?: number;
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * Estatísticas de veículos de um cliente
 */
export interface VeiculoEstatisticasCliente {
  total: number;
}
