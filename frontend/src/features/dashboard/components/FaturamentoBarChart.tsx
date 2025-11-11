/**
 * Gráfico de barras mostrando faturamento mensal (últimos 6 meses)
 * Usa Apache ECharts via echarts-for-react
 */

import ReactECharts from 'echarts-for-react';
import type { EChartsOption } from 'echarts';
import { useFaturamentoMensal } from '../hooks/useFaturamentoMensal';

export const FaturamentoBarChart = () => {
  const { data, isLoading, error } = useFaturamentoMensal();

  if (isLoading) {
    return (
      <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        <div className="animate-pulse">
          <div className="h-6 w-40 rounded bg-gray-200" />
          <div className="mt-4 h-64 rounded bg-gray-200" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-6 text-center text-red-600">
        <p className="font-medium">Erro ao carregar gráfico</p>
        <p className="mt-1 text-sm">Tente novamente mais tarde</p>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="rounded-lg border border-gray-200 bg-white p-6 text-center text-gray-500">
        <p>Nenhum dado disponível</p>
      </div>
    );
  }

  // Extrai meses e valores
  const meses = data.map((item) => item.mes);
  const valores = data.map((item) => item.valor);

  const option: EChartsOption = {
    title: {
      text: 'Faturamento Mensal',
      left: 'left',
      textStyle: {
        fontSize: 16,
        fontWeight: 600,
        color: '#1f2937',
      },
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
      formatter: (params: any) => {
        const value = params[0].value;
        return `${params[0].name}<br/>R$ ${value.toLocaleString('pt-BR', {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        })}`;
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: {
        color: '#1f2937',
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      top: '20%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: meses,
      axisLabel: {
        color: '#6b7280',
        fontSize: 12,
        rotate: 45,
      },
      axisLine: {
        lineStyle: {
          color: '#e5e7eb',
        },
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#6b7280',
        fontSize: 12,
        formatter: (value: number) => {
          return `R$ ${(value / 1000).toFixed(0)}k`;
        },
      },
      splitLine: {
        lineStyle: {
          color: '#f3f4f6',
        },
      },
    },
    series: [
      {
        name: 'Faturamento',
        type: 'bar',
        data: valores,
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: '#3b82f6', // Azul
              },
              {
                offset: 1,
                color: '#1d4ed8', // Azul escuro
              },
            ],
          },
          borderRadius: [4, 4, 0, 0],
        },
        emphasis: {
          itemStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                {
                  offset: 0,
                  color: '#2563eb',
                },
                {
                  offset: 1,
                  color: '#1e40af',
                },
              ],
            },
          },
        },
        barMaxWidth: 60,
      },
    ],
  };

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <ReactECharts
        option={option}
        style={{ height: '350px', width: '100%' }}
        notMerge={true}
        lazyUpdate={true}
        opts={{ renderer: 'canvas' }}
      />
    </div>
  );
};
