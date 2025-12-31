/**
 * Notification types - Based on backend API
 */

export const TipoNotificacao = {
  EMAIL: 'EMAIL',
  WHATSAPP: 'WHATSAPP',
  SMS: 'SMS',
  TELEGRAM: 'TELEGRAM',
} as const;

export type TipoNotificacao = typeof TipoNotificacao[keyof typeof TipoNotificacao];

export const StatusNotificacao = {
  PENDENTE: 'PENDENTE',
  ENVIADO: 'ENVIADO',
  ENTREGUE: 'ENTREGUE',
  LIDO: 'LIDO',
  FALHA: 'FALHA',
  CANCELADO: 'CANCELADO',
  AGENDADO: 'AGENDADO',
} as const;

export type StatusNotificacao = typeof StatusNotificacao[keyof typeof StatusNotificacao];

export const EventoNotificacao = {
  OS_CRIADA: 'OS_CRIADA',
  OS_AGUARDANDO_APROVACAO: 'OS_AGUARDANDO_APROVACAO',
  OS_APROVADA: 'OS_APROVADA',
  OS_EM_ANDAMENTO: 'OS_EM_ANDAMENTO',
  OS_AGUARDANDO_PECA: 'OS_AGUARDANDO_PECA',
  OS_FINALIZADA: 'OS_FINALIZADA',
  OS_ENTREGUE: 'OS_ENTREGUE',
  PAGAMENTO_PENDENTE: 'PAGAMENTO_PENDENTE',
  PAGAMENTO_CONFIRMADO: 'PAGAMENTO_CONFIRMADO',
  LEMBRETE_RETIRADA: 'LEMBRETE_RETIRADA',
  LEMBRETE_REVISAO: 'LEMBRETE_REVISAO',
} as const;

export type EventoNotificacao = typeof EventoNotificacao[keyof typeof EventoNotificacao];

export interface EventoConfig {
  habilitado: boolean;
  canais: TipoNotificacao[];
  delayMinutos: number;
}

export interface ConfiguracaoNotificacao {
  id: string;
  oficinaId: string;

  // Canais habilitados
  emailHabilitado: boolean;
  whatsappHabilitado: boolean;
  smsHabilitado: boolean;
  telegramHabilitado: boolean;

  // Status de configuracao
  temSmtpProprio: boolean;
  temEvolutionApiConfigurada: boolean;
  temTelegramConfigurado: boolean;

  // SMTP (sem senha)
  smtpHost?: string;
  smtpPort?: number;
  smtpUsername?: string;
  smtpUsarTls?: boolean;
  emailRemetente?: string;
  emailRemetenteNome?: string;

  // WhatsApp (sem token)
  evolutionApiUrl?: string;
  evolutionInstanceName?: string;
  whatsappNumero?: string;
  evolutionApiConfigurada?: boolean;

  // Telegram (sem token)
  telegramChatId?: string;
  telegramConfigurado?: boolean;

  // Horario comercial
  respeitarHorarioComercial?: boolean;
  horarioInicio?: string;
  horarioFim?: string;
  enviarSabados?: boolean;
  enviarDomingos?: boolean;

  // Eventos habilitados
  eventosHabilitados?: Record<EventoNotificacao, EventoConfig>;

  // Configuracoes avancadas
  delayEntreEnviosMs?: number;
  maxTentativasReenvio?: number;
  modoSimulacao?: boolean;
  canalFallback?: TipoNotificacao;

  // Status
  ativo?: boolean;
  podeEnviarAgora?: boolean;

  // Auditoria
  createdAt?: string;
  updatedAt?: string;
}

export interface Notificacao {
  id: string;
  evento: EventoNotificacao;
  tipo: TipoNotificacao;
  status: StatusNotificacao;
  destinatario: string;
  nomeDestinatario?: string;
  assunto?: string;
  mensagem: string;
  dataEnvio?: string;
  dataEntrega?: string;
  dataLeitura?: string;
  idExterno?: string;
  erroMensagem?: string;
  erroCodigo?: string;
  tentativas: number;
  ordemServicoId?: string;
  numeroOS?: number;
  clienteId?: string;
  nomeCliente?: string;
  motivoAgendamento?: string;
  respostaApi?: Record<string, unknown>;
  createdAt: string;
}

export interface NotificacaoFilters {
  tipo?: TipoNotificacao;
  status?: StatusNotificacao;
  evento?: EventoNotificacao;
  destinatario?: string;
  dataInicio?: string;
  dataFim?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface CreateNotificacaoRequest {
  tipo: TipoNotificacao;
  destinatario: string;
  assunto?: string;
  mensagem: string;
  evento?: EventoNotificacao;
}

export interface UpdateConfiguracaoNotificacaoRequest {
  emailHabilitado?: boolean;
  whatsappHabilitado?: boolean;
  smsHabilitado?: boolean;
  telegramHabilitado?: boolean;
  smtpHost?: string;
  smtpPort?: number;
  smtpUsername?: string;
  smtpPassword?: string;
  smtpUsarTls?: boolean;
  emailRemetente?: string;
  emailRemetenteNome?: string;
  evolutionApiUrl?: string;
  evolutionApiToken?: string;
  evolutionInstanceName?: string;
  whatsappNumero?: string;
  telegramBotToken?: string;
  telegramChatId?: string;
  respeitarHorarioComercial?: boolean;
  horarioInicio?: string;
  horarioFim?: string;
  enviarSabados?: boolean;
  enviarDomingos?: boolean;
  modoSimulacao?: boolean;
  maxTentativasReenvio?: number;
  canalFallback?: TipoNotificacao;
}

export interface NotificacaoMetricas {
  oficinaId: string;
  dataInicio: number[] | string;
  dataFim: number[] | string;
  totalEnviadas: number;
  totalEntregues: number;
  totalLidas: number;
  totalFalhas: number;
  totalPendentes: number;
  enviadasPorCanal: Record<string, number>;
  falhasPorCanal: Record<string, number>;
  enviadasPorEvento: Record<string, number>;
  taxaEntrega: number;
  taxaLeitura: number;
  taxaFalha: number;
  variacaoEnvios?: number | null;
  variacaoFalhas?: number | null;
}

export interface WhatsAppStatus {
  disponivel: boolean;
  estado: string;
  conectado: boolean;
  erro?: string;
}

export interface TesteNotificacaoRequest {
  tipo: TipoNotificacao;
  destinatario: string;
  mensagem?: string;
}

// Telegram configuration types
export interface TelegramConfigRequest {
  botToken: string;
  chatId: string;
}

export interface TelegramBotStatus {
  conectado: boolean;
  botUsername?: string;
  botNome?: string;
}
