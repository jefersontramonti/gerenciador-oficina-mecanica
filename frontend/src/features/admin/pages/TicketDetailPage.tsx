import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Clock,
  AlertTriangle,
  User,
  Send,
  Building2,
  Tag,
  Flag,
  CheckCircle2,
  XCircle,
} from 'lucide-react';
import {
  useTicketDetail,
  useResponderTicket,
  useAtribuirTicket,
  useAlterarStatusTicket,
  useAlterarPrioridadeTicket,
  useSuperAdmins,
} from '../hooks/useSaas';
import type {
  StatusTicket,
  PrioridadeTicket,
  MensagemTicket,
} from '../types';

const statusConfig: Record<string, { label: string; color: string; bgColor: string }> = {
  ABERTO: { label: 'Aberto', color: 'text-blue-700', bgColor: 'bg-blue-100' },
  EM_ANDAMENTO: { label: 'Em Andamento', color: 'text-yellow-700', bgColor: 'bg-yellow-100' },
  AGUARDANDO_CLIENTE: { label: 'Aguardando Cliente', color: 'text-orange-700', bgColor: 'bg-orange-100' },
  AGUARDANDO_INTERNO: { label: 'Aguardando Interno', color: 'text-purple-700', bgColor: 'bg-purple-100' },
  RESOLVIDO: { label: 'Resolvido', color: 'text-green-700', bgColor: 'bg-green-100' },
  FECHADO: { label: 'Fechado', color: 'text-gray-700', bgColor: 'bg-gray-100' },
};

const prioridadeConfig: Record<string, { label: string; color: string; bgColor: string }> = {
  BAIXA: { label: 'Baixa', color: 'text-gray-600', bgColor: 'bg-gray-100' },
  MEDIA: { label: 'Média', color: 'text-blue-600', bgColor: 'bg-blue-100' },
  ALTA: { label: 'Alta', color: 'text-orange-600', bgColor: 'bg-orange-100' },
  URGENTE: { label: 'Urgente', color: 'text-red-600', bgColor: 'bg-red-100' },
};

const tipoConfig: Record<string, { label: string }> = {
  TECNICO: { label: 'Técnico' },
  FINANCEIRO: { label: 'Financeiro' },
  COMERCIAL: { label: 'Comercial' },
  SUGESTAO: { label: 'Sugestão' },
  OUTRO: { label: 'Outro' },
};

const autorTipoConfig: Record<string, { label: string; color: string }> = {
  CLIENTE: { label: 'Cliente', color: 'bg-blue-500' },
  SUPORTE: { label: 'Suporte', color: 'bg-green-500' },
  SISTEMA: { label: 'Sistema', color: 'bg-gray-500' },
};

function formatDateTime(dateString: string): string {
  return new Date(dateString).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatTimeAgo(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffDays > 0) return `há ${diffDays}d`;
  if (diffHours > 0) return `há ${diffHours}h`;
  if (diffMins > 0) return `há ${diffMins}min`;
  return 'agora';
}

function MessageBubble({ mensagem }: { mensagem: MensagemTicket }) {
  const isSupport = mensagem.autorTipo === 'SUPORTE';
  const isSystem = mensagem.autorTipo === 'SISTEMA';
  const autorConfig = autorTipoConfig[mensagem.autorTipo] || autorTipoConfig.SISTEMA;

  if (isSystem) {
    return (
      <div className="flex justify-center my-4">
        <div className="bg-gray-100 text-gray-600 text-sm px-4 py-2 rounded-full">
          <span className="font-medium">Sistema:</span> {mensagem.conteudo}
          <span className="ml-2 text-xs text-gray-400">{formatTimeAgo(mensagem.criadoEm)}</span>
        </div>
      </div>
    );
  }

  return (
    <div className={`flex ${isSupport ? 'justify-end' : 'justify-start'} mb-4`}>
      <div className={`max-w-[70%] ${isSupport ? 'order-2' : 'order-1'}`}>
        <div className={`flex items-center gap-2 mb-1 ${isSupport ? 'justify-end' : 'justify-start'}`}>
          <div className={`w-2 h-2 rounded-full ${autorConfig.color}`} />
          <span className="text-sm font-medium text-gray-700">{mensagem.autorNome}</span>
          <span className="text-xs text-gray-400">{formatTimeAgo(mensagem.criadoEm)}</span>
          {mensagem.isInterno && (
            <span className="text-xs bg-yellow-100 text-yellow-700 px-2 py-0.5 rounded">
              Interno
            </span>
          )}
        </div>
        <div
          className={`rounded-2xl px-4 py-3 ${
            isSupport
              ? 'bg-primary-600 text-white rounded-br-md'
              : 'bg-gray-100 text-gray-800 rounded-bl-md'
          }`}
        >
          <p className="whitespace-pre-wrap">{mensagem.conteudo}</p>
        </div>
      </div>
    </div>
  );
}

