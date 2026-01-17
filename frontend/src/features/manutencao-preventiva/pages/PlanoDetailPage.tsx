import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Edit,
  Trash2,
  Play,
  Pause,
  CheckCircle,
  AlertTriangle,
  Car,
  Calendar,
  Wrench,
  Clock,
  Bell,
  List,
  Package,
} from 'lucide-react';
import {
  usePlano,
  useAtivarPlano,
  usePausarPlano,
  useConcluirPlano,
  useExecutarPlano,
  useDeletarPlano,
} from '../hooks/useManutencaoPreventiva';
import { showSuccess, showError } from '@/shared/utils/notifications';
import type { ExecutarPlanoRequest, ChecklistItem } from '../types';

export default function PlanoDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [showExecutarModal, setShowExecutarModal] = useState(false);
  const [showPausarModal, setShowPausarModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const { data: plano, isLoading, error } = usePlano(id);
  const ativarMutation = useAtivarPlano();
  const pausarMutation = usePausarPlano();
  const concluirMutation = useConcluirPlano();
  const executarMutation = useExecutarPlano();
  const deletarMutation = useDeletarPlano();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error || !plano) {
    return (
      <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
        Erro ao carregar plano. Tente novamente.
      </div>
    );
  }

  const handleAtivar = async () => {
    try {
      await ativarMutation.mutateAsync(plano.id);
      showSuccess('Plano reativado com sucesso');
    } catch (err) {
      console.error('Erro ao ativar plano:', err);
      showError('Erro ao reativar plano');
    }
  };

  const handleConcluir = async () => {
    if (confirm('Deseja concluir este plano? Esta ação não pode ser desfeita.')) {
      try {
        await concluirMutation.mutateAsync(plano.id);
        showSuccess('Plano concluído com sucesso');
      } catch (err) {
        console.error('Erro ao concluir plano:', err);
        showError('Erro ao concluir plano');
      }
    }
  };

  const handleDelete = async () => {
    try {
      await deletarMutation.mutateAsync(plano.id);
      showSuccess('Plano excluído com sucesso');
      navigate('/manutencao-preventiva');
    } catch (err) {
      console.error('Erro ao deletar plano:', err);
      showError('Erro ao excluir plano');
    }
  };

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3 sm:gap-4">
          <Link
            to="/manutencao-preventiva"
            className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <ArrowLeft className="h-5 w-5 text-gray-500 dark:text-gray-400" />
          </Link>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
              {plano.nome}
            </h1>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              {plano.tipoManutencao}
            </p>
          </div>
        </div>
        <div className="flex flex-col sm:flex-row gap-2">
          {plano.status === 'ATIVO' && (
            <>
              <button
                onClick={() => setShowExecutarModal(true)}
                className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
              >
                <Play className="h-4 w-4" />
                Executar
              </button>
              <button
                onClick={() => setShowPausarModal(true)}
                className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 border border-yellow-500 text-yellow-600 dark:text-yellow-400 rounded-lg hover:bg-yellow-50 dark:hover:bg-yellow-900/20"
              >
                <Pause className="h-4 w-4" />
                Pausar
              </button>
            </>
          )}
          {plano.status === 'PAUSADO' && (
            <button
              onClick={handleAtivar}
              disabled={ativarMutation.isPending}
              className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              <Play className="h-4 w-4" />
              Reativar
            </button>
          )}
          {(plano.status === 'ATIVO' || plano.status === 'PAUSADO') && (
            <button
              onClick={handleConcluir}
              disabled={concluirMutation.isPending}
              className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              <CheckCircle className="h-4 w-4" />
              Concluir
            </button>
          )}
          <Link
            to={`/manutencao-preventiva/${plano.id}/editar`}
            className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            <Edit className="h-4 w-4" />
            Editar
          </Link>
          <button
            onClick={() => setShowDeleteModal(true)}
            className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 border border-red-300 dark:border-red-700 text-red-600 dark:text-red-400 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20"
          >
            <Trash2 className="h-4 w-4" />
            Excluir
          </button>
        </div>
      </div>

      {/* Status Badge */}
      <div className="flex items-center gap-3">
        <StatusBadge status={plano.status} vencido={plano.vencido} />
        {plano.vencido && (
          <span className="text-red-600 dark:text-red-400 text-sm font-medium">
            Manutenção vencida!
          </span>
        )}
        {plano.proximoAVencer && !plano.vencido && (
          <span className="text-yellow-600 dark:text-yellow-400 text-sm font-medium">
            Vence em {plano.diasParaVencer} dias
          </span>
        )}
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 sm:gap-6">
        {/* Left Column - Details */}
        <div className="lg:col-span-2 space-y-6">
          {/* Veículo Card */}
          {plano.veiculo && (
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <div className="flex items-center gap-2 mb-4">
                <Car className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Veículo</h2>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Placa</p>
                  <p className="font-medium text-gray-900 dark:text-white">{plano.veiculo.placaFormatada || plano.veiculo.placa || '-'}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Veículo</p>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {plano.veiculo.marca} {plano.veiculo.modelo} {plano.veiculo.ano}
                  </p>
                </div>
                {plano.veiculo.quilometragem && (
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Quilometragem Atual</p>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {plano.veiculo.quilometragem.toLocaleString()} km
                    </p>
                  </div>
                )}
                {plano.veiculo.clienteNome && (
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Proprietário</p>
                    <p className="font-medium text-gray-900 dark:text-white">{plano.veiculo.clienteNome}</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Critérios Card */}
          <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
            <div className="flex items-center gap-2 mb-4">
              <Clock className="h-5 w-5 text-blue-600 dark:text-blue-400" />
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Critérios de Manutenção</h2>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Critério</p>
                <p className="font-medium text-gray-900 dark:text-white capitalize">
                  {plano.criterio ? plano.criterio.toLowerCase().replace('_', ' e ') : '-'}
                </p>
              </div>
              {plano.intervaloDias && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Intervalo (Dias)</p>
                  <p className="font-medium text-gray-900 dark:text-white">{plano.intervaloDias} dias</p>
                </div>
              )}
              {plano.intervaloKm && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Intervalo (KM)</p>
                  <p className="font-medium text-gray-900 dark:text-white">{plano.intervaloKm.toLocaleString()} km</p>
                </div>
              )}
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Antecedência Alerta</p>
                <p className="font-medium text-gray-900 dark:text-white">
                  {plano.antecedenciaDias ?? 0}d / {(plano.antecedenciaKm ?? 0).toLocaleString()}km
                </p>
              </div>
            </div>
          </div>

          {/* Execução Card */}
          <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
            <div className="flex items-center gap-2 mb-4">
              <Calendar className="h-5 w-5 text-blue-600 dark:text-blue-400" />
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Última e Próxima Execução</h2>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Última Execução</p>
                <p className="font-medium text-gray-900 dark:text-white">
                  {plano.ultimaExecucaoData
                    ? new Date(plano.ultimaExecucaoData).toLocaleDateString('pt-BR')
                    : 'Nunca'}
                </p>
              </div>
              {plano.ultimaExecucaoKm && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">KM na Última</p>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {plano.ultimaExecucaoKm.toLocaleString()} km
                  </p>
                </div>
              )}
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Próxima Previsão</p>
                <p className={`font-medium ${plano.vencido ? 'text-red-600 dark:text-red-400' : plano.proximoAVencer ? 'text-yellow-600 dark:text-yellow-400' : 'text-gray-900 dark:text-white'}`}>
                  {plano.proximaPrevisaoData
                    ? new Date(plano.proximaPrevisaoData).toLocaleDateString('pt-BR')
                    : '-'}
                </p>
              </div>
              {plano.proximaPrevisaoKm && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">KM Próxima</p>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {plano.proximaPrevisaoKm.toLocaleString()} km
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Checklist Card */}
          {plano.checklist && plano.checklist.length > 0 && (
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <div className="flex items-center gap-2 mb-4">
                <List className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Checklist</h2>
              </div>
              <div className="space-y-2">
                {plano.checklist.map((item, index) => (
                  <ChecklistItemRow key={index} item={item} />
                ))}
              </div>
            </div>
          )}

          {/* Peças Sugeridas Card */}
          {plano.pecasSugeridas && plano.pecasSugeridas.length > 0 && (
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <div className="flex items-center gap-2 mb-4">
                <Package className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Peças Sugeridas</h2>
              </div>
              <div className="space-y-2">
                {plano.pecasSugeridas.map((peca, index) => (
                  <div key={index} className="flex items-center justify-between py-2 border-b border-gray-100 dark:border-gray-700 last:border-0">
                    <span className="text-gray-900 dark:text-white">Peça #{peca.pecaId}</span>
                    <span className="text-gray-600 dark:text-gray-400">Qtd: {peca.quantidade}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Right Column - Info Cards */}
        <div className="space-y-6">
          {/* Notificações Card */}
          <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
            <div className="flex items-center gap-2 mb-4">
              <Bell className="h-5 w-5 text-blue-600 dark:text-blue-400" />
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Notificações</h2>
            </div>
            <div className="flex flex-wrap gap-2">
              {plano.canaisNotificacao.map((canal) => (
                <span
                  key={canal}
                  className="px-2 py-1 text-xs font-medium rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400"
                >
                  {canal}
                </span>
              ))}
              {plano.canaisNotificacao.length === 0 && (
                <span className="text-gray-500 dark:text-gray-400 text-sm">Nenhum canal configurado</span>
              )}
            </div>
          </div>

          {/* Valor Estimado Card */}
          {plano.valorEstimado && (
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <div className="flex items-center gap-2 mb-2">
                <Wrench className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Valor Estimado</h2>
              </div>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">
                R$ {plano.valorEstimado.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
              </p>
            </div>
          )}

          {/* Template Origem */}
          {plano.template && (
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">Baseado no Template</h3>
              <p className="font-medium text-gray-900 dark:text-white">{plano.template.nome}</p>
              <p className="text-sm text-gray-500 dark:text-gray-400">{plano.template.tipoManutencao}</p>
            </div>
          )}

          {/* Descrição */}
          {plano.descricao && (
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">Descrição</h3>
              <p className="text-gray-900 dark:text-white">{plano.descricao}</p>
            </div>
          )}

          {/* Motivo Pausa */}
          {plano.status === 'PAUSADO' && plano.motivoPausa && (
            <div className="bg-yellow-50 dark:bg-yellow-900/20 rounded-lg border border-yellow-200 dark:border-yellow-800 p-4">
              <h3 className="text-sm font-medium text-yellow-800 dark:text-yellow-400 mb-2">Motivo da Pausa</h3>
              <p className="text-yellow-700 dark:text-yellow-300">{plano.motivoPausa}</p>
            </div>
          )}

          {/* Datas */}
          <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
            <h3 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">Informações</h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500 dark:text-gray-400">Criado em</span>
                <span className="text-gray-900 dark:text-white">
                  {new Date(plano.createdAt).toLocaleDateString('pt-BR')}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500 dark:text-gray-400">Atualizado em</span>
                <span className="text-gray-900 dark:text-white">
                  {new Date(plano.updatedAt).toLocaleDateString('pt-BR')}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Modals */}
      {showExecutarModal && (
        <ExecutarPlanoModal
          plano={plano}
          onClose={() => setShowExecutarModal(false)}
          onExecutar={async (data) => {
            try {
              await executarMutation.mutateAsync({ id: plano.id, data });
              showSuccess('Manutenção executada com sucesso');
              setShowExecutarModal(false);
            } catch (err) {
              showError('Erro ao executar manutenção');
            }
          }}
          isLoading={executarMutation.isPending}
        />
      )}

      {showPausarModal && (
        <PausarPlanoModal
          onClose={() => setShowPausarModal(false)}
          onPausar={async (motivo) => {
            try {
              await pausarMutation.mutateAsync({ id: plano.id, motivo });
              showSuccess('Plano pausado com sucesso');
              setShowPausarModal(false);
            } catch (err) {
              showError('Erro ao pausar plano');
            }
          }}
          isLoading={pausarMutation.isPending}
        />
      )}

      {showDeleteModal && (
        <DeleteConfirmModal
          title="Excluir Plano"
          message="Tem certeza que deseja excluir este plano de manutenção? Esta ação não pode ser desfeita."
          onClose={() => setShowDeleteModal(false)}
          onConfirm={handleDelete}
          isLoading={deletarMutation.isPending}
        />
      )}
    </div>
  );
}

// Helper Components

function StatusBadge({ status, vencido }: { status: string; vencido: boolean }) {
  if (vencido && status === 'ATIVO') {
    return (
      <span className="inline-flex items-center gap-1 px-3 py-1 text-sm font-medium rounded-full bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400">
        <AlertTriangle className="h-4 w-4" />
        Vencido
      </span>
    );
  }

  const styles: Record<string, string> = {
    ATIVO: 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400',
    PAUSADO: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400',
    CONCLUIDO: 'bg-gray-100 dark:bg-gray-900/30 text-gray-600 dark:text-gray-400',
    VENCIDO: 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
  };

  return (
    <span className={`inline-flex items-center gap-1 px-3 py-1 text-sm font-medium rounded-full ${styles[status] || styles.ATIVO}`}>
      {status}
    </span>
  );
}

function ChecklistItemRow({ item }: { item: ChecklistItem }) {
  return (
    <div className="flex items-center gap-3 py-2 border-b border-gray-100 dark:border-gray-700 last:border-0">
      <div className={`w-2 h-2 rounded-full ${item.obrigatorio ? 'bg-red-500' : 'bg-gray-400'}`} />
      <span className="text-gray-900 dark:text-white flex-1">{item.item}</span>
      {item.obrigatorio && (
        <span className="text-xs text-red-500 dark:text-red-400">Obrigatório</span>
      )}
    </div>
  );
}

interface ExecutarPlanoModalProps {
  plano: any;
  onClose: () => void;
  onExecutar: (data: ExecutarPlanoRequest) => Promise<void>;
  isLoading: boolean;
}

function ExecutarPlanoModal({ plano, onClose, onExecutar, isLoading }: ExecutarPlanoModalProps) {
  const [dataExecucao, setDataExecucao] = useState(new Date().toISOString().split('T')[0]);
  const [kmExecucao, setKmExecucao] = useState(plano.veiculo?.quilometragem?.toString() || '');
  const [observacoes, setObservacoes] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onExecutar({
      dataExecucao,
      kmExecucao: kmExecucao ? parseInt(kmExecucao) : undefined,
      observacoes: observacoes || undefined,
    });
  };

  return (
    <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-center justify-center p-4">
      <div className="bg-white dark:bg-gray-800 rounded-lg max-w-md w-full p-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
          Registrar Execução
        </h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Data da Execução *
            </label>
            <input
              type="date"
              value={dataExecucao}
              onChange={(e) => setDataExecucao(e.target.value)}
              required
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Quilometragem Atual
            </label>
            <input
              type="number"
              value={kmExecucao}
              onChange={(e) => setKmExecucao(e.target.value)}
              placeholder="Ex: 50000"
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Observações
            </label>
            <textarea
              value={observacoes}
              onChange={(e) => setObservacoes(e.target.value)}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            />
          </div>
          <div className="flex justify-end gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
            >
              {isLoading ? 'Salvando...' : 'Registrar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

interface PausarPlanoModalProps {
  onClose: () => void;
  onPausar: (motivo?: string) => Promise<void>;
  isLoading: boolean;
}

function PausarPlanoModal({ onClose, onPausar, isLoading }: PausarPlanoModalProps) {
  const [motivo, setMotivo] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onPausar(motivo || undefined);
  };

  return (
    <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-center justify-center p-4">
      <div className="bg-white dark:bg-gray-800 rounded-lg max-w-md w-full p-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
          Pausar Plano
        </h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Motivo da Pausa (opcional)
            </label>
            <textarea
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              rows={3}
              placeholder="Ex: Veículo vendido, manutenção adiada..."
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            />
          </div>
          <div className="flex justify-end gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="px-4 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700 disabled:opacity-50"
            >
              {isLoading ? 'Salvando...' : 'Pausar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

interface DeleteConfirmModalProps {
  title: string;
  message: string;
  onClose: () => void;
  onConfirm: () => Promise<void>;
  isLoading: boolean;
}

function DeleteConfirmModal({ title, message, onClose, onConfirm, isLoading }: DeleteConfirmModalProps) {
  return (
    <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-center justify-center p-4">
      <div className="bg-white dark:bg-gray-800 rounded-lg max-w-md w-full p-6">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">{title}</h2>
        <p className="text-gray-600 dark:text-gray-400 mb-6">{message}</p>
        <div className="flex justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            Cancelar
          </button>
          <button
            onClick={onConfirm}
            disabled={isLoading}
            className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
          >
            {isLoading ? 'Excluindo...' : 'Excluir'}
          </button>
        </div>
      </div>
    </div>
  );
}
