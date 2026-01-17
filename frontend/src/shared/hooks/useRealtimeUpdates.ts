import { useEffect, useCallback, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import type { QueryKey } from '@tanstack/react-query';
import { websocketService, NotificationType } from '../services/websocket';
import type { WebSocketNotification } from '../services/websocket';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { getAccessToken } from '../services/api';

/**
 * Mapping of notification types to query keys that should be invalidated
 */
const NOTIFICATION_TO_QUERY_KEYS: Record<string, string[][]> = {
  [NotificationType.OS_STATUS_CHANGED]: [
    ['ordens-servico'],
    ['dashboard'],
  ],
  [NotificationType.OS_CREATED]: [
    ['ordens-servico'],
    ['dashboard'],
  ],
  [NotificationType.OS_UPDATED]: [
    ['ordens-servico'],
    ['dashboard'],
  ],
  [NotificationType.OS_APROVADA]: [
    ['ordens-servico'],
    ['dashboard'],
  ],
  [NotificationType.PAYMENT_RECEIVED]: [
    ['pagamentos'],
    ['financeiro'],
    ['dashboard'],
  ],
  [NotificationType.STOCK_ALERT]: [
    ['pecas'],
    ['estoque'],
    ['dashboard'],
  ],
  [NotificationType.DASHBOARD_UPDATE]: [
    ['dashboard'],
  ],
};

/**
 * Additional notification types for maintenance module
 */
const MANUTENCAO_EVENTS = {
  PLANO_CREATED: 'PLANO_CREATED',
  PLANO_UPDATED: 'PLANO_UPDATED',
  PLANO_EXECUTED: 'PLANO_EXECUTED',
  ALERTA_GERADO: 'ALERTA_GERADO',
} as const;

// Add maintenance events to mapping
NOTIFICATION_TO_QUERY_KEYS[MANUTENCAO_EVENTS.PLANO_CREATED] = [
  ['planos-manutencao'],
  ['manutencao-preventiva'],
  ['dashboard'],
];
NOTIFICATION_TO_QUERY_KEYS[MANUTENCAO_EVENTS.PLANO_UPDATED] = [
  ['planos-manutencao'],
  ['manutencao-preventiva'],
];
NOTIFICATION_TO_QUERY_KEYS[MANUTENCAO_EVENTS.PLANO_EXECUTED] = [
  ['planos-manutencao'],
  ['manutencao-preventiva'],
  ['ordens-servico'],
  ['dashboard'],
];
NOTIFICATION_TO_QUERY_KEYS[MANUTENCAO_EVENTS.ALERTA_GERADO] = [
  ['alertas-manutencao'],
  ['manutencao-preventiva'],
];

/**
 * Hook to automatically invalidate React Query cache when WebSocket notifications arrive.
 *
 * This creates a global listener that invalidates relevant queries based on notification type.
 * Should be used once at the app level (e.g., in MainLayout or App.tsx).
 *
 * @example
 * // In MainLayout.tsx
 * useRealtimeUpdates();
 */
export const useRealtimeUpdates = () => {
  const queryClient = useQueryClient();
  const { isAuthenticated } = useAuth();
  const unsubscribeRef = useRef<(() => void) | null>(null);
  const connectedRef = useRef(false);

  const handleNotification = useCallback((notification: WebSocketNotification) => {
    console.log('[RealtimeUpdates] Received notification:', notification.tipo);

    // Get query keys to invalidate for this notification type
    const queryKeys = NOTIFICATION_TO_QUERY_KEYS[notification.tipo];

    if (queryKeys && queryKeys.length > 0) {
      queryKeys.forEach((queryKey) => {
        console.log('[RealtimeUpdates] Invalidating query:', queryKey);
        queryClient.invalidateQueries({ queryKey });
      });
    }

    // If notification has specific entity ID, also invalidate detail query
    if (notification.dados?.id) {
      const entityId = notification.dados.id;

      // Invalidate specific entity based on type
      if (notification.tipo.startsWith('OS_')) {
        queryClient.invalidateQueries({
          queryKey: ['ordens-servico', 'detail', entityId]
        });
      } else if (notification.tipo.startsWith('PLANO_')) {
        queryClient.invalidateQueries({
          queryKey: ['planos-manutencao', 'detail', entityId]
        });
      }
    }
  }, [queryClient]);

  useEffect(() => {
    if (!isAuthenticated) {
      // Disconnect if not authenticated
      if (unsubscribeRef.current) {
        unsubscribeRef.current();
        unsubscribeRef.current = null;
      }
      connectedRef.current = false;
      return;
    }

    const connectAndSubscribe = async () => {
      try {
        const token = getAccessToken();
        if (!token) {
          console.warn('[RealtimeUpdates] No access token available');
          return;
        }

        // Connect to WebSocket if not already connected
        if (!websocketService.isConnected()) {
          await websocketService.connect(token);
        }

        // Subscribe to user notifications
        if (!connectedRef.current) {
          unsubscribeRef.current = websocketService.subscribeToUserNotifications(handleNotification);
          connectedRef.current = true;
          console.log('[RealtimeUpdates] Subscribed to real-time updates');
        }
      } catch (error) {
        console.error('[RealtimeUpdates] Failed to connect:', error);
        connectedRef.current = false;
      }
    };

    connectAndSubscribe();

    return () => {
      if (unsubscribeRef.current) {
        unsubscribeRef.current();
        unsubscribeRef.current = null;
      }
      connectedRef.current = false;
    };
  }, [isAuthenticated, handleNotification]);

  return {
    isConnected: connectedRef.current,
  };
};

/**
 * Hook to invalidate specific query keys when a notification type is received.
 * Use this when you need more granular control over which queries to invalidate.
 *
 * @param notificationTypes - Array of notification types to listen for
 * @param queryKeys - Array of query keys to invalidate
 *
 * @example
 * // In a specific page component
 * useQueryInvalidationOnWebSocket(
 *   [NotificationType.OS_UPDATED, NotificationType.OS_STATUS_CHANGED],
 *   [['ordens-servico', 'list'], ['ordens-servico', 'detail', id]]
 * );
 */
export const useQueryInvalidationOnWebSocket = (
  notificationTypes: string[],
  queryKeys: QueryKey[]
) => {
  const queryClient = useQueryClient();
  const { isAuthenticated } = useAuth();
  const unsubscribeRef = useRef<(() => void) | null>(null);

  const handleNotification = useCallback((notification: WebSocketNotification) => {
    if (notificationTypes.includes(notification.tipo)) {
      console.log('[QueryInvalidation] Invalidating queries for:', notification.tipo);
      queryKeys.forEach((queryKey) => {
        queryClient.invalidateQueries({ queryKey });
      });
    }
  }, [notificationTypes, queryKeys, queryClient]);

  useEffect(() => {
    if (!isAuthenticated || !websocketService.isConnected()) {
      return;
    }

    unsubscribeRef.current = websocketService.subscribeToUserNotifications(handleNotification);

    return () => {
      if (unsubscribeRef.current) {
        unsubscribeRef.current();
        unsubscribeRef.current = null;
      }
    };
  }, [isAuthenticated, handleNotification]);
};

/**
 * Hook to manually trigger a refresh of specific queries.
 * Useful for "Refresh" buttons or after specific user actions.
 *
 * @example
 * const { refresh, isRefreshing } = useManualRefresh(['ordens-servico']);
 * <button onClick={refresh}>Atualizar</button>
 */
export const useManualRefresh = (queryKeys: QueryKey[]) => {
  const queryClient = useQueryClient();

  const refresh = useCallback(() => {
    queryKeys.forEach((queryKey) => {
      queryClient.invalidateQueries({ queryKey });
    });
  }, [queryClient, queryKeys]);

  return { refresh };
};
