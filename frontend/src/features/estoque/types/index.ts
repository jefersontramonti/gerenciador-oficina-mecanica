/**
 * Enums e tipos do módulo de Estoque/Peças
 */

// ==================== ENUMS ====================

export const UnidadeMedida = {
  UNIDADE: 'UNIDADE',
  LITRO: 'LITRO',
  METRO: 'METRO',
  QUILO: 'QUILO',
} as const;

export type UnidadeMedida = (typeof UnidadeMedida)[keyof typeof UnidadeMedida];

export const UnidadeMedidaLabel: Record<UnidadeMedida, string> = {
  [UnidadeMedida.UNIDADE]: 'Unidade (UN)',
  [UnidadeMedida.LITRO]: 'Litro (L)',
  [UnidadeMedida.METRO]: 'Metro (M)',
  [UnidadeMedida.QUILO]: 'Quilograma (KG)',
};

export const UnidadeMedidaSigla: Record<UnidadeMedida, string> = {
  [UnidadeMedida.UNIDADE]: 'UN',
  [UnidadeMedida.LITRO]: 'L',
  [UnidadeMedida.METRO]: 'M',
  [UnidadeMedida.QUILO]: 'KG',
};

export const TipoMovimentacao = {
  ENTRADA: 'ENTRADA',
  SAIDA: 'SAIDA',
  AJUSTE: 'AJUSTE',
  DEVOLUCAO: 'DEVOLUCAO',
  BAIXA_OS: 'BAIXA_OS',
} as const;

export type TipoMovimentacao = (typeof TipoMovimentacao)[keyof typeof TipoMovimentacao];

export const TipoMovimentacaoLabel: Record<TipoMovimentacao, string> = {
  [TipoMovimentacao.ENTRADA]: 'Entrada de estoque',
  [TipoMovimentacao.SAIDA]: 'Saída manual de estoque',
  [TipoMovimentacao.AJUSTE]: 'Ajuste de inventário',
  [TipoMovimentacao.DEVOLUCAO]: 'Devolução (estorno)',
  [TipoMovimentacao.BAIXA_OS]: 'Baixa automática por OS',
};

export const TipoMovimentacaoColor: Record<TipoMovimentacao, string> = {
  [TipoMovimentacao.ENTRADA]: 'green',
  [TipoMovimentacao.SAIDA]: 'red',
  [TipoMovimentacao.AJUSTE]: 'yellow',
  [TipoMovimentacao.DEVOLUCAO]: 'blue',
  [TipoMovimentacao.BAIXA_OS]: 'gray',
};

// ==================== INTERFACES - PEÇA ====================

