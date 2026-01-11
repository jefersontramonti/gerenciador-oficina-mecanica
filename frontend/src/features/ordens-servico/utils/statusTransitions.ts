/**
 * Utilitários para gerenciamento de transições de status de Ordens de Serviço
 * Implementa máquina de estados com transições válidas
 */

import { StatusOS } from '../types';
import type { PerfilUsuario } from '@/features/auth/types';

/**
 * Mapeamento de transições válidas por status
 * Cada status tem uma lista de status para os quais pode transitar
 */
export const STATUS_TRANSITIONS: Record<StatusOS, StatusOS[]> = {
  [StatusOS.ORCAMENTO]: [StatusOS.APROVADO, StatusOS.CANCELADO],
  [StatusOS.APROVADO]: [StatusOS.EM_ANDAMENTO, StatusOS.CANCELADO],
  [StatusOS.EM_ANDAMENTO]: [StatusOS.AGUARDANDO_PECA, StatusOS.FINALIZADO, StatusOS.CANCELADO],
  [StatusOS.AGUARDANDO_PECA]: [StatusOS.EM_ANDAMENTO, StatusOS.CANCELADO],
  [StatusOS.FINALIZADO]: [StatusOS.ENTREGUE],
  [StatusOS.ENTREGUE]: [], // Estado final
  [StatusOS.CANCELADO]: [], // Estado final
};

/**
 * Cores para badges de status (Tailwind CSS com dark mode)
 */
export const STATUS_COLORS: Record<
  StatusOS,
  {
    bg: string;
    text: string;
    border: string;
  }
> = {
  [StatusOS.ORCAMENTO]: {
    bg: 'bg-blue-100 dark:bg-blue-900/30',
    text: 'text-blue-800 dark:text-blue-300',
    border: 'border-blue-300 dark:border-blue-700',
  },
  [StatusOS.APROVADO]: {
    bg: 'bg-green-100 dark:bg-green-900/30',
    text: 'text-green-800 dark:text-green-300',
    border: 'border-green-300 dark:border-green-700',
  },
  [StatusOS.EM_ANDAMENTO]: {
    bg: 'bg-yellow-100 dark:bg-yellow-900/30',
    text: 'text-yellow-800 dark:text-yellow-300',
    border: 'border-yellow-300 dark:border-yellow-700',
  },
  [StatusOS.AGUARDANDO_PECA]: {
    bg: 'bg-orange-100 dark:bg-orange-900/30',
    text: 'text-orange-800 dark:text-orange-300',
    border: 'border-orange-300 dark:border-orange-700',
  },
  [StatusOS.FINALIZADO]: {
    bg: 'bg-purple-100 dark:bg-purple-900/30',
    text: 'text-purple-800 dark:text-purple-300',
    border: 'border-purple-300 dark:border-purple-700',
  },
  [StatusOS.ENTREGUE]: {
    bg: 'bg-gray-100 dark:bg-gray-700',
    text: 'text-gray-800 dark:text-gray-300',
    border: 'border-gray-300 dark:border-gray-600',
  },
  [StatusOS.CANCELADO]: {
    bg: 'bg-red-100 dark:bg-red-900/30',
    text: 'text-red-800 dark:text-red-300',
    border: 'border-red-300 dark:border-red-700',
  },
};

/**
 * Labels amigáveis para cada status
 */
export const STATUS_LABELS: Record<StatusOS, string> = {
  [StatusOS.ORCAMENTO]: 'Orçamento',
  [StatusOS.APROVADO]: 'Aprovado',
  [StatusOS.EM_ANDAMENTO]: 'Em Andamento',
  [StatusOS.AGUARDANDO_PECA]: 'Aguardando Peça',
  [StatusOS.FINALIZADO]: 'Finalizado',
  [StatusOS.ENTREGUE]: 'Entregue',
  [StatusOS.CANCELADO]: 'Cancelado',
};

/**
 * Ações disponíveis por status
 */
export type ActionType = 'aprovar' | 'iniciar' | 'aguardarPeca' | 'retomar' | 'finalizar' | 'entregar' | 'cancelar' | 'editar';

export interface ActionConfig {
  type: ActionType;
  label: string;
  icon?: string;
  variant: 'primary' | 'success' | 'warning' | 'danger';
  requiredRoles?: PerfilUsuario[];
}

/**
 * Verifica se uma transição de status é válida
 */
export const canTransitionTo = (from: StatusOS, to: StatusOS): boolean => {
  return STATUS_TRANSITIONS[from]?.includes(to) ?? false;
};

/**
 * Verifica se uma OS pode ser editada (apenas ORCAMENTO e APROVADO)
 */
export const canEdit = (status: StatusOS): boolean => {
  return status === StatusOS.ORCAMENTO || status === StatusOS.APROVADO;
};

/**
 * Verifica se uma OS pode ser cancelada (qualquer status exceto finais)
 */
export const canCancel = (status: StatusOS): boolean => {
  return status !== StatusOS.ENTREGUE && status !== StatusOS.CANCELADO;
};

/**
 * Retorna as ações disponíveis para um status específico
 */
