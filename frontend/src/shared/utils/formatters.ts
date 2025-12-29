/**
 * Funções utilitárias para formatação de dados
 */

/**
 * Converts various date formats to Date object.
 * Handles: string, Date, array (Jackson LocalDateTime format), null/undefined
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const parseDate = (date: any): Date | null => {
  if (!date) return null;

  // Already a Date
  if (date instanceof Date) return date;

  // Array format from Jackson (e.g., [2025, 12, 26, 23, 57, 13])
  if (Array.isArray(date)) {
    try {
      // Jackson arrays: [year, month, day, hour?, minute?, second?, nano?]
      // Month in Jackson is 1-based, but Date constructor expects 0-based
      const [year, month, day, hour = 0, minute = 0, second = 0] = date;
      return new Date(year, month - 1, day, hour, minute, second);
    } catch {
      return null;
    }
  }

  // String format
  if (typeof date === 'string') {
    try {
      return new Date(date);
    } catch {
      return null;
    }
  }

  return null;
};

/**
 * Formata um número como moeda brasileira (R$)
 */
export const formatCurrency = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return 'R$ 0,00';

  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

/**
 * Formata uma data/hora no padrão brasileiro
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const formatDateTime = (date: any): string => {
  const dateObj = parseDate(date);
  if (!dateObj || isNaN(dateObj.getTime())) return '-';

  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(dateObj);
};

/**
 * Formata apenas a data no padrão brasileiro
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const formatDate = (date: any): string => {
  const dateObj = parseDate(date);
  if (!dateObj || isNaN(dateObj.getTime())) return '-';

  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(dateObj);
};

/**
 * Formata um número como percentual
 */
export const formatPercentage = (value: number | null | undefined, decimals = 2): string => {
  if (value === null || value === undefined) return '0%';

  return `${value.toFixed(decimals)}%`;
};

/**
 * Formata um número com separador de milhares
 */
export const formatNumber = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return '0';

  return new Intl.NumberFormat('pt-BR').format(value);
};
