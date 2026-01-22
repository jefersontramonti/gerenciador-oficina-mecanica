/**
 * Tipos para Fluxo de Caixa, DRE e Projeções Financeiras.
 */

// ========== Fluxo de Caixa ==========

export interface MovimentoDiario {
  data: string;
  receitas: number;
  despesas: number;
  saldo: number;
  saldoAcumulado: number;
}

export interface MovimentoCategoria {
  categoria: string;
  cor: string;
  valor: number;
  percentual: number;
  quantidade: number;
}

export interface FluxoCaixa {
  dataInicio: string;
  dataFim: string;
  saldoInicial: number;
  totalReceitas: number;
  totalDespesas: number;
  saldoFinal: number;
  variacaoReceitas: number;
  variacaoDespesas: number;
  variacaoSaldo: number;
  movimentosDiarios: MovimentoDiario[];
  receitasPorCategoria: MovimentoCategoria[];
  despesasPorCategoria: MovimentoCategoria[];
}

// ========== DRE Simplificado ==========

export interface ComparativoDRE {
  receitaAnterior: number;
  lucroAnterior?: number;
  variacaoReceita: number;
  variacaoLucro?: number;
}

export interface LinhaDRE {
  descricao: string;
  grupo: string;
  valor: number;
  percentualReceita: number;
  ordem: number;
  destaque: boolean;
}

// Tipos de alertas do DRE
export type TipoAlertaDRE =
  | 'MARGEM_BRUTA_BAIXA'
  | 'MARGEM_OPERACIONAL_BAIXA'
  | 'MARGEM_LIQUIDA_NEGATIVA'
  | 'CMV_ALTO'
  | 'DESPESAS_OPERACIONAIS_ALTAS'
  | 'RESULTADO_OPERACIONAL_NEGATIVO'
  | 'LUCRO_LIQUIDO_NEGATIVO'
  | 'DESPESAS_PESSOAL_ALTAS'
  | 'CUSTO_MAO_OBRA_ALTO'
  | 'SEM_RECEITA'
  | 'DEDUCOES_ALTAS';

export type NivelAlertaDRE = 'INFO' | 'WARNING' | 'CRITICAL';

export interface AlertaDRE {
  tipo: TipoAlertaDRE;
  nivel: NivelAlertaDRE;
  mensagem: string;
  valor?: number;
  limite?: number;
  sugestao?: string;
}

export interface DRESimplificado {
  mes: number;
  ano: number;
  periodo: string;

  // Receitas
  receitaBrutaServicos: number;
  receitaBrutaPecas: number;
  outrasReceitas: number;
  receitaBrutaTotal: number;

  // Deduções
  descontosConcedidos: number;
  cancelamentos: number;
  deducoesTotal: number;

  // Receita Líquida
  receitaLiquida: number;

  // Custos
  custoPecasVendidas: number;
  custoMaoObra: number;
  custosTotal: number;

  // Lucro Bruto
  lucroBruto: number;
  margemBruta: number;

  // Despesas Operacionais
  despesasAdministrativas: number;
  despesasPessoal: number;
  despesasMarketing: number;
  outrasDespesas: number;
  despesasOperacionaisTotal: number;

  // Resultado Operacional
  resultadoOperacional: number;
  margemOperacional: number;

  // Resultado Financeiro
  receitasFinanceiras: number;
  despesasFinanceiras: number;
  resultadoFinanceiro: number;

  // Resultado Antes dos Impostos
  resultadoAntesImpostos: number;

  // Impostos
  impostos: number;

  // Lucro Líquido
  lucroLiquido: number;
  margemLiquida: number;

  // Comparativos
  comparativoMesAnterior?: ComparativoDRE;
  comparativoAnoAnterior?: ComparativoDRE;

  // Alertas inteligentes
  alertas?: AlertaDRE[];

  // Detalhamento
  linhasDetalhadas?: LinhaDRE[];
}

// ========== Projeção Financeira ==========

export type Probabilidade = 'ALTA' | 'MEDIA' | 'BAIXA';
export type NivelAlerta = 'INFO' | 'WARNING' | 'CRITICAL';
export type Tendencia = 'POSITIVA' | 'ESTAVEL' | 'NEGATIVA';

export interface ReceitaEsperada {
  origem: string;
  descricao: string;
  valor: number;
  dataEsperada: string;
  probabilidade: Probabilidade;
}

export interface DespesaPrevista {
  categoria: string;
  descricao: string;
  valor: number;
  dataVencimento: string;
  status: string;
}

export interface AlertaFluxo {
  tipo: string;
  nivel: NivelAlerta;
  mensagem: string;
  data?: string;
  valor?: number;
}

export interface ProjecaoDiaria {
  data: string;
  receitasPrevistas: number;
  despesasPrevistas: number;
  saldoDia: number;
  saldoAcumulado: number;
  alertaSaldoNegativo: boolean;
}

export interface IndicadoresProjecao {
  ticketMedioMes: number;
  mediaReceitaDiaria: number;
  mediaDespesaDiaria: number;
  diasAteSaldoNegativo?: number;
  necessidadeCapitalGiro?: number;
  tendencia: Tendencia;
}

export interface ProjecaoFinanceira {
  dataBase: string;
  diasProjecao: number;
  dataFimProjecao: string;

  // Saldos
  saldoAtual: number;
  saldoProjetado: number;
  variacaoProjetada: number;

  // Receitas Esperadas
  receitasEsperadas: number;
  detalhamentoReceitas: ReceitaEsperada[];

  // Despesas Previstas
  despesasPrevistas: number;
  detalhamentoDespesas: DespesaPrevista[];

  // Alertas
  alertas: AlertaFluxo[];

  // Projeção Diária
  projecaoDiaria: ProjecaoDiaria[];

  // Indicadores
  indicadores: IndicadoresProjecao;
}
