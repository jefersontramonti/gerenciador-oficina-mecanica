import { Client, StompConfig } from '@stomp/stompjs';
import type { IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { env } from '@/config/env';

/**
 * WebSocket notification types
 */
export const NotificationType = {
  OS_STATUS_CHANGED: 'OS_STATUS_CHANGED',
  OS_CREATED: 'OS_CREATED',
  OS_UPDATED: 'OS_UPDATED',
  OS_APROVADA: 'OS_APROVADA',
  PAYMENT_RECEIVED: 'PAYMENT_RECEIVED',
  STOCK_ALERT: 'STOCK_ALERT',
  DASHBOARD_UPDATE: 'DASHBOARD_UPDATE',
} as const;

export type NotificationType = typeof NotificationType[keyof typeof NotificationType];

export interface WebSocketNotification {
  tipo: NotificationType;
  titulo: string;
  mensagem: string;
  timestamp: string;
  dados?: Record<string, any>;
}

export type NotificationCallback = (notification: WebSocketNotification) => void;

/**
 * WebSocket service for real-time notifications
 */
class WebSocketService {
  private client: Client | null = null;
  private subscriptions = new Map<string, string>();
  private callbacks = new Map<string, NotificationCallback[]>();
  private connected = false;

  /**
   * Initialize WebSocket connection
   */
  connect(accessToken: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.connected) {
        resolve();
        return;
      }

      const config: StompConfig = {
        // Use SockJS for better browser compatibility
        webSocketFactory: () => new SockJS(env.wsUrl) as any,

        // Connection headers with JWT token
        connectHeaders: {
          Authorization: `Bearer ${accessToken}`,
        },

        // Auto-reconnect configuration
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,

        // Debug logging (disable in production)
        debug: (str) => {
          if (env.isDevelopment) {
            console.log('[WebSocket]', str);
          }
        },

        // Connection callbacks
        onConnect: () => {
          console.log('[WebSocket] Connected');
          this.connected = true;
          this.resubscribeAll();
          resolve();
        },

        onStompError: (frame) => {
          console.error('[WebSocket] STOMP error:', frame);
          this.connected = false;
          reject(new Error(frame.headers['message'] || 'WebSocket connection error'));
        },

        onWebSocketClose: () => {
          console.log('[WebSocket] Connection closed');
          this.connected = false;
        },

        onWebSocketError: (error) => {
          console.error('[WebSocket] Error:', error);
          this.connected = false;
          reject(error);
        },
      };

      this.client = new Client(config);
      this.client.activate();
    });
  }

  /**
   * Disconnect WebSocket
   */
  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
      this.connected = false;
      this.subscriptions.clear();
      this.callbacks.clear();
    }
  }

  /**
   * Subscribe to user-specific notifications
   */
  subscribeToUserNotifications(callback: NotificationCallback): () => void {
    const destination = '/user/queue/notifications';
    return this.subscribe(destination, callback);
  }

  /**
   * Subscribe to broadcast topic
   */
  subscribeToBroadcast(topic: string, callback: NotificationCallback): () => void {
    const destination = `/topic/${topic}`;
    return this.subscribe(destination, callback);
  }

  /**
   * Generic subscribe method
   */
  private subscribe(destination: string, callback: NotificationCallback): () => void {
    if (!this.client || !this.connected) {
      console.warn('[WebSocket] Cannot subscribe: not connected');
      return () => {};
    }

    // Add callback to the list
    const existingCallbacks = this.callbacks.get(destination) || [];
    existingCallbacks.push(callback);
    this.callbacks.set(destination, existingCallbacks);

    // Subscribe if not already subscribed
    if (!this.subscriptions.has(destination)) {
      const subscription = this.client.subscribe(destination, (message: IMessage) => {
        try {
          const notification: WebSocketNotification = JSON.parse(message.body);
          const callbacks = this.callbacks.get(destination) || [];
          callbacks.forEach((cb) => cb(notification));
        } catch (error) {
          console.error('[WebSocket] Error parsing notification:', error);
        }
      });

      this.subscriptions.set(destination, subscription.id);
    }

    // Return unsubscribe function
    return () => {
      const callbacks = this.callbacks.get(destination) || [];
      const index = callbacks.indexOf(callback);
      if (index > -1) {
        callbacks.splice(index, 1);
      }

      if (callbacks.length === 0) {
        this.unsubscribe(destination);
      }
    };
  }

  /**
   * Unsubscribe from destination
   */
  private unsubscribe(destination: string): void {
    const subscriptionId = this.subscriptions.get(destination);
    if (subscriptionId && this.client) {
      this.client.unsubscribe(subscriptionId);
      this.subscriptions.delete(destination);
      this.callbacks.delete(destination);
    }
  }

  /**
   * Resubscribe to all destinations after reconnection
   */
  private resubscribeAll(): void {
    const destinations = Array.from(this.callbacks.keys());
    this.subscriptions.clear();

    destinations.forEach((destination) => {
      if (this.client) {
        const subscription = this.client.subscribe(destination, (message: IMessage) => {
          try {
            const notification: WebSocketNotification = JSON.parse(message.body);
            const callbacks = this.callbacks.get(destination) || [];
            callbacks.forEach((cb) => cb(notification));
          } catch (error) {
            console.error('[WebSocket] Error parsing notification:', error);
          }
        });

        this.subscriptions.set(destination, subscription.id);
      }
    });
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.connected;
  }
}

// Export singleton instance
export const websocketService = new WebSocketService();
