import { FileImage, FileText, Loader2 } from 'lucide-react';
import { useAnexoImageUrl } from '../hooks/useAnexos';
import type { AnexoResponse } from '../types';

interface AnexoThumbnailProps {
  anexo: AnexoResponse;
  className?: string;
  onClick?: () => void;
}

/**
 * Componente para exibir thumbnail de anexo com autenticação.
 * Carrega a imagem via Axios (com JWT) e exibe usando blob URL.
 */
export function AnexoThumbnail({ anexo, className = '', onClick }: AnexoThumbnailProps) {
  const { url, isLoading } = useAnexoImageUrl(
    anexo.isImagem ? anexo.id : null,
    anexo.isImagem
  );

  const baseClass = `aspect-square bg-gray-100 dark:bg-gray-700 flex items-center justify-center ${onClick ? 'cursor-pointer' : ''} ${className}`;

  if (anexo.isImagem) {
    if (isLoading) {
      return (
        <div className={baseClass} onClick={onClick}>
          <Loader2 className="h-6 w-6 sm:h-8 sm:w-8 text-gray-400 dark:text-gray-500 animate-spin" />
        </div>
      );
    }

    if (url) {
      return (
        <div className={baseClass} onClick={onClick}>
          <img
            src={url}
            alt={anexo.nomeOriginal}
            className="w-full h-full object-cover"
          />
        </div>
      );
    }

    // Fallback se não conseguiu carregar
    return (
      <div className={baseClass} onClick={onClick}>
        <FileImage className="h-10 w-10 sm:h-12 sm:w-12 text-gray-400 dark:text-gray-500" />
      </div>
    );
  }

  if (anexo.isPdf) {
    return (
      <div className={baseClass} onClick={onClick}>
        <FileText className="h-10 w-10 sm:h-12 sm:w-12 text-red-500 dark:text-red-400" />
      </div>
    );
  }

  return (
    <div className={baseClass} onClick={onClick}>
      <FileImage className="h-10 w-10 sm:h-12 sm:w-12 text-gray-400 dark:text-gray-500" />
    </div>
  );
}
