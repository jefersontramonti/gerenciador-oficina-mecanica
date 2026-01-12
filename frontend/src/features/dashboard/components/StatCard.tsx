/**
 * Card de estatística reutilizável para o dashboard
 * Memoized to prevent unnecessary re-renders
 */

import { memo } from 'react';
import type { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: string | number;
  icon: LucideIcon;
  iconColor: string;
  iconBgColor: string;
  isLoading?: boolean;
}

export const StatCard = memo(({
  title,
  value,
  icon: Icon,
  iconColor,
  iconBgColor,
  isLoading = false,
}: StatCardProps) => {
  if (isLoading) {
    return (
      <div className="rounded-lg border border-gray-200 bg-white p-3 sm:p-6 shadow-sm dark:border-gray-700 dark:bg-gray-800">
        <div className="animate-pulse">
          <div className="h-3 sm:h-4 w-16 sm:w-24 rounded bg-gray-200 dark:bg-gray-700" />
          <div className="mt-2 h-6 sm:h-8 w-12 sm:w-16 rounded bg-gray-200 dark:bg-gray-700" />
        </div>
      </div>
    );
  }

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-3 sm:p-6 shadow-sm transition-shadow hover:shadow-md dark:border-gray-700 dark:bg-gray-800">
      <div className="flex items-center justify-between gap-2">
        <div className="flex-1 min-w-0">
          <p className="text-xs sm:text-sm font-medium text-gray-600 dark:text-gray-400 truncate">{title}</p>
          <p className="mt-1 sm:mt-2 text-lg sm:text-2xl lg:text-3xl font-bold text-gray-900 dark:text-white truncate">{value}</p>
        </div>
        <div className={`rounded-lg ${iconBgColor} p-2 sm:p-3 flex-shrink-0`}>
          <Icon className={`h-4 w-4 sm:h-6 sm:w-6 ${iconColor}`} />
        </div>
      </div>
    </div>
  );
});

StatCard.displayName = 'StatCard';
