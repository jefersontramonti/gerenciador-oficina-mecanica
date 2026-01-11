/**
 * Types for the "Meu Plano" feature
 */

export type StatusOficina = 'ATIVA' | 'INATIVA' | 'SUSPENSA' | 'TRIAL' | 'CANCELADA';

export interface PlanoInfo {
  codigo: string;
  nome: string;
  descricao: string;
  valorMensal: number;
  status: StatusOficina | null;
  dataVencimento: string | null;
  diasRestantesTrial: number | null;
  // Plan limits
  maxUsuarios: number;
  maxOrdensServico: number;
  maxClientes: number;
  // Plan features
  usuariosIlimitados: boolean;
  emiteNotaFiscal: boolean;
  whatsappAutomatizado: boolean;
  manutencaoPreventiva: boolean;
  anexoImagensDocumentos: boolean;
}

export interface FeatureInfo {
  codigo: string;
  nome: string;
  descricao: string;
  categoria: string;
  habilitada: boolean;
  disponivelNoPlano: string | null;
}

export interface MeuPlanoDTO {
  planoAtual: PlanoInfo;
  todosPlanos: PlanoInfo[];
  featuresHabilitadas: FeatureInfo[];
  proximoPlano: PlanoInfo | null;
  featuresProximoPlano: FeatureInfo[];
  totalFeaturesHabilitadas: number;
  totalFeaturesDisponiveis: number;
}
