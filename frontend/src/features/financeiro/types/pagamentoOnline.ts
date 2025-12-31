/**
 * Types para Pagamentos Online e Configuração de Gateways
 */

// Tipos de Gateway
export const TipoGateway = {
  MERCADO_PAGO: 'MERCADO_PAGO',
  PAGSEGURO: 'PAGSEGURO',
  STRIPE: 'STRIPE',
  ASAAS: 'ASAAS',
  PAGARME: 'PAGARME',
} as const;

export type TipoGateway = (typeof TipoGateway)[keyof typeof TipoGateway];

export const TipoGatewayLabels: Record<TipoGateway, string> = {
  [TipoGateway.MERCADO_PAGO]: 'Mercado Pago',
  [TipoGateway.PAGSEGURO]: 'PagSeguro',
  [TipoGateway.STRIPE]: 'Stripe',
  [TipoGateway.ASAAS]: 'Asaas',
  [TipoGateway.PAGARME]: 'Pagar.me',
};

// Ambiente do Gateway
export const AmbienteGateway = {
  SANDBOX: 'SANDBOX',
  PRODUCAO: 'PRODUCAO',
} as const;

export type AmbienteGateway = (typeof AmbienteGateway)[keyof typeof AmbienteGateway];

export const AmbienteGatewayLabels: Record<AmbienteGateway, string> = {
  [AmbienteGateway.SANDBOX]: 'Sandbox (Testes)',
  [AmbienteGateway.PRODUCAO]: 'Produção',
};

// Status de Pagamento Online
export const StatusPagamentoOnline = {
  PENDENTE: 'PENDENTE',
  PROCESSANDO: 'PROCESSANDO',
  APROVADO: 'APROVADO',
  AUTORIZADO: 'AUTORIZADO',
  EM_ANALISE: 'EM_ANALISE',
  REJEITADO: 'REJEITADO',
  CANCELADO: 'CANCELADO',
  ESTORNADO: 'ESTORNADO',
  DEVOLVIDO: 'DEVOLVIDO',
  EXPIRADO: 'EXPIRADO',
} as const;

export type StatusPagamentoOnline = (typeof StatusPagamentoOnline)[keyof typeof StatusPagamentoOnline];

export const StatusPagamentoOnlineLabels: Record<StatusPagamentoOnline, string> = {
  [StatusPagamentoOnline.PENDENTE]: 'Pendente',
  [StatusPagamentoOnline.PROCESSANDO]: 'Processando',
  [StatusPagamentoOnline.APROVADO]: 'Aprovado',
  [StatusPagamentoOnline.AUTORIZADO]: 'Autorizado',
  [StatusPagamentoOnline.EM_ANALISE]: 'Em Análise',
  [StatusPagamentoOnline.REJEITADO]: 'Rejeitado',
  [StatusPagamentoOnline.CANCELADO]: 'Cancelado',
  [StatusPagamentoOnline.ESTORNADO]: 'Estornado',
  [StatusPagamentoOnline.DEVOLVIDO]: 'Devolvido',
  [StatusPagamentoOnline.EXPIRADO]: 'Expirado',
};

// Configuração de Gateway
export interface ConfiguracaoGateway {
  id: string;
  tipoGateway: TipoGateway;
  tipoGatewayDescricao: string;
  ambiente: AmbienteGateway;
  ambienteDescricao: string;
  ativo: boolean;
  padrao: boolean;
  configurado: boolean;
  taxaPercentual?: number;
  taxaFixa?: number;
  webhookUrl?: string;
  statusValidacao?: string;
  dataUltimaValidacao?: string;
  observacoes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ConfiguracaoGatewayRequest {
  tipoGateway: TipoGateway;
  ambiente: AmbienteGateway;
  accessToken?: string;
  publicKey?: string;
  clientId?: string;
  clientSecret?: string;
  ativo?: boolean;
  padrao?: boolean;
  taxaPercentual?: number;
  taxaFixa?: number;
  observacoes?: string;
}

// Pagamento Online
export interface PagamentoOnline {
  id: string;
  ordemServicoId: string;
  pagamentoId?: string;
  gateway: TipoGateway;
  gatewayDescricao: string;
  preferenceId?: string;
  idExterno?: string;
  idCobranca?: string;
  status: StatusPagamentoOnline;
  statusDescricao: string;
  statusDetalhe?: string;
  valor: number;
  valorLiquido?: number;
  valorTaxa?: number;
  metodoPagamento?: string;
  bandeiraCartao?: string;
  ultimosDigitos?: string;
  parcelas?: number;
  urlCheckout?: string;
  urlQrCode?: string;
  codigoPix?: string;
  dataExpiracao?: string;
  dataAprovacao?: string;
  erroMensagem?: string;
  erroCodigo?: string;
  tentativas?: number;
  emailPagador?: string;
  nomePagador?: string;
  documentoPagador?: string;
  expirado: boolean;
  aprovado: boolean;
  createdAt: string;
  updatedAt: string;
}

// Checkout
export interface CriarCheckoutRequest {
  ordemServicoId: string;
  valor?: number;
  gateway?: TipoGateway;
  emailPagador?: string;
  nomePagador?: string;
  documentoPagador?: string;
  descricao?: string;
  expiracaoMinutos?: number;
  metodosPermitidos?: string[];
}

export interface CheckoutResponse {
  pagamentoOnlineId: string;
  preferenceId: string;
  urlCheckout: string;
  urlQrCode?: string;
  codigoPix?: string;
  valor: number;
  dataExpiracao: string;
  status: string;
  gateway: string;
  mensagem?: string;
}

// Cores para status
export const getStatusColor = (status: StatusPagamentoOnline): string => {
  switch (status) {
    case StatusPagamentoOnline.APROVADO:
    case StatusPagamentoOnline.AUTORIZADO:
      return 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-400';
    case StatusPagamentoOnline.PENDENTE:
    case StatusPagamentoOnline.PROCESSANDO:
      return 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-400';
    case StatusPagamentoOnline.EM_ANALISE:
      return 'bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-400';
    case StatusPagamentoOnline.REJEITADO:
    case StatusPagamentoOnline.CANCELADO:
    case StatusPagamentoOnline.EXPIRADO:
      return 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-400';
    case StatusPagamentoOnline.ESTORNADO:
    case StatusPagamentoOnline.DEVOLVIDO:
      return 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300';
    default:
      return 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300';
  }
};
