/**
 * Card de estatística com indicador de variação percentual
 * Mostra a variação vs mês anterior (+15% ou -10%)
 */

import { memo } from 'react';
import type { LucideIcon } from 'lucide-react';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

interface StatCardComTrendProps {
  title: string;
  value: string | number;
  icon: LucideIcon;
  iconColor: string;
  iconBgColor: string;
  trend?: number; // Variação percentual: +15.5 ou -10.2
  trendLabel?: string; // "vs mês anterior"
  isLoading?: boolean;
  badge?: number; // Badge numérico opcional
  badgeColor?: string;
}

const formatTrend = (trend: number): string => {
  const sign = trend > 0 ? '+' : '';
  return `${sign}${trend.toFixed(1)}%`;
};

export const StatCardComTrend = memo(({
  title,
  value,
  icon: Icon,
  iconColor,
  iconBgColor,
  trend,
  trendLabel = 'vs mês anterior',
  isLoading = false,
  badge,
  badgeColor = 'bg-red-500',
}: StatCardComTrendProps) => {
  if (isLoading) {
    return (
      <div className="rounded-lg border border-gray-200 bg-white p-3 sm:p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800">
        <div className="animate-pulse">
          <div className="h-3 sm:h-4 w-16 sm:w-24 rounded bg-gray-200 dark:bg-gray-700" />
          <div className="mt-2 h-6 sm:h-8 w-12 sm:w-16 rounded bg-gray-200 dark:bg-gray-700" />
          <div className="mt-2 h-3 w-20 rounded bg-gray-200 dark:bg-gray-700" />
        </div>
      </div>
    );
  }

  const getTrendIcon = () => {
    if (trend === undefined || trend === null) return null;
    if (trend > 0) return TrendingUp;
    if (trend < 0) return TrendingDown;
    return Minus;
  };

  const getTrendColor = () => {
    if (trend === undefined || trend === null) return '';
    if (trend > 0) return 'text-green-600 dark:text-green-400';
    if (trend < 0) return 'text-red-600 dark:text-red-400';
    return 'text-gray-500 dark:text-gray-400';
  };

  const TrendIcon = getTrendIcon();

  return (
    <div className="group rounded-lg border border-gray-200 bg-white p-3 sm:p-6 shadow-sm transition-all duration-300 ease-out hover:shadow-lg hover:-translate-y-1 hover:border-gray-300 dark:border-gray-700 dark:bg-gray-800 dark:hover:border-gray-600 cursor-pointer">
      <div className="flex items-start justify-between gap-2">
        <div className="flex-1 min-w-0">
          <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400 truncate transition-colors group-hover:text-gray-800 dark:group-hover:text-gray-300">
            {title}
          </p>
          <p className="mt-1 sm:mt-2 text-lg sm:text-2xl lg:text-3xl font-bold text-gray-900 dark:text-white truncate">
            {value}
          </p>

          {/* Trend indicator */}
          {trend !== undefined && trend !== null && (
            <div className="mt-2 flex items-center gap-1">
              {TrendIcon && (
                <TrendIcon className={`h-3 w-3 sm:h-4 sm:w-4 ${getTrendColor()} transition-transform duration-300 group-hover:scale-125`} />
              )}
              <span className={`text-xs sm:text-sm font-medium ${getTrendColor()}`}>
                {formatTrend(trend)}
              </span>
              <span className="text-xs text-gray-500 dark:text-gray-400">
                {trendLabel}
              </span>
            </div>
          )}
        </div>

        <div className="relative flex-shrink-0">
          <div className={`rounded-lg ${iconBgColor} p-2 sm:p-3 transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3`}>
            <Icon className={`h-4 w-4 sm:h-6 sm:w-6 ${iconColor} transition-transform duration-300`} />
          </div>

          {/* Badge numérico */}
          {badge !== undefined && badge > 0 && (
            <span className={`absolute -top-1 -right-1 flex h-4 w-4 sm:h-5 sm:w-5 items-center justify-center rounded-full ${badgeColor} text-[10px] sm:text-xs font-bold text-white transition-transform duration-300 group-hover:scale-110 animate-pulse`}>
              {badge > 99 ? '99+' : badge}
            </span>
          )}
        </div>
      </div>
    </div>
  );
});

StatCardComTrend.displayName = 'StatCardComTrend';
