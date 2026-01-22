import { useState, useMemo } from 'react';
import {
  TrendingUp,
  TrendingDown,
  AlertTriangle,
  Calendar,
  Target,
  Activity,
  RefreshCw,
  AlertCircle,
  Info,
  Zap
} from 'lucide-react';
import ReactECharts from 'echarts-for-react';
import { useProjecao } from '../hooks/useFluxoCaixa';
import { useTheme } from '@/shared/contexts';
import type {
  ProjecaoDiaria,
  ReceitaEsperada,
  AlertaFluxo,
  NivelAlerta
} from '../types/fluxoCaixa';

type PeriodoProjecao = 7 | 30 | 60 | 90;

export default function ProjecaoPage() {
  const [dias, setDias] = useState<PeriodoProjecao>(30);
  const { data: projecao, isLoading, isFetching, error, refetch } = useProjecao(dias);
  const { theme } = useTheme();
  const isDark = theme === 'dark';

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

  // Gráfico de área - Projeção Diária
  const areaChartOptions = useMemo(() => {
    if (!projecao?.projecaoDiaria) return {};

    const datas = projecao.projecaoDiaria.map((p: ProjecaoDiaria) => formatDate(p.data));
    const saldoAcumulado = projecao.projecaoDiaria.map((p: ProjecaoDiaria) => p.saldoAcumulado);
    const receitas = projecao.projecaoDiaria.map((p: ProjecaoDiaria) => p.receitasPrevistas);

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
        data: ['Saldo Acumulado Projetado', 'Receita Diária Média'],
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
          rotate: dias > 30 ? 45 : 0,
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
          name: 'Saldo Acumulado Projetado',
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
                { offset: 0, color: 'rgba(59, 130, 246, 0.4)' },
                { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
              ]
            }
          },
          markLine: {
            silent: true,
            data: [{ yAxis: 0, lineStyle: { color: '#EF4444', width: 2, type: 'dashed' } }]
          }
        },
        {
          name: 'Receita Diária Média',
          type: 'bar',
          data: receitas,
          itemStyle: { color: '#22C55E', opacity: 0.6 },
          barWidth: '60%'
        }
      ]
    };
  }, [projecao, dias, themeColors]);

  const getAlertIcon = (nivel: NivelAlerta) => {
    switch (nivel) {
      case 'CRITICAL':
        return <AlertTriangle className="h-5 w-5 text-red-500" />;
      case 'WARNING':
        return <AlertCircle className="h-5 w-5 text-yellow-500" />;
      default:
        return <Info className="h-5 w-5 text-blue-500" />;
    }
  };

  const getAlertStyle = (nivel: NivelAlerta) => {
    switch (nivel) {
      case 'CRITICAL':
        return 'bg-red-50 dark:bg-red-900/30 border-red-200 dark:border-red-800';
      case 'WARNING':
        return 'bg-yellow-50 dark:bg-yellow-900/30 border-yellow-200 dark:border-yellow-800';
      default:
        return 'bg-blue-50 dark:bg-blue-900/30 border-blue-200 dark:border-blue-800';
    }
  };

  const getTendenciaIcon = (tendencia: string) => {
    switch (tendencia) {
      case 'POSITIVA':
        return <TrendingUp className="h-6 w-6 text-green-500" />;
      case 'NEGATIVA':
        return <TrendingDown className="h-6 w-6 text-red-500" />;
      default:
        return <Activity className="h-6 w-6 text-gray-500" />;
    }
  };

  const getProbabilidadeBadge = (prob: string) => {
    switch (prob) {
      case 'ALTA':
        return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400';
      case 'MEDIA':
        return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400';
      default:
        return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
    }
  };

  if (error) {
    return (
      <div className="p-6">
        <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg p-4">
          <p className="text-red-800 dark:text-red-200">
            Erro ao carregar projeção financeira. Por favor, tente novamente.
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
            <Target className="h-7 w-7" />
            Projeção Financeira
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            Previsão de receitas e fluxo de caixa
          </p>
        </div>

        <div className="flex items-center gap-3">
          {/* Filtro de Período */}
          <div className="flex items-center gap-2 bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-1">
            {([7, 30, 60, 90] as PeriodoProjecao[]).map((d) => (
              <button
                key={d}
                onClick={() => setDias(d)}
                className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                  dias === d
                    ? 'bg-blue-600 text-white'
                    : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
                }`}
              >
                {d}d
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

      {/* Alertas */}
      {projecao?.alertas && projecao.alertas.length > 0 && (
        <div className="space-y-3">
          {projecao.alertas.map((alerta: AlertaFluxo, index: number) => (
            <div
              key={index}
              className={`flex items-start gap-3 p-4 rounded-lg border ${getAlertStyle(alerta.nivel)}`}
            >
              {getAlertIcon(alerta.nivel)}
              <div>
                <p className="font-medium text-gray-900 dark:text-white">
                  {alerta.mensagem}
                </p>
                {alerta.valor && (
                  <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                    Valor: {formatCurrency(alerta.valor)}
                  </p>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Cards de Resumo */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Receitas Esperadas */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
          <div className="flex items-center justify-between">
            <div className="p-2 bg-green-100 dark:bg-green-900/30 rounded-lg">
              <TrendingUp className="h-5 w-5 text-green-600 dark:text-green-400" />
            </div>
          </div>
          <div className="mt-3">
            <p className="text-sm text-gray-500 dark:text-gray-400">Receitas Esperadas</p>
            <p className="text-2xl font-bold text-green-600 dark:text-green-400">
              {isLoading ? '...' : formatCurrency(projecao?.receitasEsperadas || 0)}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {projecao?.detalhamentoReceitas?.length || 0} OS pendentes
            </p>
          </div>
        </div>

        {/* Saldo Projetado */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
          <div className="flex items-center justify-between">
            <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
              <Target className="h-5 w-5 text-blue-600 dark:text-blue-400" />
            </div>
          </div>
          <div className="mt-3">
            <p className="text-sm text-gray-500 dark:text-gray-400">Saldo Projetado</p>
            <p className={`text-2xl font-bold ${
              (projecao?.saldoProjetado || 0) >= 0
                ? 'text-blue-600 dark:text-blue-400'
                : 'text-red-600 dark:text-red-400'
            }`}>
              {isLoading ? '...' : formatCurrency(projecao?.saldoProjetado || 0)}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Em {dias} dias
            </p>
          </div>
        </div>

        {/* Média Diária */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
          <div className="flex items-center justify-between">
            <div className="p-2 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
              <Zap className="h-5 w-5 text-purple-600 dark:text-purple-400" />
            </div>
          </div>
          <div className="mt-3">
            <p className="text-sm text-gray-500 dark:text-gray-400">Média Diária</p>
            <p className="text-2xl font-bold text-gray-900 dark:text-white">
              {isLoading ? '...' : formatCurrency(projecao?.indicadores?.mediaReceitaDiaria || 0)}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Baseado nos últimos 30 dias
            </p>
          </div>
        </div>

        {/* Tendência */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
          <div className="flex items-center justify-between">
            <div className="p-2 bg-gray-100 dark:bg-gray-700 rounded-lg">
              {getTendenciaIcon(projecao?.indicadores?.tendencia || 'ESTAVEL')}
            </div>
          </div>
          <div className="mt-3">
            <p className="text-sm text-gray-500 dark:text-gray-400">Tendência</p>
            <p className={`text-xl font-bold ${
              projecao?.indicadores?.tendencia === 'POSITIVA'
                ? 'text-green-600 dark:text-green-400'
                : projecao?.indicadores?.tendencia === 'NEGATIVA'
                  ? 'text-red-600 dark:text-red-400'
                  : 'text-gray-600 dark:text-gray-400'
            }`}>
              {projecao?.indicadores?.tendencia || 'ESTÁVEL'}
            </p>
          </div>
        </div>
      </div>

      {/* Gráfico de Projeção */}
      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
        <div className="flex items-center gap-2 mb-4">
          <Calendar className="h-5 w-5 text-gray-500" />
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
            Projeção para os Próximos {dias} Dias
          </h3>
        </div>
        {isLoading ? (
          <div className="h-80 flex items-center justify-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        ) : (
          <ReactECharts
            option={areaChartOptions}
            style={{ height: '320px' }}
            opts={{ renderer: 'canvas' }}
          />
        )}
      </div>

      {/* Receitas Esperadas (OS Pendentes) */}
      {projecao?.detalhamentoReceitas && projecao.detalhamentoReceitas.length > 0 && (
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
          <div className="p-4 sm:p-5 border-b border-gray-200 dark:border-gray-700">
            <h3 className="text-base sm:text-lg font-semibold text-gray-900 dark:text-white">
              Receitas Esperadas (OS Pendentes)
            </h3>
          </div>

          {/* Mobile: Cards */}
          <div className="lg:hidden divide-y divide-gray-200 dark:divide-gray-700">
            {projecao.detalhamentoReceitas.map((receita: ReceitaEsperada, index: number) => (
              <div key={index} className="p-4 space-y-3">
                {/* Header com origem e probabilidade */}
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-500 dark:text-gray-400">
                    {receita.origem.replace('OS_', '')}
                  </span>
                  <span className={`px-2 py-1 text-xs font-medium rounded-full ${getProbabilidadeBadge(receita.probabilidade)}`}>
                    {receita.probabilidade}
                  </span>
                </div>

                {/* Descrição */}
                <p className="text-sm text-gray-900 dark:text-white">
                  {receita.descricao}
                </p>

                {/* Valor */}
                <div className="flex items-center justify-between pt-2 border-t border-gray-100 dark:border-gray-700">
                  <span className="text-xs text-gray-500 dark:text-gray-400">Valor Esperado</span>
                  <span className="font-semibold text-green-600 dark:text-green-400">
                    {formatCurrency(receita.valor)}
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
                    Origem
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Descrição
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Valor
                  </th>
                  <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Probabilidade
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                {projecao.detalhamentoReceitas.map((receita: ReceitaEsperada, index: number) => (
                  <tr key={index} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                      {receita.origem.replace('OS_', '')}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                      {receita.descricao}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right font-medium text-gray-900 dark:text-white">
                      {formatCurrency(receita.valor)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <span className={`px-2 py-1 text-xs font-medium rounded-full ${getProbabilidadeBadge(receita.probabilidade)}`}>
                        {receita.probabilidade}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
