import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Webhook,
  Plus,
  Settings,
  Activity,
  CheckCircle,
  XCircle,
  AlertTriangle,
  RefreshCw,
  Trash2,
  Clock,
  Zap,
} from 'lucide-react';
import { useWebhooks, useWebhookStats, useDeleteWebhook, useReativarWebhook } from '../hooks/useWebhooks';
import type { WebhookConfig } from '../types';

export default function WebhooksListPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);

  const { data: webhooksData, isLoading } = useWebhooks({ page, size: 20 });
  const { data: stats } = useWebhookStats();
  const deleteWebhook = useDeleteWebhook();
  const reativarWebhook = useReativarWebhook();

  const webhooks = webhooksData?.content || [];
  const totalPages = webhooksData?.totalPages || 0;

  const handleDelete = async (webhook: WebhookConfig) => {
    if (!confirm(`Deseja remover o webhook "${webhook.nome}"?`)) return;
    try {
      await deleteWebhook.mutateAsync(webhook.id);
    } catch (error) {
      console.error('Erro ao remover webhook:', error);
    }
  };

  const handleReativar = async (webhook: WebhookConfig) => {
    try {
      await reativarWebhook.mutateAsync(webhook.id);
    } catch (error) {
      console.error('Erro ao reativar webhook:', error);
    }
  };

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 sm:gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
            <Webhook className="h-6 w-6 sm:h-7 sm:w-7" />
            Webhooks
          </h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
            Configure integrações com sistemas externos
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            to="/webhooks/logs"
            className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-3 sm:px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            <Activity className="h-4 w-4" />
            <span className="hidden sm:inline">Histórico</span>
          </Link>
          <Link
            to="/webhooks/novo"
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-3 sm:px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            <Plus className="h-4 w-4" />
            <span className="hidden sm:inline">Novo Webhook</span>
          </Link>
        </div>
      </div>

      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
          <div className="rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-3 sm:p-4">
            <div className="flex items-center gap-2 text-gray-600 dark:text-gray-400 text-xs sm:text-sm">
              <Webhook className="h-4 w-4" />
              Total
            </div>
            <p className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white mt-1">
              {stats.totalWebhooks}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              {stats.webhooksAtivos} ativos
            </p>
          </div>

          <div className="rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-3 sm:p-4">
            <div className="flex items-center gap-2 text-green-600 dark:text-green-400 text-xs sm:text-sm">
              <CheckCircle className="h-4 w-4" />
              Sucesso 24h
            </div>
            <p className="text-xl sm:text-2xl font-bold text-green-600 dark:text-green-400 mt-1">
              {stats.sucessos24h}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              envios bem-sucedidos
            </p>
          </div>

          <div className="rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-3 sm:p-4">
            <div className="flex items-center gap-2 text-red-600 dark:text-red-400 text-xs sm:text-sm">
              <XCircle className="h-4 w-4" />
              Falhas 24h
            </div>
            <p className="text-xl sm:text-2xl font-bold text-red-600 dark:text-red-400 mt-1">
              {stats.falhas24h}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              {stats.pendentesRetry} pendentes retry
            </p>
          </div>

          <div className="rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-3 sm:p-4">
            <div className="flex items-center gap-2 text-blue-600 dark:text-blue-400 text-xs sm:text-sm">
              <Zap className="h-4 w-4" />
              Tempo Médio
            </div>
            <p className="text-xl sm:text-2xl font-bold text-blue-600 dark:text-blue-400 mt-1">
              {stats.tempoMedioResposta24h ? `${Math.round(stats.tempoMedioResposta24h)}ms` : '-'}
            </p>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              resposta média
            </p>
          </div>
        </div>
      )}

      {/* Loading State */}
      {isLoading && (
        <div className="flex justify-center items-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 dark:border-blue-400 border-t-transparent"></div>
        </div>
      )}

      {/* Empty State */}
      {!isLoading && webhooks.length === 0 && (
        <div className="rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-8 text-center">
          <Webhook className="mx-auto h-12 w-12 text-gray-400 dark:text-gray-500" />
          <h3 className="mt-2 text-sm font-medium text-gray-900 dark:text-white">
            Nenhum webhook configurado
          </h3>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Configure webhooks para integrar com sistemas externos
          </p>
          <div className="mt-4">
            <Link
              to="/webhooks/novo"
              className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              <Plus className="h-4 w-4" />
              Criar Webhook
            </Link>
          </div>
        </div>
      )}

      {/* Mobile: Card Layout */}
      {!isLoading && webhooks.length > 0 && (
        <div className="space-y-3 lg:hidden">
          {webhooks.map((webhook) => (
            <div
              key={webhook.id}
              className="rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-4 shadow-sm"
            >
              {/* Header: Nome e Status */}
              <div className="flex items-start justify-between gap-2 mb-3">
                <div className="min-w-0 flex-1">
                  <h3 className="text-sm font-medium text-gray-900 dark:text-white truncate">
                    {webhook.nome}
                  </h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5 truncate">
                    {webhook.url}
                  </p>
                </div>
                <div className="flex items-center gap-1 flex-shrink-0">
                  {webhook.ativo ? (
                    <span className="inline-flex items-center rounded-full bg-green-100 dark:bg-green-900/30 px-2 py-0.5 text-xs font-medium text-green-800 dark:text-green-400">
                      Ativo
                    </span>
                  ) : (
                    <span className="inline-flex items-center rounded-full bg-red-100 dark:bg-red-900/30 px-2 py-0.5 text-xs font-medium text-red-800 dark:text-red-400">
                      Inativo
                    </span>
                  )}
                </div>
              </div>

              {/* Info */}
              <div className="mb-3 pb-3 border-b border-gray-200 dark:border-gray-700">
                {webhook.falhasConsecutivas > 0 && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-yellow-100 dark:bg-yellow-900/30 px-2 py-0.5 text-xs font-medium text-yellow-800 dark:text-yellow-400 mb-2">
                    <AlertTriangle className="h-3 w-3" />
                    {webhook.falhasConsecutivas} falhas consecutivas
                  </span>
                )}
                <div className="flex flex-wrap gap-1">
                  {webhook.eventos.slice(0, 2).map((evento) => (
                    <span
                      key={evento}
                      className="inline-flex items-center rounded bg-gray-100 dark:bg-gray-700 px-2 py-0.5 text-xs text-gray-600 dark:text-gray-300"
                    >
                      {evento.replace('_', ' ')}
                    </span>
                  ))}
                  {webhook.eventos.length > 2 && (
                    <span className="inline-flex items-center rounded bg-gray-100 dark:bg-gray-700 px-2 py-0.5 text-xs text-gray-600 dark:text-gray-300">
                      +{webhook.eventos.length - 2}
                    </span>
                  )}
                </div>
                {webhook.ultimaExecucaoSucesso && (
                  <p className="text-xs text-gray-400 dark:text-gray-500 mt-2 flex items-center gap-1">
                    <Clock className="h-3 w-3" />
                    Último: {new Date(webhook.ultimaExecucaoSucesso).toLocaleDateString('pt-BR')}
                  </p>
                )}
              </div>

              {/* Ações */}
              <div className="flex items-center justify-end gap-2">
                {!webhook.ativo && webhook.falhasConsecutivas >= 10 && (
                  <button
                    onClick={() => handleReativar(webhook)}
                    disabled={reativarWebhook.isPending}
                    className="flex items-center gap-1 rounded-lg bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50"
                  >
                    <RefreshCw className="h-3 w-3" />
                    Reativar
                  </button>
                )}
                <button
                  onClick={() => navigate(`/webhooks/${webhook.id}`)}
                  className="flex items-center gap-1 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700"
                >
                  <Settings className="h-3 w-3" />
                  Editar
                </button>
                <button
                  onClick={() => handleDelete(webhook)}
                  disabled={deleteWebhook.isPending}
                  className="rounded-lg border border-red-300 dark:border-red-600 p-1.5 text-red-700 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 disabled:opacity-50"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Desktop: List Layout */}
      {!isLoading && webhooks.length > 0 && (
        <div className="hidden lg:block rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden">
          <div className="divide-y divide-gray-200 dark:divide-gray-700">
            {webhooks.map((webhook) => (
              <div
                key={webhook.id}
                className="p-4 hover:bg-gray-50 dark:hover:bg-gray-700/50"
              >
                <div className="flex items-center justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="text-sm font-medium text-gray-900 dark:text-white truncate">
                        {webhook.nome}
                      </h3>
                      {webhook.ativo ? (
                        <span className="inline-flex items-center rounded-full bg-green-100 dark:bg-green-900/30 px-2 py-0.5 text-xs font-medium text-green-800 dark:text-green-400">
                          Ativo
                        </span>
                      ) : (
                        <span className="inline-flex items-center rounded-full bg-red-100 dark:bg-red-900/30 px-2 py-0.5 text-xs font-medium text-red-800 dark:text-red-400">
                          Inativo
                        </span>
                      )}
                      {webhook.falhasConsecutivas > 0 && (
                        <span className="inline-flex items-center gap-1 rounded-full bg-yellow-100 dark:bg-yellow-900/30 px-2 py-0.5 text-xs font-medium text-yellow-800 dark:text-yellow-400">
                          <AlertTriangle className="h-3 w-3" />
                          {webhook.falhasConsecutivas} falhas
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
                      {webhook.url}
                    </p>
                    <div className="flex flex-wrap gap-1 mt-2">
                      {webhook.eventos.slice(0, 4).map((evento) => (
                        <span
                          key={evento}
                          className="inline-flex items-center rounded bg-gray-100 dark:bg-gray-700 px-2 py-0.5 text-xs text-gray-600 dark:text-gray-300"
                        >
                          {evento.replace('_', ' ')}
                        </span>
                      ))}
                      {webhook.eventos.length > 4 && (
                        <span className="inline-flex items-center rounded bg-gray-100 dark:bg-gray-700 px-2 py-0.5 text-xs text-gray-600 dark:text-gray-300">
                          +{webhook.eventos.length - 4}
                        </span>
                      )}
                    </div>
                    {webhook.ultimaExecucaoSucesso && (
                      <p className="text-xs text-gray-400 dark:text-gray-500 mt-2 flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        Último sucesso: {new Date(webhook.ultimaExecucaoSucesso).toLocaleString('pt-BR')}
                      </p>
                    )}
                  </div>

                  <div className="flex items-center gap-2 flex-shrink-0">
                    {!webhook.ativo && webhook.falhasConsecutivas >= 10 && (
                      <button
                        onClick={() => handleReativar(webhook)}
                        disabled={reativarWebhook.isPending}
                        className="flex items-center gap-1 rounded-lg border border-green-300 dark:border-green-600 px-3 py-1.5 text-xs font-medium text-green-700 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/20 disabled:opacity-50"
                      >
                        <RefreshCw className="h-3 w-3" />
                        Reativar
                      </button>
                    )}
                    <button
                      onClick={() => navigate(`/webhooks/${webhook.id}`)}
                      className="flex items-center gap-1 rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-1.5 text-xs font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                    >
                      <Settings className="h-3 w-3" />
                      Editar
                    </button>
                    <button
                      onClick={() => handleDelete(webhook)}
                      disabled={deleteWebhook.isPending}
                      className="flex items-center gap-1 rounded-lg border border-red-300 dark:border-red-600 px-3 py-1.5 text-xs font-medium text-red-700 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 disabled:opacity-50"
                    >
                      <Trash2 className="h-3 w-3" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Pagination */}
      {!isLoading && totalPages > 1 && (
        <div className="flex items-center justify-between px-4 py-3 bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-1.5 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
          >
            Anterior
          </button>
          <span className="text-sm text-gray-600 dark:text-gray-400">
            Página {page + 1} de {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-1.5 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
          >
            Próxima
          </button>
        </div>
      )}
    </div>
  );
}
