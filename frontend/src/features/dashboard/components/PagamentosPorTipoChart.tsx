/**
 * Gráfico de rosca mostrando pagamentos por tipo no mês
 * Usa Apache ECharts via echarts-for-react
 */

import { useState, useEffect } from 'react';
import ReactECharts from 'echarts-for-react';
import type { EChartsOption } from 'echarts';
import { usePagamentosPorTipo } from '../hooks/useDashboardWidgets';
import { useTheme } from '@/shared/contexts';

const TIPO_COLORS: Record<string, string> = {
  DINHEIRO: '#10b981', // Verde
  PIX: '#06b6d4', // Ciano
  CARTAO_CREDITO: '#8b5cf6', // Roxo
  CARTAO_DEBITO: '#f59e0b', // Laranja
  TRANSFERENCIA: '#3b82f6', // Azul
  BOLETO: '#6b7280', // Cinza
};

const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value);
};

export const PagamentosPorTipoChart = () => {
  const { data, isLoading, error } = usePagamentosPorTipo();
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
        <p className="font-medium">Pagamentos por Tipo</p>
        <p className="mt-4 text-sm">Nenhum pagamento no mês</p>
      </div>
    );
  }

  // Prepara dados para o gráfico
  const chartData = data.map((item) => ({
    name: item.label,
    value: item.valor,
    quantidade: item.quantidade,
    itemStyle: {
      color: TIPO_COLORS[item.tipo] || '#6b7280',
    },
  }));

  // Calcula total para exibir no centro
  const total = data.reduce((sum, item) => sum + item.valor, 0);

  const option: EChartsOption = {
    title: {
      text: 'Pagamentos por Tipo',
      subtext: `Total: ${formatCurrency(total)}`,
      left: 'center',
      top: '0%',
      textStyle: {
        fontSize: isMobile ? 14 : 16,
        fontWeight: 600,
        color: isDark ? '#f9fafb' : '#1f2937',
      },
      subtextStyle: {
        fontSize: isMobile ? 12 : 14,
        color: isDark ? '#9ca3af' : '#6b7280',
      },
    },
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const item = data.find((d) => d.label === params.name);
        return `
          <div style="font-weight: 600">${params.name}</div>
          <div>${formatCurrency(params.value)}</div>
          <div style="font-size: 12px; color: ${isDark ? '#9ca3af' : '#6b7280'}">
            ${item?.quantidade || 0} pagamento(s) • ${params.percent?.toFixed(1)}%
          </div>
        `;
      },
      backgroundColor: isDark ? 'rgba(31, 41, 55, 0.95)' : 'rgba(255, 255, 255, 0.95)',
      borderColor: isDark ? '#374151' : '#e5e7eb',
      borderWidth: 1,
      textStyle: {
        color: isDark ? '#f9fafb' : '#1f2937',
      },
    },
    legend: {
      orient: isMobile ? 'horizontal' : 'vertical',
      left: isMobile ? 'center' : 'right',
      top: isMobile ? 'bottom' : 'middle',
      itemWidth: 10,
      itemHeight: 10,
      itemGap: isMobile ? 8 : 12,
      textStyle: {
        color: isDark ? '#9ca3af' : '#6b7280',
        fontSize: isMobile ? 10 : 12,
      },
    },
    series: [
      {
        name: 'Pagamentos',
        type: 'pie',
        radius: isMobile ? ['35%', '55%'] : ['40%', '60%'],
        center: isMobile ? ['50%', '45%'] : ['40%', '55%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: isDark ? '#1f2937' : '#ffffff',
          borderWidth: 2,
        },
        label: {
          show: !isMobile,
          position: 'outside',
          formatter: '{b}: {d}%',
          color: isDark ? '#d1d5db' : '#374151',
          fontSize: 11,
        },
        labelLine: {
          show: !isMobile,
          length: 10,
          length2: 15,
          lineStyle: {
            color: isDark ? '#4b5563' : '#d1d5db',
          },
        },
        emphasis: {
          label: {
            show: true,
            fontSize: isMobile ? 12 : 14,
            fontWeight: 'bold',
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.3)',
          },
        },
        data: chartData,
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
