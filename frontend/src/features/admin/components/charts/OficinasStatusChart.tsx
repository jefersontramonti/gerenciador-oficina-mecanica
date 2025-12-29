/**
 * Oficinas Status Chart Component
 * Displays workshop distribution by status
 */

import ReactECharts from 'echarts-for-react';
import type { DashboardMetrics } from '../../types';

interface OficinasStatusChartProps {
  data: DashboardMetrics;
  isLoading?: boolean;
}

export const OficinasStatusChart = ({ data, isLoading }: OficinasStatusChartProps) => {
  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-purple-500 border-t-transparent" />
      </div>
    );
  }

  const chartData = [
    { value: data.oficinasAtivas, name: 'Ativas', itemStyle: { color: '#22C55E' } },
    { value: data.oficinasTrial, name: 'Trial', itemStyle: { color: '#3B82F6' } },
    { value: data.oficinasInativas, name: 'Inativas', itemStyle: { color: '#9CA3AF' } },
    { value: data.oficinasInadimplentes, name: 'Inadimplentes', itemStyle: { color: '#EF4444' } },
  ].filter(item => item.value > 0);

  const total = chartData.reduce((sum, item) => sum + item.value, 0);

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: {
        color: '#9CA3AF',
      },
    },
    series: [
      {
        name: 'Status',
        type: 'pie',
        radius: ['50%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        label: {
          show: true,
          position: 'center',
          formatter: () => `${total}\nTotal`,
          fontSize: 16,
          fontWeight: 'bold',
          color: '#fff',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 18,
            fontWeight: 'bold',
          },
        },
        labelLine: {
          show: false,
        },
        data: chartData,
      },
    ],
  };

  return (
    <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
      <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
        Oficinas por Status
      </h3>
      <ReactECharts option={option} style={{ height: '200px' }} />
    </div>
  );
};
