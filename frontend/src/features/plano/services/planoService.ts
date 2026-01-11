/**
 * Service for fetching plan information
 */

import { api } from '@/shared/services/api';
import type { MeuPlanoDTO } from '../types';

export const planoService = {
  /**
   * Get the current workshop's plan information, enabled features,
   * and features available in the next plan (for upsell)
   */
  async getMeuPlano(): Promise<MeuPlanoDTO> {
    const { data } = await api.get<MeuPlanoDTO>('/features/meu-plano');
    return data;
  },

  /**
   * Check if a specific feature is enabled
   */
  async checkFeature(codigo: string): Promise<{ codigo: string; enabled: boolean }> {
    const { data } = await api.get<{ codigo: string; enabled: boolean }>(
      `/features/check/${codigo}`
    );
    return data;
  },

  /**
   * Check multiple features at once
   */
  async checkFeatures(codigos: string[]): Promise<Record<string, boolean>> {
    const { data } = await api.post<Record<string, boolean>>(
      '/features/check-batch',
      codigos
    );
    return data;
  },
};
