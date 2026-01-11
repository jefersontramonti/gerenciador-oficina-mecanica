/**
 * Types for SUPER_ADMIN SaaS Management
 */

// Status da oficina
export const StatusOficina = {
  TRIAL: 'TRIAL',
  ATIVA: 'ATIVA',
  INATIVA: 'INATIVA',
  SUSPENSA: 'SUSPENSA',
  CANCELADA: 'CANCELADA',
} as const;

export type StatusOficina = (typeof StatusOficina)[keyof typeof StatusOficina];

// Planos de assinatura
export const PlanoAssinatura = {
  ECONOMICO: 'ECONOMICO',
  PROFISSIONAL: 'PROFISSIONAL',
  TURBINADO: 'TURBINADO',
} as const;

export type PlanoAssinatura = (typeof PlanoAssinatura)[keyof typeof PlanoAssinatura];

// Labels para exibição
export const statusLabels: Record<StatusOficina, string> = {
  TRIAL: 'Trial',
  ATIVA: 'Ativa',
  INATIVA: 'Inativa',
  SUSPENSA: 'Suspensa',
  CANCELADA: 'Cancelada',
};

export const planoLabels: Record<PlanoAssinatura, string> = {
  ECONOMICO: 'Econômico',
  PROFISSIONAL: 'Profissional',
  TURBINADO: 'Turbinado',
};

export const planoPrecos: Record<PlanoAssinatura, number> = {
  ECONOMICO: 160.00,
  PROFISSIONAL: 250.00,
  TURBINADO: 0, // Sob consulta
};

export const planoDescricoes: Record<PlanoAssinatura, string> = {
  ECONOMICO: '1 usuário • Sem nota fiscal • Ideal para pequenas oficinas',
  PROFISSIONAL: '3 usuários • Com nota fiscal (NF-e, NFS-e, NFC-e) • Para oficinas em crescimento',
  TURBINADO: 'Usuários ilimitados • Todas as features • WhatsApp, manutenção preventiva',
};

export const planoFeatures: Record<PlanoAssinatura, { maxUsuarios: number; emiteNF: boolean; whatsapp: boolean }> = {
  ECONOMICO: { maxUsuarios: 1, emiteNF: false, whatsapp: false },
  PROFISSIONAL: { maxUsuarios: 3, emiteNF: true, whatsapp: false },
  TURBINADO: { maxUsuarios: -1, emiteNF: true, whatsapp: true },
};

// ===== DASHBOARD =====

export interface DashboardStats {
  totalOficinas: number;
  oficinasAtivas: number;
  oficinasTrial: number;
  oficinasSuspensas: number;
  oficinasCanceladas: number;
  mrrTotal: number;
  totalOrdensServico: number;
  totalClientes: number;
  totalVeiculos: number;
  pagamentosPendentes: number;
  pagamentosAtrasados: number;
}

export interface MRRBreakdown {
  plano: PlanoAssinatura;
  quantidadeOficinas: number;
  mrrPlano: number;
  percentualTotal: number;
}

// ===== ADVANCED METRICS =====

export interface DashboardMetrics {
  // Financial
  mrrTotal: number;
  mrrGrowth: number;
  arrTotal: number;
  churnRate: number;
  ltv: number;
  cac: number;

  // Workshops
  oficinasAtivas: number;
  oficinasTrial: number;
  oficinasInativas: number;
  oficinasInadimplentes: number;
  novasOficinas30d: number;
  cancelamentos30d: number;

  // Users
  usuariosAtivos: number;
  usuariosTotais: number;
  loginsMes: number;

  // General
  totalClientes: number;
  totalVeiculos: number;
  totalOS: number;
  totalOSMes: number;
  faturamentoMes: number;
}

export interface MonthlyMRRData {
  month: string;
  monthLabel: string;
  mrr: number;
  growth: number;
  oficinasAtivas: number;
}

export interface MRREvolution {
  data: MonthlyMRRData[];
  totalGrowth: number;
  averageMRR: number;
}

export interface MonthlyChurnData {
  month: string;
  monthLabel: string;
  churnRate: number;
  cancelled: number;
  activeAtStart: number;
}

export interface ChurnEvolution {
  data: MonthlyChurnData[];
  averageChurn: number;
  currentChurn: number;
  totalCancelled: number;
}

export interface MonthlySignupData {
  month: string;
  monthLabel: string;
  signups: number;
  cancellations: number;
  netGrowth: number;
  trialConversions: number;
}

export interface SignupsVsCancellations {
  data: MonthlySignupData[];
  totalSignups: number;
  totalCancellations: number;
  netGrowth: number;
}

// ===== OFICINAS =====

export interface OficinaResumo {
  id: string;
  nomeFantasia: string;
  razaoSocial: string;
  cnpjCpf: string;
  email: string;
  telefone: string;
  status: StatusOficina;
  plano: PlanoAssinatura;
  valorMensalidade: number;
  dataVencimentoPlano: string;
  diasRestantesTrial?: number;
  createdAt: string;
}

