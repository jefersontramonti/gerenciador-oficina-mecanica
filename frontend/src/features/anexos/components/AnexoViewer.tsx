import { Download, X, Loader2 } from 'lucide-react';
import { useAnexoImageUrl, useDownloadAnexo } from '../hooks/useAnexos';
import type { AnexoResponse } from '../types';

interface AnexoViewerProps {
  anexo: AnexoResponse;
  onClose: () => void;
}

/**
 * Modal de visualização de anexo com autenticação.
 * Carrega o arquivo via Axios (com JWT) e exibe usando blob URL.
 */
export function AnexoViewer({ anexo, onClose }: AnexoViewerProps) {
  const { url, isLoading } = useAnexoImageUrl(anexo.id, true);
  const downloadMutation = useDownloadAnexo();

  const handleDownload = () => {
    downloadMutation.mutate({
      id: anexo.id,
      nomeOriginal: anexo.nomeOriginal,
    });
  };

  return (
    <div
      className="fixed inset-0 bg-black/80 dark:bg-black/90 z-50 flex items-center justify-center p-2 sm:p-4"
      onClick={onClose}
    >
      <div
        className="max-w-4xl w-full max-h-[90vh] overflow-auto bg-white dark:bg-gray-800 rounded-lg"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Botão fechar no canto */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-2 right-2 sm:top-4 sm:right-4 p-2 bg-black/50 dark:bg-white/20 rounded-full hover:bg-black/70 dark:hover:bg-white/30 z-10"
        >
          <X className="h-5 w-5 text-white" />
        </button>

        {/* Conteúdo */}
        <div className="min-h-[200px] flex items-center justify-center">
          {isLoading ? (
            <Loader2 className="h-10 w-10 text-gray-400 dark:text-gray-500 animate-spin" />
          ) : url ? (
            anexo.isImagem ? (
              <img
                src={url}
                alt={anexo.nomeOriginal}
                className="max-w-full h-auto"
              />
            ) : anexo.isPdf ? (
              <iframe
                src={url}
                className="w-full h-[60vh] sm:h-[70vh]"
                title={anexo.nomeOriginal}
              />
            ) : null
          ) : (
            <p className="text-gray-500 dark:text-gray-400">
              Não foi possível carregar o arquivo
            </p>
          )}
        </div>

        {/* Footer */}
        <div className="p-3 sm:p-4 border-t border-gray-200 dark:border-gray-700 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="min-w-0 flex-1">
            <p className="font-medium text-gray-900 dark:text-white truncate">
              {anexo.nomeOriginal}
            </p>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              {anexo.tamanhoFormatado} - Enviado por{' '}
              {anexo.uploadedByNome || 'Usuário'}
            </p>
          </div>
          <div className="flex gap-2 w-full sm:w-auto">
            <button
              type="button"
              onClick={handleDownload}
              disabled={downloadMutation.isPending}
              className="flex-1 sm:flex-none flex items-center justify-center gap-1 px-3 py-1.5 bg-blue-600 dark:bg-blue-700 text-white rounded hover:bg-blue-700 dark:hover:bg-blue-600 disabled:opacity-50"
            >
              <Download className="h-4 w-4" />
              <span className="text-sm">Baixar</span>
            </button>
            <button
              type="button"
              onClick={onClose}
              className="flex-1 sm:flex-none px-3 py-1.5 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              <span className="text-sm">Fechar</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
