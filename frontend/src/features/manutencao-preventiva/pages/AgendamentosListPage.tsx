import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Plus, Calendar, Clock, CheckCircle, XCircle, AlertTriangle } from 'lucide-react';
import {
  useAgendamentos,
  useConfirmarAgendamento,
  useCancelarAgendamento,
} from '../hooks/useManutencaoPreventiva';
import { showSuccess, showError } from '@/shared/utils/notifications';
import type { AgendamentoManutencao, StatusAgendamento } from '../types';

export default function AgendamentosListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [showCancelModal, setShowCancelModal] = useState<string | null>(null);
  const [motivoCancelamento, setMotivoCancelamento] = useState('');

  const filters = {
    status: searchParams.get('status') as StatusAgendamento | undefined,
    dataInicio: searchParams.get('dataInicio') || undefined,
    dataFim: searchParams.get('dataFim') || undefined,
    page: parseInt(searchParams.get('page') || '0'),
    size: 20,
  };

  const { data: agendamentosData, isLoading, error } = useAgendamentos(filters);
  const confirmarMutation = useConfirmarAgendamento();
  const cancelarMutation = useCancelarAgendamento();

  const handleFilterChange = (key: string, value: string) => {
    const params = new URLSearchParams(searchParams);
    if (value) {
      params.set(key, value);
    } else {
      params.delete(key);
    }
    params.set('page', '0');
    setSearchParams(params);
  };

  const handleConfirmar = async (id: string) => {
    try {
      await confirmarMutation.mutateAsync(id);
      showSuccess('Agendamento confirmado com sucesso');
    } catch (err) {
      console.error('Erro ao confirmar agendamento:', err);
      showError('Erro ao confirmar agendamento');
    }
  };

  const handleCancelar = async () => {
    if (!showCancelModal) return;
    try {
      await cancelarMutation.mutateAsync({ id: showCancelModal, motivo: motivoCancelamento || undefined });
      showSuccess('Agendamento cancelado');
      setShowCancelModal(null);
      setMotivoCancelamento('');
    } catch (err) {
      console.error('Erro ao cancelar agendamento:', err);
      showError('Erro ao cancelar agendamento');
    }
  };

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
            Agendamentos
          </h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 hidden sm:block">
            Gerencie os agendamentos de manutenção preventiva
          </p>
        </div>
        <div className="flex flex-col sm:flex-row gap-2">
          <Link
            to="/manutencao-preventiva/agendamentos/novo"
            className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <Plus className="h-4 w-4" />
            <span>Novo Agendamento</span>
          </Link>
          <Link
            to="/manutencao-preventiva/calendario"
            className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            <Calendar className="h-4 w-4" />
            <span className="hidden sm:inline">Ver Calendário</span>
            <span className="sm:hidden">Calendário</span>
          </Link>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
        <div className="flex flex-col md:flex-row gap-4">
          {/* Status Filter */}
          <select
            defaultValue={filters.status || ''}
            onChange={(e) => handleFilterChange('status', e.target.value)}
            className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
          >
            <option value="">Todos os status</option>
            <option value="AGENDADO">Agendados</option>
            <option value="CONFIRMADO">Confirmados</option>
            <option value="REMARCADO">Remarcados</option>
            <option value="CANCELADO">Cancelados</option>
            <option value="REALIZADO">Realizados</option>
          </select>

          {/* Data Inicio */}
          <div className="flex items-center gap-2">
            <label className="text-sm text-gray-600 dark:text-gray-400">De:</label>
            <input
              type="date"
              defaultValue={filters.dataInicio || ''}
              onChange={(e) => handleFilterChange('dataInicio', e.target.value)}
              className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
            />
          </div>

          {/* Data Fim */}
          <div className="flex items-center gap-2">
            <label className="text-sm text-gray-600 dark:text-gray-400">Até:</label>
            <input
              type="date"
              defaultValue={filters.dataFim || ''}
              onChange={(e) => handleFilterChange('dataFim', e.target.value)}
              className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
            />
          </div>
        </div>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      ) : error ? (
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Erro ao carregar agendamentos. Tente novamente.
        </div>
      ) : agendamentosData?.content.length === 0 ? (
        <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-8 text-center">
          <Calendar className="h-12 w-12 mx-auto text-gray-400 mb-4" />
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            Nenhum agendamento encontrado
          </p>
          <Link
            to="/manutencao-preventiva/agendamentos/novo"
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <Plus className="h-4 w-4" />
            Criar primeiro agendamento
          </Link>
        </div>
      ) : (
        <>
          {/* Desktop Table */}
          <div className="hidden lg:block bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 dark:text-gray-300 uppercase">
                    Data/Hora
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 dark:text-gray-300 uppercase">
                    Cliente
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 dark:text-gray-300 uppercase">
                    Veículo
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 dark:text-gray-300 uppercase">
                    Tipo
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 dark:text-gray-300 uppercase">
                    Status
                  </th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-gray-700 dark:text-gray-300 uppercase">
                    Ações
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {agendamentosData?.content.map((agendamento) => (
                  <AgendamentoTableRow
                    key={agendamento.id}
                    agendamento={agendamento}
                    onConfirmar={() => handleConfirmar(agendamento.id)}
                    onCancelar={() => setShowCancelModal(agendamento.id)}
                    isConfirming={confirmarMutation.isPending}
                  />
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile Cards */}
          <div className="lg:hidden space-y-4">
            {agendamentosData?.content.map((agendamento) => (
              <AgendamentoCard
                key={agendamento.id}
                agendamento={agendamento}
                onConfirmar={() => handleConfirmar(agendamento.id)}
                onCancelar={() => setShowCancelModal(agendamento.id)}
                isConfirming={confirmarMutation.isPending}
              />
            ))}
          </div>

          {/* Pagination */}
          {agendamentosData && agendamentosData.totalPages > 1 && (
            <div className="flex justify-center gap-2">
              <button
                onClick={() => handleFilterChange('page', String(filters.page - 1))}
                disabled={agendamentosData.first}
                className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg disabled:opacity-50"
              >
                Anterior
              </button>
              <span className="px-4 py-2 text-gray-600 dark:text-gray-400">
                {filters.page + 1} de {agendamentosData.totalPages}
              </span>
              <button
                onClick={() => handleFilterChange('page', String(filters.page + 1))}
                disabled={agendamentosData.last}
                className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg disabled:opacity-50"
              >
                Próximo
              </button>
            </div>
          )}
        </>
      )}

      {/* Cancel Modal */}
      {showCancelModal && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-800 rounded-lg max-w-md w-full p-6">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
              Cancelar Agendamento
            </h2>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Motivo do Cancelamento (opcional)
              </label>
              <textarea
                value={motivoCancelamento}
                onChange={(e) => setMotivoCancelamento(e.target.value)}
                rows={3}
                placeholder="Informe o motivo..."
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
              />
            </div>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowCancelModal(null);
                  setMotivoCancelamento('');
                }}
                className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
              >
                Voltar
              </button>
              <button
                onClick={handleCancelar}
                disabled={cancelarMutation.isPending}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {cancelarMutation.isPending ? 'Cancelando...' : 'Confirmar Cancelamento'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// Helper Components

interface AgendamentoTableRowProps {
  agendamento: AgendamentoManutencao;
  onConfirmar: () => void;
  onCancelar: () => void;
  isConfirming: boolean;
}

function AgendamentoTableRow({ agendamento, onConfirmar, onCancelar, isConfirming }: AgendamentoTableRowProps) {
  return (
    <tr className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
      <td className="px-4 py-4">
        <div className="flex items-center gap-2">
          {agendamento.hoje && (
            <span className="px-2 py-0.5 text-xs font-medium rounded bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400">
              Hoje
            </span>
          )}
          {agendamento.passado && !agendamento.hoje && agendamento.status === 'AGENDADO' && (
            <AlertTriangle className="h-4 w-4 text-red-500" />
          )}
        </div>
        <p className="font-medium text-gray-900 dark:text-white">
          {new Date(agendamento.dataAgendamento).toLocaleDateString('pt-BR')}
        </p>
        <p className="text-sm text-gray-500 dark:text-gray-400 flex items-center gap-1">
          <Clock className="h-3 w-3" />
          {agendamento.horaAgendamento}
        </p>
      </td>
      <td className="px-4 py-4">
        <p className="text-gray-900 dark:text-white">{agendamento.cliente.nome}</p>
        {agendamento.cliente.telefone && (
          <p className="text-sm text-gray-500 dark:text-gray-400">{agendamento.cliente.telefone}</p>
        )}
      </td>
      <td className="px-4 py-4">
        <p className="text-gray-900 dark:text-white">{agendamento.veiculo.placaFormatada}</p>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          {agendamento.veiculo.marca} {agendamento.veiculo.modelo}
        </p>
      </td>
      <td className="px-4 py-4 text-gray-900 dark:text-white">
        {agendamento.tipoManutencao}
      </td>
      <td className="px-4 py-4">
        <StatusBadge status={agendamento.status} />
      </td>
      <td className="px-4 py-4 text-right">
        <div className="flex items-center justify-end gap-2">
          {agendamento.status === 'AGENDADO' && (
            <>
              <button
                onClick={onConfirmar}
                disabled={isConfirming}
                className="p-2 text-green-600 hover:bg-green-50 dark:hover:bg-green-900/20 rounded-lg disabled:opacity-50"
                title="Confirmar"
              >
                <CheckCircle className="h-4 w-4" />
              </button>
              <button
                onClick={onCancelar}
                className="p-2 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg"
                title="Cancelar"
              >
                <XCircle className="h-4 w-4" />
              </button>
            </>
          )}
          {agendamento.status === 'CONFIRMADO' && (
            <button
              onClick={onCancelar}
              className="p-2 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg"
              title="Cancelar"
            >
              <XCircle className="h-4 w-4" />
            </button>
          )}
          <Link
            to={`/manutencao-preventiva/agendamentos/${agendamento.id}`}
            className="text-blue-600 dark:text-blue-400 hover:underline text-sm"
          >
            Detalhes
          </Link>
        </div>
      </td>
    </tr>
  );
}

interface AgendamentoCardProps {
  agendamento: AgendamentoManutencao;
  onConfirmar: () => void;
  onCancelar: () => void;
  isConfirming: boolean;
}

function AgendamentoCard({ agendamento, onConfirmar, onCancelar, isConfirming }: AgendamentoCardProps) {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
      <div className="flex items-start justify-between mb-3">
        <div>
          <div className="flex items-center gap-2 mb-1">
            {agendamento.hoje && (
              <span className="px-2 py-0.5 text-xs font-medium rounded bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400">
                Hoje
              </span>
            )}
            {agendamento.passado && !agendamento.hoje && agendamento.status === 'AGENDADO' && (
              <span className="px-2 py-0.5 text-xs font-medium rounded bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400">
                Atrasado
              </span>
            )}
          </div>
          <p className="font-medium text-gray-900 dark:text-white">
            {new Date(agendamento.dataAgendamento).toLocaleDateString('pt-BR')} às {agendamento.horaAgendamento}
          </p>
          <p className="text-sm text-gray-500 dark:text-gray-400">{agendamento.tipoManutencao}</p>
        </div>
        <StatusBadge status={agendamento.status} />
      </div>

      <div className="space-y-2 mb-4">
        <div className="flex items-center justify-between text-sm">
          <span className="text-gray-500 dark:text-gray-400">Cliente:</span>
          <span className="text-gray-900 dark:text-white">{agendamento.cliente.nome}</span>
        </div>
        <div className="flex items-center justify-between text-sm">
          <span className="text-gray-500 dark:text-gray-400">Veículo:</span>
          <span className="text-gray-900 dark:text-white">
            {agendamento.veiculo.placaFormatada} - {agendamento.veiculo.marca}
          </span>
        </div>
      </div>

      <div className="flex items-center justify-end gap-2 pt-3 border-t border-gray-100 dark:border-gray-700">
        {agendamento.status === 'AGENDADO' && (
          <>
            <button
              onClick={onConfirmar}
              disabled={isConfirming}
              className="flex items-center gap-1 px-3 py-1.5 text-sm bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400 rounded-lg hover:bg-green-100 dark:hover:bg-green-900/30 disabled:opacity-50"
            >
              <CheckCircle className="h-4 w-4" />
              Confirmar
            </button>
            <button
              onClick={onCancelar}
              className="flex items-center gap-1 px-3 py-1.5 text-sm bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/30"
            >
              <XCircle className="h-4 w-4" />
              Cancelar
            </button>
          </>
        )}
        <Link
          to={`/manutencao-preventiva/agendamentos/${agendamento.id}`}
          className="px-3 py-1.5 text-sm text-blue-600 dark:text-blue-400 hover:underline"
        >
          Ver detalhes
        </Link>
      </div>
    </div>
  );
}

function StatusBadge({ status }: { status: StatusAgendamento }) {
  const styles: Record<StatusAgendamento, string> = {
    AGENDADO: 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400',
    CONFIRMADO: 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400',
    REMARCADO: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400',
    CANCELADO: 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
    REALIZADO: 'bg-gray-100 dark:bg-gray-900/30 text-gray-600 dark:text-gray-400',
  };

  return (
    <span className={`inline-flex items-center px-2 py-1 text-xs font-medium rounded-full ${styles[status]}`}>
      {status}
    </span>
  );
}