export interface OficinaDetail extends OficinaResumo {
  inscricaoEstadual?: string;
  inscricaoMunicipal?: string;
  nomeResponsavel?: string;
  // Endereço
  cep?: string;
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  estado?: string;
  // Estatísticas
  totalUsuarios: number;
  totalClientes: number;
  totalVeiculos: number;
  totalOrdensServico: number;
  totalPecas: number;
  // Financeiro
  totalFaturamento: number;
  pagamentosRealizados: number;
  pagamentosPendentes: number;
  ultimoPagamento?: string;
  // Trial
  dataInicioTrial?: string;
  dataFimTrial?: string;
}

export interface CreateOficinaRequest {
  nomeFantasia: string;
  razaoSocial: string;
  cnpjCpf: string;
  email: string;
  telefone: string;
  plano: PlanoAssinatura;
  // Admin
  nomeAdmin: string;
  emailAdmin: string;
  senhaAdmin: string;
}

export interface UpdateOficinaRequest {
  nomeFantasia?: string;
  razaoSocial?: string;
  email?: string;
  telefone?: string;
  plano?: PlanoAssinatura;
  valorMensalidade?: number;
}

export interface OficinaFilters {
  status?: StatusOficina;
  plano?: PlanoAssinatura;
  searchTerm?: string;
  page?: number;
  size?: number;
  sort?: string;
}

// ===== PAGAMENTOS =====

export const StatusPagamento = {
  PENDENTE: 'PENDENTE',
  PAGO: 'PAGO',
  ATRASADO: 'ATRASADO',
  CANCELADO: 'CANCELADO',
} as const;

export type StatusPagamento = (typeof StatusPagamento)[keyof typeof StatusPagamento];

export interface Pagamento {
  id: string;
  oficinaId: string;
  oficinaNome: string;
  valor: number;
  dataVencimento: string;
  dataPagamento?: string;
  status: StatusPagamento;
  formaPagamento?: string;
  observacao?: string;
  createdAt: string;
}

export interface RegistrarPagamentoRequest {
  oficinaId: string;
  valor: number;
  dataPagamento: string;
  formaPagamento: string;
  observacao?: string;
}

export interface PagamentoFilters {
  oficinaId?: string;
  status?: StatusPagamento;
  dataInicio?: string;
  dataFim?: string;
  page?: number;
  size?: number;
}

// ===== AUDITORIA =====

export interface AuditLog {
  id: string;
  acao: string;
  entidade: string;
  entidadeId?: string;
  usuarioEmail?: string;
  detalhes?: string;
  ipAddress?: string;
  userAgent?: string;
  timestamp: string;
}

export interface AuditFilters {
  acao?: string;
  entidade?: string;
  usuarioEmail?: string;
  ipAddress?: string;
  dataInicio?: string;
  dataFim?: string;
  page?: number;
  size?: number;
}

// ===== OFICINA METRICAS =====

export interface LoginHistorico {
  usuarioNome: string;
  usuarioEmail: string;
  dataLogin: string;
  ip?: string;
  dispositivo?: string;
}

export interface OficinaMetricas {
  // Limites e Uso
  usuariosAtivos: number;
  limiteUsuarios: number;
  espacoUsadoBytes: number;
  limiteEspacoBytes: number;
  osNoMes: number;
  limiteOSMes: number | null;

  // Estatísticas Gerais
  clientesTotal: number;
  veiculosTotal: number;
  pecasTotal: number;
  faturamentoMes: number;
  faturamentoTotal: number;

  // Atividade
  ultimoAcesso?: string;
  loginsUltimos30Dias: number;
  ultimosLogins: LoginHistorico[];

  // Performance
  osFinalizadasMes: number;
  osCanceladasMes: number;
  ticketMedio: number;
  taxaConversao: number;

  // Estoque
  pecasEstoqueBaixo: number;
  valorEstoqueTotal: number;
}

// ===== IMPERSONATE =====

export interface ImpersonateResponse {
  accessToken: string;
  redirectUrl: string;
  expiresAt: string;
  oficinaId: string;
  oficinaNome: string;
  usuarioEmail: string;
}

// ===== UPDATE LIMITES =====

export interface UpdateLimitesRequest {
  limiteUsuarios?: number;
  limiteEspaco?: number;
  limiteOSMes?: number;
  limiteApiCalls?: number;
  features?: Record<string, boolean>;
  motivo?: string;
}

// ===== CREATE OFICINA (Full) =====

export interface CreateOficinaFullRequest {
  // Dados da Oficina
  razaoSocial: string;
  nomeFantasia: string;
  cnpj: string;
  email: string;
  telefone: string;
  plano: string; // Plan code from API (e.g., "ECONOMICO", "PROFISSIONAL")

  // Endereço
  cep: string;
  logradouro: string;
  numero: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  estado: string;

  // Dados do Admin
  nomeAdmin: string;
  emailAdmin: string;
  senhaAdmin: string;
}

// ===== UPDATE OFICINA (Full) =====

export interface UpdateOficinaFullRequest {
  razaoSocial: string;
  nomeFantasia: string;
  email: string;
  telefone: string;
  plano: string; // Plan code from API (e.g., "ECONOMICO", "PROFISSIONAL")
  valorMensalidade: number; // Custom monthly value for this specific workshop

  // Endereço
  cep: string;
  logradouro: string;
  numero: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  estado: string;
}

// ===== PLANOS =====

export interface PlanoFeatures {
  emiteNotaFiscal: boolean;
  whatsappAutomatizado: boolean;
  manutencaoPreventiva: boolean;
  anexoImagensDocumentos: boolean;
  relatoriosAvancados: boolean;
  integracaoMercadoPago: boolean;
  suportePrioritario: boolean;
  backupAutomatico: boolean;
}

