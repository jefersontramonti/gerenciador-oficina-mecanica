/**
 * Types para configuração de gateway de pagamento SaaS
 */

export type TipoGateway = 'MERCADO_PAGO';

export interface ConfiguracaoGateway {
  id: string | null;
  tipo: TipoGateway;
  tipoNome: string;
  ativo: boolean;
  sandbox: boolean;
  accessTokenMasked: string | null;
  publicKeyMasked: string | null;
  temWebhookSecret: boolean;
  webhookUrl: string;
  ultimaValidacao: string | null;
  validacaoSucesso: boolean | null;
  mensagemValidacao: string | null;
  configurado: boolean;
  updatedAt: string | null;
  // Diagnostic info
  notificationUrlAtiva: boolean;
  baseUrlConfigurada: string | null;
}

export interface ConfiguracaoGatewayRequest {
  tipo?: TipoGateway;
  ativo?: boolean;
  sandbox?: boolean;
  accessToken?: string;
  publicKey?: string;
  webhookSecret?: string;
}
