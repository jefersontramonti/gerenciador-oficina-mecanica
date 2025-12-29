/**
 * MRR Evolution Chart Component
 * Displays Monthly Recurring Revenue trends over time
 */

import ReactECharts from 'echarts-for-react';
import type { MRREvolution } from '../../types';
import { formatCurrency } from '@/shared/utils/formatters';

interface MRREvolutionChartProps {
  data: MRREvolution;
  isLoading?: boolean;
}

export const MRREvolutionChart = ({ data, isLoading }: MRREvolutionChartProps) => {
  if (isLoading) {
    return (
      <div className="flex h-80 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
      </div>
    );
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: { name: string; value: number; seriesName: string }[]) => {
        const point = params[0];
        const mrrData = data.data.find(d => d.monthLabel === point.name);
        return `
          <div class="p-2">
            <div class="font-bold">${point.name}</div>
            <div>MRR: ${formatCurrency(point.value)}</div>
            ${mrrData ? `<div>Oficinas: ${mrrData.oficinasAtivas}</div>` : ''}
            ${mrrData && mrrData.growth !== 0 ? `<div>Crescimento: ${mrrData.growth > 0 ? '+' : ''}${mrrData.growth.toFixed(1)}%</div>` : ''}
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
      boundaryGap: false,
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
        formatter: (value: number) => {
          if (value >= 1000) {
            return `R$ ${(value / 1000).toFixed(0)}k`;
          }
          return `R$ ${value}`;
        },
      },
      splitLine: {
        lineStyle: {
          color: '#374151',
        },
      },
    },
    series: [
      {
        name: 'MRR',
        type: 'line',
        smooth: true,
        data: data.data.map(d => d.mrr),
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(34, 197, 94, 0.4)' },
              { offset: 1, color: 'rgba(34, 197, 94, 0.05)' },
            ],
          },
        },
        lineStyle: {
          color: '#22C55E',
          width: 3,
        },
        itemStyle: {
          color: '#22C55E',
        },
      },
    ],
  };

  return (
    <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
      <div className="mb-4 flex items-center justify-between">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
          Evolução do MRR
        </h3>
        <div className="text-right">
          <span className={`text-sm font-medium ${data.totalGrowth >= 0 ? 'text-green-500' : 'text-red-500'}`}>
            {data.totalGrowth >= 0 ? '+' : ''}{data.totalGrowth.toFixed(1)}%
          </span>
          <p className="text-xs text-gray-500 dark:text-gray-400">
            Média: {formatCurrency(data.averageMRR)}
          </p>
        </div>
      </div>
      <ReactECharts option={option} style={{ height: '300px' }} />
    </div>
  );
};
