/**
 * Types para Conciliação Bancária
 */

export type TipoTransacaoBancaria = 'CREDITO' | 'DEBITO';
export type StatusConciliacao = 'NAO_CONCILIADA' | 'CONCILIADA' | 'IGNORADA' | 'MANUAL';
export type StatusExtrato = 'PENDENTE' | 'EM_ANDAMENTO' | 'CONCLUIDO';

export interface ExtratoBancarioDTO {
  id: string;
  contaBancariaId?: string;
  contaBancariaNome?: string;
  arquivoNome: string;
  tipoArquivo: string;
  dataImportacao: string;
  dataInicio: string;
  dataFim: string;
  saldoInicial?: number;
  saldoFinal?: number;
  totalTransacoes: number;
  totalConciliadas: number;
  totalPendentes: number;
  percentualConciliado: number;
  status: StatusExtrato;
  transacoes?: TransacaoExtratoDTO[];
}

export interface TransacaoExtratoDTO {
  id: string;
  extratoId: string;
  dataTransacao: string;
  dataLancamento?: string;
  tipo: TipoTransacaoBancaria;
  valor: number;
  descricao?: string;
  identificadorBanco?: string;
  referencia?: string;
  categoriaBanco?: string;
  status: StatusConciliacao;
  pagamentoId?: string;
  dataConciliacao?: string;
  metodoConciliacao?: string;
  observacao?: string;
  sugestoes?: SugestaoConciliacaoDTO[];
}

export interface SugestaoConciliacaoDTO {
  pagamentoId: string;
  dataPagamento: string;
  valor: number;
  tipoPagamento: string;
  osNumero?: string;
  clienteNome?: string;
  score: number;
  motivoSugestao: string;
}

export interface ConciliarTransacaoDTO {
  transacaoId: string;
  pagamentoId: string;
}

export interface IgnorarTransacaoDTO {
  transacaoId: string;
  observacao?: string;
}

export interface ConciliacaoLoteDTO {
  conciliacoes?: ConciliarTransacaoDTO[];
  transacoesIgnorar?: string[];
}

export interface ConciliacaoLoteResult {
  conciliadas: number;
  ignoradas: number;
  erros: number;
}

export interface ExtratoResumo {
  totalTransacoes: number;
  totalConciliadas: number;
  totalPendentes: number;
  percentualConciliado: number;
  status: StatusExtrato;
  periodo: {
    inicio: string;
    fim: string;
  };
  saldos: {
    inicial: number;
    final: number;
  };
}

// Helpers
export const getStatusConciliacaoLabel = (status: StatusConciliacao): string => {
  const labels: Record<StatusConciliacao, string> = {
    NAO_CONCILIADA: 'Pendente',
    CONCILIADA: 'Conciliada',
    IGNORADA: 'Ignorada',
    MANUAL: 'Manual',
  };
  return labels[status] || status;
};

export const getStatusConciliacaoColor = (status: StatusConciliacao): string => {
  const colors: Record<StatusConciliacao, string> = {
    NAO_CONCILIADA: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
    CONCILIADA: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
    IGNORADA: 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400',
    MANUAL: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
  };
  return colors[status] || 'bg-gray-100 text-gray-800';
};

export const getStatusExtratoLabel = (status: StatusExtrato): string => {
  const labels: Record<StatusExtrato, string> = {
    PENDENTE: 'Pendente',
    EM_ANDAMENTO: 'Em Andamento',
    CONCLUIDO: 'Concluído',
  };
  return labels[status] || status;
};

export const getStatusExtratoColor = (status: StatusExtrato): string => {
  const colors: Record<StatusExtrato, string> = {
    PENDENTE: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
    EM_ANDAMENTO: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
    CONCLUIDO: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
  };
  return colors[status] || 'bg-gray-100 text-gray-800';
};

export const getTipoTransacaoLabel = (tipo: TipoTransacaoBancaria): string => {
  return tipo === 'CREDITO' ? 'Crédito' : 'Débito';
};

export const getTipoTransacaoColor = (tipo: TipoTransacaoBancaria): string => {
  return tipo === 'CREDITO'
    ? 'text-green-600 dark:text-green-400'
    : 'text-red-600 dark:text-red-400';
};
