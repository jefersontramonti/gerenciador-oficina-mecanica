/**
 * Service para chamadas à API de IA
 */

import { api } from '@/shared/services/api';
import type {
  ConfiguracaoIA,
  ConfiguracaoIARequest,
  AtualizarApiKeyRequest,
  DiagnosticoIARequest,
  DiagnosticoIAResponse,
  EstatisticasUsoIA,
} from '../types';

export const iaService = {
  // ===== Configuração =====

  /**
   * Busca configuração de IA da oficina atual
   */
  async getConfiguracao(): Promise<ConfiguracaoIA> {
    const { data } = await api.get<ConfiguracaoIA>('/configuracao-ia');
    return data;
  },

  /**
   * Atualiza configuração de IA
   */
  async atualizarConfiguracao(request: ConfiguracaoIARequest): Promise<ConfiguracaoIA> {
    const { data } = await api.put<ConfiguracaoIA>('/configuracao-ia', request);
    return data;
  },

  /**
   * Atualiza a API key (criptografada no backend)
   */
  async atualizarApiKey(request: AtualizarApiKeyRequest): Promise<ConfiguracaoIA> {
    const { data } = await api.put<ConfiguracaoIA>('/configuracao-ia/api-key', request);
    return data;
  },

  /**
   * Remove a API key configurada
   */
  async removerApiKey(): Promise<ConfiguracaoIA> {
    const { data } = await api.delete<ConfiguracaoIA>('/configuracao-ia/api-key');
    return data;
  },

  // ===== Diagnóstico =====

  /**
   * Gera diagnóstico assistido por IA
   */
  async gerarDiagnostico(request: DiagnosticoIARequest): Promise<DiagnosticoIAResponse> {
    const { data } = await api.post<DiagnosticoIAResponse>('/diagnostico-ia', request);
    return data;
  },

  /**
   * Verifica se a IA está disponível para a oficina
   */
  async verificarDisponibilidade(): Promise<boolean> {
    const { data } = await api.get<{ disponivel: boolean }>('/diagnostico-ia/disponivel');
    return data.disponivel;
  },

  /**
   * Obtém estatísticas de uso da IA
   */
  async getEstatisticas(): Promise<EstatisticasUsoIA> {
    const { data } = await api.get<EstatisticasUsoIA>('/diagnostico-ia/estatisticas');
    return data;
  },
};