export function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [resposta, setResposta] = useState('');
  const [isInterno, setIsInterno] = useState(false);
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [showPrioridadeModal, setShowPrioridadeModal] = useState(false);
  const [showAtribuirModal, setShowAtribuirModal] = useState(false);

  const { data: ticket, isLoading, error } = useTicketDetail(id || '');
  const { data: superAdmins } = useSuperAdmins();

  const responderMutation = useResponderTicket();
  const alterarStatusMutation = useAlterarStatusTicket();
  const alterarPrioridadeMutation = useAlterarPrioridadeTicket();
  const atribuirMutation = useAtribuirTicket();

  const handleEnviarResposta = async () => {
    if (!resposta.trim() || !id) return;

    try {
      await responderMutation.mutateAsync({
        id,
        data: { conteudo: resposta, isInterno },
      });
      setResposta('');
      setIsInterno(false);
    } catch (err) {
      console.error('Erro ao enviar resposta:', err);
    }
  };

  const handleAlterarStatus = async (novoStatus: string) => {
    if (!id) return;
    try {
      await alterarStatusMutation.mutateAsync({
        id,
        data: { status: novoStatus as StatusTicket },
      });
      setShowStatusModal(false);
    } catch (err) {
      console.error('Erro ao alterar status:', err);
    }
  };

  const handleAlterarPrioridade = async (novaPrioridade: string) => {
    if (!id) return;
    try {
      await alterarPrioridadeMutation.mutateAsync({
        id,
        data: { prioridade: novaPrioridade as PrioridadeTicket },
      });
      setShowPrioridadeModal(false);
    } catch (err) {
      console.error('Erro ao alterar prioridade:', err);
    }
  };

  const handleAtribuir = async (usuarioId: string | undefined) => {
    if (!id) return;
    try {
      await atribuirMutation.mutateAsync({
        id,
        data: { atribuidoA: usuarioId },
      });
      setShowAtribuirModal(false);
    } catch (err) {
      console.error('Erro ao atribuir ticket:', err);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    );
  }

  if (error || !ticket) {
    return (
      <div className="text-center py-12">
        <AlertTriangle className="mx-auto h-12 w-12 text-red-400" />
        <h3 className="mt-2 text-sm font-medium text-gray-900">Erro ao carregar ticket</h3>
        <p className="mt-1 text-sm text-gray-500">Não foi possível carregar os detalhes do ticket.</p>
        <div className="mt-6">
          <button
            onClick={() => navigate('/admin/tickets')}
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700"
          >
            Voltar para lista
          </button>
        </div>
      </div>
    );
  }

  const statusInfo = statusConfig[ticket.status] || statusConfig.ABERTO;
  const prioridadeInfo = prioridadeConfig[ticket.prioridade] || prioridadeConfig.MEDIA;
  const tipoInfo = tipoConfig[ticket.tipo] || tipoConfig.OUTRO;
  const isClosed = ticket.status === 'FECHADO' || ticket.status === 'RESOLVIDO';

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/admin/tickets')}
            className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-gray-900">#{ticket.numero}</h1>
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${statusInfo.bgColor} ${statusInfo.color}`}>
                {statusInfo.label}
              </span>
              {ticket.slaVencido && (
                <span className="flex items-center gap-1 px-3 py-1 rounded-full text-sm font-medium bg-red-100 text-red-700">
                  <AlertTriangle className="h-4 w-4" />
                  SLA Vencido
                </span>
              )}
            </div>
            <p className="text-gray-600 mt-1">{ticket.assunto}</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content - Messages */}
        <div className="lg:col-span-2 space-y-6">
          {/* Ticket Info Card */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 className="text-sm font-medium text-gray-500 mb-3">Descrição do Problema</h3>
            <p className="text-gray-800 whitespace-pre-wrap">{ticket.descricao}</p>
          </div>

          {/* Messages Thread */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Histórico de Mensagens ({ticket.mensagens?.length || 0})
            </h3>

            <div className="space-y-2 max-h-[500px] overflow-y-auto pr-2">
              {ticket.mensagens && ticket.mensagens.length > 0 ? (
                ticket.mensagens.map((msg) => (
                  <MessageBubble key={msg.id} mensagem={msg} />
                ))
              ) : (
                <p className="text-center text-gray-500 py-8">
                  Nenhuma mensagem ainda. Seja o primeiro a responder!
                </p>
              )}
            </div>

            {/* Reply Form */}
            {!isClosed && (
              <div className="mt-6 pt-4 border-t border-gray-200">
                <div className="space-y-3">
                  <textarea
                    value={resposta}
                    onChange={(e) => setResposta(e.target.value)}
                    placeholder="Digite sua resposta..."
                    rows={3}
                    className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-primary-500 resize-none"
                  />
                  <div className="flex items-center justify-between">
                    <label className="flex items-center gap-2 text-sm text-gray-600">
                      <input
                        type="checkbox"
                        checked={isInterno}
                        onChange={(e) => setIsInterno(e.target.checked)}
                        className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                      />
                      Nota interna (não visível ao cliente)
                    </label>
                    <button
                      onClick={handleEnviarResposta}
                      disabled={!resposta.trim() || responderMutation.isPending}
                      className="inline-flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                      <Send className="h-4 w-4" />
                      {responderMutation.isPending ? 'Enviando...' : 'Enviar'}
                    </button>
                  </div>
                </div>
              </div>
            )}

            {isClosed && (
              <div className="mt-6 pt-4 border-t border-gray-200">
                <p className="text-center text-gray-500">
                  Este ticket está {ticket.status === 'RESOLVIDO' ? 'resolvido' : 'fechado'} e não aceita novas mensagens.
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Sidebar - Details */}
        <div className="space-y-6">
          {/* Actions Card */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Ações</h3>
            <div className="space-y-3">
              <button
                onClick={() => setShowStatusModal(true)}
                className="w-full flex items-center justify-center gap-2 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
              >
                <Tag className="h-4 w-4" />
                Alterar Status
              </button>
              <button
                onClick={() => setShowPrioridadeModal(true)}
                className="w-full flex items-center justify-center gap-2 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
              >
                <Flag className="h-4 w-4" />
                Alterar Prioridade
              </button>
              <button
                onClick={() => setShowAtribuirModal(true)}
                className="w-full flex items-center justify-center gap-2 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
              >
                <User className="h-4 w-4" />
                {ticket.atribuidoAId ? 'Reatribuir' : 'Atribuir'}
              </button>
              {!isClosed && (
                <>
                  <hr className="my-2" />
                  <button
                    onClick={() => handleAlterarStatus('RESOLVIDO')}
                    className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
                  >
                    <CheckCircle2 className="h-4 w-4" />
                    Marcar como Resolvido
                  </button>
                  <button
                    onClick={() => handleAlterarStatus('FECHADO')}
                    className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
                  >
                    <XCircle className="h-4 w-4" />
                    Fechar Ticket
                  </button>
                </>
              )}
            </div>
          </div>

          {/* Details Card */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Detalhes</h3>
            <dl className="space-y-4">
              <div>
                <dt className="text-sm text-gray-500">Oficina</dt>
                <dd className="mt-1 flex items-center gap-2 text-sm font-medium text-gray-900">
                  <Building2 className="h-4 w-4 text-gray-400" />
                  {ticket.oficinaNome || 'N/A'}
                </dd>
              </div>
              <div>
                <dt className="text-sm text-gray-500">Solicitante</dt>
                <dd className="mt-1 flex items-center gap-2 text-sm font-medium text-gray-900">
                  <User className="h-4 w-4 text-gray-400" />
                  {ticket.usuarioNome}
                </dd>
              </div>
              <div>
                <dt className="text-sm text-gray-500">Tipo</dt>
                <dd className="mt-1 text-sm font-medium text-gray-900">
                  {tipoInfo.label}
                </dd>
              </div>
              <div>
                <dt className="text-sm text-gray-500">Prioridade</dt>
                <dd className="mt-1">
                  <span className={`px-2 py-1 rounded text-sm font-medium ${prioridadeInfo.bgColor} ${prioridadeInfo.color}`}>
                    {prioridadeInfo.label}
                  </span>
                </dd>
              </div>
              <div>
                <dt className="text-sm text-gray-500">Atribuído a</dt>
                <dd className="mt-1 flex items-center gap-2 text-sm font-medium text-gray-900">
                  <User className="h-4 w-4 text-gray-400" />
                  {ticket.atribuidoANome || 'Não atribuído'}
                </dd>
              </div>
              <div>
                <dt className="text-sm text-gray-500">Criado em</dt>
                <dd className="mt-1 flex items-center gap-2 text-sm text-gray-900">
                  <Clock className="h-4 w-4 text-gray-400" />
                  {formatDateTime(ticket.aberturaEm)}
                </dd>
              </div>
              {ticket.resolvidoEm && (
                <div>
                  <dt className="text-sm text-gray-500">Resolvido em</dt>
                  <dd className="mt-1 flex items-center gap-2 text-sm text-gray-900">
                    <CheckCircle2 className="h-4 w-4 text-green-500" />
                    {formatDateTime(ticket.resolvidoEm)}
                  </dd>
                </div>
              )}
              {ticket.slaMinutos && (
                <div>
                  <dt className="text-sm text-gray-500">SLA</dt>
                  <dd className={`mt-1 text-sm font-medium ${ticket.slaVencido ? 'text-red-600' : 'text-gray-900'}`}>
                    {Math.floor(ticket.slaMinutos / 60)}h {ticket.slaMinutos % 60}min
                    {ticket.slaVencido && ' (Vencido)'}
                  </dd>
                </div>
              )}
            </dl>
          </div>
        </div>
      </div>

      {/* Status Modal */}
      {showStatusModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 w-full max-w-md mx-4">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Alterar Status</h3>
            <div className="space-y-2">
              {Object.entries(statusConfig).map(([key, config]) => (
                <button
                  key={key}
                  onClick={() => handleAlterarStatus(key)}
                  disabled={key === ticket.status || alterarStatusMutation.isPending}
                  className={`w-full text-left px-4 py-3 rounded-lg border transition-colors ${
                    key === ticket.status
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                  } disabled:opacity-50`}
                >
                  <span className={`px-2 py-1 rounded text-sm font-medium ${config.bgColor} ${config.color}`}>
                    {config.label}
                  </span>
                </button>
              ))}
            </div>
            <div className="mt-4 flex justify-end">
              <button
                onClick={() => setShowStatusModal(false)}
                className="px-4 py-2 text-gray-600 hover:text-gray-800"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Prioridade Modal */}
      {showPrioridadeModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 w-full max-w-md mx-4">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Alterar Prioridade</h3>
            <div className="space-y-2">
              {Object.entries(prioridadeConfig).map(([key, config]) => (
                <button
                  key={key}
                  onClick={() => handleAlterarPrioridade(key)}
                  disabled={key === ticket.prioridade || alterarPrioridadeMutation.isPending}
                  className={`w-full text-left px-4 py-3 rounded-lg border transition-colors ${
                    key === ticket.prioridade
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                  } disabled:opacity-50`}
                >
                  <span className={`px-2 py-1 rounded text-sm font-medium ${config.bgColor} ${config.color}`}>
                    {config.label}
                  </span>
                </button>
              ))}
            </div>
            <div className="mt-4 flex justify-end">
              <button
                onClick={() => setShowPrioridadeModal(false)}
                className="px-4 py-2 text-gray-600 hover:text-gray-800"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Atribuir Modal */}
      {showAtribuirModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 w-full max-w-md mx-4">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Atribuir Ticket</h3>
            <div className="space-y-2">
              <button
                onClick={() => handleAtribuir(undefined)}
                disabled={atribuirMutation.isPending}
                className={`w-full text-left px-4 py-3 rounded-lg border transition-colors ${
                  !ticket.atribuidoAId
                    ? 'border-primary-500 bg-primary-50'
                    : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                } disabled:opacity-50`}
              >
                <span className="text-gray-600">Não atribuído</span>
              </button>
              {superAdmins?.map((admin) => (
                <button
                  key={admin.id}
                  onClick={() => handleAtribuir(admin.id)}
                  disabled={admin.id === ticket.atribuidoAId || atribuirMutation.isPending}
                  className={`w-full text-left px-4 py-3 rounded-lg border transition-colors ${
                    admin.id === ticket.atribuidoAId
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                  } disabled:opacity-50`}
                >
                  <div className="flex items-center gap-3">
                    <User className="h-8 w-8 text-gray-400" />
                    <div>
                      <p className="font-medium text-gray-900">{admin.nome}</p>
                      <p className="text-sm text-gray-500">{admin.email}</p>
                    </div>
                  </div>
                </button>
              ))}
            </div>
            <div className="mt-4 flex justify-end">
              <button
                onClick={() => setShowAtribuirModal(false)}
                className="px-4 py-2 text-gray-600 hover:text-gray-800"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
