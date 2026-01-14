import { useCallback, useState } from 'react';
import { useDropzone } from 'react-dropzone';
import { Upload, X, FileImage, FileText, Loader2 } from 'lucide-react';
import { useUploadAnexo } from '../hooks/useAnexos';
import type { CategoriaAnexo, EntidadeTipo } from '../types';
import { categoriaLabels, categoriasPorEntidade } from '../types';

interface AnexoUploadProps {
  entidadeTipo: EntidadeTipo;
  entidadeId: string;
  onUploadComplete?: () => void;
}

interface FileToUpload {
  file: File;
  preview: string;
  categoria: CategoriaAnexo;
  descricao: string;
}

const ACCEPTED_TYPES = {
  'image/jpeg': ['.jpg', '.jpeg'],
  'image/png': ['.png'],
  'image/webp': ['.webp'],
  'application/pdf': ['.pdf'],
};

const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

export function AnexoUpload({
  entidadeTipo,
  entidadeId,
  onUploadComplete,
}: AnexoUploadProps) {
  const [files, setFiles] = useState<FileToUpload[]>([]);
  const uploadMutation = useUploadAnexo();

  const categorias = categoriasPorEntidade[entidadeTipo];

  const onDrop = useCallback(
    (acceptedFiles: File[]) => {
      const newFiles = acceptedFiles.map((file) => ({
        file,
        preview: file.type.startsWith('image/')
          ? URL.createObjectURL(file)
          : '',
        categoria: categorias[0], // Categoria padrão
        descricao: '',
      }));
      setFiles((prev) => [...prev, ...newFiles]);
    },
    [categorias]
  );

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: ACCEPTED_TYPES,
    maxSize: MAX_FILE_SIZE,
    multiple: true,
  });

  const removeFile = (index: number) => {
    setFiles((prev) => {
      const newFiles = [...prev];
      if (newFiles[index].preview) {
        URL.revokeObjectURL(newFiles[index].preview);
      }
      newFiles.splice(index, 1);
      return newFiles;
    });
  };

  const updateFile = (
    index: number,
    field: 'categoria' | 'descricao',
    value: string
  ) => {
    setFiles((prev) => {
      const newFiles = [...prev];
      newFiles[index] = { ...newFiles[index], [field]: value };
      return newFiles;
    });
  };

  const handleUpload = async () => {
    for (const fileToUpload of files) {
      await uploadMutation.mutateAsync({
        file: fileToUpload.file,
        entidadeTipo,
        entidadeId,
        categoria: fileToUpload.categoria,
        descricao: fileToUpload.descricao || undefined,
      });
    }
    // Limpa lista após upload
    files.forEach((f) => {
      if (f.preview) URL.revokeObjectURL(f.preview);
    });
    setFiles([]);
    onUploadComplete?.();
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="space-y-4">
      {/* Dropzone */}
      <div
        {...getRootProps()}
        className={`
          border-2 border-dashed rounded-lg p-4 sm:p-6 text-center cursor-pointer transition-colors
          ${isDragActive
            ? 'border-blue-500 dark:border-blue-400 bg-blue-50 dark:bg-blue-900/20'
            : 'border-gray-300 dark:border-gray-600 hover:border-gray-400 dark:hover:border-gray-500 hover:bg-gray-50 dark:hover:bg-gray-700/50'
          }
        `}
      >
        <input {...getInputProps()} />
        <Upload className="mx-auto h-8 w-8 sm:h-10 sm:w-10 text-gray-400 dark:text-gray-500 mb-2 sm:mb-3" />
        {isDragActive ? (
          <p className="text-blue-600 dark:text-blue-400 font-medium text-sm sm:text-base">
            Solte os arquivos aqui...
          </p>
        ) : (
          <>
            <p className="text-gray-600 dark:text-gray-300 font-medium text-sm sm:text-base">
              Arraste arquivos ou clique para selecionar
            </p>
            <p className="text-gray-400 dark:text-gray-500 text-xs sm:text-sm mt-1">
              JPG, PNG, WebP ou PDF (máx. 5MB cada)
            </p>
          </>
        )}
      </div>

      {/* Lista de arquivos a serem enviados */}
      {files.length > 0 && (
        <div className="space-y-3">
          <h4 className="font-medium text-gray-700 dark:text-gray-200 text-sm sm:text-base">
            Arquivos selecionados ({files.length})
          </h4>

          {files.map((fileToUpload, index) => (
            <div
              key={index}
              className="flex flex-col sm:flex-row sm:items-start gap-3 p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg border border-gray-200 dark:border-gray-600"
            >
              {/* Preview */}
              <div className="w-14 h-14 sm:w-16 sm:h-16 flex-shrink-0 bg-white dark:bg-gray-800 rounded border border-gray-200 dark:border-gray-600 flex items-center justify-center overflow-hidden">
                {fileToUpload.preview ? (
                  <img
                    src={fileToUpload.preview}
                    alt={fileToUpload.file.name}
                    className="w-full h-full object-cover"
                  />
                ) : fileToUpload.file.type === 'application/pdf' ? (
                  <FileText className="h-6 w-6 sm:h-8 sm:w-8 text-red-500 dark:text-red-400" />
                ) : (
                  <FileImage className="h-6 w-6 sm:h-8 sm:w-8 text-gray-400 dark:text-gray-500" />
                )}
              </div>

              {/* Info e controles */}
              <div className="flex-1 min-w-0 space-y-2">
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0 flex-1">
                    <p className="font-medium text-xs sm:text-sm text-gray-900 dark:text-white truncate">
                      {fileToUpload.file.name}
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      {formatFileSize(fileToUpload.file.size)}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => removeFile(index)}
                    className="text-gray-400 dark:text-gray-500 hover:text-red-500 dark:hover:text-red-400 p-1 flex-shrink-0"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>

                <div className="flex flex-col sm:flex-row gap-2">
                  <select
                    value={fileToUpload.categoria}
                    onChange={(e) =>
                      updateFile(index, 'categoria', e.target.value)
                    }
                    className="flex-1 text-xs sm:text-sm border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded px-2 py-1.5 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 focus:border-transparent"
                  >
                    {categorias.map((cat) => (
                      <option key={cat} value={cat}>
                        {categoriaLabels[cat]}
                      </option>
                    ))}
                  </select>
                  <input
                    type="text"
                    placeholder="Descrição (opcional)"
                    value={fileToUpload.descricao}
                    onChange={(e) =>
                      updateFile(index, 'descricao', e.target.value)
                    }
                    className="flex-1 text-xs sm:text-sm border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 rounded px-2 py-1.5 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 focus:border-transparent"
                  />
                </div>
              </div>
            </div>
          ))}

          {/* Botão de upload */}
          <button
            type="button"
            onClick={handleUpload}
            disabled={uploadMutation.isPending}
            className="w-full flex items-center justify-center gap-2 bg-blue-600 dark:bg-blue-700 hover:bg-blue-700 dark:hover:bg-blue-600 text-white font-medium py-2 px-4 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-sm sm:text-base"
          >
            {uploadMutation.isPending ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Enviando...
              </>
            ) : (
              <>
                <Upload className="h-4 w-4" />
                Enviar {files.length} arquivo{files.length > 1 ? 's' : ''}
              </>
            )}
          </button>
        </div>
      )}
    </div>
  );
}
