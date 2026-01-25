export enum StatusLead {
  NOVO = 'NOVO',
  CONTATADO = 'CONTATADO',
  QUALIFICADO = 'QUALIFICADO',
  CONVERTIDO = 'CONVERTIDO',
  PERDIDO = 'PERDIDO',
}

export interface Lead {
  id: string;
  nome: string;
  email: string;
  whatsapp: string;
  origem: string;
  status: StatusLead;
  observacoes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LeadResumo {
  id: string;
  nome: string;
  email: string;
  whatsapp: string;
  origem: string;
  status: StatusLead;
  createdAt: string;
}

export interface CreateLeadRequest {
  nome: string;
  email: string;
  whatsapp: string;
  origem?: string;
}

export interface UpdateLeadRequest {
  status?: StatusLead;
  observacoes?: string;
}

export interface LeadStats {
  totalNovos: number;
  totalContatados: number;
  totalQualificados: number;
  totalConvertidos: number;
  totalPerdidos: number;
  totalGeral: number;
}

export interface LeadFilters {
  status?: StatusLead;
  origem?: string;
  nome?: string;
  email?: string;
}
