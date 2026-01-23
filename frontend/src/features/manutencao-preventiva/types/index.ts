// Enums
export type CriterioManutencao = 'TEMPO' | 'KM' | 'AMBOS';
export type StatusPlanoManutencao = 'ATIVO' | 'PAUSADO' | 'CONCLUIDO' | 'VENCIDO';
export type StatusAgendamento = 'AGENDADO' | 'CONFIRMADO' | 'REMARCADO' | 'CANCELADO' | 'REALIZADO';
export type CanalNotificacao = 'WHATSAPP' | 'EMAIL' | 'TELEGRAM' | 'SMS' | 'PUSH' | 'INTERNO';

// Agendamento de Notificação Personalizado
export interface AgendamentoNotificacao {
  data: string; // formato YYYY-MM-DD
  hora: string; // formato HH:mm
  enviado?: boolean;
  enviadoEm?: string;
  erroEnvio?: string;
}

// Checklist
export interface ChecklistItem {
  item: string;
  obrigatorio: boolean;
}

export interface ChecklistExecutado {
  item: string;
  executado: boolean;
  observacao?: string;
}

export interface PecaSugerida {
  pecaId: string;
  quantidade: number;
}

export interface PecaUtilizada {
  pecaId: string;
  descricao: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
}

// Veículo resumido
export interface VeiculoResumo {
  id: string;
  placa: string;
  placaFormatada: string;
  marca: string;
  modelo: string;
  ano: number;
  quilometragem?: number;
  clienteNome?: string;
}

// Cliente resumido
export interface ClienteResumo {
  id: string;
  nome: string;
  telefone?: string;
  email?: string;
}

// Template resumido
export interface TemplateResumo {
  id: string;
  nome: string;
  tipoManutencao: string;
}

// Plano resumido
export interface PlanoResumo {
  id: string;
  nome: string;
  tipoManutencao: string;
}

// ==================== TEMPLATE ====================

