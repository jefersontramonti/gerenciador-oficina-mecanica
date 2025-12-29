/**
 * Signups vs Cancellations Chart Component
 * Displays comparison of new signups vs cancellations
 */

import ReactECharts from 'echarts-for-react';
import type { SignupsVsCancellations } from '../../types';

interface SignupsVsCancellationsChartProps {
  data: SignupsVsCancellations;
  isLoading?: boolean;
}

export const SignupsVsCancellationsChart = ({ data, isLoading }: SignupsVsCancellationsChartProps) => {
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
      axisPointer: {
        type: 'shadow',
      },
      formatter: (params: { name: string; value: number; seriesName: string; color: string }[]) => {
        const monthData = data.data.find(d => d.monthLabel === params[0].name);
        let html = `<div class="p-2"><div class="font-bold">${params[0].name}</div>`;
        params.forEach(p => {
          html += `<div style="color: ${p.color}">${p.seriesName}: ${p.value}</div>`;
        });
        if (monthData) {
          html += `<div class="mt-1 border-t border-gray-600 pt-1">Saldo: ${monthData.netGrowth >= 0 ? '+' : ''}${monthData.netGrowth}</div>`;
          if (monthData.trialConversions > 0) {
            html += `<div>Conversões: ${monthData.trialConversions}</div>`;
          }
        }
        html += '</div>';
        return html;
      },
    },
    legend: {
      data: ['Novos Cadastros', 'Cancelamentos'],
      textStyle: {
        color: '#9CA3AF',
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
      },
      splitLine: {
        lineStyle: {
          color: '#374151',
        },
      },
    },
    series: [
      {
        name: 'Novos Cadastros',
        type: 'bar',
        stack: 'total',
        emphasis: {
          focus: 'series',
        },
        data: data.data.map(d => d.signups),
        itemStyle: {
          color: '#22C55E',
          borderRadius: [4, 4, 0, 0],
        },
      },
      {
        name: 'Cancelamentos',
        type: 'bar',
        stack: 'total',
        emphasis: {
          focus: 'series',
        },
        data: data.data.map(d => -d.cancellations),
        itemStyle: {
          color: '#EF4444',
          borderRadius: [0, 0, 4, 4],
        },
      },
    ],
  };

  return (
    <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
      <div className="mb-4 flex items-center justify-between">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
          Novos Cadastros vs Cancelamentos
        </h3>
        <div className="flex gap-4 text-right">
          <div>
            <span className="text-sm font-medium text-green-500">
              +{data.totalSignups}
            </span>
            <p className="text-xs text-gray-500 dark:text-gray-400">Cadastros</p>
          </div>
          <div>
            <span className="text-sm font-medium text-red-500">
              -{data.totalCancellations}
            </span>
            <p className="text-xs text-gray-500 dark:text-gray-400">Cancelamentos</p>
          </div>
          <div>
            <span className={`text-sm font-medium ${data.netGrowth >= 0 ? 'text-green-500' : 'text-red-500'}`}>
              {data.netGrowth >= 0 ? '+' : ''}{data.netGrowth}
            </span>
            <p className="text-xs text-gray-500 dark:text-gray-400">Saldo</p>
          </div>
        </div>
      </div>
      <ReactECharts option={option} style={{ height: '300px' }} />
    </div>
  );
};
