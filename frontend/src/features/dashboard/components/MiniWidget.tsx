/**
 * Mini Widget expansível para o dashboard
 * Componente base que pode ser colapsado/expandido
 */

import { memo, useState, type ReactNode } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

interface MiniWidgetProps {
  title: string;
  icon: LucideIcon;
  iconColor: string;
  iconBgColor: string;
  summary: ReactNode; // Conteúdo resumido (sempre visível)
  children: ReactNode; // Conteúdo expandido
  defaultExpanded?: boolean;
  isLoading?: boolean;
  badge?: number;
  badgeColor?: string;
}

export const MiniWidget = memo(({
  title,
  icon: Icon,
  iconColor,
  iconBgColor,
  summary,
  children,
  defaultExpanded = false,
  isLoading = false,
  badge,
  badgeColor = 'bg-blue-500',
}: MiniWidgetProps) => {
  const [isExpanded, setIsExpanded] = useState(defaultExpanded);

  if (isLoading) {
    return (
      <div className="rounded-lg border border-gray-200 bg-white shadow-sm dark:border-gray-700 dark:bg-gray-800">
        <div className="p-3 sm:p-4">
          <div className="animate-pulse">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="h-8 w-8 sm:h-10 sm:w-10 rounded-lg bg-gray-200 dark:bg-gray-700" />
                <div className="h-4 w-24 rounded bg-gray-200 dark:bg-gray-700" />
              </div>
              <div className="h-5 w-5 rounded bg-gray-200 dark:bg-gray-700" />
            </div>
            <div className="mt-3 h-16 rounded bg-gray-200 dark:bg-gray-700" />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="group rounded-lg border border-gray-200 bg-white shadow-sm transition-all duration-300 ease-out hover:shadow-lg hover:-translate-y-1 hover:border-gray-300 dark:border-gray-700 dark:bg-gray-800 dark:hover:border-gray-600">
      {/* Header */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full p-3 sm:p-4 flex items-center justify-between cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700/50 rounded-t-lg transition-colors"
        aria-expanded={isExpanded}
      >
        <div className="flex items-center gap-2 sm:gap-3">
          <div className={`relative rounded-lg ${iconBgColor} p-2 transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3`}>
            <Icon className={`h-4 w-4 sm:h-5 sm:w-5 ${iconColor} transition-transform duration-300`} />
            {badge !== undefined && badge > 0 && (
              <span className={`absolute -top-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full ${badgeColor} text-[10px] font-bold text-white transition-transform duration-300 group-hover:scale-110 animate-pulse`}>
                {badge > 99 ? '99+' : badge}
              </span>
            )}
          </div>
          <h3 className="text-sm sm:text-base font-semibold text-gray-900 dark:text-white">
            {title}
          </h3>
        </div>
        <div className="text-gray-400 dark:text-gray-500 transition-transform duration-300 group-hover:scale-110">
          {isExpanded ? (
            <ChevronUp className="h-4 w-4 sm:h-5 sm:w-5 transition-transform duration-200" />
          ) : (
            <ChevronDown className="h-4 w-4 sm:h-5 sm:w-5 transition-transform duration-200" />
          )}
        </div>
      </button>

      {/* Summary (sempre visível) */}
      <div className="px-3 sm:px-4 pb-3 sm:pb-4">
        {summary}
      </div>

      {/* Expanded Content */}
      {isExpanded && (
        <div className="border-t border-gray-200 dark:border-gray-700 p-3 sm:p-4 bg-gray-50 dark:bg-gray-700/30 rounded-b-lg">
          {children}
        </div>
      )}
    </div>
  );
});

MiniWidget.displayName = 'MiniWidget';
