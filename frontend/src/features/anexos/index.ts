// Components
export {
  AnexoUpload,
  AnexoLista,
  AnexosSection,
  AnexoThumbnail,
  AnexoViewer,
} from './components';

// Hooks
export {
  useAnexos,
  useAnexo,
  useQuota,
  useAnexosCount,
  useUploadAnexo,
  useDeleteAnexo,
  useDownloadAnexo,
  useAnexoImageUrl,
  anexoKeys,
} from './hooks/useAnexos';

// Service
export { anexoService } from './services/anexoService';

// Types
export type {
  EntidadeTipo,
  CategoriaAnexo,
  AnexoResponse,
  AnexoUploadRequest,
  QuotaResponse,
} from './types';

export { categoriaLabels, categoriasPorEntidade } from './types';
