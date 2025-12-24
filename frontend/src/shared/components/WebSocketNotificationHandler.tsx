import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useWebSocket } from '../hooks/useWebSocket';
import { NotificationType } from '../services/websocket';
import type { WebSocketNotification } from '../services/websocket';
import { showSuccess, showInfo, showWarning } from '../utils/notifications';

/**
 * WebSocketNotificationHandler
 *
 * Invisible component that handles WebSocket notifications and displays toast messages.
 * Automatically subscribes to user-specific and broadcast notifications when connected.
 *
 * Architecture:
 * - Uses useWebSocket hook to access connection state
 * - Subscribes to /user/queue/notifications for user-specific messages
 * - Subscribes to /topic/os-updates for service order updates
 * - Maps notification types to appropriate toast styles
 *
 * Notification Flow:
 * Backend -> WebSocket -> This component -> Toast notification
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

    // Handler for all notifications (decides which toast to show)
    const handleNotification = (notification: WebSocketNotification) => {
      console.log('[WebSocketNotificationHandler] Received notification:', notification);

      // Map notification type to appropriate toast
      switch (notification.tipo) {
        case NotificationType.OS_STATUS_CHANGED:
          showInfo(`${notification.titulo}: ${notification.mensagem}`);
          // Invalidate OS cache
          queryClient.invalidateQueries({ queryKey: ['ordens-servico'] });
          break;

        case NotificationType.OS_CREATED:
          showSuccess(`${notification.titulo}: ${notification.mensagem}`);
          queryClient.invalidateQueries({ queryKey: ['ordens-servico'] });
          break;

        case NotificationType.OS_UPDATED:
          showInfo(`${notification.titulo}: ${notification.mensagem}`);
          queryClient.invalidateQueries({ queryKey: ['ordens-servico'] });
          break;

        case NotificationType.OS_APROVADA:
        case 'OS_APROVADA': // Handle raw string from backend
          showSuccess(notification.mensagem || `OS aprovada pelo cliente!`);
          // Invalidate OS cache to reflect the new status
          queryClient.invalidateQueries({ queryKey: ['ordens-servico'] });
          queryClient.invalidateQueries({ queryKey: ['dashboard'] });
          break;

        case NotificationType.PAYMENT_RECEIVED:
          showSuccess(`${notification.titulo}: ${notification.mensagem}`);
          break;

        case NotificationType.STOCK_ALERT:
          showWarning(`${notification.titulo}: ${notification.mensagem}`);
          break;

        case NotificationType.DASHBOARD_UPDATE:
          // Silent update (don't show toast for dashboard metrics)
          console.log('[WebSocketNotificationHandler] Dashboard update received');
          queryClient.invalidateQueries({ queryKey: ['dashboard'] });
          break;

        default:
          // Fallback for unknown notification types
          showInfo(notification.mensagem);
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
