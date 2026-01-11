/**
 * Gráfico de pizza mostrando distribuição de OS por status
 * Usa Apache ECharts via echarts-for-react
 */

import { useState, useEffect } from 'react';
import ReactECharts from 'echarts-for-react';
import type { EChartsOption } from 'echarts';
import { useOSByStatus } from '../hooks/useOSByStatus';
import { useTheme } from '@/shared/contexts';

export const OSStatusPieChart = () => {
  const { data, isLoading, error } = useOSByStatus();
  const { theme } = useTheme();
  const [isMobile, setIsMobile] = useState(window.innerWidth < 640);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 640);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const isDark = theme === 'dark';

  if (isLoading) {
    return (
      <div className="rounded-lg border border-gray-200 bg-white p-4 sm:p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800">
        <div className="animate-pulse">
          <div className="h-5 sm:h-6 w-32 rounded bg-gray-200 dark:bg-gray-700" />
          <div className="mt-4 h-48 sm:h-64 rounded bg-gray-200 dark:bg-gray-700" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4 sm:p-6 text-center text-red-600 dark:border-red-800 dark:bg-red-900/20 dark:text-red-400">
        <p className="font-medium">Erro ao carregar gráfico</p>
        <p className="mt-1 text-sm">Tente novamente mais tarde</p>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="rounded-lg border border-gray-200 bg-white p-4 sm:p-6 text-center text-gray-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400">
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
      text: 'OS por Status',
      left: 'left',
      textStyle: {
        fontSize: isMobile ? 14 : 16,
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
      orient: isMobile ? 'horizontal' : 'vertical',
      right: isMobile ? 'center' : 16,
      left: isMobile ? 'center' : undefined,
      top: isMobile ? 'bottom' : 'middle',
      bottom: isMobile ? 0 : undefined,
      textStyle: {
        fontSize: isMobile ? 10 : 12,
        color: isDark ? '#d1d5db' : '#4b5563',
      },
      itemGap: isMobile ? 8 : 12,
      itemWidth: isMobile ? 14 : 25,
      itemHeight: isMobile ? 10 : 14,
    },
    series: [
      {
        name: 'Status',
        type: 'pie',
        radius: isMobile ? ['30%', '55%'] : ['40%', '70%'],
        center: isMobile ? ['50%', '40%'] : ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: isMobile ? 4 : 8,
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
            fontSize: isMobile ? 14 : 20,
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
    <div className="rounded-lg border border-gray-200 bg-white p-4 sm:p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800">
      <ReactECharts
        option={option}
        style={{ height: isMobile ? '280px' : '350px', width: '100%' }}
        notMerge={true}
        lazyUpdate={true}
        opts={{ renderer: 'canvas' }}
      />
    </div>
  );
};
