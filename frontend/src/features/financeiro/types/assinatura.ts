// Types for subscription management (Cobrança Recorrente)

export type PeriodicidadeAssinatura = 'SEMANAL' | 'QUINZENAL' | 'MENSAL' | 'TRIMESTRAL' | 'SEMESTRAL' | 'ANUAL';
export type StatusAssinatura = 'ATIVA' | 'PAUSADA' | 'CANCELADA' | 'INADIMPLENTE';
export type StatusFaturaAssinatura = 'PENDENTE' | 'PAGA' | 'VENCIDA' | 'CANCELADA';

// Plano de Assinatura
export interface PlanoAssinaturaDTO {
  id?: string;
  nome: string;
  descricao?: string;
  valor: number;
  periodicidade: PeriodicidadeAssinatura;
  servicosIncluidos?: string[];
  limites?: Record<string, number>;
  descontos?: Record<string, number>;
  ativo: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// Assinatura
export interface AssinaturaDTO {
  id: string;
  clienteId: string;
  clienteNome: string;
  clienteCpfCnpj?: string;
  clienteTelefone?: string;
  planoId: string;
  planoNome: string;
  periodicidade: PeriodicidadeAssinatura;
  valorAtual: number;
  status: StatusAssinatura;
  dataInicio: string;
  dataProximoVencimento: string;
  diaVencimento: number;
  dataFim?: string;
  dataCancelamento?: string;
  motivoCancelamento?: string;
  gatewaySubscriptionId?: string;
  cobrancaAutomatica: boolean;
  ultimaCobranca?: string;
  proximaCobranca?: string;
  totalFaturas: number;
  faturasPagas: number;
  faturasVencidas: number;
  valorTotalPago: number;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateAssinaturaDTO {
  clienteId: string;
  planoId: string;
  diaVencimento?: number;
  cobrancaAutomatica?: boolean;
  observacoes?: string;
}

export interface CancelarAssinaturaDTO {
  motivo: string;
  cancelarFaturasPendentes?: boolean;
}

// Fatura de Assinatura
export interface FaturaAssinaturaDTO {
  id: string;
  assinaturaId: string;
  clienteNome: string;
  planoNome: string;
  numeroFatura: string;
  mesReferencia: string;
  valor: number;
  status: StatusFaturaAssinatura;
  dataVencimento: string;
  dataPagamento?: string;
  formaPagamento?: string;
  gatewayPaymentId?: string;
  linkPagamento?: string;
  qrCodePix?: string;
  tentativasCobranca: number;
  ultimaTentativa?: string;
  observacao?: string;
  createdAt: string;
}

export interface RegistrarPagamentoDTO {
  formaPagamento?: string;
  observacao?: string;
}

// Helpers
export const periodicidadeLabels: Record<PeriodicidadeAssinatura, string> = {
  SEMANAL: 'Semanal',
  QUINZENAL: 'Quinzenal',
  MENSAL: 'Mensal',
  TRIMESTRAL: 'Trimestral',
  SEMESTRAL: 'Semestral',
  ANUAL: 'Anual',
};

export const statusAssinaturaLabels: Record<StatusAssinatura, string> = {
  ATIVA: 'Ativa',
  PAUSADA: 'Pausada',
  CANCELADA: 'Cancelada',
  INADIMPLENTE: 'Inadimplente',
};

export const statusAssinaturaColors: Record<StatusAssinatura, string> = {
  ATIVA: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
  PAUSADA: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
  CANCELADA: 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400',
  INADIMPLENTE: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
};

export const statusFaturaLabels: Record<StatusFaturaAssinatura, string> = {
  PENDENTE: 'Pendente',
  PAGA: 'Paga',
  VENCIDA: 'Vencida',
  CANCELADA: 'Cancelada',
};

export const statusFaturaColors: Record<StatusFaturaAssinatura, string> = {
  PENDENTE: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
  PAGA: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
  VENCIDA: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
  CANCELADA: 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400',
};

export function formatPeriodicidade(periodicidade: PeriodicidadeAssinatura): string {
  return periodicidadeLabels[periodicidade] || periodicidade;
}

export function formatStatusAssinatura(status: StatusAssinatura): string {
  return statusAssinaturaLabels[status] || status;
}

export function formatStatusFatura(status: StatusFaturaAssinatura): string {
  return statusFaturaLabels[status] || status;
}
