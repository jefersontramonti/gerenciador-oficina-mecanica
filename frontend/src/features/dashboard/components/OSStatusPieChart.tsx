/**
 * Gráfico de pizza mostrando distribuição de OS por status
 * Usa Apache ECharts via echarts-for-react
 */

import ReactECharts from 'echarts-for-react';
import type { EChartsOption } from 'echarts';
import { useOSByStatus } from '../hooks/useOSByStatus';
import { useTheme } from '@/shared/contexts';

export const OSStatusPieChart = () => {
  const { data, isLoading, error } = useOSByStatus();
  const { theme } = useTheme();

  const isDark = theme === 'dark';

  if (isLoading) {
    return (
      <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800">
        <div className="animate-pulse">
          <div className="h-6 w-32 rounded bg-gray-200 dark:bg-gray-700" />
          <div className="mt-4 h-64 rounded bg-gray-200 dark:bg-gray-700" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-6 text-center text-red-600 dark:border-red-800 dark:bg-red-900/20 dark:text-red-400">
        <p className="font-medium">Erro ao carregar gráfico</p>
        <p className="mt-1 text-sm">Tente novamente mais tarde</p>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="rounded-lg border border-gray-200 bg-white p-6 text-center text-gray-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400">
        <p>Nenhum dado disponível</p>
      </div>
    );
  }

  // Transforma dados para formato do ECharts
  const chartData = data.map((item) => ({
    name: item.label,
    value: item.count,
    itemStyle: { color: item.color },
  }));

  const option: EChartsOption = {
    title: {
      text: 'Ordens de Serviço por Status',
      left: 'left',
      textStyle: {
        fontSize: 16,
        fontWeight: 600,
        color: isDark ? '#f9fafb' : '#1f2937',
      },
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      backgroundColor: isDark ? 'rgba(31, 41, 55, 0.95)' : 'rgba(255, 255, 255, 0.95)',
      borderColor: isDark ? '#374151' : '#e5e7eb',
      borderWidth: 1,
      textStyle: {
        color: isDark ? '#f9fafb' : '#1f2937',
      },
    },
    legend: {
      orient: 'vertical',
      right: 16,
      top: 'middle',
      textStyle: {
        fontSize: 12,
        color: isDark ? '#d1d5db' : '#4b5563',
      },
      itemGap: 12,
    },
    series: [
      {
        name: 'Status',
        type: 'pie',
        radius: ['40%', '70%'], // Donut chart
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: isDark ? '#1f2937' : '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
          position: 'center',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold',
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
        labelLine: {
          show: false,
        },
        data: chartData,
      },
    ],
    // Responsivo
    grid: {
      containLabel: true,
    },
  };

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800">
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
