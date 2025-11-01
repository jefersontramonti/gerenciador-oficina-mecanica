import { useEffect } from 'react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { websocketService } from '../services/websocket';
import type { NotificationCallback } from '../services/websocket';
import { getAccessToken } from '../services/api';

/**
 * Hook to automatically connect/disconnect WebSocket based on auth state
 */
export const useWebSocket = () => {
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    if (isAuthenticated) {
      const token = getAccessToken();
      if (token) {
        websocketService.connect(token).catch((error) => {
          console.error('[WebSocket] Connection failed:', error);
        });
      }
    } else {
      websocketService.disconnect();
    }

    return () => {
      websocketService.disconnect();
    };
  }, [isAuthenticated]);

  return {
    isConnected: websocketService.isConnected(),
    subscribeToUserNotifications: (callback: NotificationCallback) =>
      websocketService.subscribeToUserNotifications(callback),
    subscribeToBroadcast: (topic: string, callback: NotificationCallback) =>
      websocketService.subscribeToBroadcast(topic, callback),
  };
};
