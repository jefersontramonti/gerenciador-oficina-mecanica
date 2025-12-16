/**
 * Botões de ação contextuais para Ordem de Serviço
 * Renderiza ações disponíveis baseado no status atual e perfil do usuário
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle, Play, XCircle, TruckIcon, Edit, AlertCircle } from 'lucide-react';
import { showError, showSuccess, showWarning } from '@/shared/utils/notifications';
import { useAuth } from '@/features/auth/hooks/useAuth';
import type { OrdemServico } from '../types';
import type { ResumoFinanceiro } from '@/features/financeiro/types/pagamento';
import { getAvailableActions, getConfirmationMessage, type ActionType } from '../utils/statusTransitions';
import {
  useAprovarOrdemServico,
  useIniciarOrdemServico,
  useFinalizarOrdemServico,
  useEntregarOrdemServico,
  useCancelarOrdemServico,
} from '../hooks/useOrdensServico';

interface ActionButtonsProps {
  ordemServico: OrdemServico;
  resumoFinanceiro?: ResumoFinanceiro;
  onActionComplete?: () => void;
}

export const ActionButtons: React.FC<ActionButtonsProps> = ({ ordemServico, resumoFinanceiro, onActionComplete }) => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [cancelMotivo, setCancelMotivo] = useState('');

  // Mutations
  const aprovarMutation = useAprovarOrdemServico();
  const iniciarMutation = useIniciarOrdemServico();
  const finalizarMutation = useFinalizarOrdemServico();
  const entregarMutation = useEntregarOrdemServico();
  const cancelarMutation = useCancelarOrdemServico();

  // Obter ações disponíveis baseado no status e perfil do usuário
  const availableActions = getAvailableActions(ordemServico.status, user?.perfil);

  if (availableActions.length === 0) {
    return null;
  }

  const handleAction = async (action: ActionType) => {
    // Validação de pagamento para entrega
    if (action === 'entregar' && !resumoFinanceiro?.quitada) {
      showError('A OS deve estar quitada antes da entrega');
      return;
    }

    const confirmMessage = getConfirmationMessage(action);

    // Ação de editar (navegar para página de edição)
    if (action === 'editar') {
      navigate(`/ordens-servico/${ordemServico.id}/editar`);
      return;
    }

    // Ação de cancelar (mostrar modal)
    if (action === 'cancelar') {
      setShowCancelModal(true);
      return;
    }

    // Confirmar ação
    if (confirmMessage && !window.confirm(confirmMessage)) {
      return;
    }

    try {
      switch (action) {
        case 'aprovar':
          await aprovarMutation.mutateAsync({
            id: ordemServico.id,
            aprovadoPeloCliente: true,
          });
          break;
        case 'iniciar':
          await iniciarMutation.mutateAsync(ordemServico.id);
          break;
        case 'finalizar':
          await finalizarMutation.mutateAsync(ordemServico.id);
          break;
        case 'entregar':
          await entregarMutation.mutateAsync(ordemServico.id);
          break;
      }

      showSuccess('Ação realizada com sucesso!');
      onActionComplete?.();
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message || error.message || 'Erro ao realizar ação';
      showError(`Erro: ${errorMessage}`);
    }
  };

  const handleCancelConfirm = async () => {
    if (!cancelMotivo.trim()) {
      showWarning('Informe o motivo do cancelamento');
      return;
    }

    try {
      await cancelarMutation.mutateAsync({
        id: ordemServico.id,
        data: { motivo: cancelMotivo },
      });

      showSuccess('OS cancelada com sucesso!');
      setShowCancelModal(false);
      setCancelMotivo('');
      onActionComplete?.();
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message || error.message || 'Erro ao cancelar OS';
      showError(`Erro: ${errorMessage}`);
    }
  };

  const getButtonVariant = (variant: string) => {
    const variants = {
      primary: 'bg-blue-600 text-white hover:bg-blue-700',
      success: 'bg-green-600 text-white hover:bg-green-700',
      warning: 'bg-yellow-600 text-white hover:bg-yellow-700',
      danger: 'border border-red-600 text-red-600 hover:bg-red-50',
    };
    return variants[variant as keyof typeof variants] || variants.primary;
  };

  const getActionIcon = (action: ActionType) => {
    const icons: Record<ActionType, React.ReactNode> = {
      aprovar: <CheckCircle className="h-4 w-4" />,
      iniciar: <Play className="h-4 w-4" />,
      finalizar: <CheckCircle className="h-4 w-4" />,
      entregar: <TruckIcon className="h-4 w-4" />,
      cancelar: <XCircle className="h-4 w-4" />,
      editar: <Edit className="h-4 w-4" />,
    };
    return icons[action];
  };

  const isLoading =
    aprovarMutation.isPending ||
    iniciarMutation.isPending ||
    finalizarMutation.isPending ||
    entregarMutation.isPending ||
    cancelarMutation.isPending;

  return (
    <>
      <div className="flex flex-wrap gap-3">
        {availableActions.map((action) => {
          const isEntregarDisabled = action.type === 'entregar' && !resumoFinanceiro?.quitada;

          return (
            <div key={action.type} className="relative">
              <button
                type="button"
                onClick={() => handleAction(action.type)}
                disabled={isLoading || isEntregarDisabled}
                className={`flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${getButtonVariant(
                  action.variant
                )}`}
                title={
                  isEntregarDisabled
                    ? 'A OS deve estar quitada antes da entrega'
                    : undefined
                }
              >
                {getActionIcon(action.type)}
                {action.label}
              </button>

              {/* Alerta visual para entrega bloqueada */}
              {isEntregarDisabled && (
                <div className="absolute -right-1 -top-1" title="Pagamento pendente">
                  <AlertCircle className="h-4 w-4 text-red-600" />
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Modal de Cancelamento */}
      {showCancelModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
            <h3 className="text-lg font-semibold text-gray-900">Cancelar Ordem de Serviço</h3>
            <p className="mt-2 text-sm text-gray-600">
              Informe o motivo do cancelamento da OS #{ordemServico.numero}:
            </p>

            <textarea
              value={cancelMotivo}
              onChange={(e) => setCancelMotivo(e.target.value)}
              className="mt-4 w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500/20"
              rows={4}
              placeholder="Exemplo: Cliente desistiu do serviço"
            />

            <div className="mt-6 flex justify-end gap-3">
              <button
                type="button"
                onClick={() => {
                  setShowCancelModal(false);
                  setCancelMotivo('');
                }}
                disabled={cancelarMutation.isPending}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleCancelConfirm}
                disabled={cancelarMutation.isPending}
                className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
              >
                {cancelarMutation.isPending ? 'Cancelando...' : 'Confirmar Cancelamento'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
