/**
 * Utilitários para formatação de datas
 * Lida com diferentes formatos retornados pelo backend Java (LocalDateTime)
 */

import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

/**
 * Tipo que representa os possíveis formatos de data vindos do backend
 * - string: ISO 8601 (ex: "2025-11-02T15:30:00")
 * - number[]: Array Java LocalDateTime (ex: [2025, 11, 2, 15, 30, 0])
 * - null/undefined: Campo não preenchido
 */
export type BackendDate = string | number[] | null | undefined;

/**
 * Formata uma data do backend para exibição no formato brasileiro
 * @param dateValue Data em qualquer formato aceito pelo backend
 * @param fallback Texto a exibir se a data for nula/inválida (padrão: "Nunca")
 * @returns String formatada (ex: "02/11/2025 às 15:30") ou fallback
 */
export const formatDateTime = (
  dateValue?: BackendDate,
  fallback: string = 'Nunca'
): string => {
  // Se for nulo, undefined ou string vazia
  if (!dateValue) return fallback;

  try {
    let date: Date;

    // Verifica se é array (formato Java LocalDateTime serializado como array)
    if (Array.isArray(dateValue)) {
      const [year, month, day, hour = 0, minute = 0, second = 0] = dateValue;
      date = new Date(year, month - 1, day, hour, minute, second);
    } else {
      // Tenta parsear como string ISO
      date = new Date(dateValue);
    }

    // Verifica se a data é válida
    if (isNaN(date.getTime())) {
      console.error('[dateFormatter] Data inválida:', dateValue);
      return fallback;
    }

    return format(date, "dd/MM/yyyy 'às' HH:mm", { locale: ptBR });
  } catch (error) {
    console.error('[dateFormatter] Erro ao formatar data:', dateValue, error);
    return fallback;
  }
};

/**
 * Formata apenas a data (sem hora)
 * @param dateValue Data em qualquer formato aceito pelo backend
 * @param fallback Texto a exibir se a data for nula/inválida
 * @returns String formatada (ex: "02/11/2025") ou fallback
 */
export const formatDate = (
  dateValue?: BackendDate,
  fallback: string = '-'
): string => {
  if (!dateValue) return fallback;

  try {
    let date: Date;

    if (Array.isArray(dateValue)) {
      const [year, month, day] = dateValue;
      date = new Date(year, month - 1, day);
    } else {
      date = new Date(dateValue);
    }

    if (isNaN(date.getTime())) {
      console.error('[dateFormatter] Data inválida:', dateValue);
      return fallback;
    }

    return format(date, 'dd/MM/yyyy', { locale: ptBR });
  } catch (error) {
    console.error('[dateFormatter] Erro ao formatar data:', dateValue, error);
    return fallback;
  }
};

/**
 * Formata apenas a hora
 * @param dateValue Data em qualquer formato aceito pelo backend
 * @param fallback Texto a exibir se a data for nula/inválida
 * @returns String formatada (ex: "15:30") ou fallback
 */
export const formatTime = (
  dateValue?: BackendDate,
  fallback: string = '-'
): string => {
  if (!dateValue) return fallback;

  try {
    let date: Date;

    if (Array.isArray(dateValue)) {
      const [year, month, day, hour = 0, minute = 0] = dateValue;
      date = new Date(year, month - 1, day, hour, minute);
    } else {
      date = new Date(dateValue);
    }

    if (isNaN(date.getTime())) {
      console.error('[dateFormatter] Data inválida:', dateValue);
      return fallback;
    }

    return format(date, 'HH:mm', { locale: ptBR });
  } catch (error) {
    console.error('[dateFormatter] Erro ao formatar hora:', dateValue, error);
    return fallback;
  }
};
