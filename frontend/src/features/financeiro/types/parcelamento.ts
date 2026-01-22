/**
 * Types para o módulo de Parcelamento
 */

export type TipoJuros = 'SEM_JUROS' | 'JUROS_SIMPLES' | 'JUROS_COMPOSTO';

export const TIPO_JUROS_LABELS: Record<TipoJuros, string> = {
  SEM_JUROS: 'Sem juros',
  JUROS_SIMPLES: 'Juros simples',
  JUROS_COMPOSTO: 'Juros composto',
};

export interface TabelaJuros {
  id: string;
  parcelasMinimo: number;
  parcelasMaximo: number;
  percentualJuros: number;
  tipoJuros: TipoJuros;
  tipoJurosDescricao: string;
  repassarCliente: boolean;
  ativo: boolean;
  descricaoFaixa: string;
  createdAt: string;
  updatedAt: string;
}

export interface TabelaJurosRequest {
  parcelasMinimo: number;
  parcelasMaximo: number;
  percentualJuros: number;
  tipoJuros: TipoJuros;
  repassarCliente: boolean;
  ativo: boolean;
}

export interface ConfiguracaoParcelamento {
  id: string;
  parcelasMaximas: number;
  valorMinimoParcela: number;
  valorMinimoParcelamento: number;
  aceitaVisa: boolean;
  aceitaMastercard: boolean;
  aceitaElo: boolean;
  aceitaAmex: boolean;
  aceitaHipercard: boolean;
  exibirValorTotal: boolean;
  exibirJuros: boolean;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
  faixasJuros: TabelaJuros[];
  bandeirasAceitas: string[];
}

export interface ConfiguracaoParcelamentoRequest {
  parcelasMaximas: number;
  valorMinimoParcela: number;
  valorMinimoParcelamento: number;
  aceitaVisa: boolean;
  aceitaMastercard: boolean;
  aceitaElo: boolean;
  aceitaAmex: boolean;
  aceitaHipercard: boolean;
  exibirValorTotal: boolean;
  exibirJuros: boolean;
  ativo: boolean;
}

export interface OpcaoParcelamento {
  parcelas: number;
  valorParcela: number;
  valorTotal: number;
  valorJuros: number;
  percentualJurosMensal: number;
  cetAnual: number;
  semJuros: boolean;
  textoExibicao: string;
  disponivel: boolean;
  mensagemIndisponivel: string | null;
}

export interface SimulacaoParcelamento {
  valorOriginal: number;
  parcelasMaximas: number;
  opcoes: OpcaoParcelamento[];
}
