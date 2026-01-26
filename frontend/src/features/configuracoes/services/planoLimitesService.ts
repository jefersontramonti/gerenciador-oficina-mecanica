import { api } from '@/shared/services/api';
import type { UsoLimites } from '../types/planoLimites';

export const planoLimitesService = {
  /**
   * Obtém uso atual vs limites do plano
   */
  async obterUsoAtual(): Promise<UsoLimites> {
    const { data } = await api.get<UsoLimites>('/oficinas/limites');
    return data;
  },
};
