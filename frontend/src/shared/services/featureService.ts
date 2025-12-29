/**
 * Feature Flag Service - Para oficinas verificarem suas features habilitadas
 */

import { api } from './api';

export interface OficinaFeatures {
  oficinaId: string | null;
  features: Record<string, boolean>;
}

export interface FeatureCheckResult {
  codigo: string;
  enabled: boolean;
  oficinaId?: string;
  reason?: string;
}

export const featureService = {
  /**
   * Retorna todas as features habilitadas para a oficina do usuário logado
   */
  async getMyFeatures(): Promise<OficinaFeatures> {
    const { data } = await api.get<OficinaFeatures>('/features/me');
    return data;
  },

  /**
   * Verifica se uma feature específica está habilitada
   */
  async checkFeature(codigo: string): Promise<FeatureCheckResult> {
    const { data } = await api.get<FeatureCheckResult>(`/features/check/${codigo}`);
    return data;
  },

  /**
   * Verifica múltiplas features de uma vez
   */
  async checkFeaturesBatch(codigos: string[]): Promise<Record<string, boolean>> {
    const { data } = await api.post<Record<string, boolean>>('/features/check-batch', codigos);
    return data;
  },
};
