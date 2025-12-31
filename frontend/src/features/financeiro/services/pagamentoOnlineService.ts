/**
 * Service para Pagamentos Online e Configuração de Gateways
 */

import { api } from '@/shared/services/api';
import type {
  ConfiguracaoGateway,
  ConfiguracaoGatewayRequest,
  PagamentoOnline,
  CriarCheckoutRequest,
  CheckoutResponse,
} from '../types/pagamentoOnline';
import { TipoGateway } from '../types/pagamentoOnline';

// ========== Configuração de Gateway ==========

export const gatewayService = {
  /**
   * Lista todas as configurações de gateway da oficina
   */
  listar: async (): Promise<ConfiguracaoGateway[]> => {
    const response = await api.get('/configuracoes/gateways');
    return response.data;
  },

  /**
   * Busca configuração por ID
   */
  buscarPorId: async (id: string): Promise<ConfiguracaoGateway> => {
    const response = await api.get(`/configuracoes/gateways/${id}`);
    return response.data;
  },

  /**
   * Busca configuração por tipo
   */
  buscarPorTipo: async (tipo: TipoGateway): Promise<ConfiguracaoGateway | null> => {
    try {
      const response = await api.get(`/configuracoes/gateways/tipo/${tipo}`);
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) return null;
      throw error;
    }
  },

  /**
   * Cria nova configuração
   */
  criar: async (data: ConfiguracaoGatewayRequest): Promise<ConfiguracaoGateway> => {
    const response = await api.post('/configuracoes/gateways', data);
    return response.data;
  },

  /**
   * Atualiza configuração existente
   */
  atualizar: async (id: string, data: ConfiguracaoGatewayRequest): Promise<ConfiguracaoGateway> => {
    const response = await api.put(`/configuracoes/gateways/${id}`, data);
    return response.data;
  },

  /**
   * Valida credenciais do gateway
   */
  validar: async (id: string): Promise<ConfiguracaoGateway> => {
    const response = await api.post(`/configuracoes/gateways/${id}/validar`);
    return response.data;
  },

  /**
   * Ativa o gateway
   */
  ativar: async (id: string): Promise<ConfiguracaoGateway> => {
    const response = await api.post(`/configuracoes/gateways/${id}/ativar`);
    return response.data;
  },

  /**
   * Desativa o gateway
   */
  desativar: async (id: string): Promise<ConfiguracaoGateway> => {
    const response = await api.post(`/configuracoes/gateways/${id}/desativar`);
    return response.data;
  },

  /**
   * Remove configuração
   */
  remover: async (id: string): Promise<void> => {
    await api.delete(`/configuracoes/gateways/${id}`);
  },

  /**
   * Busca Public Key para uso no frontend (Checkout Bricks)
   */
  getPublicKey: async (): Promise<{ publicKey: string; ambiente: string } | null> => {
    try {
      const response = await api.get('/configuracoes/gateways/public-key');
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) return null;
      throw error;
    }
  },
};

// ========== Pagamentos Online ==========

export const pagamentoOnlineService = {
  /**
   * Cria checkout de pagamento
   */
  criarCheckout: async (data: CriarCheckoutRequest): Promise<CheckoutResponse> => {
    const response = await api.post('/pagamentos-online/checkout', data);
    return response.data;
  },

  /**
   * Busca pagamento online por ID
   */
  buscarPorId: async (id: string): Promise<PagamentoOnline> => {
    const response = await api.get(`/pagamentos-online/${id}`);
    return response.data;
  },

  /**
   * Lista pagamentos online de uma OS
   */
  listarPorOS: async (osId: string): Promise<PagamentoOnline[]> => {
    const response = await api.get(`/pagamentos-online/ordem-servico/${osId}`);
    return response.data;
  },

  /**
   * Lista todos os pagamentos online
   */
  listar: async (page = 0, size = 20): Promise<{ content: PagamentoOnline[]; totalElements: number }> => {
    const response = await api.get('/pagamentos-online', { params: { page, size } });
    return response.data;
  },

  /**
   * Força atualização de status
   */
  atualizarStatus: async (id: string): Promise<PagamentoOnline> => {
    const response = await api.post(`/pagamentos-online/${id}/atualizar-status`);
    return response.data;
  },
};
