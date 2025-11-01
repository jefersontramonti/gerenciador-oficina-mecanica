/**
 * Tipos e interfaces para o módulo de Ordens de Serviço
 * Baseado na API: /api/ordens-servico
 */

/**
 * Enum de status da Ordem de Serviço (máquina de estados)
 */
export const StatusOS = {
  ORCAMENTO: 'ORCAMENTO',
  APROVADO: 'APROVADO',
  EM_ANDAMENTO: 'EM_ANDAMENTO',
  AGUARDANDO_PECA: 'AGUARDANDO_PECA',
  FINALIZADO: 'FINALIZADO',
  ENTREGUE: 'ENTREGUE',
  CANCELADO: 'CANCELADO',
} as const;

export type StatusOS = typeof StatusOS[keyof typeof StatusOS];

/**
 * Enum de tipo de item da OS (peça ou serviço)
 */
export const TipoItem = {
  PECA: 'PECA',
  SERVICO: 'SERVICO',
} as const;

export type TipoItem = typeof TipoItem[keyof typeof TipoItem];

/**
 * Cliente resumido (retornado nas respostas de OS)
 */
export interface ClienteResumo {
  id: string;
  nome: string;
  cpfCnpj: string;
  telefone?: string;
  celular?: string;
}

/**
 * Veículo resumido (retornado nas respostas de OS)
 */
export interface VeiculoResumo {
  id: string;
  placa: string;
  marca: string;
  modelo: string;
  ano: number;
  cor?: string;
}

/**
 * Mecânico resumido (retornado nas respostas de OS)
 */
export interface MecanicoResumo {
  id: string;
  nome: string;
  email: string;
  perfil: string;
}

/**
 * Item da Ordem de Serviço (peça ou serviço)
 */
export interface ItemOS {
  id?: string;
  tipo: TipoItem;
  pecaId?: string;
  descricao: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
  desconto: number;
}

/**
 * Ordem de Serviço (entidade completa)
 */
export interface OrdemServico {
  id: string;
  numero: number;
  veiculoId?: string;
  veiculo?: VeiculoResumo;
  cliente?: ClienteResumo;
  usuarioId?: string;
  mecanico?: MecanicoResumo;
  status: StatusOS;
  dataAbertura: string | number[];
  dataPrevisao?: string | number[];
  dataFinalizacao?: string | number[];
  dataEntrega?: string | number[];
  problemasRelatados: string;
  diagnostico?: string;
  observacoes?: string;
  valorMaoObra: number;
  valorPecas: number;
  valorTotal: number;
  descontoPercentual: number;
  descontoValor: number;
  valorFinal: number;
  aprovadoPeloCliente: boolean;
  itens: ItemOS[];
  createdAt: string | number[];
  updatedAt: string | number[];
}

/**
 * Request para criar Item da OS
 */
export interface CreateItemOSRequest {
  tipo: TipoItem;
  pecaId?: string;
  descricao: string;
  quantidade: number;
  valorUnitario: number;
  desconto?: number;
}

/**
 * Request para criar Ordem de Serviço
 */
export interface CreateOrdemServicoRequest {
  veiculoId: string;
  usuarioId: string;
  problemasRelatados: string;
  diagnostico?: string;
  observacoes?: string;
  dataPrevisao?: string;
  valorMaoObra: number;
  descontoPercentual?: number;
  descontoValor?: number;
  itens: CreateItemOSRequest[];
}

/**
 * Request para atualizar Ordem de Serviço
 * Só permite editar se status = ORCAMENTO ou APROVADO
 */
export interface UpdateOrdemServicoRequest {
  veiculoId?: string;
  usuarioId?: string;
  problemasRelatados?: string;
  diagnostico?: string;
  observacoes?: string;
  dataPrevisao?: string;
  valorMaoObra?: number;
  descontoPercentual?: number;
  descontoValor?: number;
  itens?: CreateItemOSRequest[];
}

/**
 * Request para cancelar OS
 */
export interface CancelarOrdemServicoRequest {
  motivo: string;
}

/**
 * Filtros para listagem de OS
 */
export interface OrdemServicoFilters {
  status?: StatusOS;
  veiculoId?: string;
  usuarioId?: string;
  dataInicio?: string;
  dataFim?: string;
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * Resumo de OS (para cards e listas)
 */
export interface OrdemServicoResumo {
  id: string;
  numero: number;
  status: StatusOS;
  veiculoPlaca: string;
  clienteNome: string;
  dataAbertura: string | number[];
  valorFinal: number;
}

/**
 * Estatísticas do dashboard de OS
 */
export interface DashboardContagem {
  ORCAMENTO: number;
  APROVADO: number;
  EM_ANDAMENTO: number;
  AGUARDANDO_PECA: number;
  FINALIZADO: number;
  ENTREGUE: number;
  CANCELADO: number;
  total: number;
}

/**
 * Faturamento do dashboard
 */
export interface DashboardFaturamento {
  periodo: string;
  totalServicos: number;
  faturamentoTotal: number;
  ticketMedio: number;
}

/**
 * Tipo para formulário (com valores iniciais)
 */
export interface OrdemServicoFormData {
  veiculoId: string;
  usuarioId: string;
  problemasRelatados: string;
  diagnostico: string;
  observacoes: string;
  dataAbertura: string;
  dataPrevisao: string;
  valorMaoObra: number;
  valorPecas: number;
  valorTotal: number;
  descontoPercentual: number;
  descontoValor: number;
  valorFinal: number;
  itens: ItemOS[];
}