export interface Plano {
  id: string;
  codigo: string;
  nome: string;
  descricao?: string;

  // Pricing
  valorMensal: number;
  valorAnual?: number;
  trialDias: number;
  descontoAnual?: number;

  // Limits (-1 = unlimited)
  limiteUsuarios: number;
  limiteOsMes: number;
  limiteClientes: number;
  limiteEspacoMb: number;
  limiteApiCalls: number;
  limiteWhatsappMensagens: number;
  limiteEmailsMes: number;

  // Features
  features: PlanoFeatures;

  // Display & Marketing
  ativo: boolean;
  visivel: boolean;
  recomendado: boolean;
  corDestaque?: string;
  tagPromocao?: string;
  ordemExibicao: number;

  // Computed
  precoSobConsulta: boolean;
  usuariosIlimitados: boolean;
  espacoIlimitado: boolean;

  // Timestamps
  createdAt: string;
  updatedAt: string;
}

export interface CreatePlanoRequest {
  codigo: string;
  nome: string;
  descricao?: string;
  valorMensal: number;
  valorAnual?: number;
  trialDias?: number;
  limiteUsuarios?: number;
  limiteOsMes?: number;
  limiteClientes?: number;
  limiteEspacoMb?: number;
  limiteApiCalls?: number;
  limiteWhatsappMensagens?: number;
  limiteEmailsMes?: number;
  features?: Partial<PlanoFeatures>;
  ativo?: boolean;
  visivel?: boolean;
  recomendado?: boolean;
  corDestaque?: string;
  tagPromocao?: string;
  ordemExibicao?: number;
}

export interface UpdatePlanoRequest {
  codigo?: string;
  nome?: string;
  descricao?: string;
  valorMensal?: number;
  valorAnual?: number;
  trialDias?: number;
  limiteUsuarios?: number;
  limiteOsMes?: number;
  limiteClientes?: number;
  limiteEspacoMb?: number;
  limiteApiCalls?: number;
  limiteWhatsappMensagens?: number;
  limiteEmailsMes?: number;
  features?: Partial<PlanoFeatures>;
  ativo?: boolean;
  visivel?: boolean;
  recomendado?: boolean;
  corDestaque?: string;
  tagPromocao?: string;
  ordemExibicao?: number;
}

export interface AlterarPlanoOficinaRequest {
  novoPlano: string;
  aplicarImediatamente?: boolean;
  manterPrecoAntigo?: boolean;
  motivo?: string;
}

export interface PlanoStatistics {
  totalPlanos: number;
  planosAtivos: number;
  oficinasPerPlano: Record<string, number>;
  mrrPerPlano: Record<string, number>;
}

// ===== FATURAS =====

export const StatusFatura = {
  PENDENTE: 'PENDENTE',
  PAGO: 'PAGO',
  VENCIDO: 'VENCIDO',
  CANCELADO: 'CANCELADO',
} as const;

export type StatusFatura = (typeof StatusFatura)[keyof typeof StatusFatura];

export const statusFaturaLabels: Record<StatusFatura, string> = {
  PENDENTE: 'Pendente',
  PAGO: 'Pago',
  VENCIDO: 'Vencido',
  CANCELADO: 'Cancelado',
};

export const statusFaturaCores: Record<StatusFatura, string> = {
  PENDENTE: 'yellow',
  PAGO: 'green',
  VENCIDO: 'red',
  CANCELADO: 'gray',
};

export interface ItemFatura {
  id: string;
  descricao: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
}

export interface FaturaResumo {
  id: string;
  numero: string;
  oficinaId: string;
  oficinaNome: string;
  status: StatusFatura;
  statusLabel: string;
  statusCor: string;
  valorTotal: number;
  mesReferencia: string;
  mesReferenciaFormatado: string;
  dataEmissao: string;
  dataVencimento: string;
  dataPagamento?: string;
  diasAteVencimento: number;
  vencida: boolean;
  pagavel: boolean;
}

