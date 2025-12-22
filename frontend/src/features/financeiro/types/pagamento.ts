/**
 * Tipos para o módulo de pagamentos
 */

export const TipoPagamento = {
  DINHEIRO: 'DINHEIRO',
  PIX: 'PIX',
  CARTAO_CREDITO: 'CARTAO_CREDITO',
  CARTAO_DEBITO: 'CARTAO_DEBITO',
  BOLETO: 'BOLETO',
  TRANSFERENCIA: 'TRANSFERENCIA',
  CHEQUE: 'CHEQUE'
} as const;

export type TipoPagamento = (typeof TipoPagamento)[keyof typeof TipoPagamento];

export const StatusPagamento = {
  PENDENTE: 'PENDENTE',
  PAGO: 'PAGO',
  CANCELADO: 'CANCELADO',
  ESTORNADO: 'ESTORNADO',
  VENCIDO: 'VENCIDO'
} as const;

export type StatusPagamento = (typeof StatusPagamento)[keyof typeof StatusPagamento];

export interface Pagamento {
  id: string;
  ordemServicoId: string;
  tipo: TipoPagamento;
  status: StatusPagamento;
  valor: number;
  parcelas: number;
  parcelaAtual: number;
  dataVencimento?: string;
  dataPagamento?: string;
  observacao?: string;
  comprovante?: string;
  notaFiscalId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PagamentoRequestDTO {
  ordemServicoId: string;
  tipo: TipoPagamento;
  valor: number;
  parcelas: number;
  parcelaAtual?: number;
  dataVencimento?: string;
  observacao?: string;
}

export interface ConfirmarPagamentoDTO {
  dataPagamento: string;
  comprovante?: string;
}

export interface ResumoFinanceiro {
  totalPago: number;
  totalPendente: number;
  quitada: boolean;
}

export interface FiltrosPagamento {
  tipo?: TipoPagamento;
  status?: StatusPagamento;
  dataInicio?: string;
  dataFim?: string;
}

export const TipoPagamentoLabels = {
  DINHEIRO: 'Dinheiro',
  PIX: 'PIX',
  CARTAO_CREDITO: 'Cartão de Crédito',
  CARTAO_DEBITO: 'Cartão de Débito',
  BOLETO: 'Boleto',
  TRANSFERENCIA: 'Transferência',
  CHEQUE: 'Cheque'
} as const;

export const StatusPagamentoLabels = {
  PENDENTE: 'Pendente',
  PAGO: 'Pago',
  CANCELADO: 'Cancelado',
  ESTORNADO: 'Estornado',
  VENCIDO: 'Vencido'
} as const;

export const StatusPagamentoColors = {
  PENDENTE: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
  PAGO: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
  CANCELADO: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
  ESTORNADO: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
  VENCIDO: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400'
} as const;
