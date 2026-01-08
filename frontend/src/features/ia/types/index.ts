/**
 * Types para o módulo de IA
 */

export const ProvedorIA = {
  ANTHROPIC: 'ANTHROPIC',
  OPENAI: 'OPENAI',
} as const;
export type ProvedorIA = (typeof ProvedorIA)[keyof typeof ProvedorIA];

export const OrigemDiagnostico = {
  TEMPLATE: 'TEMPLATE',
  CACHE: 'CACHE',
  IA_HAIKU: 'IA_HAIKU',
  IA_SONNET: 'IA_SONNET',
} as const;
export type OrigemDiagnostico = (typeof OrigemDiagnostico)[keyof typeof OrigemDiagnostico];

export const Gravidade = {
  BAIXA: 'BAIXA',
  MEDIA: 'MEDIA',
  ALTA: 'ALTA',
  CRITICA: 'CRITICA',
} as const;
export type Gravidade = (typeof Gravidade)[keyof typeof Gravidade];

export const Urgencia = {
  BAIXA: 'BAIXA',
  MEDIA: 'MEDIA',
  ALTA: 'ALTA',
  IMEDIATA: 'IMEDIATA',
} as const;
export type Urgencia = (typeof Urgencia)[keyof typeof Urgencia];

// ===== Configuração IA =====

export interface ConfiguracaoIA {
  id: string;
  provedorAtual: ProvedorIA;
  modeloPadrao: string;
  modeloAvancado: string;
  iaHabilitada: boolean;
  apiKeyConfigurada: boolean;
  usarPreValidacao: boolean;
  usarCache: boolean;
  usarRoteamentoInteligente: boolean;
  maxRequisicoesDia: number;
  maxTokensResposta: number;
  totalRequisicoes: number;
  totalCacheHits: number;
  totalTemplateHits: number;
  totalTokensConsumidos: number;
  custoEstimadoTotal: number;
  requisicoesHoje: number;
}

export interface ConfiguracaoIARequest {
  provedorAtual?: ProvedorIA;
  modeloPadrao?: string;
  modeloAvancado?: string;
  iaHabilitada?: boolean;
  usarPreValidacao?: boolean;
  usarCache?: boolean;
  usarRoteamentoInteligente?: boolean;
  maxRequisicoesDia?: number;
  maxTokensResposta?: number;
}

export interface AtualizarApiKeyRequest {
  apiKey: string;
}

// ===== Diagnóstico IA =====

export interface DiagnosticoIARequest {
  veiculoId: string;
  problemasRelatados: string;
}

export interface CausaPossivel {
  descricao: string;
  probabilidade: number;
  gravidade: Gravidade;
}

export interface PecaProvavel {
  nome: string;
  codigoReferencia: string;
  urgencia: Urgencia;
  custoEstimado: number;
}

export interface FaixaCusto {
  minimo: number;
  maximo: number;
  moeda: string;
}

export interface MetadadosDiagnostico {
  origem: OrigemDiagnostico;
  modeloUtilizado: string;
  tokensConsumidos: number;
  custoEstimado: number;
  tempoProcessamentoMs: number;
}

export interface DiagnosticoIAResponse {
  resumo: string;
  causasPossiveis: CausaPossivel[];
  acoesRecomendadas: string[];
  pecasProvaveis: PecaProvavel[];
  estimativaTempo: string;
  faixaCusto: FaixaCusto;
  metadados: MetadadosDiagnostico;
}

export interface EstatisticasUsoIA {
  totalRequisicoes: number;
  cacheHits: number;
  templateHits: number;
  tokensConsumidos: number;
  custoTotal: number;
  taxaEconomia: number;
  requisicoesHoje: number;
  requisicoesRestantes: number;
}