export interface Peca {
  id: string;
  codigo: string;
  descricao: string;
  marca?: string;
  aplicacao?: string;
  localizacao?: string; // Deprecated - usar localArmazenamento
  localArmazenamentoId?: string;
  localArmazenamento?: LocalArmazenamentoResumo;
  unidadeMedida: UnidadeMedida;
  quantidadeAtual: number;
  quantidadeMinima: number;
  estoqueBaixo: boolean; // Calculado pelo backend
  valorCusto: number;
  valorVenda: number;
  margemLucro: number; // Calculado pelo backend
  valorTotalEstoque: number; // Calculado pelo backend
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PecaResumo {
  id: string;
  codigo: string;
  descricao: string;
  quantidadeAtual: number;
  unidadeMedida: UnidadeMedida;
}

// ==================== DTOs - PEÇA ====================

export interface CreatePecaRequest {
  codigo: string;
  descricao: string;
  marca?: string;
  aplicacao?: string;
  localizacao?: string;
  localArmazenamentoId?: string;
  unidadeMedida: UnidadeMedida;
  quantidadeMinima: number;
  valorCusto: number;
  valorVenda: number;
}

export interface UpdatePecaRequest {
  codigo?: string;
  descricao?: string;
  marca?: string;
  aplicacao?: string;
  localizacao?: string;
  localArmazenamentoId?: string;
  unidadeMedida?: UnidadeMedida;
  quantidadeMinima?: number;
  valorCusto?: number;
  valorVenda?: number;
}

export interface PecaFilters {
  codigo?: string;
  descricao?: string;
  marca?: string;
  unidadeMedida?: UnidadeMedida;
  ativo?: boolean;
  estoqueBaixo?: boolean;
  localArmazenamentoId?: string;
  page?: number;
  size?: number;
  sort?: string[];
}

// ==================== INTERFACES - MOVIMENTAÇÃO ====================

export interface MovimentacaoEstoque {
  id: string;
  peca: {
    id: string;
    codigo: string;
    descricao: string;
  };
  usuario: {
    id: string;
    nome: string;
    email: string;
  };
  numeroOS?: number;
  tipo: TipoMovimentacao;
  quantidade: number;
  quantidadeAnterior: number;
  quantidadeAtual: number;
  valorUnitario: number;
  valorTotal: number;
  motivo?: string;
  observacao?: string;
  dataMovimentacao: string;
  createdAt: string;
}

// ==================== DTOs - MOVIMENTAÇÃO ====================

export interface CreateEntradaRequest {
  pecaId: string;
  quantidade: number;
  valorUnitario: number;
  motivo?: string;
  observacao?: string;
}

export interface CreateSaidaRequest {
  pecaId: string;
  quantidade: number;
  valorUnitario: number;
  motivo?: string;
  observacao?: string;
}

export interface CreateAjusteRequest {
  pecaId: string;
  quantidadeNova: number;
  valorUnitario: number;
  motivo: string;
  observacao?: string;
}

export interface MovimentacaoFilters {
  pecaId?: string;
  tipo?: TipoMovimentacao;
  dataInicio?: string;
  dataFim?: string;
  usuarioId?: string;
  page?: number;
  size?: number;
  sort?: string[];
}

// ==================== RESPONSES PAGINADOS ====================

export interface PagedResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

// ==================== HELPER TYPES ====================

export type StockStatus = 'OK' | 'ATENCAO' | 'BAIXO' | 'ZERADO';

export interface StockStatusInfo {
  status: StockStatus;
  label: string;
  color: string;
  bgColor: string;
  textColor: string;
}

// ==================== UTILS ====================

export const getStockStatus = (
  quantidadeAtual: number,
  quantidadeMinima: number
): StockStatusInfo => {
  if (quantidadeAtual === 0) {
    return {
      status: 'ZERADO',
      label: 'ZERADO',
      color: 'red',
      bgColor: 'bg-red-100 dark:bg-red-950/40',
      textColor: 'text-red-800 dark:text-red-300',
    };
  }

  if (quantidadeAtual <= quantidadeMinima) {
    return {
      status: 'BAIXO',
      label: 'ESTOQUE BAIXO',
      color: 'orange',
      bgColor: 'bg-orange-100 dark:bg-orange-950/40',
      textColor: 'text-orange-800 dark:text-orange-300',
    };
  }

  if (quantidadeAtual < quantidadeMinima * 2) {
    return {
      status: 'ATENCAO',
      label: 'ATENÇÃO',
      color: 'yellow',
      bgColor: 'bg-yellow-100 dark:bg-yellow-950/40',
      textColor: 'text-yellow-800 dark:text-yellow-300',
    };
  }

  return {
    status: 'OK',
    label: 'OK',
    color: 'green',
    bgColor: 'bg-green-100 dark:bg-green-950/40',
    textColor: 'text-green-800 dark:text-green-300',
  };
};

export const getMargemLucroStatus = (margemLucro: number) => {
  if (margemLucro >= 30) {
    return {
      label: 'Ótima',
      color: 'green',
      bgColor: 'bg-green-100 dark:bg-green-950/40',
      textColor: 'text-green-800 dark:text-green-300',
    };
  }

  if (margemLucro >= 10) {
    return {
      label: 'Boa',
      color: 'yellow',
      bgColor: 'bg-yellow-100 dark:bg-yellow-950/40',
      textColor: 'text-yellow-800 dark:text-yellow-300',
    };
  }

  return {
    label: 'Baixa',
    color: 'red',
    bgColor: 'bg-red-100 dark:bg-red-950/40',
    textColor: 'text-red-800 dark:text-red-300',
  };
};

export const getMovimentacaoSinal = (tipo: TipoMovimentacao): string => {
  switch (tipo) {
    case TipoMovimentacao.ENTRADA:
    case TipoMovimentacao.DEVOLUCAO:
      return '+';
    case TipoMovimentacao.SAIDA:
    case TipoMovimentacao.BAIXA_OS:
      return '-';
    case TipoMovimentacao.AJUSTE:
      return '±';
    default:
      return '';
  }
};

// ==================== ENUMS - LOCAL ARMAZENAMENTO ====================

export const TipoLocal = {
  DEPOSITO: 'DEPOSITO',
  PRATELEIRA: 'PRATELEIRA',
  GAVETA: 'GAVETA',
  VITRINE: 'VITRINE',
  AREA: 'AREA',
} as const;

export type TipoLocal = (typeof TipoLocal)[keyof typeof TipoLocal];

export const TipoLocalLabel: Record<TipoLocal, string> = {
  [TipoLocal.DEPOSITO]: 'Depósito',
  [TipoLocal.PRATELEIRA]: 'Prateleira',
  [TipoLocal.GAVETA]: 'Gaveta',
  [TipoLocal.VITRINE]: 'Vitrine',
  [TipoLocal.AREA]: 'Área',
};

export const TipoLocalIcon: Record<TipoLocal, string> = {
  [TipoLocal.DEPOSITO]: '🏢',
  [TipoLocal.PRATELEIRA]: '📦',
  [TipoLocal.GAVETA]: '🗄️',
  [TipoLocal.VITRINE]: '🪟',
  [TipoLocal.AREA]: '📍',
};

// ==================== INTERFACES - LOCAL ARMAZENAMENTO ====================

export interface LocalArmazenamento {
  id: string;
  codigo: string;
  descricao: string;
  tipo: TipoLocal;
  localizacaoPai?: LocalArmazenamentoResumo; // Renomeado de localPai para corresponder ao backend
  capacidadeMaxima?: number;
  observacoes?: string;

  // Campos computados enviados pelo backend
  caminhoCompleto: string;
  nivel: number;
  isRaiz: boolean;
  temFilhos: boolean;

  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LocalArmazenamentoResumo {
  id: string;
  codigo: string;
  descricao: string;
  tipo: TipoLocal;
  caminhoCompleto: string; // Adicionado - backend envia este campo
}

// ==================== DTOs - LOCAL ARMAZENAMENTO ====================

export interface CreateLocalArmazenamentoRequest {
  codigo: string;
  descricao: string;
  tipo: TipoLocal;
  localizacaoPaiId?: string; // Corrigido para corresponder ao backend
  capacidadeMaxima?: number; // Corrigido para corresponder ao backend
  observacoes?: string;
}

export interface UpdateLocalArmazenamentoRequest {
  codigo?: string;
  descricao?: string;
  tipo?: TipoLocal;
  localizacaoPaiId?: string; // Corrigido para corresponder ao backend
  capacidadeMaxima?: number; // Corrigido para corresponder ao backend
  observacoes?: string;
}

export interface LocalArmazenamentoFilters {
  tipo?: TipoLocal;
  descricao?: string;
  ativo?: boolean;
  page?: number;
  size?: number;
  sort?: string[];
}