export interface Fatura extends FaturaResumo {
  oficinaCnpj?: string;
  oficinaEmail?: string;
  planoCodigo?: string;
  valorBase: number;
  valorDesconto: number;
  valorAcrescimos: number;
  metodoPagamento?: string;
  transacaoId?: string;
  qrCodePix?: string;
  linkPagamento?: string;
  observacao?: string;
  tentativasCobranca: number;
  itens: ItemFatura[];
  cancelavel: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFaturaRequest {
  oficinaId: string;
  mesReferencia: string;
  dataVencimento: string;
  itens: {
    descricao: string;
    quantidade: number;
    valorUnitario: number;
  }[];
  desconto?: number;
  observacao?: string;
}

export interface RegistrarPagamentoFaturaRequest {
  dataPagamento: string;
  metodoPagamento: string;
  transacaoId?: string;
  observacao?: string;
}

export interface FaturaFilters {
  oficinaId?: string;
  status?: StatusFatura;
  dataInicio?: string;
  dataFim?: string;
  page?: number;
  size?: number;
}

export interface FaturaSummary {
  totalPendentes: number;
  totalVencidas: number;
  totalPagas: number;
  totalCanceladas: number;
  valorPendente: number;
  valorVencido: number;
  valorRecebidoMes: number;
  oficinasInadimplentes: number;
}

// ===== INADIMPLÊNCIA =====

export interface InadimplenciaFaixa {
  faixa: string;
  quantidadeFaturas: number;
  quantidadeOficinas: number;
  valorTotal: number;
}

export interface InadimplenciaDashboard {
  valorTotalInadimplente: number;
  oficinasInadimplentes: number;
  faturasVencidas: number;
  porFaixaAtraso: Record<string, InadimplenciaFaixa>;
  top10Inadimplentes: OficinaInadimplente[];
  valorRecuperadoMes: number;
  acordosAtivos: number;
  valorEmAcordos: number;
}

export interface FaturaVencidaResumo {
  faturaId: string;
  numero: string;
  valor: number;
  dataVencimento: string;
  diasAtraso: number;
  mesReferencia: string;
}

export interface OficinaInadimplente {
  oficinaId: string;
  nomeFantasia: string;
  cnpj: string;
  email: string;
  telefone: string;
  planoNome: string;
  faturasVencidas: number;
  valorTotalDevido: number;
  faturaMaisAntigaVencimento: string;
  diasAtrasoMaior: number;
  notificacoesEnviadas: number;
  ultimaNotificacao?: string;
  possuiAcordoAtivo: boolean;
  valorAcordoAtivo: number;
  ultimoAcesso?: string;
  podeNotificar: boolean;
  podeSuspender: boolean;
  podeCancelar: boolean;
  faturasVencidasList: FaturaVencidaResumo[];
}

// ===== AÇÕES EM MASSA =====

export const AcaoInadimplencia = {
  NOTIFICAR: 'NOTIFICAR',
  NOTIFICAR_URGENTE: 'NOTIFICAR_URGENTE',
  SUSPENDER: 'SUSPENDER',
  REATIVAR: 'REATIVAR',
  CANCELAR: 'CANCELAR',
} as const;

export type AcaoInadimplencia = (typeof AcaoInadimplencia)[keyof typeof AcaoInadimplencia];

export const acaoInadimplenciaLabels: Record<AcaoInadimplencia, string> = {
  NOTIFICAR: 'Enviar Notificação',
  NOTIFICAR_URGENTE: 'Enviar Notificação Urgente',
  SUSPENDER: 'Suspender Acesso',
  REATIVAR: 'Reativar Acesso',
  CANCELAR: 'Cancelar Assinatura',
};

export interface AcaoMassaInadimplenciaRequest {
  oficinaIds: string[];
  acao: AcaoInadimplencia;
  mensagemCustomizada?: string;
}

export interface ResultadoIndividual {
  oficinaId: string;
  oficinaNome?: string;
  sucesso: boolean;
  mensagem: string;
}

export interface AcaoMassaResult {
  totalProcessadas: number;
  totalSucesso: number;
  totalFalha: number;
  resultados: ResultadoIndividual[];
}

// ===== ACORDOS =====

export const StatusAcordo = {
  ATIVO: 'ATIVO',
  QUITADO: 'QUITADO',
  QUEBRADO: 'QUEBRADO',
  CANCELADO: 'CANCELADO',
} as const;

export type StatusAcordo = (typeof StatusAcordo)[keyof typeof StatusAcordo];

export const statusAcordoLabels: Record<StatusAcordo, string> = {
  ATIVO: 'Ativo',
  QUITADO: 'Quitado',
  QUEBRADO: 'Quebrado',
  CANCELADO: 'Cancelado',
};

export const statusAcordoCores: Record<StatusAcordo, string> = {
  ATIVO: 'green',
  QUITADO: 'blue',
  QUEBRADO: 'red',
  CANCELADO: 'gray',
};

export const StatusParcela = {
  PENDENTE: 'PENDENTE',
  PAGO: 'PAGO',
  VENCIDO: 'VENCIDO',
  CANCELADO: 'CANCELADO',
} as const;

export type StatusParcela = (typeof StatusParcela)[keyof typeof StatusParcela];

export const statusParcelaLabels: Record<StatusParcela, string> = {
  PENDENTE: 'Pendente',
  PAGO: 'Pago',
  VENCIDO: 'Vencido',
  CANCELADO: 'Cancelado',
};

export interface FaturaAcordo {
  faturaId: string;
  numero: string;
  valorOriginal: number;
  dataVencimento: string;
  mesReferencia: string;
}

export interface ParcelaAcordo {
  id: string;
  numeroParcela: number;
  valor: number;
  dataVencimento: string;
  dataPagamento?: string;
  status: StatusParcela;
  statusLabel: string;
}

export interface Acordo {
  id: string;
  numero: string;
  oficinaId: string;
  oficinaNome: string;
  status: StatusAcordo;
  statusLabel: string;
  valorOriginal: number;
  valorDesconto: number;
  valorAcordado: number;
  percentualDesconto: number;
  totalParcelas: number;
  parcelasPagas: number;
  parcelasPendentes: number;
  valorPago: number;
  valorRestante: number;
  dataAcordo: string;
  primeiroVencimento: string;
  proximoVencimento?: string;
  observacoes?: string;
  criadoPor?: string;
  criadoPorNome?: string;
  createdAt: string;
  updatedAt: string;
  faturas: FaturaAcordo[];
  parcelas: ParcelaAcordo[];
}

export interface CriarAcordoRequest {
  faturaIds: string[];
  valorTotalAcordado: number;
  numeroParcelas: number;
  primeiroVencimento: string;
  percentualDesconto?: number;
  observacoes?: string;
  enviarNotificacao?: boolean;
}

export interface AcordoFilters {
  oficinaId?: string;
  status?: StatusAcordo;
  page?: number;
  size?: number;
}

// ===== RELATÓRIOS =====

export type TipoRelatorio = 'FINANCEIRO' | 'OPERACIONAL' | 'CRESCIMENTO' | 'INADIMPLENCIA' | 'CUSTOMIZADO';
export type FormatoExport = 'PDF' | 'EXCEL' | 'CSV' | 'JSON';
export type AgrupamentoRelatorio = 'DIA' | 'SEMANA' | 'MES' | 'TRIMESTRE' | 'ANO';

export interface RelatorioRequest {
  tipo: TipoRelatorio;
  dataInicio: string;
  dataFim: string;
  oficinaIds?: string[];
  planoIds?: string[];
  status?: string[];
  agrupamento?: AgrupamentoRelatorio;
  formato?: FormatoExport;
  incluirGraficos?: boolean;
  incluirDetalhamento?: boolean;
  incluirComparativo?: boolean;
}

export interface RelatorioSummary {
  relatoriosDisponiveis: RelatorioDisponivel[];
  relatoriosRecentes: RelatorioRecente[];
  periodoDisponivel: {
    dataMinima: string;
    dataMaxima: string;
  };
}

export interface RelatorioDisponivel {
  tipo: string;
  nome: string;
  descricao: string;
  icone: string;
  formatosDisponiveis: string[];
}

export interface RelatorioRecente {
  id: string;
  tipo: string;
  nome: string;
  geradoEm: string;
  formato: string;
  url: string;
}

// Relatório Financeiro
export interface RelatorioFinanceiro {
  dataInicio: string;
  dataFim: string;
  receitaTotal: number;
  receitaMensal: number;
  mrrAtual: number;
  arrAtual: number;
  ticketMedio: number;
  receitaPeriodoAnterior: number;
  variacaoPercentual: number;
  totalFaturas: number;
  faturasPagas: number;
  faturasPendentes: number;
  faturasVencidas: number;
  faturasCanceladas: number;
  valorFaturasPagas: number;
  valorFaturasPendentes: number;
  valorFaturasVencidas: number;
  oficinasInadimplentes: number;
  valorInadimplente: number;
  taxaInadimplencia: number;
  receitaPorPlano: ReceitaPorPlano[];
  evolucaoMensal: EvolucaoMensalFinanceiro[];
  topOficinas: OficinaReceita[];
}

export interface ReceitaPorPlano {
  planoNome: string;
  planoCodigo: string;
  quantidadeOficinas: number;
  receitaTotal: number;
  percentualReceita: number;
}

export interface EvolucaoMensalFinanceiro {
  mesAno: string;
  receita: number;
  mrr: number;
  novasOficinas: number;
  cancelamentos: number;
}

export interface OficinaReceita {
  oficinaId: string;
  nomeFantasia: string;
  cnpj: string;
  plano: string;
  receitaTotal: number;
  mesesAtivo: number;
}

// Relatório Operacional
export interface RelatorioOperacional {
  dataInicio: string;
  dataFim: string;
  totalOficinas: number;
  oficinasAtivas: number;
  oficinasEmTrial: number;
  oficinasSuspensas: number;
  oficinasCanceladas: number;
  totalUsuarios: number;
  usuariosAtivos: number;
  loginsPeriodo: number;
  mediaLoginsPorOficina: number;
  totalOrdensServico: number;
  ordensServicoPeriodo: number;
  totalClientes: number;
  clientesPeriodo: number;
  totalVeiculos: number;
  veiculosPeriodo: number;
  mediaOSPorOficina: number;
  mediaClientesPorOficina: number;
  mediaUsuariosPorOficina: number;
  distribuicaoPlanos: DistribuicaoPlano[];
  distribuicaoStatus: DistribuicaoStatus[];
  oficinaMaisAtivas: OficinaAtividade[];
  oficinaMenosAtivas: OficinaAtividade[];
  evolucaoMensal: EvolucaoOperacional[];
}

export interface DistribuicaoPlano {
  planoNome: string;
  planoCodigo: string;
  quantidade: number;
  percentual: number;
}

export interface DistribuicaoStatus {
  status: string;
  quantidade: number;
  percentual: number;
}

export interface OficinaAtividade {
  oficinaId: string;
  nomeFantasia: string;
  plano: string;
  ordensServico: number;
  clientes: number;
  usuarios: number;
  loginsMes: number;
}

export interface EvolucaoOperacional {
  mesAno: string;
  oficinasAtivas: number;
  ordensServico: number;
  clientes: number;
  usuarios: number;
}

// Relatório de Crescimento
export interface RelatorioCrescimento {
  dataInicio: string;
  dataFim: string;
  novasOficinas: number;
  cancelamentos: number;
  crescimentoLiquido: number;
  taxaCrescimento: number;
  churnRate: number;
  churnMRR: number;
  oficinasChurned: number;
  cac: number;
  ltv: number;
  ltvCacRatio: number;
  diasMediaConversao: number;
  trialsIniciados: number;
  trialsConvertidos: number;
  taxaConversaoTrial: number;
  mediaDiasTrial: number;
  taxaRetencao30d: number;
  taxaRetencao90d: number;
  taxaRetencao12m: number;
  mrrInicio: number;
  mrrFim: number;
  mrrNovo: number;
  mrrExpansao: number;
  mrrContracao: number;
  mrrChurn: number;
  mrrReativacao: number;
  evolucaoMensal: EvolucaoCrescimento[];
  cohortAnalysis: CohortData[];
  motivosCancelamento: MotivoCancelamento[];
  fontesAquisicao: FonteAquisicao[];
}

export interface EvolucaoCrescimento {
  mesAno: string;
  novasOficinas: number;
  cancelamentos: number;
  crescimentoLiquido: number;
  mrr: number;
  churnRate: number;
}

export interface CohortData {
  cohortMes: string;
  oficinasTotais: number;
  oficinasAtivas: number;
  taxaRetencao: number;
}

export interface MotivoCancelamento {
  motivo: string;
  quantidade: number;
  percentual: number;
}

export interface FonteAquisicao {
  fonte: string;
  quantidade: number;
  percentual: number;
  receitaGerada: number;
}

// ===== TICKETS =====

export const TipoTicket = {
  TECNICO: 'TECNICO',
  FINANCEIRO: 'FINANCEIRO',
  COMERCIAL: 'COMERCIAL',
  SUGESTAO: 'SUGESTAO',
  OUTRO: 'OUTRO',
} as const;

export type TipoTicket = (typeof TipoTicket)[keyof typeof TipoTicket];

export const tipoTicketLabels: Record<TipoTicket, string> = {
  TECNICO: 'Técnico',
  FINANCEIRO: 'Financeiro',
  COMERCIAL: 'Comercial',
  SUGESTAO: 'Sugestão',
  OUTRO: 'Outro',
};

export const PrioridadeTicket = {
  BAIXA: 'BAIXA',
  MEDIA: 'MEDIA',
  ALTA: 'ALTA',
  URGENTE: 'URGENTE',
} as const;

export type PrioridadeTicket = (typeof PrioridadeTicket)[keyof typeof PrioridadeTicket];

export const prioridadeTicketLabels: Record<PrioridadeTicket, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
  URGENTE: 'Urgente',
};

