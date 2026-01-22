/**
 * Tipos para o módulo de Despesas
 */

// Categorias de despesa (match backend enum)
export type CategoriaDespesa =
  // Pessoal
  | 'SALARIOS'
  | 'ENCARGOS_SOCIAIS'
  | 'BENEFICIOS'
  | 'PROLABORE'
  // Instalações
  | 'ALUGUEL'
  | 'CONDOMINIO'
  | 'IPTU'
  | 'MANUTENCAO_PREDIAL'
  // Utilidades
  | 'ENERGIA_ELETRICA'
  | 'AGUA'
  | 'GAS'
  | 'TELEFONE'
  | 'INTERNET'
  // Operacional
  | 'COMPRA_PECAS'
  | 'FERRAMENTAS'
  | 'MATERIAL_CONSUMO'
  | 'MATERIAL_LIMPEZA'
  | 'DESCARTE_RESIDUOS'
  // Administrativo
  | 'CONTABILIDADE'
  | 'ADVOCACIA'
  | 'SISTEMAS_SOFTWARE'
  | 'MATERIAL_ESCRITORIO'
  | 'TAXAS_BANCARIAS'
  // Marketing
  | 'PUBLICIDADE'
  | 'MARKETING_DIGITAL'
  | 'BRINDES'
  // Impostos
  | 'IMPOSTOS_FEDERAIS'
  | 'IMPOSTOS_ESTADUAIS'
  | 'IMPOSTOS_MUNICIPAIS'
  | 'TAXAS_LICENCAS'
  // Veículos
  | 'COMBUSTIVEL'
  | 'MANUTENCAO_VEICULOS'
  | 'SEGURO_VEICULOS'
  // Seguros
  | 'SEGURO_ESTABELECIMENTO'
  | 'SEGURO_RESPONSABILIDADE'
  // Financeiro
  | 'JUROS_EMPRESTIMOS'
  | 'TARIFAS_CARTAO'
  // Outros
  | 'OUTRAS_DESPESAS';

export type StatusDespesa = 'PENDENTE' | 'PAGA' | 'VENCIDA' | 'CANCELADA';

export type TipoPagamento = 'DINHEIRO' | 'CARTAO_CREDITO' | 'CARTAO_DEBITO' | 'PIX' | 'TRANSFERENCIA' | 'BOLETO';

// Informação de categoria
export interface CategoriaInfo {
  codigo: CategoriaDespesa;
  descricao: string;
  grupo: string;
  cor: string;
}

// Request para criar despesa
export interface DespesaCreateRequest {
  categoria: CategoriaDespesa;
  descricao: string;
  valor: number;
  dataVencimento: string; // ISO date
  numeroDocumento?: string;
  fornecedor?: string;
  observacoes?: string;
  recorrente?: boolean;
}

// Request para atualizar despesa
export interface DespesaUpdateRequest {
  categoria?: CategoriaDespesa;
  descricao?: string;
  valor?: number;
  dataVencimento?: string;
  numeroDocumento?: string;
  fornecedor?: string;
  observacoes?: string;
  recorrente?: boolean;
}

// Request para registrar pagamento
export interface DespesaPagamentoRequest {
  dataPagamento: string; // ISO date
  tipoPagamento: TipoPagamento;
}

// Resposta completa da despesa
export interface DespesaResponse {
  id: string;
  categoria: CategoriaDespesa;
  categoriaDescricao: string;
  categoriaGrupo: string;
  categoriaCor: string;
  descricao: string;
  valor: number;
  dataVencimento: string;
  dataPagamento?: string;
  status: StatusDespesa;
  statusDescricao: string;
  statusCor: string;
  numeroDocumento?: string;
  fornecedor?: string;
  observacoes?: string;
  recorrente: boolean;
  tipoPagamento?: TipoPagamento;
  movimentacaoEstoqueId?: string;
  createdAt: string;
  updatedAt: string;
  vencida: boolean;
  diasAtraso?: number;
}

// Item de lista resumido
export interface DespesaListItem {
  id: string;
  categoria: CategoriaDespesa;
  categoriaDescricao: string;
  categoriaCor: string;
  descricao: string;
  valor: number;
  dataVencimento: string;
  dataPagamento?: string;
  status: StatusDespesa;
  statusDescricao: string;
  statusCor: string;
  fornecedor?: string;
  recorrente: boolean;
  vencida: boolean;
}

// Resumo para dashboard
export interface DespesaResumo {
  totalPendente: number;
  totalVencido: number;
  totalPagoMes: number;
  quantidadePendente: number;
  quantidadeVencida: number;
  quantidadeAVencer7Dias: number;
}

// Filtros para listagem
export interface DespesaFiltros {
  status?: StatusDespesa;
  categoria?: CategoriaDespesa;
  dataInicio?: string;
  dataFim?: string;
  page?: number;
  size?: number;
}

