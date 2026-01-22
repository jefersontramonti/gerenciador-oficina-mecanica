/**
 * Service para Parcelamento
 */

import { api } from '@/shared/services/api';
import type {
  ConfiguracaoParcelamento,
  ConfiguracaoParcelamentoRequest,
  TabelaJuros,
  TabelaJurosRequest,
  SimulacaoParcelamento,
} from '../types/parcelamento';

export const parcelamentoService = {
  // ========== Simulação ==========

  /**
   * Simula parcelamento para um valor
   */
  simular: async (valor: number): Promise<SimulacaoParcelamento> => {
    const response = await api.get('/financeiro/parcelamento/simular', {
      params: { valor },
    });
    return response.data;
  },

  // ========== Configuração ==========

  /**
   * Busca configuração de parcelamento da oficina
   */
  buscarConfiguracao: async (): Promise<ConfiguracaoParcelamento> => {
    const response = await api.get('/financeiro/parcelamento/configuracao');
    return response.data;
  },

  /**
   * Salva configuração de parcelamento
   */
  salvarConfiguracao: async (
    data: ConfiguracaoParcelamentoRequest
  ): Promise<ConfiguracaoParcelamento> => {
    const response = await api.put('/financeiro/parcelamento/configuracao', data);
    return response.data;
  },

  // ========== Faixas de Juros ==========

  /**
   * Lista faixas de juros da oficina
   */
  listarFaixasJuros: async (): Promise<TabelaJuros[]> => {
    const response = await api.get('/financeiro/parcelamento/faixas-juros');
    return response.data;
  },

  /**
   * Cria nova faixa de juros
   */
  criarFaixaJuros: async (data: TabelaJurosRequest): Promise<TabelaJuros> => {
    const response = await api.post('/financeiro/parcelamento/faixas-juros', data);
    return response.data;
  },

  /**
   * Atualiza faixa de juros
   */
  atualizarFaixaJuros: async (
    id: string,
    data: TabelaJurosRequest
  ): Promise<TabelaJuros> => {
    const response = await api.put(`/financeiro/parcelamento/faixas-juros/${id}`, data);
    return response.data;
  },

  /**
   * Remove faixa de juros
   */
  removerFaixaJuros: async (id: string): Promise<void> => {
    await api.delete(`/financeiro/parcelamento/faixas-juros/${id}`);
  },
};