export const prioridadeTicketCores: Record<PrioridadeTicket, string> = {
  BAIXA: 'gray',
  MEDIA: 'blue',
  ALTA: 'orange',
  URGENTE: 'red',
};

export const StatusTicket = {
  ABERTO: 'ABERTO',
  EM_ANDAMENTO: 'EM_ANDAMENTO',
  AGUARDANDO_CLIENTE: 'AGUARDANDO_CLIENTE',
  AGUARDANDO_INTERNO: 'AGUARDANDO_INTERNO',
  RESOLVIDO: 'RESOLVIDO',
  FECHADO: 'FECHADO',
} as const;

export type StatusTicket = (typeof StatusTicket)[keyof typeof StatusTicket];

export const statusTicketLabels: Record<StatusTicket, string> = {
  ABERTO: 'Aberto',
  EM_ANDAMENTO: 'Em Andamento',
  AGUARDANDO_CLIENTE: 'Aguardando Cliente',
  AGUARDANDO_INTERNO: 'Aguardando Interno',
  RESOLVIDO: 'Resolvido',
  FECHADO: 'Fechado',
};

export const statusTicketCores: Record<StatusTicket, string> = {
  ABERTO: 'blue',
  EM_ANDAMENTO: 'yellow',
  AGUARDANDO_CLIENTE: 'purple',
  AGUARDANDO_INTERNO: 'orange',
  RESOLVIDO: 'green',
  FECHADO: 'gray',
};

