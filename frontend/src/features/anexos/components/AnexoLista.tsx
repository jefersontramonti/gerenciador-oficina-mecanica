import { useState } from 'react';
import {
  Download,
  Trash2,
  Eye,
  EyeOff,
  Loader2,
  Image as ImageIcon,
} from 'lucide-react';
import { useAnexos, useDeleteAnexo, useDownloadAnexo, useAlterarVisibilidade } from '../hooks/useAnexos';
import { AnexoThumbnail } from './AnexoThumbnail';
import { AnexoViewer } from './AnexoViewer';
import type { AnexoResponse, EntidadeTipo } from '../types';
import { categoriaLabels } from '../types';

interface AnexoListaProps {
  entidadeTipo: EntidadeTipo;
  entidadeId: string;
  showActions?: boolean;
  showVisibilityToggle?: boolean;
}

export function AnexoLista({
  entidadeTipo,
  entidadeId,
  showActions = true,
  showVisibilityToggle = false,
}: AnexoListaProps) {
  const { data: anexos, isLoading, error } = useAnexos(entidadeTipo, entidadeId);
  const deleteMutation = useDeleteAnexo();
  const downloadMutation = useDownloadAnexo();
  const visibilidadeMutation = useAlterarVisibilidade();
  const [viewingAnexo, setViewingAnexo] = useState<AnexoResponse | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Loader2 className="h-6 w-6 animate-spin text-gray-400 dark:text-gray-500" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-8 text-red-500 dark:text-red-400">
        Erro ao carregar anexos
      </div>
    );
  }

  if (!anexos || anexos.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500 dark:text-gray-400">
        <ImageIcon className="h-12 w-12 mx-auto mb-2 text-gray-300 dark:text-gray-600" />
        <p>Nenhum anexo encontrado</p>
      </div>
    );
  }

  const handleDelete = async (id: string) => {
    await deleteMutation.mutateAsync({
      id,
      entidadeTipo,
      entidadeId,
    });
    setConfirmDelete(null);
  };

  const handleDownload = (anexo: AnexoResponse) => {
    downloadMutation.mutate({
      id: anexo.id,
      nomeOriginal: anexo.nomeOriginal,
    });
  };

  const handleView = (anexo: AnexoResponse) => {
    if (anexo.isImagem || anexo.isPdf) {
      setViewingAnexo(anexo);
    } else {
      handleDownload(anexo);
    }
  };

  const handleToggleVisibilidade = (anexo: AnexoResponse) => {
    visibilidadeMutation.mutate({
      id: anexo.id,
      visivelParaCliente: !anexo.visivelParaCliente,
      entidadeTipo,
      entidadeId,
    });
  };

  // Agrupa anexos por categoria
  const anexosPorCategoria = anexos.reduce(
    (acc, anexo) => {
      const cat = anexo.categoria;
      if (!acc[cat]) acc[cat] = [];
      acc[cat].push(anexo);
      return acc;
    },
    {} as Record<string, AnexoResponse[]>
  );

  return (
    <>
      <div className="space-y-6">
        {Object.entries(anexosPorCategoria).map(([categoria, anexosList]) => (
          <div key={categoria}>
            <h4 className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-2">
              {categoriaLabels[categoria as keyof typeof categoriaLabels] || categoria}
            </h4>
            {/* Grid responsivo: 2 colunas em mobile, aumentando conforme tela */}
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-2 sm:gap-3">
              {anexosList.map((anexo) => (
                <div
                  key={anexo.id}
                  className="group relative bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden hover:shadow-md dark:hover:shadow-gray-900/50 transition-shadow"
                >
                  {/* Thumbnail com autenticação */}
                  <AnexoThumbnail
                    anexo={anexo}
                    onClick={() => handleView(anexo)}
                  />

                  {/* Badge de visibilidade para cliente */}
                  {showVisibilityToggle && anexo.visivelParaCliente && (
                    <div className="absolute top-1 left-1">
                      <span className="inline-flex items-center gap-1 rounded-full bg-green-100 dark:bg-green-900/50 px-1.5 py-0.5 text-xs font-medium text-green-700 dark:text-green-400">
                        <Eye className="h-3 w-3" />
                        <span className="hidden sm:inline">Visível</span>
                      </span>
                    </div>
                  )}

                  {/* Info */}
                  <div className="p-2">
                    <p
                      className="text-xs font-medium text-gray-700 dark:text-gray-200 truncate"
                      title={anexo.nomeOriginal}
                    >
                      {anexo.nomeOriginal}
                    </p>
                    <p className="text-xs text-gray-400 dark:text-gray-500">
                      {anexo.tamanhoFormatado}
                    </p>
                  </div>

                  {/* Actions overlay - visível em hover (desktop) ou sempre visível em mobile via botões menores */}
                  {showActions && (
                    <div className="absolute inset-0 bg-black/50 dark:bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-1 sm:gap-2">
                      {/* Toggle visibilidade para cliente */}
                      {showVisibilityToggle && (
                        <button
                          type="button"
                          onClick={() => handleToggleVisibilidade(anexo)}
                          disabled={visibilidadeMutation.isPending}
                          className={`p-1.5 sm:p-2 rounded-full transition-colors ${
                            anexo.visivelParaCliente
                              ? 'bg-green-500 dark:bg-green-600 hover:bg-green-600 dark:hover:bg-green-500'
                              : 'bg-white dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600'
                          }`}
                          title={anexo.visivelParaCliente ? 'Ocultar para cliente' : 'Mostrar para cliente'}
                        >
                          {anexo.visivelParaCliente ? (
                            <Eye className="h-3 w-3 sm:h-4 sm:w-4 text-white" />
                          ) : (
                            <EyeOff className="h-3 w-3 sm:h-4 sm:w-4 text-gray-700 dark:text-gray-200" />
                          )}
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => handleView(anexo)}
                        className="p-1.5 sm:p-2 bg-white dark:bg-gray-700 rounded-full hover:bg-gray-100 dark:hover:bg-gray-600"
                        title="Visualizar"
                      >
                        <Eye className="h-3 w-3 sm:h-4 sm:w-4 text-gray-700 dark:text-gray-200" />
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDownload(anexo)}
                        className="p-1.5 sm:p-2 bg-white dark:bg-gray-700 rounded-full hover:bg-gray-100 dark:hover:bg-gray-600"
                        title="Baixar"
                      >
                        <Download className="h-3 w-3 sm:h-4 sm:w-4 text-gray-700 dark:text-gray-200" />
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirmDelete(anexo.id)}
                        className="p-1.5 sm:p-2 bg-white dark:bg-gray-700 rounded-full hover:bg-red-50 dark:hover:bg-red-900/30"
                        title="Remover"
                      >
                        <Trash2 className="h-3 w-3 sm:h-4 sm:w-4 text-red-500 dark:text-red-400" />
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Modal de visualização com autenticação */}
      {viewingAnexo && (
        <AnexoViewer
          anexo={viewingAnexo}
          onClose={() => setViewingAnexo(null)}
        />
      )}

      {/* Modal de confirmação de exclusão */}
      {confirmDelete && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-800 rounded-lg p-4 sm:p-6 max-w-sm w-full shadow-xl">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
              Confirmar exclusão
            </h3>
            <p className="text-gray-600 dark:text-gray-400 mb-4 text-sm sm:text-base">
              Tem certeza que deseja remover este anexo? Esta ação não pode ser
              desfeita.
            </p>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setConfirmDelete(null)}
                className="px-3 sm:px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-50 dark:hover:bg-gray-700 text-sm sm:text-base"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => handleDelete(confirmDelete)}
                disabled={deleteMutation.isPending}
                className="px-3 sm:px-4 py-2 bg-red-600 dark:bg-red-700 text-white rounded hover:bg-red-700 dark:hover:bg-red-600 disabled:opacity-50 text-sm sm:text-base"
              >
                {deleteMutation.isPending ? 'Removendo...' : 'Remover'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
