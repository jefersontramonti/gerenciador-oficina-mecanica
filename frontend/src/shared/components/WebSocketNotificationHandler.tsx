import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useWebSocket } from '../hooks/useWebSocket';
import { NotificationType } from '../services/websocket';
import type { WebSocketNotification } from '../services/websocket';
import { showSuccess, showWarning } from '../utils/notifications';

/**
 * WebSocketNotificationHandler
 *
 * Invisible component that handles WebSocket notifications and cache invalidation.
 * Automatically subscribes to user-specific and broadcast notifications when connected.
 *
 * Architecture:
 * - Uses useWebSocket hook to access connection state
 * - Subscribes to /user/queue/notifications for user-specific messages
 * - Subscribes to /topic/os-updates for service order updates
 * - Invalidates React Query cache when notifications arrive
 *
 * Note: Toast notifications for OS status changes are handled by the UI components
 * (ActionButtons, etc.) to avoid duplicate toasts. This handler only invalidates cache.
 */
export const WebSocketNotificationHandler = () => {
  const { isConnected, subscribeToUserNotifications, subscribeToBroadcast } = useWebSocket();
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!isConnected) {
      console.log('[WebSocketNotificationHandler] Not connected, skipping subscriptions');
      return;
    }

    console.log('[WebSocketNotificationHandler] Connected, subscribing to notifications');

    // Handler for all notifications (invalidate cache, show toasts only for non-OS events)
    const handleNotification = (notification: WebSocketNotification) => {
      console.log('[WebSocketNotificationHandler] Received notification:', notification);

      // Map notification type to appropriate action
      switch (notification.tipo) {
        case NotificationType.OS_STATUS_CHANGED:
        case NotificationType.OS_CREATED:
        case NotificationType.OS_UPDATED:
        case NotificationType.OS_APROVADA:
        case 'OS_APROVADA': // Handle raw string from backend
          // ONLY invalidate cache - UI components handle their own toasts
          console.log('[WebSocketNotificationHandler] OS event - invalidating cache only');
          queryClient.invalidateQueries({ queryKey: ['ordens-servico'] });
          queryClient.invalidateQueries({ queryKey: ['dashboard'] });
          break;

        case NotificationType.PAYMENT_RECEIVED:
          showSuccess(`${notification.titulo}: ${notification.mensagem}`);
          queryClient.invalidateQueries({ queryKey: ['pagamentos'] });
          queryClient.invalidateQueries({ queryKey: ['ordens-servico'] });
          break;

        case NotificationType.STOCK_ALERT:
          showWarning(`${notification.titulo}: ${notification.mensagem}`);
          queryClient.invalidateQueries({ queryKey: ['estoque'] });
          break;

        case NotificationType.DASHBOARD_UPDATE:
          // Silent update (don't show toast for dashboard metrics)
          console.log('[WebSocketNotificationHandler] Dashboard update received');
          queryClient.invalidateQueries({ queryKey: ['dashboard'] });
          break;

        case 'EMAIL_AGENDADO':
          // Mostrar aviso quando email foi agendado por horário comercial
          showWarning(`${notification.titulo}: ${notification.mensagem}`);
          break;

        default:
          // For unknown types, just log
          console.log('[WebSocketNotificationHandler] Unknown notification type:', notification.tipo);
      }
    };

    // Subscribe to user-specific notifications (/user/queue/notifications)
    const unsubscribeUser = subscribeToUserNotifications(handleNotification);

    // Subscribe to broadcast topics
    const unsubscribeOsUpdates = subscribeToBroadcast('os-updates', handleNotification);
    const unsubscribeStockAlerts = subscribeToBroadcast('estoque-alerts', handleNotification);

    console.log('[WebSocketNotificationHandler] Subscriptions active');

    // Cleanup: unsubscribe when component unmounts or connection drops
    return () => {
      console.log('[WebSocketNotificationHandler] Cleaning up subscriptions');
      unsubscribeUser?.();
      unsubscribeOsUpdates?.();
      unsubscribeStockAlerts?.();
    };
  }, [isConnected, subscribeToUserNotifications, subscribeToBroadcast, queryClient]);

  // This component renders nothing (invisible handler)
  return null;
};