export const TipoAutorMensagem = {
  CLIENTE: 'CLIENTE',
  SUPORTE: 'SUPORTE',
  SISTEMA: 'SISTEMA',
} as const;

export type TipoAutorMensagem = (typeof TipoAutorMensagem)[keyof typeof TipoAutorMensagem];

export interface MensagemTicket {
  id: string;
  ticketId: string;
  autorId?: string;
  autorNome: string;
  autorTipo: TipoAutorMensagem;
  autorTipoDescricao: string;
  isInterno: boolean;
  conteudo: string;
  anexos: string[];
  criadoEm: string;
}

export interface Ticket {
  id: string;
  numero: string;
  oficinaId?: string;
  oficinaNome?: string;
  usuarioId?: string;
  usuarioNome: string;
  usuarioEmail: string;
  tipo: TipoTicket;
  tipoDescricao: string;
  prioridade: PrioridadeTicket;
  prioridadeDescricao: string;
  status: StatusTicket;
  statusDescricao: string;
  assunto: string;
  descricao?: string;
  anexos?: string[];
  atribuidoAId?: string;
  atribuidoANome?: string;
  slaMinutos?: number;
  respostaInicialEm?: string;
  tempoRespostaMinutos?: number;
  slaCumprido: boolean;
  slaVencido: boolean;
  minutosRestantesSla?: number;
  aberturaEm: string;
  atualizadoEm: string;
  resolvidoEm?: string;
  fechadoEm?: string;
  totalMensagens: number;
  ultimaMensagem?: MensagemTicket;
}

