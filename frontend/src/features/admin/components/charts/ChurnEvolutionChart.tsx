/**
 * Churn Evolution Chart Component
 * Displays churn rate trends over time
 */

import ReactECharts from 'echarts-for-react';
import type { ChurnEvolution } from '../../types';

interface ChurnEvolutionChartProps {
  data: ChurnEvolution;
  isLoading?: boolean;
}

export const ChurnEvolutionChart = ({ data, isLoading }: ChurnEvolutionChartProps) => {
  if (isLoading) {
    return (
      <div className="flex h-80 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-red-500 border-t-transparent" />
      </div>
    );
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: { name: string; value: number }[]) => {
        const point = params[0];
        const churnData = data.data.find(d => d.monthLabel === point.name);
        return `
          <div class="p-2">
            <div class="font-bold">${point.name}</div>
            <div>Churn: ${point.value.toFixed(2)}%</div>
            ${churnData ? `<div>Cancelamentos: ${churnData.cancelled}</div>` : ''}
            ${churnData ? `<div>Ativos no início: ${churnData.activeAtStart}</div>` : ''}
          </div>
        `;
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: data.data.map(d => d.monthLabel),
      axisLabel: {
        color: '#9CA3AF',
      },
      axisLine: {
        lineStyle: {
          color: '#374151',
        },
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#9CA3AF',
        formatter: '{value}%',
      },
      splitLine: {
        lineStyle: {
          color: '#374151',
        },
      },
    },
    series: [
      {
        name: 'Churn Rate',
        type: 'bar',
        data: data.data.map(d => d.churnRate),
        itemStyle: {
          color: (params: { value: number }) => {
            const value = params.value;
            if (value > 5) return '#EF4444';
            if (value > 2) return '#F59E0B';
            return '#22C55E';
          },
          borderRadius: [4, 4, 0, 0],
        },
      },
    ],
  };

  return (
    <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
      <div className="mb-4 flex items-center justify-between">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
          Taxa de Churn
        </h3>
        <div className="text-right">
          <span className={`text-sm font-medium ${data.currentChurn <= 2 ? 'text-green-500' : data.currentChurn <= 5 ? 'text-yellow-500' : 'text-red-500'}`}>
            Atual: {data.currentChurn.toFixed(2)}%
          </span>
          <p className="text-xs text-gray-500 dark:text-gray-400">
            Média: {data.averageChurn.toFixed(2)}% | Total: {data.totalCancelled}
          </p>
        </div>
      </div>
      <ReactECharts option={option} style={{ height: '300px' }} />
    </div>
  );
};
