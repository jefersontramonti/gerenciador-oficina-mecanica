/**
 * Service for Comunicados (Oficina side)
 */

import { api } from '@/shared/services/api';
import type { ComunicadoOficina, ComunicadoOficinaDetail, ComunicadoAlerta } from '../types';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const comunicadoOficinaService = {
  /**
   * Lista comunicados recebidos pela oficina
   */
  async findAll(page = 0, size = 20): Promise<PageResponse<ComunicadoOficina>> {
    const { data } = await api.get<PageResponse<ComunicadoOficina>>('/comunicados', {
      params: { page, size },
    });
    return data;
  },

  /**
   * Busca detalhes de um comunicado (marca como lido automaticamente)
   */
  async findById(id: string): Promise<ComunicadoOficinaDetail> {
    const { data } = await api.get<ComunicadoOficinaDetail>(`/comunicados/${id}`);
    return data;
  },

  /**
   * Confirma leitura de um comunicado
   */
  async confirmar(id: string): Promise<void> {
    await api.post(`/comunicados/${id}/confirmar`);
  },

  /**
   * Conta comunicados não lidos
   */
  async contarNaoLidos(): Promise<number> {
    const { data } = await api.get<number>('/comunicados/nao-lidos/count');
    return data;
  },

  /**
   * Retorna dados para alerta no dashboard
   */
  async getAlerta(): Promise<ComunicadoAlerta> {
    const { data } = await api.get<ComunicadoAlerta>('/comunicados/alerta');
    return data;
  },

  /**
   * Retorna comunicados para exibir no login
   */
  async getComunicadosLogin(): Promise<ComunicadoOficina[]> {
    const { data } = await api.get<ComunicadoOficina[]>('/comunicados/login');
    return data;
  },

  /**
   * Marca todos como lidos
   */
  async marcarTodosLidos(): Promise<void> {
    await api.post('/comunicados/marcar-todos-lidos');
  },
};
