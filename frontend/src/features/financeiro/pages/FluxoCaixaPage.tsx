import { useState, useMemo } from 'react';
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  Calendar,
  ArrowUpRight,
  ArrowDownRight,
  BarChart3,
  PieChart as PieChartIcon,
  RefreshCw,
  AlertTriangle,
  AlertCircle,
  Info,
  ChevronDown,
  ChevronUp,
  Lightbulb,
} from 'lucide-react';
import ReactECharts from 'echarts-for-react';
import { useFluxoCaixaUltimosDias } from '../hooks/useFluxoCaixa';
import { useTheme } from '@/shared/contexts';
import type { MovimentoDiario, MovimentoCategoria } from '../types/fluxoCaixa';

type PeriodoFiltro = 7 | 15 | 30 | 60 | 90;

// Tipos de alertas do Fluxo de Caixa
type NivelAlertaFluxo = 'CRITICAL' | 'WARNING' | 'INFO';

interface AlertaFluxoCaixa {
  nivel: NivelAlertaFluxo;
  mensagem: string;
  sugestao?: string;
}

// Função para gerar alertas baseados no fluxo de caixa
function gerarAlertasFluxoCaixa(fluxo: {
  saldoFinal: number;
  totalReceitas: number;
  totalDespesas: number;
  variacaoReceitas: number;
  variacaoDespesas?: number;
  variacaoSaldo?: number;
}): AlertaFluxoCaixa[] {
  const alertas: AlertaFluxoCaixa[] = [];

  // Alerta crítico: Saldo final negativo
  if (fluxo.saldoFinal < 0) {
    alertas.push({
      nivel: 'CRITICAL',
      mensagem: `O saldo do período está negativo em ${new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Math.abs(fluxo.saldoFinal))}`,
      sugestao: 'As despesas superaram as receitas. Revise os gastos e busque aumentar o faturamento ou reduzir custos.',
    });
  }

  // Alerta de atenção: Queda significativa nas receitas
  if (fluxo.variacaoReceitas < -20) {
    alertas.push({
      nivel: 'WARNING',
      mensagem: `Receitas caíram ${Math.abs(fluxo.variacaoReceitas).toFixed(1)}% em relação ao período anterior`,
      sugestao: 'Analise as causas da queda. Verifique sazonalidade, concorrência ou problemas operacionais.',
    });
  } else if (fluxo.variacaoReceitas < -10) {
    alertas.push({
      nivel: 'INFO',
      mensagem: `Receitas reduziram ${Math.abs(fluxo.variacaoReceitas).toFixed(1)}% em relação ao período anterior`,
    });
  }

  // Alerta de atenção: Despesas muito altas em relação às receitas
  if (fluxo.totalReceitas > 0) {
    const percentualDespesas = (fluxo.totalDespesas / fluxo.totalReceitas) * 100;
    if (percentualDespesas > 90) {
      alertas.push({
        nivel: 'WARNING',
        mensagem: `As despesas representam ${percentualDespesas.toFixed(0)}% das receitas`,
        sugestao: 'Margem de lucro muito baixa. Considere revisar preços ou reduzir custos operacionais.',
      });
    }
  }

  // Alerta informativo: Receitas em crescimento
  if (fluxo.variacaoReceitas > 15) {
    alertas.push({
      nivel: 'INFO',
      mensagem: `Receitas aumentaram ${fluxo.variacaoReceitas.toFixed(1)}% - bom desempenho!`,
    });
  }

  return alertas;
}

// Configuração visual dos alertas
function getAlertaConfigFluxo(nivel: NivelAlertaFluxo) {
  switch (nivel) {
    case 'CRITICAL':
      return {
        icon: AlertCircle,
        bgColor: 'bg-red-50 dark:bg-red-900/30',
        borderColor: 'border-red-200 dark:border-red-800',
        textColor: 'text-red-800 dark:text-red-200',
        iconColor: 'text-red-600 dark:text-red-400',
        badgeColor: 'bg-red-100 dark:bg-red-800 text-red-800 dark:text-red-200',
        label: 'Crítico',
      };
    case 'WARNING':
      return {
        icon: AlertTriangle,
        bgColor: 'bg-yellow-50 dark:bg-yellow-900/30',
        borderColor: 'border-yellow-200 dark:border-yellow-800',
        textColor: 'text-yellow-800 dark:text-yellow-200',
        iconColor: 'text-yellow-600 dark:text-yellow-400',
        badgeColor: 'bg-yellow-100 dark:bg-yellow-800 text-yellow-800 dark:text-yellow-200',
        label: 'Atenção',
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
        label: 'Info',
      };
  }
}