export interface TemplateManutencao {
  id: string;
  oficinaId?: string;
  global: boolean;
  nome: string;
  descricao?: string;
  tipoManutencao: string;
  intervaloDias?: number;
  intervaloKm?: number;
  criterio: CriterioManutencao;
  antecedenciaDias: number;
  antecedenciaKm: number;
  checklist?: ChecklistItem[];
  pecasSugeridas?: PecaSugerida[];
  valorEstimado?: number;
  tempoEstimadoMinutos?: number;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TemplateManutencaoRequest {
  nome: string;
  descricao?: string;
  tipoManutencao: string;
  intervaloDias?: number;
  intervaloKm?: number;
  criterio: CriterioManutencao;
  antecedenciaDias?: number;
  antecedenciaKm?: number;
  checklist?: ChecklistItem[];
  pecasSugeridas?: { pecaId: string; quantidade: number }[];
  valorEstimado?: number;
  tempoEstimadoMinutos?: number;
}

// ==================== PLANO ====================

export interface PlanoManutencao {
  id: string;
  veiculo: VeiculoResumo;
  template?: TemplateResumo;
  nome: string;
  descricao?: string;
  tipoManutencao: string;
  criterio: CriterioManutencao;
  intervaloDias?: number;
  intervaloKm?: number;
  antecedenciaDias: number;
  antecedenciaKm: number;
  canaisNotificacao: string[];
  ultimaExecucaoData?: string;
  ultimaExecucaoKm?: number;
  proximaPrevisaoData?: string;
  proximaPrevisaoKm?: number;
  status: StatusPlanoManutencao;
  motivoPausa?: string;
  proximoAVencer: boolean;
  vencido: boolean;
  diasParaVencer?: number;
  checklist?: ChecklistItem[];
  pecasSugeridas?: PecaSugerida[];
  valorEstimado?: number;
  agendamentosNotificacao?: AgendamentoNotificacao[];
  createdAt: string;
  updatedAt: string;
}

export interface PlanoManutencaoRequest {
  veiculoId: string;
  templateId?: string;
  nome: string;
  descricao?: string;
  tipoManutencao: string;
  criterio: CriterioManutencao;
  intervaloDias?: number;
  intervaloKm?: number;
  antecedenciaDias?: number;
  antecedenciaKm?: number;
  canaisNotificacao?: string[];
  ultimaExecucaoData?: string;
  ultimaExecucaoKm?: number;
  checklist?: ChecklistItem[];
  pecasSugeridas?: PecaSugerida[];
  valorEstimado?: number;
  agendamentosNotificacao?: AgendamentoNotificacao[];
}

export interface ExecutarPlanoRequest {
  dataExecucao: string;
  kmExecucao?: number;
  ordemServicoId?: string;
  checklistExecutado?: ChecklistExecutado[];
  pecasUtilizadas?: PecaUtilizada[];
  valorMaoObra?: number;
  valorPecas?: number;
  observacoes?: string;
  observacoesMecanico?: string;
  criarOrdemServico?: boolean;
}

export interface AplicarTemplateRequest {
  veiculoId: string;
  ultimaExecucaoData?: string;
  ultimaExecucaoKm?: number;
}

// ==================== AGENDAMENTO ====================

export interface AgendamentoManutencao {
  id: string;
  plano?: PlanoResumo;
  veiculo: VeiculoResumo;
  cliente: ClienteResumo;
  dataAgendamento: string;
  horaAgendamento: string;
  duracaoEstimadaMinutos: number;
  tipoManutencao: string;
  descricao?: string;
  status: StatusAgendamento;
  confirmadoEm?: string;
  confirmadoVia?: string;
  confirmadoPor?: string;
  canceladoEm?: string;
  motivoCancelamento?: string;
  realizadoEm?: string;
  ordemServicoId?: string;
  lembreteEnviado: boolean;
  observacoes?: string;
  observacoesInternas?: string;
  hoje: boolean;
  passado: boolean;
  createdAt: string;
  /** Feedback sobre o status das notificações (presente na criação do agendamento). */
  notificacaoFeedback?: NotificacaoFeedback;
}

export interface AgendamentoManutencaoRequest {
  planoId?: string;
  veiculoId: string;
  clienteId: string;
  dataAgendamento: string;
  horaAgendamento: string;
  duracaoEstimadaMinutos?: number;
  tipoManutencao: string;
  descricao?: string;
  observacoes?: string;
  observacoesInternas?: string;
  enviarConfirmacao?: boolean;
  /** Canais de notificacao: WHATSAPP, EMAIL, TELEGRAM */
  canaisNotificacao?: string[];
}

export interface RemarcarAgendamentoRequest {
  novaData: string;
  novaHora: string;
  motivo?: string;
}

// ==================== CALENDÁRIO ====================

export interface CalendarioEvento {
  id: string;
  titulo: string;
  descricao?: string;
  inicio: string;
  fim: string;
  status: StatusAgendamento;
  cor: string;
  veiculoId?: string;
  veiculoPlaca?: string;
  veiculoDescricao?: string;
  clienteId?: string;
  clienteNome?: string;
  tipoManutencao: string;
}

// ==================== DASHBOARD ====================

export interface DashboardManutencao {
  estatisticas: EstatisticasManutencao;
  proximasManutencoes: PlanoManutencao[];
  agendamentosHoje: AgendamentoManutencao[];
  alertasPendentes: number;
}

export interface EstatisticasManutencao {
  totalPlanosAtivos: number;
  planosVencidos: number;
  planosProximos30Dias: number;
  manutencoesRealizadasMes: number;
  agendamentosHoje: number;
  agendamentosSemana: number;
  taxaExecucao: number;
  planosPorStatus: Record<string, number>;
  manutencoesPorTipo: Record<string, number>;
}

// ==================== FILTROS ====================

export interface PlanoFilters {
  veiculoId?: string;
  status?: StatusPlanoManutencao;
  tipoManutencao?: string;
  busca?: string;
  page?: number;
  size?: number;
}

export interface AgendamentoFilters {
  veiculoId?: string;
  clienteId?: string;
  status?: StatusAgendamento;
  dataInicio?: string;
  dataFim?: string;
  page?: number;
  size?: number;
}

export interface TemplateFilters {
  tipoManutencao?: string;
  busca?: string;
  page?: number;
  size?: number;
}

// ==================== FEEDBACK DE NOTIFICAÇÃO ====================

/**
 * Feedback detalhado por canal de notificação.
 */
export interface CanalFeedback {
  canal: string;
  criado: boolean;
  destinatario?: string;
  status: string;
  motivo?: string;
}

/**
 * Feedback sobre o status das notificações após criar um agendamento.
 * Informa ao usuário o que aconteceu com cada canal de notificação.
 */
export interface NotificacaoFeedback {
  /** Indica se pelo menos uma notificação foi criada. */
  notificacoesCriadas: boolean;
  /** Indica se as notificações serão enviadas imediatamente. */
  envioImediato: boolean;
  /** Motivo pelo qual o envio não é imediato (ex: "Fora do horário comercial"). */
  motivoAtraso?: string;
  /** Horário em que as notificações agendadas serão enviadas. */
  horarioPrevistaEnvio?: string;
  /** Quantidade total de notificações criadas. */
  totalNotificacoes: number;
  /** Detalhes de cada canal de notificação. */
  canais: CanalFeedback[];
  /** Mensagem resumida para exibir ao usuário. */
  mensagemUsuario: string;
}
