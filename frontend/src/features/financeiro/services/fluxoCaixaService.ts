import { api } from '@/shared/services/api';
import type { FluxoCaixa, DRESimplificado, ProjecaoFinanceira } from '../types/fluxoCaixa';

const BASE_URL = '/financeiro/fluxo-caixa';

/**
 * Serviço para API de Fluxo de Caixa, DRE e Projeções.
 */
export const fluxoCaixaService = {
  // ========== Fluxo de Caixa ==========

  /**
   * Busca fluxo de caixa para um período.
   */
  async getFluxoCaixa(inicio: string, fim: string): Promise<FluxoCaixa> {
    const response = await api.get<FluxoCaixa>(BASE_URL, {
      params: { inicio, fim }
    });
    return response.data;
  },

  /**
   * Busca fluxo de caixa do mês atual.
   */
  async getFluxoCaixaMesAtual(): Promise<FluxoCaixa> {
    const response = await api.get<FluxoCaixa>(`${BASE_URL}/mes-atual`);
    return response.data;
  },

  /**
   * Busca fluxo de caixa dos últimos N dias.
   */
  async getFluxoCaixaUltimosDias(dias: number = 30): Promise<FluxoCaixa> {
    const response = await api.get<FluxoCaixa>(`${BASE_URL}/ultimos-dias`, {
      params: { dias }
    });
    return response.data;
  },

  // ========== DRE Simplificado ==========

  /**
   * Busca DRE de um mês específico.
   */
  async getDRE(mes: number, ano: number): Promise<DRESimplificado> {
    const response = await api.get<DRESimplificado>(`${BASE_URL}/dre`, {
      params: { mes, ano }
    });
    return response.data;
  },

  /**
   * Busca DRE do mês atual.
   */
  async getDREMesAtual(): Promise<DRESimplificado> {
    const response = await api.get<DRESimplificado>(`${BASE_URL}/dre/mes-atual`);
    return response.data;
  },

  /**
   * Busca DRE do mês anterior.
   */
  async getDREMesAnterior(): Promise<DRESimplificado> {
    const response = await api.get<DRESimplificado>(`${BASE_URL}/dre/mes-anterior`);
    return response.data;
  },

  // ========== Projeção Financeira ==========

  /**
   * Busca projeção financeira para os próximos N dias.
   */
  async getProjecao(dias: number = 30): Promise<ProjecaoFinanceira> {
    const response = await api.get<ProjecaoFinanceira>(`${BASE_URL}/projecao`, {
      params: { dias }
    });
    return response.data;
  },

  /**
   * Busca projeção semanal (7 dias).
   */
  async getProjecaoSemanal(): Promise<ProjecaoFinanceira> {
    const response = await api.get<ProjecaoFinanceira>(`${BASE_URL}/projecao/semanal`);
    return response.data;
  },

  /**
   * Busca projeção mensal (30 dias).
   */
  async getProjecaoMensal(): Promise<ProjecaoFinanceira> {
    const response = await api.get<ProjecaoFinanceira>(`${BASE_URL}/projecao/mensal`);
    return response.data;
  },

  /**
   * Busca projeção trimestral (90 dias).
   */
  async getProjecaoTrimestral(): Promise<ProjecaoFinanceira> {
    const response = await api.get<ProjecaoFinanceira>(`${BASE_URL}/projecao/trimestral`);
    return response.data;
  },
};

export default fluxoCaixaService;
