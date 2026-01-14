import { api } from '@/shared/services/api';
import type {
  AlterarVisibilidadeRequest,
  AnexoResponse,
  AnexoUploadRequest,
  EntidadeTipo,
  QuotaResponse,
} from '../types';

const BASE_URL = '/anexos';

/**
 * Serviço para gerenciamento de anexos.
 */
export const anexoService = {
  /**
   * Faz upload de um arquivo.
   */
  async upload(request: AnexoUploadRequest): Promise<AnexoResponse> {
    const formData = new FormData();
    formData.append('file', request.file);
    formData.append('entidadeTipo', request.entidadeTipo);
    formData.append('entidadeId', String(request.entidadeId));

    if (request.categoria) {
      formData.append('categoria', request.categoria);
    }
    if (request.descricao) {
      formData.append('descricao', request.descricao);
    }

    const response = await api.post<AnexoResponse>(`${BASE_URL}/upload`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  /**
   * Lista anexos de uma entidade.
   */
  async listarPorEntidade(
    entidadeTipo: EntidadeTipo,
    entidadeId: string
  ): Promise<AnexoResponse[]> {
    const response = await api.get<AnexoResponse[]>(
      `${BASE_URL}/entidade/${entidadeTipo}/${entidadeId}`
    );
    return response.data;
  },

  /**
   * Busca anexo por ID.
   */
  async buscarPorId(id: string): Promise<AnexoResponse> {
    const response = await api.get<AnexoResponse>(`${BASE_URL}/${id}`);
    return response.data;
  },

  /**
   * Faz download de um arquivo.
   */
  async download(id: string): Promise<Blob> {
    const response = await api.get(`${BASE_URL}/${id}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },

  /**
   * Obtém arquivo para visualização inline (com autenticação).
   */
  async view(id: string): Promise<Blob> {
    const response = await api.get(`${BASE_URL}/${id}/view`, {
      responseType: 'blob',
    });
    return response.data;
  },

  /**
   * Retorna URL para visualização do arquivo.
   * @deprecated Use `view()` com blob URL para requisições autenticadas
   */
  getViewUrl(id: string): string {
    return `${api.defaults.baseURL}${BASE_URL}/${id}/view`;
  },

  /**
   * Retorna URL para download do arquivo.
   */
  getDownloadUrl(id: string): string {
    return `${api.defaults.baseURL}${BASE_URL}/${id}/download`;
  },

  /**
   * Deleta um anexo.
   */
  async deletar(id: string): Promise<void> {
    await api.delete(`${BASE_URL}/${id}`);
  },

  /**
   * Retorna informações de quota.
   */
  async getQuota(): Promise<QuotaResponse> {
    const response = await api.get<QuotaResponse>(`${BASE_URL}/quota`);
    return response.data;
  },

  /**
   * Conta anexos de uma entidade.
   */
  async contarPorEntidade(
    entidadeTipo: EntidadeTipo,
    entidadeId: string
  ): Promise<number> {
    const response = await api.get<number>(
      `${BASE_URL}/entidade/${entidadeTipo}/${entidadeId}/count`
    );
    return response.data;
  },

  /**
   * Altera a visibilidade de um anexo para o cliente.
   */
  async alterarVisibilidade(
    id: string,
    request: AlterarVisibilidadeRequest
  ): Promise<AnexoResponse> {
    const response = await api.patch<AnexoResponse>(
      `${BASE_URL}/${id}/visibilidade`,
      request
    );
    return response.data;
  },
};
