/**
 * Tipos para o módulo de Notas Fiscais
 */

export const TipoNotaFiscal = {
  NFE: 'NFE',
  NFSE: 'NFSE',
  NFCE: 'NFCE',
} as const;

export type TipoNotaFiscal = (typeof TipoNotaFiscal)[keyof typeof TipoNotaFiscal];

export const StatusNotaFiscal = {
  DIGITACAO: 'DIGITACAO',
  AUTORIZADA: 'AUTORIZADA',
  CANCELADA: 'CANCELADA',
  DENEGADA: 'DENEGADA',
  REJEITADA: 'REJEITADA',
} as const;

export type StatusNotaFiscal =
  (typeof StatusNotaFiscal)[keyof typeof StatusNotaFiscal];

export interface NotaFiscal {
  id: string;
  ordemServicoId: string;
  tipo: TipoNotaFiscal;
  status: StatusNotaFiscal;
  numero: number;
  serie: number;
  chaveAcesso?: string;
  protocoloAutorizacao?: string;
  dataHoraAutorizacao?: string;
  valorTotal: number;
  naturezaOperacao?: string;
  cfop?: string;
  informacoesComplementares?: string;
  dataEmissao: string;
  protocoloCancelamento?: string;
  dataHoraCancelamento?: string;
  justificativaCancelamento?: string;
  createdAt: string;
  updatedAt: string;
}

export interface NotaFiscalResumo {
  id: string;
  numero: number;
  serie: number;
  tipo: TipoNotaFiscal;
  status: StatusNotaFiscal;
  valorTotal: number;
  dataEmissao: string;
  chaveAcesso?: string;
}

export interface NotaFiscalRequestDTO {
  ordemServicoId: string;
  tipo: TipoNotaFiscal;
  serie: number;
  valorTotal: number;
  naturezaOperacao?: string;
  cfop?: string;
  informacoesComplementares?: string;
  dataEmissao: string;
}

export interface FiltrosNotaFiscal {
  status?: StatusNotaFiscal;
  tipo?: TipoNotaFiscal;
  dataInicio?: string;
  dataFim?: string;
}

export const TipoNotaFiscalLabels = {
  NFE: 'NF-e (Produto)',
  NFSE: 'NFS-e (Serviço)',
  NFCE: 'NFC-e (Consumidor)',
} as const;

export const StatusNotaFiscalLabels = {
  DIGITACAO: 'Digitação',
  AUTORIZADA: 'Autorizada',
  CANCELADA: 'Cancelada',
  DENEGADA: 'Denegada',
  REJEITADA: 'Rejeitada',
} as const;

export const StatusNotaFiscalColors = {
  DIGITACAO: 'bg-gray-100 text-gray-800',
  AUTORIZADA: 'bg-green-100 text-green-800',
  CANCELADA: 'bg-red-100 text-red-800',
  DENEGADA: 'bg-orange-100 text-orange-800',
  REJEITADA: 'bg-red-100 text-red-800',
} as const;
