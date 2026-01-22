/**
 * Types para o módulo Minha Conta - Faturas SaaS
 */

export enum StatusFatura {
  PENDENTE = 'PENDENTE',
  VENCIDO = 'VENCIDO',
  PAGO = 'PAGO',
  CANCELADO = 'CANCELADO',
}

export const StatusFaturaLabels: Record<StatusFatura, string> = {
  [StatusFatura.PENDENTE]: 'Pendente',
  [StatusFatura.VENCIDO]: 'Vencido',
  [StatusFatura.PAGO]: 'Pago',
  [StatusFatura.CANCELADO]: 'Cancelado',
};

export const StatusFaturaCores: Record<StatusFatura, { bg: string; text: string }> = {
  [StatusFatura.PENDENTE]: {
    bg: 'bg-yellow-100 dark:bg-yellow-900/30',
    text: 'text-yellow-800 dark:text-yellow-400',
  },
  [StatusFatura.VENCIDO]: {
    bg: 'bg-red-100 dark:bg-red-900/30',
    text: 'text-red-800 dark:text-red-400',
  },
  [StatusFatura.PAGO]: {
    bg: 'bg-green-100 dark:bg-green-900/30',
    text: 'text-green-800 dark:text-green-400',
  },
  [StatusFatura.CANCELADO]: {
    bg: 'bg-gray-100 dark:bg-gray-700',
    text: 'text-gray-800 dark:text-gray-400',
  },
};

export interface FaturaResumo {
  id: string;
  numero: string;
  oficinaId: string;
  oficinaNome: string;
  status: StatusFatura;
  statusLabel: string;
  statusCor: string;
  valorTotal: number;
  mesReferencia: string;
  mesReferenciaFormatado: string;
  dataEmissao: string;
  dataVencimento: string;
  dataPagamento?: string;
  diasAteVencimento: number;
  vencida: boolean;
  pagavel: boolean;
}

export interface ItemFatura {
  id: string;
  descricao: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
  tipo: 'MENSALIDADE' | 'ADICIONAL' | 'TAXA' | 'DESCONTO';
}

export interface Fatura extends FaturaResumo {
  planoCodigo: string;
  desconto?: number;
  valorFinal: number;
  observacao?: string;
  metodoPagamento?: string;
  transacaoId?: string;
  linkPagamento?: string;
  qrCodePix?: string;
  itens: ItemFatura[];
}

export interface MinhaContaResumo {
  planoCodigo: string;
  planoNome: string;
  valorMensalidade: number;
  dataVencimentoPlano: string;
  totalFaturas: number;
  faturasPendentes: number;
  faturasVencidas: number;
  faturasPagas: number;
  valorPendente: number;
  valorVencido: number;
  valorPagoUltimos12Meses: number;
  proximaFatura?: FaturaResumo;
  statusConta: 'EM_DIA' | 'INADIMPLENTE';
  contaEmDia: boolean;
}

export interface IniciarPagamentoResponse {
  faturaId: string;
  faturaNumero: string;
  valor: number;
  preferenceId: string;
  initPoint: string;
  sandboxInitPoint?: string;
  pixQrCode?: string;
  pixQrCodeText?: string;
  pixExpirationDate?: string;
  status: 'CREATED' | 'ERROR';
  message: string;
}

export interface FiltrosFatura {
  status?: StatusFatura;
}
