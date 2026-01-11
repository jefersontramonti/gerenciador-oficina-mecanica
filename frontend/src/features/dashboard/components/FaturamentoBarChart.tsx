/**
 * Gráfico de barras mostrando faturamento mensal (últimos 6 meses)
 * Usa Apache ECharts via echarts-for-react
 */

import { useState, useEffect } from 'react';
import ReactECharts from 'echarts-for-react';
import type { EChartsOption } from 'echarts';
import { useFaturamentoMensal } from '../hooks/useFaturamentoMensal';
import { useTheme } from '@/shared/contexts';

export const FaturamentoBarChart = () => {
  const { data, isLoading, error } = useFaturamentoMensal();
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
          <div className="h-5 sm:h-6 w-32 sm:w-40 rounded bg-gray-200 dark:bg-gray-700" />
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

  // Extrai meses e valores
  const meses = data.map((item) => item.mes);
  const valores = data.map((item) => item.valor);

  const option: EChartsOption = {
    title: {
      text: 'Faturamento Mensal',
      left: 'left',
      textStyle: {
        fontSize: isMobile ? 14 : 16,
        fontWeight: 600,
        color: isDark ? '#f9fafb' : '#1f2937',
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
      backgroundColor: isDark ? 'rgba(31, 41, 55, 0.95)' : 'rgba(255, 255, 255, 0.95)',
      borderColor: isDark ? '#374151' : '#e5e7eb',
      borderWidth: 1,
      textStyle: {
        color: isDark ? '#f9fafb' : '#1f2937',
      },
    },
    grid: {
      left: isMobile ? '2%' : '3%',
      right: isMobile ? '2%' : '4%',
      bottom: isMobile ? '15%' : '10%',
      top: isMobile ? '15%' : '20%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: meses,
      axisLabel: {
        color: isDark ? '#9ca3af' : '#6b7280',
        fontSize: isMobile ? 10 : 12,
        rotate: isMobile ? 60 : 45,
      },
      axisLine: {
        lineStyle: {
          color: isDark ? '#374151' : '#e5e7eb',
        },
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: isDark ? '#9ca3af' : '#6b7280',
        fontSize: isMobile ? 10 : 12,
        formatter: (value: number) => {
          if (isMobile) {
            return `${(value / 1000).toFixed(0)}k`;
          }
          return `R$ ${(value / 1000).toFixed(0)}k`;
        },
      },
      splitLine: {
        lineStyle: {
          color: isDark ? '#374151' : '#f3f4f6',
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
        barMaxWidth: isMobile ? 40 : 60,
      },
    ],
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
