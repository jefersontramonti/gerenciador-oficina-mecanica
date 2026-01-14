import { useState } from 'react';
import { ChevronDown, ChevronUp, Paperclip, Plus, X } from 'lucide-react';
import { AnexoUpload } from './AnexoUpload';
import { AnexoLista } from './AnexoLista';
import { useAnexosCount } from '../hooks/useAnexos';
import type { EntidadeTipo } from '../types';

interface AnexosSectionProps {
  entidadeTipo: EntidadeTipo;
  entidadeId: string;
  title?: string;
  defaultExpanded?: boolean;
  showUpload?: boolean;
  showVisibilityToggle?: boolean;
}

/**
 * Seção completa de anexos com upload e listagem.
 * Pode ser usada em qualquer página de detalhes.
 */
export function AnexosSection({
  entidadeTipo,
  entidadeId,
  title = 'Anexos',
  defaultExpanded = true,
  showUpload = true,
  showVisibilityToggle = false,
}: AnexosSectionProps) {
  const [expanded, setExpanded] = useState(defaultExpanded);
  const [showUploadForm, setShowUploadForm] = useState(false);
  const { data: count = 0 } = useAnexosCount(entidadeTipo, entidadeId);

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 shadow-sm">
      {/* Header */}
      <button
        type="button"
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center justify-between p-3 sm:p-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors rounded-t-lg"
      >
        <div className="flex items-center gap-2">
          <Paperclip className="h-4 w-4 sm:h-5 sm:w-5 text-gray-500 dark:text-gray-400" />
          <h3 className="font-semibold text-gray-900 dark:text-white text-sm sm:text-base">
            {title}
          </h3>
          {count > 0 && (
            <span className="bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400 text-xs font-medium px-2 py-0.5 rounded-full">
              {count}
            </span>
          )}
        </div>
        {expanded ? (
          <ChevronUp className="h-4 w-4 sm:h-5 sm:w-5 text-gray-400 dark:text-gray-500" />
        ) : (
          <ChevronDown className="h-4 w-4 sm:h-5 sm:w-5 text-gray-400 dark:text-gray-500" />
        )}
      </button>

      {/* Content */}
      {expanded && (
        <div className="border-t border-gray-200 dark:border-gray-700 p-3 sm:p-4 space-y-4">
          {/* Botão de adicionar / Form de upload */}
          {showUpload && (
            <>
              {!showUploadForm ? (
                <button
                  type="button"
                  onClick={() => setShowUploadForm(true)}
                  className="flex items-center gap-2 text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 font-medium text-sm sm:text-base"
                >
                  <Plus className="h-4 w-4" />
                  Adicionar anexo
                </button>
              ) : (
                <div className="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3 sm:p-4 border border-gray-200 dark:border-gray-600">
                  <div className="flex items-center justify-between mb-3">
                    <h4 className="font-medium text-gray-700 dark:text-gray-200 text-sm sm:text-base">
                      Novo anexo
                    </h4>
                    <button
                      type="button"
                      onClick={() => setShowUploadForm(false)}
                      className="text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300 p-1"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                  <AnexoUpload
                    entidadeTipo={entidadeTipo}
                    entidadeId={entidadeId}
                    onUploadComplete={() => setShowUploadForm(false)}
                  />
                </div>
              )}
            </>
          )}

          {/* Lista de anexos */}
          <AnexoLista
            entidadeTipo={entidadeTipo}
            entidadeId={entidadeId}
            showActions={showUpload}
            showVisibilityToggle={showVisibilityToggle}
          />
        </div>
      )}
    </div>
  );
}
