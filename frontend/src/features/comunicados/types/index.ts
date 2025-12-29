/**
 * Types for Comunicados (Oficina side)
 */

export interface ComunicadoOficina {
  id: string;
  titulo: string;
  resumo?: string;
  tipo: string;
  tipoDescricao: string;
  prioridade: string;
  prioridadeDescricao: string;
  dataEnvio: string | number;
  visualizado: boolean;
  dataVisualizacao?: string | number;
  confirmado: boolean;
  dataConfirmacao?: string | number;
  requerConfirmacao: boolean;
  autorNome: string;
}

export interface ComunicadoOficinaDetail extends ComunicadoOficina {
  conteudo: string;
}

export interface ComunicadoAlerta {
  totalNaoLidos: number;
  pendentesConfirmacao: number;
  urgentes: ComunicadoOficina[];
  temAlerta: boolean;
}

export const prioridadeColors: Record<string, string> = {
  BAIXA: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
  NORMAL: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300',
  ALTA: 'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-300',
  URGENTE: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300',
};

export const tipoIcons: Record<string, string> = {
  NOVIDADE: '🎉',
  ATUALIZACAO: '🔄',
  MANUTENCAO: '🔧',
  PROMOCAO: '🏷️',
  ALERTA: '⚠️',
  OUTRO: '📢',
};