export default function FluxoCaixaPage() {
  const [periodo, setPeriodo] = useState<PeriodoFiltro>(30);
  const [alertasExpandidos, setAlertasExpandidos] = useState(true);
  const { data: fluxo, isLoading, isFetching, error, refetch } = useFluxoCaixaUltimosDias(periodo);
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  // Gerar alertas inteligentes baseados no fluxo de caixa
  const alertas = useMemo(() => {
    if (!fluxo) return [];
    return gerarAlertasFluxoCaixa({
      saldoFinal: fluxo.saldoFinal,
      totalReceitas: fluxo.totalReceitas,
      totalDespesas: fluxo.totalDespesas,
      variacaoReceitas: fluxo.variacaoReceitas,
      variacaoDespesas: fluxo.variacaoDespesas,
      variacaoSaldo: fluxo.variacaoSaldo,
    });
  }, [fluxo]);

  const alertasCount = useMemo(() => ({
    critical: alertas.filter((a) => a.nivel === 'CRITICAL').length,
    warning: alertas.filter((a) => a.nivel === 'WARNING').length,
    info: alertas.filter((a) => a.nivel === 'INFO').length,
    total: alertas.length,
  }), [alertas]);

  // Cores do tema para ECharts
  const themeColors = useMemo(() => ({
    text: isDark ? '#E5E7EB' : '#374151',
    textMuted: isDark ? '#9CA3AF' : '#6B7280',
    border: isDark ? '#374151' : '#E5E7EB',
    background: isDark ? '#1F2937' : '#FFFFFF',
    gridLine: isDark ? '#374151' : '#E5E7EB',
  }), [isDark]);

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(value);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit'
    });
  };

  // Gráfico de linha - Fluxo de Caixa Diário
  const lineChartOptions = useMemo(() => {
    if (!fluxo?.movimentosDiarios) return {};

    const datas = fluxo.movimentosDiarios.map((m: MovimentoDiario) => formatDate(m.data));
    const receitas = fluxo.movimentosDiarios.map((m: MovimentoDiario) => m.receitas);
    const despesas = fluxo.movimentosDiarios.map((m: MovimentoDiario) => m.despesas);
    const saldoAcumulado = fluxo.movimentosDiarios.map((m: MovimentoDiario) => m.saldoAcumulado);

    return {
      tooltip: {
        trigger: 'axis',
        backgroundColor: themeColors.background,
        borderColor: themeColors.border,
        textStyle: { color: themeColors.text },
        formatter: (params: any[]) => {
          let result = `<strong>${params[0].axisValue}</strong><br/>`;
          params.forEach(p => {
            const color = p.color;
            result += `<span style="display:inline-block;width:10px;height:10px;background:${color};border-radius:50%;margin-right:5px;"></span>`;
            result += `${p.seriesName}: ${formatCurrency(p.value)}<br/>`;
          });
          return result;
        }
      },
      legend: {
        data: ['Receitas', 'Despesas', 'Saldo Acumulado'],
        bottom: 0,
        textStyle: { color: themeColors.text }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '15%',
        top: '10%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: datas,
        axisLabel: {
          rotate: periodo > 30 ? 45 : 0,
          color: themeColors.textMuted
        },
        axisLine: { lineStyle: { color: themeColors.gridLine } },
        splitLine: { lineStyle: { color: themeColors.gridLine } }
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          color: themeColors.textMuted,
          formatter: (value: number) => {
            if (value >= 1000) {
              return `R$ ${(value / 1000).toFixed(1)}k`;
            }
            return `R$ ${value}`;
          }
        },
        axisLine: { lineStyle: { color: themeColors.gridLine } },
        splitLine: { lineStyle: { color: themeColors.gridLine } }
      },
      series: [
        {
          name: 'Receitas',
          type: 'bar',
          data: receitas,
          itemStyle: { color: '#22C55E' },
          barGap: '0%'
        },
        {
          name: 'Despesas',
          type: 'bar',
          data: despesas,
          itemStyle: { color: '#EF4444' }
        },
        {
          name: 'Saldo Acumulado',
          type: 'line',
          data: saldoAcumulado,
          smooth: true,
          itemStyle: { color: '#3B82F6' },
          lineStyle: { width: 3 },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
                { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
              ]
            }
          }
        }
      ]
    };
  }, [fluxo, periodo, themeColors]);

  // Gráfico de pizza - Receitas por Categoria
  const pieChartOptions = useMemo(() => {
    if (!fluxo?.receitasPorCategoria || fluxo.receitasPorCategoria.length === 0) return null;

    const data = fluxo.receitasPorCategoria.map((c: MovimentoCategoria) => ({
      name: c.categoria.replace('_', ' '),
      value: c.valor,
      itemStyle: { color: c.cor }
    }));

    return {
      tooltip: {
        trigger: 'item',
        backgroundColor: themeColors.background,
        borderColor: themeColors.border,
        textStyle: { color: themeColors.text },
        formatter: (params: any) => {
          return `<strong>${params.name}</strong><br/>` +
            `${formatCurrency(params.value)} (${params.percent.toFixed(1)}%)`;
        }
      },
      legend: {
        orient: 'vertical',
        right: 10,
        top: 'center',
        textStyle: { color: themeColors.text }
      },
      series: [
        {
          name: 'Receitas',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: themeColors.background,
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 14,
              fontWeight: 'bold',
              color: themeColors.text,
              formatter: '{b}\n{d}%'
            }
          },
          labelLine: {
            show: false
          },
          data
        }
      ]
    };
  }, [fluxo, themeColors]);

  if (error) {
    return (
      <div className="p-6">
        <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg p-4">
          <p className="text-red-800 dark:text-red-200">
            Erro ao carregar fluxo de caixa. Por favor, tente novamente.
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
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Fluxo de Caixa
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Análise de receitas e despesas do período
          </p>
        </div>

        <div className="flex items-center gap-3">
          {/* Filtro de Período */}
          <div className="flex items-center gap-2 bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-1">
            {([7, 15, 30, 60, 90] as PeriodoFiltro[]).map((dias) => (
              <button
                key={dias}
                onClick={() => setPeriodo(dias)}
                className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                  periodo === dias
                    ? 'bg-blue-600 text-white'
                    : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
                }`}
              >
                {dias}d
              </button>
            ))}
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

      {/* Cards de Resumo */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Receitas */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
          <div className="flex items-center justify-between">
            <div className="p-2 bg-green-100 dark:bg-green-900/30 rounded-lg">
              <TrendingUp className="h-5 w-5 text-green-600 dark:text-green-400" />
            </div>
            {fluxo && fluxo.variacaoReceitas !== 0 && (
              <span className={`flex items-center text-sm font-medium ${
                fluxo.variacaoReceitas > 0 ? 'text-green-600' : 'text-red-600'
              }`}>
                {fluxo.variacaoReceitas > 0 ? (
                  <ArrowUpRight className="h-4 w-4" />
                ) : (
                  <ArrowDownRight className="h-4 w-4" />
                )}
                {Math.abs(fluxo.variacaoReceitas).toFixed(1)}%
              </span>
            )}
          </div>
          <div className="mt-3">
            <p className="text-sm text-gray-500 dark:text-gray-400">Total Receitas</p>
            <p className="text-2xl font-bold text-gray-900 dark:text-white">
              {isLoading ? '...' : formatCurrency(fluxo?.totalReceitas || 0)}
            </p>
          </div>
        </div>

        {/* Despesas */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
          <div className="flex items-center justify-between">
            <div className="p-2 bg-red-100 dark:bg-red-900/30 rounded-lg">
              <TrendingDown className="h-5 w-5 text-red-600 dark:text-red-400" />
            </div>
          </div>
          <div className="mt-3">
            <p className="text-sm text-gray-500 dark:text-gray-400">Total Despesas</p>
            <p className="text-2xl font-bold text-gray-900 dark:text-white">
              {isLoading ? '...' : formatCurrency(fluxo?.totalDespesas || 0)}
            </p>
          </div>
        </div>

        {/* Saldo Final */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
          <div className="flex items-center justify-between">
            <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
              <DollarSign className="h-5 w-5 text-blue-600 dark:text-blue-400" />
            </div>
          </div>
          <div className="mt-3">
            <p className="text-sm text-gray-500 dark:text-gray-400">Saldo no Período</p>
            <p className={`text-2xl font-bold ${
              (fluxo?.saldoFinal || 0) >= 0
                ? 'text-green-600 dark:text-green-400'
                : 'text-red-600 dark:text-red-400'
            }`}>
              {isLoading ? '...' : formatCurrency(fluxo?.saldoFinal || 0)}
            </p>
          </div>
        </div>

        {/* Período */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
          <div className="flex items-center justify-between">
            <div className="p-2 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
              <Calendar className="h-5 w-5 text-purple-600 dark:text-purple-400" />
            </div>
          </div>
          <div className="mt-3">
            <p className="text-sm text-gray-500 dark:text-gray-400">Período</p>
            <p className="text-lg font-semibold text-gray-900 dark:text-white">
              {isLoading ? '...' : `${formatDate(fluxo?.dataInicio || '')} - ${formatDate(fluxo?.dataFim || '')}`}
            </p>
          </div>
        </div>
      </div>

      {/* Alertas Inteligentes */}
      {alertas.length > 0 && (
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
              {alertas.map((alerta, index) => {
                const config = getAlertaConfigFluxo(alerta.nivel);
                const IconComponent = config.icon;

                return (
                  <div key={index} className={`p-4 ${config.bgColor}`}>
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

      {/* Gráficos */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Gráfico de Linha - Fluxo Diário */}
        <div className="lg:col-span-2 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
          <div className="flex items-center gap-2 mb-4">
            <BarChart3 className="h-5 w-5 text-gray-500" />
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              Movimentação Diária
            </h3>
          </div>
          {isLoading ? (
            <div className="h-80 flex items-center justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
          ) : (
            <ReactECharts
              option={lineChartOptions}
              style={{ height: '320px' }}
              opts={{ renderer: 'canvas' }}
            />
          )}
        </div>

        {/* Gráfico de Pizza - Receitas por Categoria */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
          <div className="flex items-center gap-2 mb-4">
            <PieChartIcon className="h-5 w-5 text-gray-500" />
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              Receitas por Forma de Pagamento
            </h3>
          </div>
          {isLoading ? (
            <div className="h-80 flex items-center justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
          ) : pieChartOptions ? (
            <ReactECharts
              option={pieChartOptions}
              style={{ height: '320px' }}
              opts={{ renderer: 'canvas' }}
            />
          ) : (
            <div className="h-80 flex items-center justify-center text-gray-500 dark:text-gray-400">
              Sem dados de receitas no período
            </div>
          )}
        </div>
      </div>

      {/* Detalhamento Diário */}
      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div className="p-4 sm:p-5 border-b border-gray-200 dark:border-gray-700">
          <h3 className="text-base sm:text-lg font-semibold text-gray-900 dark:text-white">
            Detalhamento Diário
          </h3>
        </div>

        {/* Mobile: Cards */}
        <div className="lg:hidden divide-y divide-gray-200 dark:divide-gray-700">
          {isLoading ? (
            <div className="p-8 flex justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
          ) : fluxo?.movimentosDiarios?.slice().reverse().slice(0, 10).map((m: MovimentoDiario) => (
            <div key={m.data} className="p-4 space-y-3">
              {/* Data */}
              <div className="flex items-center justify-between">
                <span className="font-medium text-gray-900 dark:text-white">
                  {new Date(m.data).toLocaleDateString('pt-BR', {
                    weekday: 'short',
                    day: '2-digit',
                    month: '2-digit'
                  })}
                </span>
                <span className={`text-sm font-semibold px-2 py-1 rounded ${
                  m.saldo >= 0
                    ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400'
                    : 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400'
                }`}>
                  {m.saldo >= 0 ? '+' : ''}{formatCurrency(m.saldo)}
                </span>
              </div>

              {/* Grid de valores */}
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div className="bg-green-50 dark:bg-green-900/20 rounded-lg p-2">
                  <p className="text-gray-500 dark:text-gray-400 text-xs">Receitas</p>
                  <p className="text-green-600 dark:text-green-400 font-medium">
                    {formatCurrency(m.receitas)}
                  </p>
                </div>
                <div className="bg-red-50 dark:bg-red-900/20 rounded-lg p-2">
                  <p className="text-gray-500 dark:text-gray-400 text-xs">Despesas</p>
                  <p className="text-red-600 dark:text-red-400 font-medium">
                    {formatCurrency(m.despesas)}
                  </p>
                </div>
              </div>

              {/* Saldo acumulado */}
              <div className="flex items-center justify-between pt-2 border-t border-gray-100 dark:border-gray-700">
                <span className="text-xs text-gray-500 dark:text-gray-400">Saldo Acumulado</span>
                <span className={`font-semibold ${
                  m.saldoAcumulado >= 0 ? 'text-blue-600 dark:text-blue-400' : 'text-red-600 dark:text-red-400'
                }`}>
                  {formatCurrency(m.saldoAcumulado)}
                </span>
              </div>
            </div>
          ))}
        </div>

        {/* Desktop: Tabela */}
        <div className="hidden lg:block overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead className="bg-gray-50 dark:bg-gray-900">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Data
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Receitas
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Despesas
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Saldo do Dia
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Saldo Acumulado
                </th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
              {isLoading ? (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
                  </td>
                </tr>
              ) : fluxo?.movimentosDiarios?.slice().reverse().slice(0, 10).map((m: MovimentoDiario) => (
                <tr key={m.data} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                    {new Date(m.data).toLocaleDateString('pt-BR', {
                      weekday: 'short',
                      day: '2-digit',
                      month: '2-digit'
                    })}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-green-600 dark:text-green-400">
                    {formatCurrency(m.receitas)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-red-600 dark:text-red-400">
                    {formatCurrency(m.despesas)}
                  </td>
                  <td className={`px-6 py-4 whitespace-nowrap text-sm text-right font-medium ${
                    m.saldo >= 0 ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'
                  }`}>
                    {formatCurrency(m.saldo)}
                  </td>
                  <td className={`px-6 py-4 whitespace-nowrap text-sm text-right font-medium ${
                    m.saldoAcumulado >= 0 ? 'text-blue-600 dark:text-blue-400' : 'text-red-600 dark:text-red-400'
                  }`}>
                    {formatCurrency(m.saldoAcumulado)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