export interface TicketDetail extends Ticket {
  oficinaCnpj?: string;
  oficinaEmail?: string;
  atribuidoAEmail?: string;
  mensagens: MensagemTicket[];
}

export interface CreateTicketRequest {
  oficinaId?: string;
  usuarioId?: string;
  usuarioNome: string;
  usuarioEmail: string;
  tipo: TipoTicket;
  prioridade: PrioridadeTicket;
  assunto: string;
  descricao: string;
  anexos?: string[];
  atribuidoA?: string;
}

export interface ResponderTicketRequest {
  conteudo: string;
  isInterno: boolean;
  anexos?: string[];
  autorId?: string;
  autorNome?: string;
}

export interface AtribuirTicketRequest {
  atribuidoA?: string;
}

export interface AlterarStatusTicketRequest {
  status: StatusTicket;
}

export interface AlterarPrioridadeTicketRequest {
  prioridade: PrioridadeTicket;
}

export interface TicketFilters {
  oficinaId?: string;
  status?: StatusTicket;
  tipo?: TipoTicket;
  prioridade?: PrioridadeTicket;
  atribuidoA?: string;
  busca?: string;
  page?: number;
  size?: number;
}

export interface TicketMetricas {
  totalTickets: number;
  ticketsAbertos: number;
  ticketsEmAndamento: number;
  ticketsAguardando: number;
  ticketsResolvidos: number;
  ticketsFechados: number;
  urgentes: number;
  alta: number;
  media: number;
  baixa: number;
  comSlaVencido: number;
  dentroSla: number;
  percentualDentroSla: number;
  tempoMedioRespostaMinutos?: number;
  tempoMedioResolucaoMinutos?: number;
  novosUltimos30d: number;
  resolvidosUltimos30d: number;
  naoAtribuidos: number;
}

export interface TicketEnums {
  tipos: TipoTicket[];
  prioridades: PrioridadeTicket[];
  status: StatusTicket[];
}

// ===== SUPER ADMINS =====

export interface SuperAdmin {
  id: string;
  nome: string;
  email: string;
}

// ===== COMUNICADOS =====

export const TipoComunicado = {
  NOVIDADE: 'NOVIDADE',
  MANUTENCAO: 'MANUTENCAO',
  FINANCEIRO: 'FINANCEIRO',
  ATUALIZACAO: 'ATUALIZACAO',
  ALERTA: 'ALERTA',
  PROMOCAO: 'PROMOCAO',
  OUTRO: 'OUTRO',
} as const;

export type TipoComunicado = (typeof TipoComunicado)[keyof typeof TipoComunicado];

export const tipoComunicadoLabels: Record<TipoComunicado, string> = {
  NOVIDADE: 'Novidade',
  MANUTENCAO: 'Manutenção',
  FINANCEIRO: 'Financeiro',
  ATUALIZACAO: 'Atualização',
  ALERTA: 'Alerta',
  PROMOCAO: 'Promoção',
  OUTRO: 'Outro',
};

export const StatusComunicado = {
  RASCUNHO: 'RASCUNHO',
  AGENDADO: 'AGENDADO',
  ENVIADO: 'ENVIADO',
  CANCELADO: 'CANCELADO',
} as const;

export type StatusComunicado = (typeof StatusComunicado)[keyof typeof StatusComunicado];

export const statusComunicadoLabels: Record<StatusComunicado, string> = {
  RASCUNHO: 'Rascunho',
  AGENDADO: 'Agendado',
  ENVIADO: 'Enviado',
  CANCELADO: 'Cancelado',
};

export const statusComunicadoCores: Record<StatusComunicado, string> = {
  RASCUNHO: 'gray',
  AGENDADO: 'blue',
  ENVIADO: 'green',
  CANCELADO: 'red',
};

export const PrioridadeComunicado = {
  BAIXA: 'BAIXA',
  NORMAL: 'NORMAL',
  ALTA: 'ALTA',
  URGENTE: 'URGENTE',
} as const;

export type PrioridadeComunicado = (typeof PrioridadeComunicado)[keyof typeof PrioridadeComunicado];

export const prioridadeComunicadoLabels: Record<PrioridadeComunicado, string> = {
  BAIXA: 'Baixa',
  NORMAL: 'Normal',
  ALTA: 'Alta',
  URGENTE: 'Urgente',
};

export const prioridadeComunicadoCores: Record<PrioridadeComunicado, string> = {
  BAIXA: 'gray',
  NORMAL: 'blue',
  ALTA: 'orange',
  URGENTE: 'red',
};

export interface Comunicado {
  id: string;
  titulo: string;
  resumo?: string;
  tipo: TipoComunicado;
  tipoDescricao: string;
  prioridade: PrioridadeComunicado;
  prioridadeDescricao: string;
  status: StatusComunicado;
  statusDescricao: string;
  autorId: string;
  autorNome: string;
  dataAgendamento?: string;
  dataEnvio?: string;
  totalDestinatarios: number;
  totalVisualizacoes: number;
  totalConfirmacoes: number;
  taxaVisualizacao: number;
  taxaConfirmacao: number;
  requerConfirmacao: boolean;
  exibirNoLogin: boolean;
  createdAt: string;
}

