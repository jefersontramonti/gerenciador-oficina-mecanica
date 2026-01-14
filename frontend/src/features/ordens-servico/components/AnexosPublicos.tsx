import { useState, useEffect } from 'react';
import { Loader2, Image as ImageIcon, Download, Eye, X, FileText } from 'lucide-react';
import axios from 'axios';
import type { AnexoPublicoResponse, CategoriaAnexo } from '@/features/anexos/types';

// API URL base - usa variável de ambiente para funcionar em dev e produção
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

interface AnexosPublicosProps {
  token: string;
}

/**
 * Componente para exibir anexos públicos na página de aprovação de orçamento.
 * Não requer autenticação - usa token da URL.
 */
export function AnexosPublicos({ token }: AnexosPublicosProps) {
  const [anexos, setAnexos] = useState<AnexoPublicoResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [_error, setError] = useState<string | null>(null);
  const [viewingAnexo, setViewingAnexo] = useState<AnexoPublicoResponse | null>(null);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [loadingImage, setLoadingImage] = useState(false);

  // Carrega anexos públicos
  useEffect(() => {
    const fetchAnexos = async () => {
      try {
        const response = await axios.get<AnexoPublicoResponse[]>(
          `${API_BASE_URL}/api/public/orcamento/${token}/anexos`
        );
        setAnexos(response.data);
      } catch (err: any) {
        // Silently ignore errors - attachments are optional
        console.error('Error fetching public attachments:', err);
      } finally {
        setLoading(false);
      }
    };

    if (token) {
      fetchAnexos();
    }
  }, [token]);

  // Carrega imagem para visualização
  useEffect(() => {
    if (!viewingAnexo) {
      setImageUrl(null);
      return;
    }

    let blobUrl: string | null = null;
    let cancelled = false;

    const fetchImage = async () => {
      setLoadingImage(true);
      try {
        const response = await axios.get(
          `${API_BASE_URL}/api/public/orcamento/${token}/anexos/${viewingAnexo.id}/view`,
          { responseType: 'blob' }
        );
        if (!cancelled) {
          blobUrl = URL.createObjectURL(response.data);
          setImageUrl(blobUrl);
        }
      } catch (err) {
        console.error('Error loading image:', err);
        if (!cancelled) {
          setError('Erro ao carregar imagem');
        }
      } finally {
        if (!cancelled) {
          setLoadingImage(false);
        }
      }
    };

    if (viewingAnexo.isImagem) {
      fetchImage();
    }

    return () => {
      cancelled = true;
      if (blobUrl) {
        URL.revokeObjectURL(blobUrl);
      }
    };
  }, [viewingAnexo, token]);

  const handleView = (anexo: AnexoPublicoResponse) => {
    if (anexo.isImagem) {
      setViewingAnexo(anexo);
    } else {
      // Download para PDFs
      handleDownload(anexo);
    }
  };

  const handleDownload = async (anexo: AnexoPublicoResponse) => {
    try {
      const response = await axios.get(
        `${API_BASE_URL}/api/public/orcamento/${token}/anexos/${anexo.id}/view`,
        { responseType: 'blob' }
      );
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = anexo.nomeOriginal;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Error downloading file:', err);
    }
  };

  const closeViewer = () => {
    setViewingAnexo(null);
  };

  // Não mostra nada se estiver carregando ou não tiver anexos
  if (loading) {
    return (
      <div className="flex items-center justify-center py-6">
        <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
      </div>
    );
  }

  if (anexos.length === 0) {
    return null; // Não mostra seção se não tiver anexos
  }

  // Labels para categorias
  const categoriaLabels: Record<CategoriaAnexo, string> = {
    FOTO_VEICULO: 'Foto do Veículo',
    DIAGNOSTICO: 'Diagnóstico',
    AUTORIZACAO: 'Autorização',
    LAUDO_TECNICO: 'Laudo Técnico',
    DOCUMENTO_PESSOAL: 'Documento Pessoal',
    DOCUMENTO_EMPRESA: 'Documento Empresa',
    CONTRATO: 'Contrato',
    DOCUMENTO_VEICULO: 'Documento do Veículo',
    FOTO_PECA: 'Foto da Peça',
    NOTA_FISCAL: 'Nota Fiscal',
    CERTIFICADO: 'Certificado',
    OUTROS: 'Outros',
  };

  return (
    <>
      <div className="mt-6 rounded-lg bg-gray-50 p-4">
        <h3 className="mb-3 text-lg font-semibold text-gray-900 flex items-center gap-2">
          <ImageIcon className="h-5 w-5 text-blue-600" />
          Fotos e Documentos
        </h3>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          {anexos.map((anexo) => (
            <div
              key={anexo.id}
              className="group relative bg-white border border-gray-200 rounded-lg overflow-hidden cursor-pointer hover:shadow-md transition-shadow"
              onClick={() => handleView(anexo)}
            >
              {/* Thumbnail placeholder */}
              <div className="aspect-square bg-gray-100 flex items-center justify-center">
                {anexo.isImagem ? (
                  <ThumbnailImage token={token} anexoId={anexo.id} />
                ) : (
                  <FileText className="h-12 w-12 text-gray-400" />
                )}
              </div>

              {/* Overlay com ações */}
              <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleView(anexo);
                  }}
                  className="p-2 bg-white rounded-full hover:bg-gray-100"
                  title="Visualizar"
                >
                  <Eye className="h-4 w-4 text-gray-700" />
                </button>
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleDownload(anexo);
                  }}
                  className="p-2 bg-white rounded-full hover:bg-gray-100"
                  title="Baixar"
                >
                  <Download className="h-4 w-4 text-gray-700" />
                </button>
              </div>

              {/* Info */}
              <div className="p-2">
                <p className="text-xs font-medium text-gray-700 truncate" title={anexo.nomeOriginal}>
                  {anexo.nomeOriginal}
                </p>
                <p className="text-xs text-gray-500">
                  {categoriaLabels[anexo.categoria] || anexo.categoria}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Modal de visualização */}
      {viewingAnexo && (
        <div
          className="fixed inset-0 bg-black/80 z-50 flex items-center justify-center p-4"
          onClick={closeViewer}
        >
          <div
            className="relative max-w-4xl max-h-[90vh] w-full"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Botão fechar */}
            <button
              type="button"
              onClick={closeViewer}
              className="absolute -top-10 right-0 text-white hover:text-gray-300 p-2"
            >
              <X className="h-6 w-6" />
            </button>

            {/* Conteúdo */}
            <div className="bg-white rounded-lg overflow-hidden">
              {loadingImage ? (
                <div className="flex items-center justify-center h-64">
                  <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
                </div>
              ) : imageUrl ? (
                <img
                  src={imageUrl}
                  alt={viewingAnexo.nomeOriginal}
                  className="w-full h-auto max-h-[80vh] object-contain"
                />
              ) : (
                <div className="flex items-center justify-center h-64 text-gray-500">
                  Erro ao carregar imagem
                </div>
              )}
            </div>

            {/* Nome do arquivo */}
            <div className="mt-2 text-center text-white">
              <p className="text-sm">{viewingAnexo.nomeOriginal}</p>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

/**
 * Componente para carregar thumbnail de imagem pública.
 */
function ThumbnailImage({ token, anexoId }: { token: string; anexoId: string }) {
  const [url, setUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let blobUrl: string | null = null;
    let cancelled = false;

    const fetchThumbnail = async () => {
      try {
        const response = await axios.get(
          `${API_BASE_URL}/api/public/orcamento/${token}/anexos/${anexoId}/view`,
          { responseType: 'blob' }
        );
        if (!cancelled) {
          blobUrl = URL.createObjectURL(response.data);
          setUrl(blobUrl);
        }
      } catch (err) {
        console.error('Error loading thumbnail:', err);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchThumbnail();

    return () => {
      cancelled = true;
      if (blobUrl) {
        URL.revokeObjectURL(blobUrl);
      }
    };
  }, [token, anexoId]);

  if (loading) {
    return <Loader2 className="h-8 w-8 animate-spin text-gray-300" />;
  }

  if (!url) {
    return <ImageIcon className="h-12 w-12 text-gray-300" />;
  }

  return (
    <img
      src={url}
      alt=""
      className="w-full h-full object-cover"
    />
  );
}
