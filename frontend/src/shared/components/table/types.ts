import type { ReactNode } from 'react';

/**
 * Column definition for DataTable
 *
 * @template T - The type of data being displayed in the table
 */
export interface ColumnDef<T> {
  /**
   * Unique identifier for the column
   */
  id: string;

  /**
   * Column header text
   */
  header: string;

  /**
   * Function to render the cell content
   * Can return string, number, or React element
   */
  cell: (row: T) => ReactNode;

  /**
   * Optional alignment for the column
   * @default 'left'
   */
  align?: 'left' | 'center' | 'right';

  /**
   * Optional width class (Tailwind)
   * @example 'w-48', 'w-1/4'
   */
  width?: string;

  /**
   * Optional className for header cell
   */
  headerClassName?: string;

  /**
   * Optional className for data cells
   */
  cellClassName?: string;
}

/**
 * Pagination data from Spring Boot Page response
 */
export interface PaginationData {
  /**
   * Current page number (0-indexed)
   */
  number: number;

  /**
   * Total number of pages
   */
  totalPages: number;

  /**
   * Total number of elements across all pages
   */
  totalElements: number;

  /**
   * Whether this is the first page
   */
  first: boolean;

  /**
   * Whether this is the last page
   */
  last: boolean;

  /**
   * Number of elements in current page
   */
  size: number;

  /**
   * Number of elements in current page content
   */
  numberOfElements: number;
}

/**
 * Action button configuration for DataTable rows
 */
export interface RowAction<T> {
  /**
   * Icon component to render
   */
  icon: React.ComponentType<{ className?: string }>;

  /**
   * Tooltip/title for the action
   */
  title: string;

  /**
   * Click handler
   */
  onClick: (row: T) => void;

  /**
   * Color variant
   */
  variant: 'primary' | 'secondary' | 'danger' | 'success' | 'warning';

  /**
   * Optional: only show action if condition is true
   */
  show?: (row: T) => boolean;

  /**
   * Optional: disable action based on condition
   */
  disabled?: (row: T) => boolean;
}
