/**
 * Botões de ação contextuais para Ordem de Serviço
 * Renderiza ações disponíveis baseado no status atual e perfil do usuário
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle, Play, XCircle, TruckIcon, Edit, AlertCircle, Clock, PlayCircle } from 'lucide-react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import type { OrdemServico } from '../types';
import type { ResumoFinanceiro } from '@/features/financeiro/types/pagamento';
import { getAvailableActions, getConfirmationMessage, type ActionType } from '../utils/statusTransitions';
import {
  useAprovarOrdemServico,
  useIniciarOrdemServico,
  useAguardarPecaOrdemServico,
  useRetomarOrdemServico,
  useFinalizarOrdemServico,
  useEntregarOrdemServico,
  useCancelarOrdemServico,
} from '../hooks/useOrdensServico';
import { FinalizarOSModal } from './FinalizarOSModal';

type ToastState = {
  visible: boolean;
  type: 'success' | 'error' | 'warning';
  message: string;
};

interface ActionButtonsProps {
  ordemServico: OrdemServico;
  resumoFinanceiro?: ResumoFinanceiro;
  onActionComplete?: () => void;
}

export const ActionButtons: React.FC<ActionButtonsProps> = ({ ordemServico, resumoFinanceiro, onActionComplete }) => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [showFinalizarModal, setShowFinalizarModal] = useState(false);
  const [showAguardandoPecaModal, setShowAguardandoPecaModal] = useState(false);
  const [cancelMotivo, setCancelMotivo] = useState('');
  const [descricaoPeca, setDescricaoPeca] = useState('');
  const [toast, setToast] = useState<ToastState>({ visible: false, type: 'success', message: '' });

  const showToast = (type: 'success' | 'error' | 'warning', message: string) => {
    setToast({ visible: true, type, message });
  };

  const handleCloseToast = () => {
    setToast(prev => ({ ...prev, visible: false }));
  };

  // Mutations
  const aprovarMutation = useAprovarOrdemServico();
  const iniciarMutation = useIniciarOrdemServico();
  const aguardarPecaMutation = useAguardarPecaOrdemServico();
  const retomarMutation = useRetomarOrdemServico();
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
      showToast('error', 'A OS deve estar quitada antes da entrega');
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

    // Ação de finalizar (mostrar modal para suportar modelo hibrido)
    if (action === 'finalizar') {
      setShowFinalizarModal(true);
      return;
    }

    // Ação de aguardar peça (mostrar modal para informar a peça)
    if (action === 'aguardarPeca') {
      setShowAguardandoPecaModal(true);
      return;
    }

    // Confirmar ação
    if (confirmMessage && !window.confirm(confirmMessage)) {
      return;
    }

    try {
      let successMessage = 'Ação realizada com sucesso!';

      switch (action) {
        case 'aprovar':
          await aprovarMutation.mutateAsync({
            id: ordemServico.id,
            aprovadoPeloCliente: true,
          });
          successMessage = `OS #${ordemServico.numero} aprovada com sucesso!`;
          break;
        case 'iniciar':
          await iniciarMutation.mutateAsync(ordemServico.id);
          successMessage = `OS #${ordemServico.numero} iniciada com sucesso!`;
          break;
        case 'retomar':
          await retomarMutation.mutateAsync(ordemServico.id);
          successMessage = `OS #${ordemServico.numero} retomada com sucesso!`;
          break;
        case 'entregar':
          await entregarMutation.mutateAsync(ordemServico.id);
          successMessage = `Veículo entregue! OS #${ordemServico.numero} concluída.`;
          break;
      }

      showToast('success', successMessage);
      onActionComplete?.();
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message || error.message || 'Erro ao realizar ação';
      showToast('error', `Erro: ${errorMessage}`);
    }
  };

  const handleAguardarPecaConfirm = async () => {
    if (!descricaoPeca.trim()) {
      showToast('warning', 'Informe a descrição da peça aguardada');
      return;
    }

    try {
      await aguardarPecaMutation.mutateAsync({
        id: ordemServico.id,
        data: { descricaoPeca },
      });

      showToast('success', 'OS em aguardando peça!');
      setShowAguardandoPecaModal(false);
      setDescricaoPeca('');
      onActionComplete?.();
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message || error.message || 'Erro ao atualizar status';
      showToast('error', `Erro: ${errorMessage}`);
    }
  };

  const handleCancelConfirm = async () => {
    if (!cancelMotivo.trim()) {
      showToast('warning', 'Informe o motivo do cancelamento');
      return;
    }

    try {
      await cancelarMutation.mutateAsync({
        id: ordemServico.id,
        data: { motivo: cancelMotivo },
      });

      showToast('success', 'OS cancelada com sucesso!');
      setShowCancelModal(false);
      setCancelMotivo('');
      onActionComplete?.();
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message || error.message || 'Erro ao cancelar OS';
      showToast('error', `Erro: ${errorMessage}`);
    }
  };

  const getButtonVariant = (variant: string) => {
    const variants = {
      primary: 'bg-blue-600 text-white hover:bg-blue-700 dark:bg-blue-700 dark:hover:bg-blue-600',
      success: 'bg-green-600 text-white hover:bg-green-700 dark:bg-green-700 dark:hover:bg-green-600',
      warning: 'bg-yellow-600 text-white hover:bg-yellow-700 dark:bg-yellow-700 dark:hover:bg-yellow-600',
      danger: 'border border-red-600 text-red-600 hover:bg-red-50 dark:border-red-500 dark:text-red-400 dark:hover:bg-red-900/20',
    };
    return variants[variant as keyof typeof variants] || variants.primary;
  };

  const getActionIcon = (action: ActionType) => {
    const icons: Record<ActionType, React.ReactNode> = {
      aprovar: <CheckCircle className="h-4 w-4" />,
      iniciar: <Play className="h-4 w-4" />,
      aguardarPeca: <Clock className="h-4 w-4" />,
      retomar: <PlayCircle className="h-4 w-4" />,
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
    aguardarPecaMutation.isPending ||
    retomarMutation.isPending ||
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-lg bg-white p-4 sm:p-6 shadow-xl dark:bg-gray-800">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Cancelar Ordem de Serviço</h3>
            <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
              Informe o motivo do cancelamento da OS #{ordemServico.numero}:
            </p>

            <textarea
              value={cancelMotivo}
              onChange={(e) => setCancelMotivo(e.target.value)}
              className="mt-4 w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white dark:focus:border-red-400"
              rows={4}
              placeholder="Exemplo: Cliente desistiu do serviço"
            />

            <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <button
                type="button"
                onClick={() => {
                  setShowCancelModal(false);
                  setCancelMotivo('');
                }}
                disabled={cancelarMutation.isPending}
                className="w-full sm:w-auto rounded-lg border border-gray-300 px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                Voltar
              </button>
              <button
                type="button"
                onClick={handleCancelConfirm}
                disabled={cancelarMutation.isPending}
                className="w-full sm:w-auto rounded-lg bg-red-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50 dark:bg-red-700 dark:hover:bg-red-800"
              >
                {cancelarMutation.isPending ? 'Cancelando...' : 'Confirmar Cancelamento'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de Aguardando Peça */}
      {showAguardandoPecaModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-lg bg-white p-4 sm:p-6 shadow-xl dark:bg-gray-800">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Aguardar Peça</h3>
            <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
              Informe a peça que está sendo aguardada para a OS #{ordemServico.numero}:
            </p>

            <textarea
              value={descricaoPeca}
              onChange={(e) => setDescricaoPeca(e.target.value)}
              className="mt-4 w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-yellow-500 focus:outline-none focus:ring-2 focus:ring-yellow-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white dark:focus:border-yellow-400"
              rows={3}
              placeholder="Ex: Filtro de óleo Mann W 712/83"
            />

            <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <button
                type="button"
                onClick={() => {
                  setShowAguardandoPecaModal(false);
                  setDescricaoPeca('');
                }}
                disabled={aguardarPecaMutation.isPending}
                className="w-full sm:w-auto rounded-lg border border-gray-300 px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                Voltar
              </button>
              <button
                type="button"
                onClick={handleAguardarPecaConfirm}
                disabled={aguardarPecaMutation.isPending}
                className="w-full sm:w-auto rounded-lg bg-yellow-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-yellow-700 disabled:opacity-50 dark:bg-yellow-700 dark:hover:bg-yellow-800"
              >
                {aguardarPecaMutation.isPending ? 'Salvando...' : 'Confirmar'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de Finalização (suporta VALOR_FIXO e POR_HORA) */}
      <FinalizarOSModal
        os={ordemServico}
        isOpen={showFinalizarModal}
        onClose={() => setShowFinalizarModal(false)}
        onSuccess={() => {
          showToast('success', `OS #${ordemServico.numero} finalizada com sucesso!`);
          onActionComplete?.();
        }}
      />

      {/* Toast Notification - Novo estilo centralizado */}
      <div
        className={`fixed inset-0 flex items-start justify-center z-50 pointer-events-none px-4 pt-4 transition-all duration-500 ease-out ${
          toast.visible ? 'opacity-100' : 'opacity-0'
        }`}
      >
        <div
          className={`pointer-events-auto transform transition-all duration-500 ease-out w-full max-w-xs sm:max-w-sm ${
            toast.visible
              ? 'translate-y-[30vh] sm:translate-y-[40vh] scale-100'
              : '-translate-y-full scale-90'
          }`}
        >
          <div className={`
            flex flex-col items-center gap-2 sm:gap-3 px-5 sm:px-8 py-5 sm:py-6 rounded-xl sm:rounded-2xl shadow-2xl backdrop-blur-sm
            border text-center
            ${toast.type === 'success'
              ? 'bg-slate-800/95 border-slate-700'
              : toast.type === 'warning'
              ? 'bg-slate-800/95 border-yellow-800'
              : 'bg-slate-800/95 border-red-800'
            }
          `}>
            {/* Ícone */}
            <div className={`
              w-12 h-12 sm:w-16 sm:h-16 rounded-full flex items-center justify-center
              ${toast.type === 'success'
                ? 'bg-green-500/20 text-green-400'
                : toast.type === 'warning'
                ? 'bg-yellow-500/20 text-yellow-400'
                : 'bg-red-500/20 text-red-400'
              }
            `}>
              {toast.type === 'success' && <CheckCircle className="h-6 w-6 sm:h-8 sm:w-8" />}
              {toast.type === 'warning' && <AlertCircle className="h-6 w-6 sm:h-8 sm:w-8" />}
              {toast.type === 'error' && <XCircle className="h-6 w-6 sm:h-8 sm:w-8" />}
            </div>

            {/* Título */}
            <h3 className="text-lg sm:text-xl font-bold text-white">
              {toast.type === 'success' ? 'Sucesso!' : toast.type === 'warning' ? 'Atenção!' : 'Erro!'}
            </h3>

            {/* Mensagem */}
            <p className="text-sm sm:text-base text-gray-400 mb-2 sm:mb-4">
              {toast.message}
            </p>

            {/* Botão OK */}
            <button
              onClick={handleCloseToast}
              className={`
                w-full py-2 sm:py-2.5 px-6 rounded-lg font-semibold text-sm sm:text-base transition-colors
                ${toast.type === 'success'
                  ? 'bg-green-600 hover:bg-green-700 active:bg-green-800 text-white'
                  : toast.type === 'warning'
                  ? 'bg-yellow-600 hover:bg-yellow-700 active:bg-yellow-800 text-white'
                  : 'bg-red-600 hover:bg-red-700 active:bg-red-800 text-white'
                }
              `}
            >
              OK
            </button>
          </div>
        </div>
      </div>

      {/* Backdrop quando toast está visível */}
      <div
        className={`fixed inset-0 bg-black/50 backdrop-blur-sm z-40 transition-opacity duration-300 ${
          toast.visible ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}
        onClick={handleCloseToast}
      />
    </>
  );
};