export interface LeituraResumo {
  oficinaId: string;
  oficinaNome: string;
  visualizado: boolean;
  dataVisualizacao?: string;
  confirmado: boolean;
  dataConfirmacao?: string;
}

export interface ComunicadoDetail extends Comunicado {
  conteudo: string;
  planosAlvo?: string[];
  oficinasAlvo?: string[];
  statusOficinasAlvo?: string[];
  podeEditar: boolean;
  podeEnviar: boolean;
  podeCancelar: boolean;
  updatedAt: string;
  leiturasRecentes: LeituraResumo[];
}

export interface CreateComunicadoRequest {
  titulo: string;
  resumo?: string;
  conteudo: string;
  tipo: TipoComunicado;
  prioridade: PrioridadeComunicado;
  planosAlvo?: string[];
  oficinasAlvo?: string[];
  statusOficinasAlvo?: string[];
  requerConfirmacao?: boolean;
  exibirNoLogin?: boolean;
  dataAgendamento?: string;
  enviarAgora?: boolean;
}

export interface UpdateComunicadoRequest {
  titulo: string;
  resumo?: string;
  conteudo: string;
  tipo?: TipoComunicado;
  prioridade?: PrioridadeComunicado;
  planosAlvo?: string[];
  oficinasAlvo?: string[];
  statusOficinasAlvo?: string[];
  requerConfirmacao?: boolean;
  exibirNoLogin?: boolean;
  dataAgendamento?: string;
}

export interface ComunicadoFilters {
  status?: StatusComunicado;
  tipo?: TipoComunicado;
  prioridade?: PrioridadeComunicado;
  busca?: string;
  page?: number;
  size?: number;
}

export interface ComunicadoMetricas {
  totalRascunhos: number;
  totalAgendados: number;
  totalEnviados: number;
  totalCancelados: number;
  enviadosNoMes: number;
  destinatariosNoMes: number;
  visualizacoesNoMes: number;
  taxaVisualizacaoMedia: number;
}

export interface ComunicadoEnums {
  tipos: TipoComunicado[];
  prioridades: PrioridadeComunicado[];
  status: StatusComunicado[];
}

// ===== FEATURE FLAGS =====

export const CategoriaFeatureFlag = {
  GERAL: 'GERAL',
  COMUNICACAO: 'COMUNICACAO',
  RELATORIOS: 'RELATORIOS',
  PREMIUM: 'PREMIUM',
  FINANCEIRO: 'FINANCEIRO',
  INTEGRACAO: 'INTEGRACAO',
  OPERACIONAL: 'OPERACIONAL',
  FISCAL: 'FISCAL',
  MOBILE: 'MOBILE',
  BRANDING: 'BRANDING',
  MARKETING: 'MARKETING',
  SEGURANCA: 'SEGURANCA',
} as const;

export type CategoriaFeatureFlag = (typeof CategoriaFeatureFlag)[keyof typeof CategoriaFeatureFlag];

export const categoriaFeatureFlagLabels: Record<string, string> = {
  GERAL: 'Geral',
  COMUNICACAO: 'Comunicação',
  RELATORIOS: 'Relatórios',
  PREMIUM: 'Premium',
  FINANCEIRO: 'Financeiro',
  INTEGRACAO: 'Integração',
  OPERACIONAL: 'Operacional',
  FISCAL: 'Fiscal',
  MOBILE: 'Mobile',
  BRANDING: 'Branding',
  MARKETING: 'Marketing',
  SEGURANCA: 'Segurança',
};

export interface FeatureFlag {
  id: string;
  codigo: string;
  nome: string;
  descricao?: string;
  habilitadoGlobal: boolean;
  habilitadoPorPlano: Record<string, boolean>;
  habilitadoPorOficina: string[];
  percentualRollout: number;
  dataInicio?: string;
  dataFim?: string;
  categoria: string;
  requerAutorizacao: boolean;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFeatureFlagRequest {
  codigo: string;
  nome: string;
  descricao?: string;
  habilitadoGlobal?: boolean;
  habilitadoPorPlano?: Record<string, boolean>;
  habilitadoPorOficina?: string[];
  percentualRollout?: number;
  dataInicio?: string;
  dataFim?: string;
  categoria?: string;
  requerAutorizacao?: boolean;
}

export interface UpdateFeatureFlagRequest {
  nome?: string;
  descricao?: string;
  habilitadoGlobal?: boolean;
  habilitadoPorPlano?: Record<string, boolean>;
  habilitadoPorOficina?: string[];
  percentualRollout?: number;
  dataInicio?: string;
  dataFim?: string;
  categoria?: string;
  requerAutorizacao?: boolean;
}

export interface ToggleFeatureFlagRequest {
  habilitadoGlobal?: boolean;
  planosHabilitar?: string[];
  planosDesabilitar?: string[];
  oficinasHabilitar?: string[];
  oficinasDesabilitar?: string[];
  percentualRollout?: number;
}

export interface OficinaFeatureFlags {
  oficinaId: string;
  features: Record<string, boolean>;
}

export interface FeatureFlagStats {
  codigo: string;
  nome: string;
  habilitadoGlobal: boolean;
  totalOficinasHabilitadas: number;
  planosHabilitados: string[];
  percentualRollout: number;
  ativo: boolean;
  totalOficinasPorPlano?: number;
}