export const getAvailableActions = (
  currentStatus: StatusOS,
  userRole?: PerfilUsuario
): ActionConfig[] => {
  const actions: ActionConfig[] = [];

  // Editar (apenas ORCAMENTO e APROVADO)
  if (canEdit(currentStatus)) {
    actions.push({
      type: 'editar',
      label: 'Editar',
      variant: 'primary',
      requiredRoles: ['ADMIN', 'GERENTE', 'ATENDENTE'],
    });
  }

  // Aprovar (ORCAMENTO → APROVADO)
  if (canTransitionTo(currentStatus, StatusOS.APROVADO)) {
    actions.push({
      type: 'aprovar',
      label: 'Aprovar Orçamento',
      variant: 'success',
      requiredRoles: ['ADMIN', 'GERENTE', 'ATENDENTE'],
    });
  }

  // Iniciar (APROVADO → EM_ANDAMENTO) ou Retomar (AGUARDANDO_PECA → EM_ANDAMENTO)
  if (canTransitionTo(currentStatus, StatusOS.EM_ANDAMENTO)) {
    if (currentStatus === StatusOS.AGUARDANDO_PECA) {
      actions.push({
        type: 'retomar',
        label: 'Retomar Serviço',
        variant: 'primary',
        requiredRoles: ['ADMIN', 'GERENTE', 'MECANICO'],
      });
    } else {
      actions.push({
        type: 'iniciar',
        label: 'Iniciar Serviço',
        variant: 'primary',
        requiredRoles: ['ADMIN', 'GERENTE', 'MECANICO'],
      });
    }
  }

  // Aguardar Peça (EM_ANDAMENTO → AGUARDANDO_PECA)
  if (canTransitionTo(currentStatus, StatusOS.AGUARDANDO_PECA)) {
    actions.push({
      type: 'aguardarPeca',
      label: 'Aguardar Peça',
      variant: 'warning',
      requiredRoles: ['ADMIN', 'GERENTE', 'MECANICO'],
    });
  }

  // Finalizar (EM_ANDAMENTO → FINALIZADO)
  if (canTransitionTo(currentStatus, StatusOS.FINALIZADO)) {
    actions.push({
      type: 'finalizar',
      label: 'Finalizar Serviço',
      variant: 'success',
      requiredRoles: ['ADMIN', 'GERENTE', 'MECANICO'],
    });
  }

  // Entregar (FINALIZADO → ENTREGUE)
  if (canTransitionTo(currentStatus, StatusOS.ENTREGUE)) {
    actions.push({
      type: 'entregar',
      label: 'Entregar Veículo',
      variant: 'success',
      requiredRoles: ['ADMIN', 'GERENTE', 'ATENDENTE'],
    });
  }

  // Cancelar (qualquer status não final)
  if (canCancel(currentStatus)) {
    actions.push({
      type: 'cancelar',
      label: 'Cancelar OS',
      variant: 'danger',
      requiredRoles: ['ADMIN', 'GERENTE'],
    });
  }

  // Filtrar por role do usuário (se fornecido)
  if (userRole) {
    return actions.filter(
      (action) => !action.requiredRoles || action.requiredRoles.includes(userRole)
    );
  }

  return actions;
};

/**
 * Retorna a próxima transição padrão (fluxo feliz)
 */
export const getNextDefaultStatus = (currentStatus: StatusOS): StatusOS | null => {
  const transitions = STATUS_TRANSITIONS[currentStatus];
  if (!transitions || transitions.length === 0) return null;

  // Fluxo feliz (ignora CANCELADO e AGUARDANDO_PECA)
  const happyPath = transitions.filter(
    (status) => status !== StatusOS.CANCELADO && status !== StatusOS.AGUARDANDO_PECA
  );

  return happyPath.length > 0 ? happyPath[0] : null;
};

/**
 * Verifica se um status é final (não tem mais transições)
 */
export const isFinalStatus = (status: StatusOS): boolean => {
  return STATUS_TRANSITIONS[status].length === 0;
};

/**
 * Retorna a ordem do status no fluxo (para ordenação e timeline)
 */
export const getStatusOrder = (status: StatusOS): number => {
  const order: Record<StatusOS, number> = {
    [StatusOS.ORCAMENTO]: 1,
    [StatusOS.APROVADO]: 2,
    [StatusOS.EM_ANDAMENTO]: 3,
    [StatusOS.AGUARDANDO_PECA]: 3.5, // Mesmo nível que EM_ANDAMENTO
    [StatusOS.FINALIZADO]: 4,
    [StatusOS.ENTREGUE]: 5,
    [StatusOS.CANCELADO]: 99, // Último (status de erro)
  };

  return order[status] ?? 0;
};

/**
 * Retorna mensagem de confirmação para uma ação
 */
export const getConfirmationMessage = (action: ActionType): string => {
  const messages: Record<ActionType, string> = {
    aprovar: 'Tem certeza que deseja aprovar este orçamento?',
    iniciar: 'Tem certeza que deseja iniciar este serviço?',
    aguardarPeca: '', // Modal vai pedir descrição da peça
    retomar: 'Tem certeza que deseja retomar a execução do serviço?',
    finalizar:
      'Tem certeza que deseja finalizar este serviço? Esta ação irá baixar as peças do estoque.',
    entregar:
      'Tem certeza que deseja marcar este veículo como entregue? Certifique-se de que o pagamento foi realizado.',
    cancelar: 'Tem certeza que deseja cancelar esta OS? Informe o motivo abaixo.',
    editar: '',
  };

  return messages[action] ?? 'Tem certeza que deseja realizar esta ação?';
};
