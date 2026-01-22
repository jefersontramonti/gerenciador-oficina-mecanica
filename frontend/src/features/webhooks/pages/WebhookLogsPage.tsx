import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowLeft,
  Activity,
  CheckCircle,
  XCircle,
  Clock,
  RefreshCw,
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  ExternalLink,
} from 'lucide-react';
import { useWebhookLogs } from '../hooks/useWebhooks';
import { StatusWebhookLog, statusDescricoes, eventoDescricoes } from '../types';
import type { WebhookLog, TipoEventoWebhook } from '../types';

const statusColors: Record<StatusWebhookLog, string> = {
  PENDENTE: 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300',
  SUCESSO: 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-400',
  FALHA: 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-400',
  AGUARDANDO_RETRY: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-400',
  ESGOTADO: 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-400',
};

const statusIcons: Record<StatusWebhookLog, React.ReactNode> = {
  PENDENTE: <Clock className="h-4 w-4" />,
  SUCESSO: <CheckCircle className="h-4 w-4" />,
  FALHA: <XCircle className="h-4 w-4" />,
  AGUARDANDO_RETRY: <RefreshCw className="h-4 w-4" />,
  ESGOTADO: <AlertTriangle className="h-4 w-4" />,
};

export default function WebhookLogsPage() {
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<StatusWebhookLog | ''>('');
  const [expandedLog, setExpandedLog] = useState<string | null>(null);

  const { data: logsData, isLoading } = useWebhookLogs({
    page,
    size: 50,
    status: statusFilter || undefined,
  });

  const logs = logsData?.content || [];
  const totalPages = logsData?.totalPages || 0;

  const toggleExpand = (logId: string) => {
    setExpandedLog(expandedLog === logId ? null : logId);
  };

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="flex items-center gap-4">
          <Link
            to="/webhooks"
            className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <ArrowLeft className="h-5 w-5 text-gray-600 dark:text-gray-400" />
          </Link>
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
              <Activity className="h-6 w-6" />
              Histórico de Webhooks
            </h1>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Logs de envios e tentativas de retry
            </p>
          </div>
        </div>

        {/* Filter */}
        <div className="flex items-center gap-2">
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value as StatusWebhookLog | '');
              setPage(0);
            }}
            className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-3 py-2 text-sm"
          >
            <option value="">Todos os status</option>
            {Object.entries(StatusWebhookLog).map(([key, value]) => (
              <option key={key} value={value}>
                {statusDescricoes[value]}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Loading State */}
      {isLoading && (
        <div className="flex justify-center items-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 dark:border-blue-400 border-t-transparent"></div>
        </div>
      )}

      {/* Empty State */}
      {!isLoading && logs.length === 0 && (
        <div className="rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-8 text-center">
          <Activity className="mx-auto h-12 w-12 text-gray-400 dark:text-gray-500" />
          <h3 className="mt-2 text-sm font-medium text-gray-900 dark:text-white">
            Nenhum log encontrado
          </h3>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Os logs aparecerão aqui quando webhooks forem disparados
          </p>
        </div>
      )}

      {/* Mobile: Card Layout */}
      {!isLoading && logs.length > 0 && (
        <div className="space-y-3 lg:hidden">
          {logs.map((log) => (
            <div
              key={log.id}
              className="rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden"
            >
              <LogEntry
                log={log}
                isExpanded={expandedLog === log.id}
                onToggle={() => toggleExpand(log.id)}
              />
            </div>
          ))}
        </div>
      )}

      {/* Desktop: List Layout */}
      {!isLoading && logs.length > 0 && (
        <div className="hidden lg:block rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden">
          <div className="divide-y divide-gray-200 dark:divide-gray-700">
            {logs.map((log) => (
              <LogEntry
                key={log.id}
                log={log}
                isExpanded={expandedLog === log.id}
                onToggle={() => toggleExpand(log.id)}
              />
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

function LogEntry({
  log,
  isExpanded,
  onToggle,
}: {
  log: WebhookLog;
  isExpanded: boolean;
  onToggle: () => void;
}) {
  const eventoInfo = eventoDescricoes[log.evento as TipoEventoWebhook];

  return (
    <div className="p-4">
      <div
        className="flex items-start gap-3 cursor-pointer"
        onClick={onToggle}
      >
        <div className="flex-shrink-0 mt-0.5">
          <span
            className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium ${
              statusColors[log.status]
            }`}
          >
            {statusIcons[log.status]}
            {statusDescricoes[log.status]}
          </span>
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-gray-900 dark:text-white">
              {log.webhookNome}
            </span>
            <span className="text-xs text-gray-500 dark:text-gray-400">
              →
            </span>
            <span className="text-sm text-gray-600 dark:text-gray-400">
              {eventoInfo?.nome || log.evento}
            </span>
          </div>

          <div className="flex flex-wrap items-center gap-2 mt-1 text-xs text-gray-500 dark:text-gray-400">
            <span>
              {new Date(log.createdAt).toLocaleString('pt-BR')}
            </span>
            {log.httpStatus && (
              <>
                <span>•</span>
                <span className={log.httpStatus >= 200 && log.httpStatus < 300 ? 'text-green-600' : 'text-red-600'}>
                  HTTP {log.httpStatus}
                </span>
              </>
            )}
            {log.tempoRespostaMs && (
              <>
                <span>•</span>
                <span>{log.tempoRespostaMs}ms</span>
              </>
            )}
            {log.tentativa > 1 && (
              <>
                <span>•</span>
                <span>Tentativa {log.tentativa}</span>
              </>
            )}
          </div>

          {log.erroMensagem && (
            <p className="text-xs text-red-600 dark:text-red-400 mt-1 truncate">
              {log.erroMensagem}
            </p>
          )}
        </div>

        <button className="flex-shrink-0 p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded">
          {isExpanded ? (
            <ChevronUp className="h-4 w-4 text-gray-400" />
          ) : (
            <ChevronDown className="h-4 w-4 text-gray-400" />
          )}
        </button>
      </div>

      {/* Expanded Content */}
      {isExpanded && (
        <div className="mt-4 pl-4 border-l-2 border-gray-200 dark:border-gray-700 space-y-4">
          {/* URL */}
          <div>
            <p className="text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">URL</p>
            <div className="flex items-center gap-2">
              <code className="text-xs bg-gray-100 dark:bg-gray-900 px-2 py-1 rounded text-gray-800 dark:text-gray-200 break-all">
                {log.url}
              </code>
              <a
                href={log.url}
                target="_blank"
                rel="noopener noreferrer"
                className="text-blue-600 hover:text-blue-700"
                onClick={(e) => e.stopPropagation()}
              >
                <ExternalLink className="h-3 w-3" />
              </a>
            </div>
          </div>

          {/* Payload */}
          {log.payload && (
            <div>
              <p className="text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Payload Enviado</p>
              <pre className="text-xs bg-gray-100 dark:bg-gray-900 p-3 rounded overflow-x-auto max-h-48 text-gray-800 dark:text-gray-200">
                {JSON.stringify(JSON.parse(log.payload), null, 2)}
              </pre>
            </div>
          )}

          {/* Response */}
          {log.responseBody && (
            <div>
              <p className="text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Resposta</p>
              <pre className="text-xs bg-gray-100 dark:bg-gray-900 p-3 rounded overflow-x-auto max-h-32 text-gray-800 dark:text-gray-200">
                {log.responseBody}
              </pre>
            </div>
          )}

          {/* Next Retry */}
          {log.proximaTentativa && log.status === 'AGUARDANDO_RETRY' && (
            <div className="flex items-center gap-2 text-xs text-yellow-700 dark:text-yellow-400">
              <Clock className="h-4 w-4" />
              Próxima tentativa: {new Date(log.proximaTentativa).toLocaleString('pt-BR')}
            </div>
          )}

          {/* Entity Info */}
          {log.entidadeId && (
            <div className="text-xs text-gray-500 dark:text-gray-400">
              {log.entidadeTipo}: {log.entidadeId}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
