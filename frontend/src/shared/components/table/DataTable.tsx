import type { ColumnDef, PaginationData, RowAction } from './types';
import { Pagination } from './Pagination';

interface DataTableProps<T> {
  /**
   * Array of data to display
   */
  data: T[];

  /**
   * Column definitions
   */
  columns: ColumnDef<T>[];

  /**
   * Loading state
   */
  isLoading: boolean;

  /**
   * Message to show when no data
   * @default 'Nenhum registro encontrado'
   */
  emptyMessage?: string;

  /**
   * Optional pagination data
   */
  pagination?: PaginationData;

  /**
   * Optional pagination change handler
   */
  onPageChange?: (page: number) => void;

  /**
   * Optional row actions (view, edit, delete, etc)
   */
  actions?: RowAction<T>[];

  /**
   * Optional: Function to get unique key for each row
   * @default (row, index) => index
   */
  getRowKey?: (row: T, index: number) => string | number;

  /**
   * Optional: Row click handler
   */
  onRowClick?: (row: T) => void;

  /**
   * Optional: Additional className for table container
   */
  className?: string;

  /**
   * Optional: Show zebra striping
   * @default false
   */
  striped?: boolean;
}

/**
 * Generic DataTable component
 *
 * Displays data in a table with loading states, empty states, pagination, and actions.
 *
 * @example
 * ```tsx
 * const columns: ColumnDef<Cliente>[] = [
 *   {
 *     id: 'nome',
 *     header: 'Nome',
 *     cell: (row) => row.nome,
 *   },
 *   {
 *     id: 'email',
 *     header: 'Email',
 *     cell: (row) => row.email,
 *   },
 * ];
 *
 * <DataTable
 *   data={clientes}
 *   columns={columns}
 *   isLoading={isLoading}
 *   pagination={pagination}
 *   onPageChange={handlePageChange}
 * />
 * ```
 */
export function DataTable<T>({
  data,
  columns,
  isLoading,
  emptyMessage = 'Nenhum registro encontrado',
  pagination,
  onPageChange,
  actions,
  getRowKey = (_, index) => index,
  onRowClick,
  className = '',
  striped = false,
}: DataTableProps<T>) {
  // Calculate colspan for loading/empty states
  const colspan = columns.length + (actions ? 1 : 0);

  // Get alignment class for header/cell
  const getAlignClass = (align?: 'left' | 'center' | 'right') => {
    switch (align) {
      case 'center':
        return 'text-center';
      case 'right':
        return 'text-right';
      default:
        return 'text-left';
    }
  };

  // Get action button color classes
  const getActionClasses = (variant: RowAction<T>['variant']) => {
    switch (variant) {
      case 'primary':
        return 'text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300';
      case 'secondary':
        return 'text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200';
      case 'danger':
        return 'text-red-600 dark:text-red-400 hover:text-red-800 dark:hover:text-red-300';
      case 'success':
        return 'text-green-600 dark:text-green-400 hover:text-green-800 dark:hover:text-green-300';
      case 'warning':
        return 'text-orange-600 dark:text-orange-400 hover:text-orange-800 dark:hover:text-orange-300';
    }
  };

  return (
    <div className={`overflow-hidden rounded-lg bg-white dark:bg-gray-800 shadow ${className}`}>
      <div className="overflow-x-auto">
        <table className="w-full">
          {/* Header */}
          <thead className="bg-gray-50 dark:bg-gray-900">
            <tr>
              {columns.map((column) => (
                <th
                  key={column.id}
                  className={`
                    px-6 py-3 text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300
                    ${getAlignClass(column.align)}
                    ${column.width || ''}
                    ${column.headerClassName || ''}
                  `}
                >
                  {column.header}
                </th>
              ))}
              {actions && (
                <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300">
                  Ações
                </th>
              )}
            </tr>
          </thead>

          {/* Body */}
          <tbody className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800">
            {/* Loading state */}
            {isLoading ? (
              <tr>
                <td colSpan={colspan} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                  Carregando...
                </td>
              </tr>
            ) : /* Empty state */ data.length === 0 ? (
              <tr>
                <td colSpan={colspan} className="px-6 py-12 text-center text-gray-500 dark:text-gray-400">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              /* Data rows */
              data.map((row, rowIndex) => (
                <tr
                  key={getRowKey(row, rowIndex)}
                  className={`
                    ${onRowClick ? 'cursor-pointer' : ''}
                    ${striped && rowIndex % 2 === 1 ? 'bg-gray-50 dark:bg-gray-900' : ''}
                    hover:bg-gray-50 dark:hover:bg-gray-700
                  `}
                  onClick={() => onRowClick?.(row)}
                >
                  {columns.map((column) => (
                    <td
                      key={column.id}
                      className={`
                        px-6 py-4 text-sm
                        ${getAlignClass(column.align)}
                        ${column.cellClassName || 'text-gray-600 dark:text-gray-400'}
                      `}
                    >
                      {column.cell(row)}
                    </td>
                  ))}

                  {/* Actions column */}
                  {actions && (
                    <td className="px-6 py-4 text-right text-sm">
                      <div className="flex items-center justify-end gap-2">
                        {actions.map((action, actionIndex) => {
                          // Check if action should be shown
                          if (action.show && !action.show(row)) {
                            return null;
                          }

                          const Icon = action.icon;
                          const isDisabled = action.disabled?.(row) || false;

                          return (
                            <button
                              key={actionIndex}
                              onClick={(e) => {
                                e.stopPropagation(); // Prevent row click
                                action.onClick(row);
                              }}
                              className={`
                                ${getActionClasses(action.variant)}
                                ${isDisabled ? 'opacity-50 cursor-not-allowed' : ''}
                              `}
                              title={action.title}
                              disabled={isDisabled}
                            >
                              <Icon className="h-5 w-5" />
                            </button>
                          );
                        })}
                      </div>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {pagination && onPageChange && (
        <Pagination pagination={pagination} onPageChange={onPageChange} />
      )}
    </div>
  );
}
