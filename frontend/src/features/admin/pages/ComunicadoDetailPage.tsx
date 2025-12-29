/**
 * Comunicado Detail Page - View comunicado details and stats
 */

import { Link, useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Edit,
  Send,
  Clock,
  X,
  Trash2,
  Users,
  Eye,
  CheckCircle,
  Building2,
  Calendar,
  User,
  Loader2,
} from 'lucide-react';
import {
  useComunicadoDetail,
  useEnviarComunicado,
  useCancelarComunicado,
  useDeleteComunicado,
} from '../hooks/useSaas';
import { showSuccess, showError } from '@/shared/utils/notifications';
import {
  statusComunicadoLabels,
  statusComunicadoCores,
  tipoComunicadoLabels,
  prioridadeComunicadoLabels,
  prioridadeComunicadoCores,
} from '../types';
import type { StatusComunicado, PrioridadeComunicado } from '../types';

export function ComunicadoDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: comunicado, isLoading } = useComunicadoDetail(id);
  const enviarMutation = useEnviarComunicado();
  const cancelarMutation = useCancelarComunicado();
  const deleteMutation = useDeleteComunicado();

  const getStatusColor = (status: StatusComunicado) => {
    const colors: Record<string, string> = {
      gray: 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400',
      blue: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
      green: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
      red: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
    };
    return colors[statusComunicadoCores[status]] || colors.gray;
  };

  const getPrioridadeColor = (prioridade: PrioridadeComunicado) => {
    const colors: Record<string, string> = {
      gray: 'bg-gray-100 text-gray-600 dark:bg-gray-900/30 dark:text-gray-400',
      blue: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400',
      orange: 'bg-orange-100 text-orange-600 dark:bg-orange-900/30 dark:text-orange-400',
      red: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400',
    };
    return colors[prioridadeComunicadoCores[prioridade]] || colors.gray;
  };

  const formatDate = (dateValue: string | number) => {
    if (!dateValue) return '-';
    // Se for número (timestamp em segundos), converter para milissegundos
    const date = typeof dateValue === 'number'
      ? new Date(dateValue * 1000)
      : new Date(dateValue);
    // Verificar se a data está no passado distante (antes de 2020) - indica timestamp em segundos
    if (date.getFullYear() < 2020 && typeof dateValue === 'string' && !isNaN(Number(dateValue))) {
      return new Date(Number(dateValue) * 1000).toLocaleDateString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    }
    return date.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const handleEnviar = async () => {
    if (!id) return;

    const confirmed = window.confirm(
      'Tem certeza que deseja enviar este comunicado agora? Esta acao nao pode ser desfeita.'
    );

    if (!confirmed) return;

    try {
      await enviarMutation.mutateAsync(id);
      showSuccess('Comunicado enviado com sucesso!');
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao enviar comunicado');
    }
  };

  const handleCancelar = async () => {
    if (!id) return;

    const confirmed = window.confirm('Tem certeza que deseja cancelar este comunicado?');

    if (!confirmed) return;

    try {
      await cancelarMutation.mutateAsync(id);
      showSuccess('Comunicado cancelado com sucesso!');
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao cancelar comunicado');
    }
  };

  const handleDelete = async () => {
    if (!id) return;

    const confirmed = window.confirm(
      'Tem certeza que deseja excluir este comunicado? Esta acao nao pode ser desfeita.'
    );

    if (!confirmed) return;

    try {
      await deleteMutation.mutateAsync(id);
      showSuccess('Comunicado excluido com sucesso!');
      navigate('/admin/comunicados');
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao excluir comunicado');
    }
  };

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
      </div>
    );
  }

  if (!comunicado) {
    return (
      <div className="min-h-screen bg-gray-50 p-6 dark:bg-gray-900">
        <div className="mx-auto max-w-4xl text-center">
          <p className="text-gray-500 dark:text-gray-400">Comunicado nao encontrado</p>
          <Link
            to="/admin/comunicados"
            className="mt-4 inline-flex items-center gap-2 text-blue-600 hover:text-blue-700"
          >
            <ArrowLeft className="h-4 w-4" />
            Voltar para lista
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6 dark:bg-gray-900">
      <div className="mx-auto max-w-4xl">
        {/* Header */}
        <div className="mb-6 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link
              to="/admin/comunicados"
              className="rounded-lg p-2 text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800"
            >
              <ArrowLeft className="h-5 w-5" />
            </Link>
            <div>
              <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                Detalhes do Comunicado
              </h1>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {comunicado.podeEditar && (
              <>
                <Link
                  to={`/admin/comunicados/${id}/editar`}
                  className="flex items-center gap-2 rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                >
                  <Edit className="h-4 w-4" />
                  Editar
                </Link>
                <button
                  onClick={handleDelete}
                  disabled={deleteMutation.isPending}
                  className="flex items-center gap-2 rounded-lg border border-red-300 px-3 py-2 text-sm text-red-600 hover:bg-red-50 dark:border-red-600 dark:text-red-400 dark:hover:bg-red-900/20"
                >
                  {deleteMutation.isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Trash2 className="h-4 w-4" />
                  )}
                  Excluir
                </button>
              </>
            )}
            {comunicado.podeCancelar && (
              <button
                onClick={handleCancelar}
                disabled={cancelarMutation.isPending}
                className="flex items-center gap-2 rounded-lg border border-orange-300 px-3 py-2 text-sm text-orange-600 hover:bg-orange-50 dark:border-orange-600 dark:text-orange-400 dark:hover:bg-orange-900/20"
              >
                {cancelarMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <X className="h-4 w-4" />
                )}
                Cancelar
              </button>
            )}
            {comunicado.podeEnviar && (
              <button
                onClick={handleEnviar}
                disabled={enviarMutation.isPending}
                className="flex items-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
              >
                {enviarMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Send className="h-4 w-4" />
                )}
                Enviar Agora
              </button>
            )}
          </div>
        </div>

        {/* Stats Cards (for sent comunicados) */}
        {comunicado.status === 'ENVIADO' && (
          <div className="mb-6 grid gap-4 md:grid-cols-4">
            <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-blue-100 p-2 dark:bg-blue-900/30">
                  <Users className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Destinatarios</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">
                    {comunicado.totalDestinatarios}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-green-100 p-2 dark:bg-green-900/30">
                  <Eye className="h-5 w-5 text-green-600 dark:text-green-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Visualizacoes</p>
                  <p className="text-xl font-bold text-gray-900 dark:text-white">
                    {comunicado.totalVisualizacoes} ({comunicado.taxaVisualizacao.toFixed(0)}%)
                  </p>
                </div>
              </div>
            </div>
            {comunicado.requerConfirmacao && (
              <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-purple-100 p-2 dark:bg-purple-900/30">
                    <CheckCircle className="h-5 w-5 text-purple-600 dark:text-purple-400" />
                  </div>
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Confirmacoes</p>
                    <p className="text-xl font-bold text-gray-900 dark:text-white">
                      {comunicado.totalConfirmacoes} ({comunicado.taxaConfirmacao.toFixed(0)}%)
                    </p>
                  </div>
                </div>
              </div>
            )}
            <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-orange-100 p-2 dark:bg-orange-900/30">
                  <Calendar className="h-5 w-5 text-orange-600 dark:text-orange-400" />
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Enviado em</p>
                  <p className="text-sm font-bold text-gray-900 dark:text-white">
                    {comunicado.dataEnvio ? formatDate(comunicado.dataEnvio) : '-'}
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Main Content */}
        <div className="space-y-6">
          {/* Header Info */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <div className="mb-4 flex flex-wrap items-center gap-2">
              <span className={`rounded-full px-3 py-1 text-sm font-medium ${getStatusColor(comunicado.status)}`}>
                {statusComunicadoLabels[comunicado.status]}
              </span>
              <span className={`rounded-full px-3 py-1 text-sm font-medium ${getPrioridadeColor(comunicado.prioridade)}`}>
                {prioridadeComunicadoLabels[comunicado.prioridade]}
              </span>
              <span className="rounded-full bg-gray-100 px-3 py-1 text-sm font-medium text-gray-600 dark:bg-gray-900/30 dark:text-gray-400">
                {tipoComunicadoLabels[comunicado.tipo]}
              </span>
              {comunicado.requerConfirmacao && (
                <span className="rounded-full bg-purple-100 px-3 py-1 text-sm font-medium text-purple-800 dark:bg-purple-900/30 dark:text-purple-400">
                  Requer Confirmacao
                </span>
              )}
              {comunicado.exibirNoLogin && (
                <span className="rounded-full bg-yellow-100 px-3 py-1 text-sm font-medium text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400">
                  Exibir no Login
                </span>
              )}
            </div>

            <h2 className="mb-2 text-xl font-bold text-gray-900 dark:text-white">
              {comunicado.titulo}
            </h2>

            {comunicado.resumo && (
              <p className="mb-4 text-gray-600 dark:text-gray-400">{comunicado.resumo}</p>
            )}

            <div className="flex flex-wrap items-center gap-4 text-sm text-gray-500 dark:text-gray-400">
              <span className="flex items-center gap-1">
                <User className="h-4 w-4" />
                Autor: {comunicado.autorNome}
              </span>
              <span className="flex items-center gap-1">
                <Calendar className="h-4 w-4" />
                Criado: {formatDate(comunicado.createdAt)}
              </span>
              {comunicado.dataAgendamento && comunicado.status === 'AGENDADO' && (
                <span className="flex items-center gap-1 text-blue-600 dark:text-blue-400">
                  <Clock className="h-4 w-4" />
                  Agendado: {formatDate(comunicado.dataAgendamento)}
                </span>
              )}
            </div>
          </div>

          {/* Content */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <h3 className="mb-4 font-medium text-gray-900 dark:text-white">Conteudo</h3>
            <div className="prose prose-sm max-w-none dark:prose-invert">
              <div className="whitespace-pre-wrap text-gray-700 dark:text-gray-300">
                {comunicado.conteudo}
              </div>
            </div>
          </div>

          {/* Segmentation */}
          {(comunicado.planosAlvo?.length || comunicado.oficinasAlvo?.length || comunicado.statusOficinasAlvo?.length) && (
            <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
              <h3 className="mb-4 font-medium text-gray-900 dark:text-white">Segmentacao</h3>
              <div className="grid gap-4 md:grid-cols-3">
                {comunicado.planosAlvo && comunicado.planosAlvo.length > 0 && (
                  <div>
                    <p className="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                      Planos Alvo
                    </p>
                    <div className="flex flex-wrap gap-1">
                      {comunicado.planosAlvo.map((plano) => (
                        <span
                          key={plano}
                          className="rounded-full bg-blue-100 px-2 py-0.5 text-xs text-blue-800 dark:bg-blue-900/30 dark:text-blue-400"
                        >
                          {plano}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
                {comunicado.statusOficinasAlvo && comunicado.statusOficinasAlvo.length > 0 && (
                  <div>
                    <p className="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                      Status Oficinas
                    </p>
                    <div className="flex flex-wrap gap-1">
                      {comunicado.statusOficinasAlvo.map((status) => (
                        <span
                          key={status}
                          className="rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-800 dark:bg-gray-900/30 dark:text-gray-400"
                        >
                          {status}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
                {comunicado.oficinasAlvo && comunicado.oficinasAlvo.length > 0 && (
                  <div>
                    <p className="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                      Oficinas Especificas
                    </p>
                    <p className="text-sm text-gray-600 dark:text-gray-400">
                      {comunicado.oficinasAlvo.length} oficinas selecionadas
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Recent Readings (for sent comunicados) */}
          {comunicado.status === 'ENVIADO' && comunicado.leiturasRecentes && comunicado.leiturasRecentes.length > 0 && (
            <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
              <h3 className="mb-4 font-medium text-gray-900 dark:text-white">
                Leituras Recentes
              </h3>
              <div className="divide-y divide-gray-200 dark:divide-gray-700">
                {comunicado.leiturasRecentes.map((leitura) => (
                  <div key={leitura.oficinaId} className="flex items-center justify-between py-3">
                    <div className="flex items-center gap-3">
                      <div className="rounded-lg bg-gray-100 p-2 dark:bg-gray-700">
                        <Building2 className="h-4 w-4 text-gray-600 dark:text-gray-400" />
                      </div>
                      <div>
                        <p className="font-medium text-gray-900 dark:text-white">
                          {leitura.oficinaNome}
                        </p>
                        {leitura.dataVisualizacao && (
                          <p className="text-xs text-gray-500 dark:text-gray-400">
                            Visualizado: {formatDate(leitura.dataVisualizacao)}
                          </p>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {leitura.visualizado && (
                        <span className="flex items-center gap-1 rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-800 dark:bg-green-900/30 dark:text-green-400">
                          <Eye className="h-3 w-3" />
                          Visualizado
                        </span>
                      )}
                      {leitura.confirmado && (
                        <span className="flex items-center gap-1 rounded-full bg-purple-100 px-2 py-0.5 text-xs text-purple-800 dark:bg-purple-900/30 dark:text-purple-400">
                          <CheckCircle className="h-3 w-3" />
                          Confirmado
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
