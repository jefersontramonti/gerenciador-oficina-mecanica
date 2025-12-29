/**
 * Comunicados Page - Lista de comunicados recebidos pela oficina
 */

import { useState } from 'react';
import {
  Bell,
  CheckCircle,
  Eye,
  Clock,
  AlertTriangle,
  X,
  ChevronLeft,
  ChevronRight,
  CheckCheck,
} from 'lucide-react';
import {
  useComunicadosOficina,
  useComunicadoDetail,
  useConfirmarComunicado,
  useMarcarTodosLidos,
} from '../hooks/useComunicados';
import { prioridadeColors, tipoIcons } from '../types';
import type { ComunicadoOficina } from '../types';
import { showSuccess, showError } from '@/shared/utils/notifications';

export function ComunicadosPage() {
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const { data, isLoading, error } = useComunicadosOficina(page, 20);
  const { data: detail, isLoading: isLoadingDetail } = useComunicadoDetail(selectedId || undefined);
  const confirmarMutation = useConfirmarComunicado();
  const marcarTodosMutation = useMarcarTodosLidos();

  const handleConfirmar = async () => {
    if (!selectedId) return;
    try {
      await confirmarMutation.mutateAsync(selectedId);
      showSuccess('Leitura confirmada!');
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao confirmar leitura');
    }
  };

  const handleMarcarTodos = async () => {
    try {
      await marcarTodosMutation.mutateAsync();
      showSuccess('Todos os comunicados foram marcados como lidos');
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao marcar como lidos');
    }
  };

  const formatDate = (dateValue?: string | number) => {
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

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 p-6 dark:bg-gray-900">
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Erro ao carregar comunicados. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6 dark:bg-gray-900">
      <div className="mx-auto max-w-6xl">
        {/* Header */}
        <div className="mb-6 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Bell className="h-8 w-8 text-blue-600" />
            <div>
              <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Comunicados</h1>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Avisos e atualizacoes do sistema
              </p>
            </div>
          </div>
          <button
            onClick={handleMarcarTodos}
            disabled={marcarTodosMutation.isPending}
            className="flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
          >
            <CheckCheck className="h-4 w-4" />
            Marcar todos como lidos
          </button>
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          {/* Lista de comunicados */}
          <div className="space-y-4">
            {isLoading ? (
              <div className="flex items-center justify-center py-12">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
              </div>
            ) : data?.content.length === 0 ? (
              <div className="rounded-lg bg-white p-8 text-center shadow dark:bg-gray-800">
                <Bell className="mx-auto h-12 w-12 text-gray-400" />
                <p className="mt-4 text-gray-500 dark:text-gray-400">
                  Nenhum comunicado recebido
                </p>
              </div>
            ) : (
              <>
                {data?.content.map((comunicado) => (
                  <ComunicadoCard
                    key={comunicado.id}
                    comunicado={comunicado}
                    isSelected={selectedId === comunicado.id}
                    onClick={() => setSelectedId(comunicado.id)}
                  />
                ))}

                {/* Pagination */}
                {data && data.totalPages > 1 && (
                  <div className="flex items-center justify-center gap-2 pt-4">
                    <button
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={page === 0}
                      className="rounded-lg p-2 text-gray-600 hover:bg-gray-100 disabled:opacity-50 dark:text-gray-400 dark:hover:bg-gray-700"
                    >
                      <ChevronLeft className="h-5 w-5" />
                    </button>
                    <span className="text-sm text-gray-600 dark:text-gray-400">
                      Pagina {page + 1} de {data.totalPages}
                    </span>
                    <button
                      onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
                      disabled={page >= data.totalPages - 1}
                      className="rounded-lg p-2 text-gray-600 hover:bg-gray-100 disabled:opacity-50 dark:text-gray-400 dark:hover:bg-gray-700"
                    >
                      <ChevronRight className="h-5 w-5" />
                    </button>
                  </div>
                )}
              </>
            )}
          </div>

          {/* Detalhes do comunicado selecionado */}
          <div className="lg:sticky lg:top-6">
            {selectedId ? (
              <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
                {isLoadingDetail ? (
                  <div className="flex items-center justify-center py-12">
                    <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
                  </div>
                ) : detail ? (
                  <>
                    <div className="mb-4 flex items-start justify-between">
                      <div className="flex items-center gap-2">
                        <span className="text-2xl">{tipoIcons[detail.tipo] || '📢'}</span>
                        <span
                          className={`rounded-full px-3 py-1 text-xs font-medium ${prioridadeColors[detail.prioridade]}`}
                        >
                          {detail.prioridadeDescricao}
                        </span>
                      </div>
                      <button
                        onClick={() => setSelectedId(null)}
                        className="rounded-lg p-1 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700"
                      >
                        <X className="h-5 w-5" />
                      </button>
                    </div>

                    <h2 className="mb-2 text-xl font-bold text-gray-900 dark:text-white">
                      {detail.titulo}
                    </h2>

                    {detail.resumo && (
                      <p className="mb-4 text-sm text-gray-600 dark:text-gray-400">
                        {detail.resumo}
                      </p>
                    )}

                    <div className="mb-4 flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400">
                      <span className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {formatDate(detail.dataEnvio)}
                      </span>
                      <span>Por: {detail.autorNome}</span>
                    </div>

                    <div className="mb-6 rounded-lg bg-gray-50 p-4 dark:bg-gray-700">
                      <div
                        className="prose prose-sm max-w-none dark:prose-invert"
                        dangerouslySetInnerHTML={{ __html: detail.conteudo.replace(/\n/g, '<br>') }}
                      />
                    </div>

                    {/* Status de leitura */}
                    <div className="mb-4 space-y-2 border-t pt-4 dark:border-gray-700">
                      <div className="flex items-center gap-2 text-sm">
                        {detail.visualizado ? (
                          <span className="flex items-center gap-1 text-green-600">
                            <Eye className="h-4 w-4" />
                            Visualizado em {formatDate(detail.dataVisualizacao)}
                          </span>
                        ) : (
                          <span className="flex items-center gap-1 text-gray-500">
                            <Eye className="h-4 w-4" />
                            Nao visualizado
                          </span>
                        )}
                      </div>

                      {detail.requerConfirmacao && (
                        <div className="flex items-center gap-2 text-sm">
                          {detail.confirmado ? (
                            <span className="flex items-center gap-1 text-green-600">
                              <CheckCircle className="h-4 w-4" />
                              Confirmado em {formatDate(detail.dataConfirmacao)}
                            </span>
                          ) : (
                            <span className="flex items-center gap-1 text-orange-600">
                              <AlertTriangle className="h-4 w-4" />
                              Aguardando confirmacao
                            </span>
                          )}
                        </div>
                      )}
                    </div>

                    {/* Botao de confirmar */}
                    {detail.requerConfirmacao && !detail.confirmado && (
                      <button
                        onClick={handleConfirmar}
                        disabled={confirmarMutation.isPending}
                        className="flex w-full items-center justify-center gap-2 rounded-lg bg-green-600 px-4 py-3 font-medium text-white hover:bg-green-700 disabled:opacity-50"
                      >
                        <CheckCircle className="h-5 w-5" />
                        {confirmarMutation.isPending ? 'Confirmando...' : 'Confirmar Leitura'}
                      </button>
                    )}
                  </>
                ) : null}
              </div>
            ) : (
              <div className="rounded-lg bg-white p-12 text-center shadow dark:bg-gray-800">
                <Bell className="mx-auto h-16 w-16 text-gray-300 dark:text-gray-600" />
                <p className="mt-4 text-gray-500 dark:text-gray-400">
                  Selecione um comunicado para ver os detalhes
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// Card do comunicado
function ComunicadoCard({
  comunicado,
  isSelected,
  onClick,
}: {
  comunicado: ComunicadoOficina;
  isSelected: boolean;
  onClick: () => void;
}) {
  const formatDate = (dateValue?: string | number) => {
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
      });
    }
    return date.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
    });
  };

  return (
    <button
      onClick={onClick}
      className={`w-full rounded-lg bg-white p-4 text-left shadow transition-all hover:shadow-md dark:bg-gray-800 ${
        isSelected ? 'ring-2 ring-blue-500' : ''
      } ${!comunicado.visualizado ? 'border-l-4 border-blue-500' : ''}`}
    >
      <div className="flex items-start gap-3">
        <span className="text-xl">{tipoIcons[comunicado.tipo] || '📢'}</span>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <h3
              className={`truncate font-medium ${
                !comunicado.visualizado
                  ? 'text-gray-900 dark:text-white'
                  : 'text-gray-600 dark:text-gray-400'
              }`}
            >
              {comunicado.titulo}
            </h3>
            {!comunicado.visualizado && (
              <span className="rounded-full bg-blue-500 px-2 py-0.5 text-xs text-white">Novo</span>
            )}
          </div>
          {comunicado.resumo && (
            <p className="mt-1 truncate text-sm text-gray-500 dark:text-gray-400">
              {comunicado.resumo}
            </p>
          )}
          <div className="mt-2 flex items-center gap-3 text-xs text-gray-400">
            <span
              className={`rounded-full px-2 py-0.5 ${prioridadeColors[comunicado.prioridade]}`}
            >
              {comunicado.prioridadeDescricao}
            </span>
            <span>{formatDate(comunicado.dataEnvio)}</span>
            {comunicado.requerConfirmacao && !comunicado.confirmado && (
              <span className="flex items-center gap-1 text-orange-500">
                <AlertTriangle className="h-3 w-3" />
                Requer confirmacao
              </span>
            )}
          </div>
        </div>
      </div>
    </button>
  );
}
