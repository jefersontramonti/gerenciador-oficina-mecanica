import { useState } from 'react';
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  ArrowUpRight,
  ArrowDownRight,
  ChevronLeft,
  ChevronRight,
  FileText,
  RefreshCw,
  AlertTriangle,
  AlertCircle,
  Info,
  ChevronDown,
  ChevronUp,
  Lightbulb
} from 'lucide-react';
import { useDRE } from '../hooks/useFluxoCaixa';
import type { NivelAlertaDRE } from '../types/fluxoCaixa';

export default function DREPage() {
  const hoje = new Date();
  const [mes, setMes] = useState(hoje.getMonth() + 1);
  const [ano, setAno] = useState(hoje.getFullYear());
  const [alertasExpandidos, setAlertasExpandidos] = useState(true);

  const { data: dre, isLoading, isFetching, error, refetch } = useDRE(mes, ano);

  // Função para obter ícone e cores baseado no nível do alerta
  const getAlertaConfig = (nivel: NivelAlertaDRE) => {
    switch (nivel) {
      case 'CRITICAL':
        return {
          icon: AlertCircle,
          bgColor: 'bg-red-50 dark:bg-red-900/30',
          borderColor: 'border-red-200 dark:border-red-800',
          textColor: 'text-red-800 dark:text-red-200',
          iconColor: 'text-red-600 dark:text-red-400',
          badgeColor: 'bg-red-100 dark:bg-red-800 text-red-800 dark:text-red-200',
          label: 'Crítico'
        };
      case 'WARNING':
        return {
          icon: AlertTriangle,
          bgColor: 'bg-yellow-50 dark:bg-yellow-900/30',
          borderColor: 'border-yellow-200 dark:border-yellow-800',
          textColor: 'text-yellow-800 dark:text-yellow-200',
          iconColor: 'text-yellow-600 dark:text-yellow-400',
          badgeColor: 'bg-yellow-100 dark:bg-yellow-800 text-yellow-800 dark:text-yellow-200',
          label: 'Atenção'
        };
      case 'INFO':
      default:
        return {
          icon: Info,
          bgColor: 'bg-blue-50 dark:bg-blue-900/30',
          borderColor: 'border-blue-200 dark:border-blue-800',
          textColor: 'text-blue-800 dark:text-blue-200',
          iconColor: 'text-blue-600 dark:text-blue-400',
          badgeColor: 'bg-blue-100 dark:bg-blue-800 text-blue-800 dark:text-blue-200',
          label: 'Info'
        };
    }
  };

  // Conta alertas por nível
  const contarAlertas = () => {
    if (!dre?.alertas) return { critical: 0, warning: 0, info: 0, total: 0 };
    const critical = dre.alertas.filter(a => a.nivel === 'CRITICAL').length;
    const warning = dre.alertas.filter(a => a.nivel === 'WARNING').length;
    const info = dre.alertas.filter(a => a.nivel === 'INFO').length;
    return { critical, warning, info, total: critical + warning + info };
  };

  const alertasCount = contarAlertas();

  const formatCurrency = (value: number | undefined) => {
    if (value === undefined || value === null) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(value);
  };

  const formatPercent = (value: number | undefined) => {
    if (value === undefined || value === null) return '0,0%';
    return `${value.toFixed(1)}%`;
  };

  const navegarMes = (direcao: 'anterior' | 'proximo') => {
    if (direcao === 'anterior') {
      if (mes === 1) {
        setMes(12);
        setAno(ano - 1);
      } else {
        setMes(mes - 1);
      }
    } else {
      if (mes === 12) {
        setMes(1);
        setAno(ano + 1);
      } else {
        setMes(mes + 1);
      }
    }
  };

  const meses = [
    'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
  ];

  if (error) {
    return (
      <div className="p-6">
        <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg p-4">
          <p className="text-red-800 dark:text-red-200">
            Erro ao carregar DRE. Por favor, tente novamente.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
            <FileText className="h-7 w-7" />
            DRE Simplificado
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Demonstração do Resultado do Exercício
          </p>
        </div>

        <div className="flex items-center gap-3">
          {/* Navegação de Mês */}
          <div className="flex items-center gap-2 bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-1">
            <button
              onClick={() => navegarMes('anterior')}
              className="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 rounded hover:bg-gray-100 dark:hover:bg-gray-700"
            >
              <ChevronLeft className="h-5 w-5" />
            </button>
            <span className="px-4 py-1 text-sm font-medium text-gray-900 dark:text-white min-w-[140px] text-center">
              {meses[mes - 1]} {ano}
            </span>
            <button
              onClick={() => navegarMes('proximo')}
              className="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 rounded hover:bg-gray-100 dark:hover:bg-gray-700"
            >
              <ChevronRight className="h-5 w-5" />
            </button>
          </div>

          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="p-2 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 disabled:opacity-50"
          >
            <RefreshCw className={`h-5 w-5 ${isFetching ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Cards de Destaque */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Receita Bruta */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
          <div className="flex items-center justify-between">
            <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
              <DollarSign className="h-5 w-5 text-blue-600 dark:text-blue-400" />
            </div>
            {dre?.comparativoMesAnterior?.variacaoReceita !== undefined && (
              <span className={`flex items-center text-sm font-medium ${
                dre.comparativoMesAnterior.variacaoReceita >= 0 ? 'text-green-600' : 'text-red-600'
              }`}>
                {dre.comparativoMesAnterior.variacaoReceita >= 0 ? (
                  <ArrowUpRight className="h-4 w-4" />
                ) : (
                  <ArrowDownRight className="h-4 w-4" />
                )}
                {Math.abs(dre.comparativoMesAnterior.variacaoReceita).toFixed(1)}%
              </span>
            )}
          </div>
          <div className="mt-3">
            <p className="text-sm text-gray-500 dark:text-gray-400">Receita Bruta</p>
            <p className="text-2xl font-bold text-gray-900 dark:text-white">
              {isLoading ? '...' : formatCurrency(dre?.receitaBrutaTotal)}
            </p>
          </div>
        </div>

        {/* Lucro Bruto */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
          <div className="flex items-center justify-between">
            <div className="p-2 bg-green-100 dark:bg-green-900/30 rounded-lg">
              <TrendingUp className="h-5 w-5 text-green-600 dark:text-green-400" />
            </div>
            <span className="text-sm font-medium text-gray-500">
              Margem: {formatPercent(dre?.margemBruta)}
            </span>
          </div>
          <div className="mt-3">
            <p className="text-sm text-gray-500 dark:text-gray-400">Lucro Bruto</p>
            <p className={`text-2xl font-bold ${
              (dre?.lucroBruto || 0) >= 0
                ? 'text-green-600 dark:text-green-400'
                : 'text-red-600 dark:text-red-400'
            }`}>
              {isLoading ? '...' : formatCurrency(dre?.lucroBruto)}
            </p>
          </div>
        </div>

        {/* Lucro Líquido */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
          <div className="flex items-center justify-between">
            <div className={`p-2 rounded-lg ${
              (dre?.lucroLiquido || 0) >= 0
                ? 'bg-green-100 dark:bg-green-900/30'
                : 'bg-red-100 dark:bg-red-900/30'
            }`}>
              {(dre?.lucroLiquido || 0) >= 0 ? (
                <TrendingUp className="h-5 w-5 text-green-600 dark:text-green-400" />
              ) : (
                <TrendingDown className="h-5 w-5 text-red-600 dark:text-red-400" />
              )}
            </div>
            <span className="text-sm font-medium text-gray-500">
              Margem: {formatPercent(dre?.margemLiquida)}
            </span>
          </div>
          <div className="mt-3">
            <p className="text-sm text-gray-500 dark:text-gray-400">Lucro Líquido</p>
            <p className={`text-2xl font-bold ${
              (dre?.lucroLiquido || 0) >= 0
                ? 'text-green-600 dark:text-green-400'
                : 'text-red-600 dark:text-red-400'
            }`}>
              {isLoading ? '...' : formatCurrency(dre?.lucroLiquido)}
            </p>
          </div>
        </div>
      </div>

      {/* Alertas Inteligentes */}
      {dre?.alertas && dre.alertas.length > 0 && (
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
          <button
            onClick={() => setAlertasExpandidos(!alertasExpandidos)}
            className="w-full p-4 flex items-center justify-between hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
          >
            <div className="flex items-center gap-3">
              <div className="p-2 bg-amber-100 dark:bg-amber-900/30 rounded-lg">
                <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-400" />
              </div>
              <div className="text-left">
                <h3 className="text-base font-semibold text-gray-900 dark:text-white">
                  Alertas Inteligentes
                </h3>
                <div className="flex items-center gap-2 mt-1">
                  {alertasCount.critical > 0 && (
                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 dark:bg-red-900/50 text-red-800 dark:text-red-200">
                      {alertasCount.critical} crítico{alertasCount.critical > 1 ? 's' : ''}
                    </span>
                  )}
                  {alertasCount.warning > 0 && (
                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-yellow-100 dark:bg-yellow-900/50 text-yellow-800 dark:text-yellow-200">
                      {alertasCount.warning} atenção
                    </span>
                  )}
                  {alertasCount.info > 0 && (
                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 dark:bg-blue-900/50 text-blue-800 dark:text-blue-200">
                      {alertasCount.info} info
                    </span>
                  )}
                </div>
              </div>
            </div>
            {alertasExpandidos ? (
              <ChevronUp className="h-5 w-5 text-gray-400" />
            ) : (
              <ChevronDown className="h-5 w-5 text-gray-400" />
            )}
          </button>

          {alertasExpandidos && (
            <div className="border-t border-gray-200 dark:border-gray-700 divide-y divide-gray-200 dark:divide-gray-700">
              {dre.alertas.map((alerta, index) => {
                const config = getAlertaConfig(alerta.nivel);
                const IconComponent = config.icon;

                return (
                  <div
                    key={index}
                    className={`p-4 ${config.bgColor}`}
                  >
                    <div className="flex items-start gap-3">
                      <IconComponent className={`h-5 w-5 mt-0.5 flex-shrink-0 ${config.iconColor}`} />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${config.badgeColor}`}>
                            {config.label}
                          </span>
                        </div>
                        <p className={`text-sm font-medium ${config.textColor}`}>
                          {alerta.mensagem}
                        </p>
                        {alerta.sugestao && (
                          <div className="mt-2 flex items-start gap-2">
                            <Lightbulb className="h-4 w-4 mt-0.5 flex-shrink-0 text-gray-400 dark:text-gray-500" />
                            <p className="text-xs text-gray-600 dark:text-gray-400">
                              <strong>Sugestão:</strong> {alerta.sugestao}
                            </p>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* DRE Detalhado */}
      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div className="p-5 border-b border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
            Demonstração Detalhada - {dre?.periodo || `${meses[mes - 1]}/${ano}`}
          </h3>
        </div>

        {isLoading ? (
          <div className="p-8 text-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          </div>
        ) : (
          <div className="divide-y divide-gray-200 dark:divide-gray-700">
            {/* RECEITAS */}
            <div className="bg-blue-50 dark:bg-blue-900/20 px-4 sm:px-6 py-2 sm:py-3">
              <span className="text-xs sm:text-sm font-semibold text-blue-800 dark:text-blue-300 uppercase">
                Receitas
              </span>
            </div>
            <LinhaDRE
              label="Receita Bruta de Serviços"
              valor={dre?.receitaBrutaServicos}
              percentual={dre?.receitaBrutaTotal ? (dre.receitaBrutaServicos / dre.receitaBrutaTotal * 100) : 0}
            />
            <LinhaDRE
              label="Receita Bruta de Peças"
              valor={dre?.receitaBrutaPecas}
              percentual={dre?.receitaBrutaTotal ? (dre.receitaBrutaPecas / dre.receitaBrutaTotal * 100) : 0}
            />
            <LinhaDRE
              label="Outras Receitas"
              valor={dre?.outrasReceitas}
              percentual={dre?.receitaBrutaTotal ? (dre.outrasReceitas / dre.receitaBrutaTotal * 100) : 0}
            />
            <LinhaDRE
              label="RECEITA BRUTA TOTAL"
              valor={dre?.receitaBrutaTotal}
              destaque
              corValor="text-blue-600 dark:text-blue-400"
            />

            {/* DEDUÇÕES */}
            <div className="bg-orange-50 dark:bg-orange-900/20 px-4 sm:px-6 py-2 sm:py-3">
              <span className="text-xs sm:text-sm font-semibold text-orange-800 dark:text-orange-300 uppercase">
                Deduções
              </span>
            </div>
            <LinhaDRE
              label="(-) Descontos Concedidos"
              valor={dre?.descontosConcedidos}
              negativo
            />
            <LinhaDRE
              label="(-) Cancelamentos"
              valor={dre?.cancelamentos}
              negativo
            />
            <LinhaDRE
              label="RECEITA LÍQUIDA"
              valor={dre?.receitaLiquida}
              destaque
              corValor="text-blue-600 dark:text-blue-400"
            />

            {/* CUSTOS */}
            <div className="bg-red-50 dark:bg-red-900/20 px-4 sm:px-6 py-2 sm:py-3">
              <span className="text-xs sm:text-sm font-semibold text-red-800 dark:text-red-300 uppercase">
                Custos
              </span>
            </div>
            <LinhaDRE
              label="(-) Custo das Peças Vendidas"
              valor={dre?.custoPecasVendidas}
              negativo
            />
            <LinhaDRE
              label="(-) Custo de Mão de Obra"
              valor={dre?.custoMaoObra}
              negativo
            />
            <LinhaDRE
              label="LUCRO BRUTO"
              valor={dre?.lucroBruto}
              destaque
              corValor={(dre?.lucroBruto || 0) >= 0 ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}
              extra={`Margem: ${formatPercent(dre?.margemBruta)}`}
            />

            {/* DESPESAS OPERACIONAIS */}
            <div className="bg-purple-50 dark:bg-purple-900/20 px-4 sm:px-6 py-2 sm:py-3">
              <span className="text-xs sm:text-sm font-semibold text-purple-800 dark:text-purple-300 uppercase">
                Despesas Operacionais
              </span>
            </div>
            <LinhaDRE
              label="(-) Despesas Administrativas"
              valor={dre?.despesasAdministrativas}
              negativo
            />
            <LinhaDRE
              label="(-) Despesas com Pessoal"
              valor={dre?.despesasPessoal}
              negativo
            />
            <LinhaDRE
              label="(-) Despesas com Marketing"
              valor={dre?.despesasMarketing}
              negativo
            />
            <LinhaDRE
              label="(-) Outras Despesas"
              valor={dre?.outrasDespesas}
              negativo
            />
            <LinhaDRE
              label="RESULTADO OPERACIONAL (EBIT)"
              valor={dre?.resultadoOperacional}
              destaque
              corValor={(dre?.resultadoOperacional || 0) >= 0 ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}
              extra={`Margem: ${formatPercent(dre?.margemOperacional)}`}
            />

            {/* RESULTADO FINANCEIRO */}
            <div className="bg-cyan-50 dark:bg-cyan-900/20 px-4 sm:px-6 py-2 sm:py-3">
              <span className="text-xs sm:text-sm font-semibold text-cyan-800 dark:text-cyan-300 uppercase">
                Resultado Financeiro
              </span>
            </div>
            <LinhaDRE
              label="(+) Receitas Financeiras"
              valor={dre?.receitasFinanceiras}
            />
            <LinhaDRE
              label="(-) Despesas Financeiras"
              valor={dre?.despesasFinanceiras}
              negativo
            />

            {/* IMPOSTOS E LUCRO LÍQUIDO */}
            <div className="bg-gray-100 dark:bg-gray-700 px-4 sm:px-6 py-2 sm:py-3">
              <span className="text-xs sm:text-sm font-semibold text-gray-800 dark:text-gray-200 uppercase">
                Resultado Final
              </span>
            </div>
            <LinhaDRE
              label="Resultado Antes dos Impostos"
              valor={dre?.resultadoAntesImpostos}
            />
            <LinhaDRE
              label="(-) Impostos Estimados"
              valor={dre?.impostos}
              negativo
            />
            <div className="bg-gradient-to-r from-green-50 to-green-100 dark:from-green-900/30 dark:to-green-800/30 px-4 sm:px-6 py-3 sm:py-4">
              <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2">
                <span className="text-base sm:text-lg font-bold text-gray-900 dark:text-white">
                  LUCRO LÍQUIDO
                </span>
                <div className="text-right">
                  <span className={`text-xl sm:text-2xl font-bold ${
                    (dre?.lucroLiquido || 0) >= 0
                      ? 'text-green-600 dark:text-green-400'
                      : 'text-red-600 dark:text-red-400'
                  }`}>
                    {formatCurrency(dre?.lucroLiquido)}
                  </span>
                  <p className="text-xs sm:text-sm text-gray-500 dark:text-gray-400">
                    Margem Líquida: {formatPercent(dre?.margemLiquida)}
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// Componente de linha do DRE
interface LinhaDREProps {
  label: string;
  valor?: number;
  percentual?: number;
  negativo?: boolean;
  destaque?: boolean;
  corValor?: string;
  extra?: string;
}

function LinhaDRE({ label, valor, percentual, negativo, destaque, corValor, extra }: LinhaDREProps) {
  const formatCurrency = (value: number | undefined) => {
    if (value === undefined || value === null) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(value);
  };

  return (
    <div className={`px-4 sm:px-6 py-3 flex flex-col sm:flex-row sm:justify-between sm:items-center gap-1 sm:gap-4 ${
      destaque ? 'bg-gray-50 dark:bg-gray-700' : ''
    }`}>
      <span className={`text-xs sm:text-sm ${
        destaque ? 'font-semibold text-gray-900 dark:text-white' : 'text-gray-600 dark:text-gray-300'
      }`}>
        {label}
      </span>
      <div className="text-right sm:text-right flex items-center justify-end gap-2 sm:block">
        <span className={`text-sm sm:text-sm ${
          destaque ? 'font-bold' : 'font-medium'
        } ${
          corValor
            ? corValor
            : negativo
              ? 'text-red-600 dark:text-red-400'
              : 'text-gray-900 dark:text-white'
        }`}>
          {negativo && (valor || 0) > 0 ? '-' : ''}{formatCurrency(valor)}
        </span>
        {percentual !== undefined && percentual > 0 && (
          <span className="text-xs text-gray-500 dark:text-gray-400">
            ({percentual.toFixed(1)}%)
          </span>
        )}
        {extra && (
          <p className="text-xs text-gray-500 dark:text-gray-400 hidden sm:block">{extra}</p>
        )}
      </div>
    </div>
  );
}
