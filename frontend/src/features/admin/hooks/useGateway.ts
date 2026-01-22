/**
 * Hooks para configuração de gateway de pagamento
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { gatewayService } from '../services/gatewayService';
import type { ConfiguracaoGatewayRequest } from '../types/gateway';

// Query keys
export const gatewayKeys = {
  all: ['gateway'] as const,
  mercadoPago: () => [...gatewayKeys.all, 'mercadopago'] as const,
  status: () => [...gatewayKeys.all, 'status'] as const,
};

/**
 * Hook para buscar configuração do Mercado Pago.
 */
export function useConfigMercadoPago() {
  return useQuery({
    queryKey: gatewayKeys.mercadoPago(),
    queryFn: () => gatewayService.getMercadoPago(),
    staleTime: 1000 * 60 * 5, // 5 minutos
  });
}

/**
 * Hook para salvar configuração do Mercado Pago.
 */
export function useSalvarMercadoPago() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: ConfiguracaoGatewayRequest) =>
      gatewayService.salvarMercadoPago(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: gatewayKeys.mercadoPago() });
      queryClient.invalidateQueries({ queryKey: gatewayKeys.status() });
    },
  });
}

/**
 * Hook para validar credenciais do Mercado Pago.
 */
export function useValidarMercadoPago() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => gatewayService.validarMercadoPago(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: gatewayKeys.mercadoPago() });
    },
  });
}

/**
 * Hook para verificar status do Mercado Pago.
 */
export function useStatusMercadoPago() {
  return useQuery({
    queryKey: gatewayKeys.status(),
    queryFn: () => gatewayService.statusMercadoPago(),
    staleTime: 1000 * 60 * 1, // 1 minuto
  });
}
