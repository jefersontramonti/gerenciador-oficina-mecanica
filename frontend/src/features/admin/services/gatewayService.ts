/**
 * Service para configuração de gateway de pagamento SaaS
 */

import { api } from '@/shared/services/api';
import type { ConfiguracaoGateway, ConfiguracaoGatewayRequest } from '../types/gateway';

export const gatewayService = {
  /**
   * Busca configuração do Mercado Pago.
   */
  async getMercadoPago(): Promise<ConfiguracaoGateway> {
    const { data } = await api.get<ConfiguracaoGateway>('/saas/configuracoes/gateway/mercadopago');
    return data;
  },

  /**
   * Salva configuração do Mercado Pago.
   */
  async salvarMercadoPago(request: ConfiguracaoGatewayRequest): Promise<ConfiguracaoGateway> {
    const { data } = await api.post<ConfiguracaoGateway>(
      '/saas/configuracoes/gateway/mercadopago',
      request
    );
    return data;
  },

  /**
   * Valida credenciais do Mercado Pago.
   */
  async validarMercadoPago(): Promise<ConfiguracaoGateway> {
    const { data } = await api.post<ConfiguracaoGateway>(
      '/saas/configuracoes/gateway/mercadopago/validar'
    );
    return data;
  },

  /**
   * Verifica se o Mercado Pago está configurado.
   */
  async statusMercadoPago(): Promise<boolean> {
    const { data } = await api.get<boolean>('/saas/configuracoes/gateway/mercadopago/status');
    return data;
  },
};
