/**
 * Webhook types - Based on backend API
 */

export const TipoEventoWebhook = {
  // Ordens de Serviço
  OS_CRIADA: 'OS_CRIADA',
  OS_STATUS_ALTERADO: 'OS_STATUS_ALTERADO',
  OS_APROVADA: 'OS_APROVADA',
  OS_FINALIZADA: 'OS_FINALIZADA',
  OS_ENTREGUE: 'OS_ENTREGUE',
  OS_CANCELADA: 'OS_CANCELADA',
  // Clientes
  CLIENTE_CRIADO: 'CLIENTE_CRIADO',
  CLIENTE_ATUALIZADO: 'CLIENTE_ATUALIZADO',
  // Veículos
  VEICULO_CRIADO: 'VEICULO_CRIADO',
  VEICULO_ATUALIZADO: 'VEICULO_ATUALIZADO',
  // Financeiro
  PAGAMENTO_RECEBIDO: 'PAGAMENTO_RECEBIDO',
  PAGAMENTO_CANCELADO: 'PAGAMENTO_CANCELADO',
  // Estoque
  ESTOQUE_BAIXO: 'ESTOQUE_BAIXO',
  ESTOQUE_MOVIMENTADO: 'ESTOQUE_MOVIMENTADO',
  // Manutenção
  MANUTENCAO_VENCIDA: 'MANUTENCAO_VENCIDA',
  AGENDAMENTO_CRIADO: 'AGENDAMENTO_CRIADO',
} as const;

export type TipoEventoWebhook = typeof TipoEventoWebhook[keyof typeof TipoEventoWebhook];

export const StatusWebhookLog = {
  PENDENTE: 'PENDENTE',
  SUCESSO: 'SUCESSO',
  FALHA: 'FALHA',
  AGUARDANDO_RETRY: 'AGUARDANDO_RETRY',
  ESGOTADO: 'ESGOTADO',
} as const;

export type StatusWebhookLog = typeof StatusWebhookLog[keyof typeof StatusWebhookLog];

// Descrições dos eventos para exibição
export const eventoDescricoes: Record<TipoEventoWebhook, { nome: string; descricao: string }> = {
  OS_CRIADA: { nome: 'OS Criada', descricao: 'Quando uma nova OS é criada' },
  OS_STATUS_ALTERADO: { nome: 'Status da OS Alterado', descricao: 'Quando o status de uma OS muda' },
  OS_APROVADA: { nome: 'OS Aprovada', descricao: 'Quando o cliente aprova o orçamento' },
  OS_FINALIZADA: { nome: 'OS Finalizada', descricao: 'Quando a OS é finalizada' },
  OS_ENTREGUE: { nome: 'OS Entregue', descricao: 'Quando o veículo é entregue' },
  OS_CANCELADA: { nome: 'OS Cancelada', descricao: 'Quando uma OS é cancelada' },
  CLIENTE_CRIADO: { nome: 'Cliente Criado', descricao: 'Quando um novo cliente é cadastrado' },
  CLIENTE_ATUALIZADO: { nome: 'Cliente Atualizado', descricao: 'Quando os dados do cliente são atualizados' },
  VEICULO_CRIADO: { nome: 'Veículo Criado', descricao: 'Quando um novo veículo é cadastrado' },
  VEICULO_ATUALIZADO: { nome: 'Veículo Atualizado', descricao: 'Quando os dados do veículo são atualizados' },
  PAGAMENTO_RECEBIDO: { nome: 'Pagamento Recebido', descricao: 'Quando um pagamento é confirmado' },
  PAGAMENTO_CANCELADO: { nome: 'Pagamento Cancelado', descricao: 'Quando um pagamento é cancelado' },
  ESTOQUE_BAIXO: { nome: 'Estoque Baixo', descricao: 'Quando uma peça atinge o estoque mínimo' },
  ESTOQUE_MOVIMENTADO: { nome: 'Estoque Movimentado', descricao: 'Quando há entrada ou saída de peças' },
  MANUTENCAO_VENCIDA: { nome: 'Manutenção Vencida', descricao: 'Quando uma manutenção preventiva vence' },
  AGENDAMENTO_CRIADO: { nome: 'Agendamento Criado', descricao: 'Quando um agendamento é criado' },
};

// Descrições dos status
export const statusDescricoes: Record<StatusWebhookLog, string> = {
  PENDENTE: 'Pendente',
  SUCESSO: 'Sucesso',
  FALHA: 'Falha',
  AGUARDANDO_RETRY: 'Aguardando Retry',
  ESGOTADO: 'Tentativas Esgotadas',
};

export interface WebhookConfig {
  id: string;
  oficinaId: string;
  nome: string;
  descricao?: string;
  url: string;
  temSecret: boolean;
  headers?: Record<string, string>;
  eventos: TipoEventoWebhook[];
  maxTentativas: number;
  timeoutSegundos: number;
  ativo: boolean;
  falhasConsecutivas: number;
  ultimaExecucaoSucesso?: string;
  ultimaFalha?: string;
  createdAt: string;
  updatedAt: string;
}

export interface WebhookConfigCreateRequest {
  nome: string;
  descricao?: string;
  url: string;
  secret?: string;
  headers?: Record<string, string>;
  eventos: TipoEventoWebhook[];
  maxTentativas?: number;
  timeoutSegundos?: number;
}

export interface WebhookConfigUpdateRequest {
  nome?: string;
  descricao?: string;
  url?: string;
  secret?: string;
  removerSecret?: boolean;
  headers?: Record<string, string>;
  eventos?: TipoEventoWebhook[];
  maxTentativas?: number;
  timeoutSegundos?: number;
  ativo?: boolean;
}

export interface WebhookLog {
  id: string;
  webhookConfigId: string;
  webhookNome: string;
  oficinaId: string;
  evento: TipoEventoWebhook;
  eventoNome: string;
  entidadeId?: string;
  entidadeTipo?: string;
  url: string;
  payload?: string;
  httpStatus?: number;
  responseBody?: string;
  erroMensagem?: string;
  tempoRespostaMs?: number;
  tentativa: number;
  status: StatusWebhookLog;
  statusDescricao: string;
  proximaTentativa?: string;
  createdAt: string;
}

export interface WebhookStats {
  totalWebhooks: number;
  webhooksAtivos: number;
  webhooksInativos: number;
  sucessos24h: number;
  falhas24h: number;
  tempoMedioResposta24h?: number;
  pendentesRetry: number;
}

export interface WebhookTestRequest {
  webhookId: string;
  evento: TipoEventoWebhook;
}

export interface WebhookTestResult {
  sucesso: boolean;
  httpStatus?: number;
  responseBody?: string;
  erro?: string;
  tempoRespostaMs?: number;
  payloadEnviado?: string;
}

export interface WebhookEvento {
  codigo: string;
  nome: string;
  descricao: string;
}

export interface WebhookFilters {
  page?: number;
  size?: number;
  sort?: string;
}

export interface WebhookLogFilters extends WebhookFilters {
  status?: StatusWebhookLog;
  evento?: TipoEventoWebhook;
}