// Resposta paginada
export interface DespesaPageResponse {
  content: DespesaListItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Mapeamento de cores por status
export const STATUS_COLORS: Record<StatusDespesa, string> = {
  PENDENTE: '#F59E0B',
  PAGA: '#22C55E',
  VENCIDA: '#EF4444',
  CANCELADA: '#6B7280',
};

// Mapeamento de labels por status
export const STATUS_LABELS: Record<StatusDespesa, string> = {
  PENDENTE: 'Pendente',
  PAGA: 'Paga',
  VENCIDA: 'Vencida',
  CANCELADA: 'Cancelada',
};

// Agrupamento de categorias para select
export const CATEGORIAS_AGRUPADAS = {
  PESSOAL: [
    { value: 'SALARIOS', label: 'Salários e Ordenados' },
    { value: 'ENCARGOS_SOCIAIS', label: 'Encargos Sociais (INSS, FGTS)' },
    { value: 'BENEFICIOS', label: 'Benefícios (VT, VR, Plano Saúde)' },
    { value: 'PROLABORE', label: 'Pró-labore' },
  ],
  INSTALACOES: [
    { value: 'ALUGUEL', label: 'Aluguel' },
    { value: 'CONDOMINIO', label: 'Condomínio' },
    { value: 'IPTU', label: 'IPTU' },
    { value: 'MANUTENCAO_PREDIAL', label: 'Manutenção Predial' },
  ],
  UTILIDADES: [
    { value: 'ENERGIA_ELETRICA', label: 'Energia Elétrica' },
    { value: 'AGUA', label: 'Água e Esgoto' },
    { value: 'GAS', label: 'Gás' },
    { value: 'TELEFONE', label: 'Telefone' },
    { value: 'INTERNET', label: 'Internet' },
  ],
  OPERACIONAL: [
    { value: 'COMPRA_PECAS', label: 'Compra de Peças (Estoque)' },
    { value: 'FERRAMENTAS', label: 'Ferramentas e Equipamentos' },
    { value: 'MATERIAL_CONSUMO', label: 'Material de Consumo' },
    { value: 'MATERIAL_LIMPEZA', label: 'Material de Limpeza' },
    { value: 'DESCARTE_RESIDUOS', label: 'Descarte de Resíduos' },
  ],
  ADMINISTRATIVO: [
    { value: 'CONTABILIDADE', label: 'Contabilidade' },
    { value: 'ADVOCACIA', label: 'Advocacia' },
    { value: 'SISTEMAS_SOFTWARE', label: 'Sistemas e Software' },
    { value: 'MATERIAL_ESCRITORIO', label: 'Material de Escritório' },
    { value: 'TAXAS_BANCARIAS', label: 'Taxas Bancárias' },
  ],
  MARKETING: [
    { value: 'PUBLICIDADE', label: 'Publicidade e Propaganda' },
    { value: 'MARKETING_DIGITAL', label: 'Marketing Digital' },
    { value: 'BRINDES', label: 'Brindes e Cortesias' },
  ],
  IMPOSTOS: [
    { value: 'IMPOSTOS_FEDERAIS', label: 'Impostos Federais' },
    { value: 'IMPOSTOS_ESTADUAIS', label: 'Impostos Estaduais' },
    { value: 'IMPOSTOS_MUNICIPAIS', label: 'Impostos Municipais' },
    { value: 'TAXAS_LICENCAS', label: 'Taxas e Licenças' },
  ],
  VEICULOS: [
    { value: 'COMBUSTIVEL', label: 'Combustível' },
    { value: 'MANUTENCAO_VEICULOS', label: 'Manutenção de Veículos' },
    { value: 'SEGURO_VEICULOS', label: 'Seguro de Veículos' },
  ],
  SEGUROS: [
    { value: 'SEGURO_ESTABELECIMENTO', label: 'Seguro do Estabelecimento' },
    { value: 'SEGURO_RESPONSABILIDADE', label: 'Seguro Responsabilidade Civil' },
  ],
  FINANCEIRO: [
    { value: 'JUROS_EMPRESTIMOS', label: 'Juros de Empréstimos' },
    { value: 'TARIFAS_CARTAO', label: 'Tarifas de Cartão' },
  ],
  OUTROS: [
    { value: 'OUTRAS_DESPESAS', label: 'Outras Despesas' },
  ],
};

// Labels dos grupos
export const GRUPO_LABELS: Record<string, string> = {
  PESSOAL: 'Despesas com Pessoal',
  INSTALACOES: 'Instalações',
  UTILIDADES: 'Utilidades',
  OPERACIONAL: 'Operacional',
  ADMINISTRATIVO: 'Administrativo',
  MARKETING: 'Marketing e Vendas',
  IMPOSTOS: 'Impostos e Taxas',
  VEICULOS: 'Veículos da Oficina',
  SEGUROS: 'Seguros',
  FINANCEIRO: 'Financeiras',
  OUTROS: 'Outras',
};
