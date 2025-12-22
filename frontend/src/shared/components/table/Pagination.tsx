import type { PaginationData } from './types';

interface PaginationProps {
  /**
   * Pagination data from Spring Boot Page response
   */
  pagination: PaginationData;

  /**
   * Callback when page changes
   */
  onPageChange: (page: number) => void;

  /**
   * Optional: show pagination even on single page
   * @default false
   */
  showOnSinglePage?: boolean;
}

/**
 * Pagination component for DataTable
 *
 * Displays page info and navigation buttons
 */
export function Pagination({ pagination, onPageChange, showOnSinglePage = false }: PaginationProps) {
  const { number, totalPages, totalElements, first, last } = pagination;

  // Don't render if only one page (unless showOnSinglePage is true)
  if (totalPages <= 1 && !showOnSinglePage) {
    return null;
  }

  return (
    <div className="flex items-center justify-between border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 px-6 py-3">
      {/* Page info */}
      <div className="text-sm text-gray-700 dark:text-gray-300">
        Página {number + 1} de {totalPages} ({totalElements} total)
      </div>

      {/* Navigation buttons */}
      <div className="flex gap-2">
        <button
          onClick={() => onPageChange(number - 1)}
          disabled={first}
          className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:cursor-not-allowed disabled:opacity-50"
          aria-label="Página anterior"
        >
          Anterior
        </button>
        <button
          onClick={() => onPageChange(number + 1)}
          disabled={last}
          className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:cursor-not-allowed disabled:opacity-50"
          aria-label="Próxima página"
        >
          Próxima
        </button>
      </div>
    </div>
  );
}
