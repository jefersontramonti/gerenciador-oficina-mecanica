import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Calendar,
  Clock,
  Car,
  User,
  Phone,
  Mail,
  CheckCircle,
  XCircle,
  Edit,
  Trash2,
  CalendarCheck,
} from 'lucide-react';
import {
  useAgendamento,
  useConfirmarAgendamento,
  useCancelarAgendamento,
  useDeletarAgendamento,
} from '../hooks/useManutencaoPreventiva';
import { showSuccess, showError } from '@/shared/utils/notifications';
import type { StatusAgendamento } from '../types';

export default function AgendamentoDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [motivoCancelamento, setMotivoCancelamento] = useState('');

  const { data: agendamento, isLoading, error } = useAgendamento(id);
  const confirmarMutation = useConfirmarAgendamento();
  const cancelarMutation = useCancelarAgendamento();
  const deletarMutation = useDeletarAgendamento();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error || !agendamento) {
    return (
      <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
        Erro ao carregar agendamento. Tente novamente.
      </div>
    );
  }

  const handleConfirmar = async () => {
    try {
      await confirmarMutation.mutateAsync(id!);
      showSuccess('Agendamento confirmado com sucesso');
      setShowConfirmModal(false);
    } catch (error) {
      console.error('Erro ao confirmar:', error);
      showError('Erro ao confirmar agendamento');
    }
  };

  const handleCancelar = async () => {
    try {
      await cancelarMutation.mutateAsync({ id: id!, motivo: motivoCancelamento });
      showSuccess('Agendamento cancelado');
      setShowCancelModal(false);
      setMotivoCancelamento('');
    } catch (error) {
      console.error('Erro ao cancelar:', error);
      showError('Erro ao cancelar agendamento');
    }
  };

  const handleDeletar = async () => {
    try {
      await deletarMutation.mutateAsync(id!);
      showSuccess('Agendamento excluído com sucesso');
      navigate('/manutencao-preventiva/agendamentos');
    } catch (error) {
      console.error('Erro ao deletar:', error);
      showError('Erro ao excluir agendamento');
    }
  };

  const statusColors: Record<StatusAgendamento, string> = {
    AGENDADO: 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400',
    CONFIRMADO: 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400',
    REMARCADO: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400',
    CANCELADO: 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
    REALIZADO: 'bg-gray-100 dark:bg-gray-900/30 text-gray-600 dark:text-gray-400',
  };

  const canConfirm = agendamento.status === 'AGENDADO';
  const canCancel = ['AGENDADO', 'CONFIRMADO'].includes(agendamento.status);
  const canEdit = ['AGENDADO', 'CONFIRMADO'].includes(agendamento.status);

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3 sm:gap-4">
          <Link
            to="/manutencao-preventiva/agendamentos"
            className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <ArrowLeft className="h-5 w-5 text-gray-500 dark:text-gray-400" />
          </Link>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
              Agendamento - {agendamento.tipoManutencao}
            </h1>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              {new Date(agendamento.dataAgendamento).toLocaleDateString('pt-BR')} às {agendamento.horaAgendamento}
            </p>
          </div>
        </div>
        <div className="flex flex-col sm:flex-row gap-2">
          {canConfirm && (
            <button
              onClick={() => setShowConfirmModal(true)}
              className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
            >
              <CheckCircle className="h-4 w-4" />
              Confirmar
            </button>
          )}
          {canEdit && (
            <Link
              to={`/manutencao-preventiva/agendamentos/${id}/editar`}
              className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              <Edit className="h-4 w-4" />
              Editar
            </Link>
          )}
          {canCancel && (
            <button
              onClick={() => setShowCancelModal(true)}
              className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 border border-red-300 dark:border-red-700 text-red-600 dark:text-red-400 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20"
            >
              <XCircle className="h-4 w-4" />
              Cancelar
            </button>
          )}
        </div>
      </div>

      {/* Status Badge */}
      <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-sm font-medium ${statusColors[agendamento.status]}`}>
        <CalendarCheck className="h-4 w-4" />
        {agendamento.status}
      </div>

      {/* Main Content */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        {/* Left Column */}
        <div className="space-y-6">
          {/* Data e Hora */}
          <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Data e Hora</h2>
            <div className="space-y-3">
              <div className="flex items-center gap-3">
                <Calendar className="h-5 w-5 text-gray-400" />
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Data</p>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {new Date(agendamento.dataAgendamento).toLocaleDateString('pt-BR', {
                      weekday: 'long',
                      day: 'numeric',
                      month: 'long',
                      year: 'numeric',
                    })}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <Clock className="h-5 w-5 text-gray-400" />
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Horário</p>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {agendamento.horaAgendamento} ({agendamento.duracaoEstimadaMinutos} min)
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Veículo */}
          <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Veículo</h2>
            <div className="flex items-start gap-3">
              <Car className="h-5 w-5 text-gray-400 mt-0.5" />
              <div>
                <p className="font-medium text-gray-900 dark:text-white">
                  {agendamento.veiculo.placaFormatada}
                </p>
                <p className="text-gray-600 dark:text-gray-400">
                  {agendamento.veiculo.marca} {agendamento.veiculo.modelo}
                </p>
                {agendamento.veiculo.ano && (
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    Ano: {agendamento.veiculo.ano}
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* Cliente */}
          <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Cliente</h2>
            <div className="space-y-3">
              <div className="flex items-center gap-3">
                <User className="h-5 w-5 text-gray-400" />
                <div>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {agendamento.cliente.nome}
                  </p>
                </div>
              </div>
              {agendamento.cliente.telefone && (
                <div className="flex items-center gap-3">
                  <Phone className="h-5 w-5 text-gray-400" />
                  <p className="text-gray-600 dark:text-gray-400">
                    {agendamento.cliente.telefone}
                  </p>
                </div>
              )}
              {agendamento.cliente.email && (
                <div className="flex items-center gap-3">
                  <Mail className="h-5 w-5 text-gray-400" />
                  <p className="text-gray-600 dark:text-gray-400">
                    {agendamento.cliente.email}
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Column */}
        <div className="space-y-6">
          {/* Detalhes da Manutenção */}
          <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
              Detalhes da Manutenção
            </h2>
            <div className="space-y-4">
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Tipo</p>
                <p className="font-medium text-gray-900 dark:text-white">
                  {agendamento.tipoManutencao}
                </p>
              </div>
              {agendamento.descricao && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Descrição</p>
                  <p className="text-gray-900 dark:text-white">{agendamento.descricao}</p>
                </div>
              )}
              {agendamento.observacoes && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Observações</p>
                  <p className="text-gray-900 dark:text-white">{agendamento.observacoes}</p>
                </div>
              )}
              {agendamento.observacoesInternas && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Observações Internas</p>
                  <p className="text-gray-900 dark:text-white">{agendamento.observacoesInternas}</p>
                </div>
              )}
            </div>
          </div>

          {/* Confirmação */}
          {agendamento.confirmadoEm && (
            <div className="bg-green-50 dark:bg-green-900/20 rounded-lg border border-green-200 dark:border-green-800 p-4">
              <h3 className="font-medium text-green-800 dark:text-green-400 mb-2">Confirmado</h3>
              <p className="text-sm text-green-700 dark:text-green-300">
                Em {new Date(agendamento.confirmadoEm).toLocaleString('pt-BR')}
                {agendamento.confirmadoPor && ` via ${agendamento.confirmadoPor}`}
              </p>
            </div>
          )}

          {/* Cancelamento */}
          {agendamento.status === 'CANCELADO' && agendamento.motivoCancelamento && (
            <div className="bg-red-50 dark:bg-red-900/20 rounded-lg border border-red-200 dark:border-red-800 p-4">
              <h3 className="font-medium text-red-800 dark:text-red-400 mb-2">Cancelado</h3>
              <p className="text-sm text-red-700 dark:text-red-300">
                Motivo: {agendamento.motivoCancelamento}
              </p>
              {agendamento.canceladoEm && (
                <p className="text-xs text-red-600 dark:text-red-400 mt-1">
                  Em {new Date(agendamento.canceladoEm).toLocaleString('pt-BR')}
                </p>
              )}
            </div>
          )}

          {/* Ações de exclusão */}
          <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Ações</h2>
            <button
              onClick={() => setShowDeleteModal(true)}
              className="flex items-center gap-2 text-red-600 dark:text-red-400 hover:underline"
            >
              <Trash2 className="h-4 w-4" />
              Excluir agendamento
            </button>
          </div>
        </div>
      </div>

      {/* Modal Confirmar */}
      {showConfirmModal && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-800 rounded-lg p-6 max-w-sm w-full">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
              Confirmar Agendamento
            </h3>
            <p className="text-gray-600 dark:text-gray-400 mb-6">
              Deseja confirmar este agendamento?
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setShowConfirmModal(false)}
                className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg"
              >
                Cancelar
              </button>
              <button
                onClick={handleConfirmar}
                disabled={confirmarMutation.isPending}
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
              >
                {confirmarMutation.isPending ? 'Confirmando...' : 'Confirmar'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Cancelar */}
      {showCancelModal && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-800 rounded-lg p-6 max-w-md w-full">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
              Cancelar Agendamento
            </h3>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Motivo do cancelamento
              </label>
              <textarea
                value={motivoCancelamento}
                onChange={(e) => setMotivoCancelamento(e.target.value)}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                placeholder="Informe o motivo..."
              />
            </div>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setShowCancelModal(false)}
                className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg"
              >
                Voltar
              </button>
              <button
                onClick={handleCancelar}
                disabled={cancelarMutation.isPending}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {cancelarMutation.isPending ? 'Cancelando...' : 'Cancelar Agendamento'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Deletar */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-800 rounded-lg p-6 max-w-sm w-full">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
              Excluir Agendamento
            </h3>
            <p className="text-gray-600 dark:text-gray-400 mb-6">
              Tem certeza que deseja excluir este agendamento? Esta ação não pode ser desfeita.
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setShowDeleteModal(false)}
                className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg"
              >
                Cancelar
              </button>
              <button
                onClick={handleDeletar}
                disabled={deletarMutation.isPending}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {deletarMutation.isPending ? 'Excluindo...' : 'Excluir'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
