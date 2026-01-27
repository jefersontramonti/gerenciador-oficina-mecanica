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
  EMAIL_AGENDADO: 'EMAIL_AGENDADO',
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
 * WebSocket service for real-time notifications.
 * Supports queuing subscriptions that are processed when connection is established.
 */
class WebSocketService {
  private client: Client | null = null;
  private subscriptions = new Map<string, string>();
  private callbacks = new Map<string, NotificationCallback[]>();
  private connected = false;
  private connecting = false;
  private pendingSubscriptions: Array<{ destination: string; callback: NotificationCallback }> = [];

  /**
   * Initialize WebSocket connection
   */
  connect(accessToken: string): Promise<void> {
    return new Promise((resolve, reject) => {
      // Already connected
      if (this.connected && this.client?.active) {
        resolve();
        return;
      }

      // Connection in progress
      if (this.connecting) {
        console.log('[WebSocket] Connection already in progress, skipping');
        resolve();
        return;
      }

      this.connecting = true;

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
          this.connecting = false;
          this.resubscribeAll();
          this.processPendingSubscriptions();
          resolve();
        },

        onStompError: (frame) => {
          console.error('[WebSocket] STOMP error:', frame);
          this.connected = false;
          this.connecting = false;
          reject(new Error(frame.headers['message'] || 'WebSocket connection error'));
        },

        onWebSocketClose: () => {
          console.log('[WebSocket] Connection closed');
          this.connected = false;
          this.connecting = false;
        },

        onWebSocketError: (error) => {
          console.error('[WebSocket] Error:', error);
          this.connected = false;
          this.connecting = false;
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
      this.connecting = false;
      this.subscriptions.clear();
      this.callbacks.clear();
      this.pendingSubscriptions = [];
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
   * Generic subscribe method - queues subscription if not connected
   */
  private subscribe(destination: string, callback: NotificationCallback): () => void {
    // If not connected or client not active, queue the subscription for later
    // Check both our flag AND the STOMP client's active state
    if (!this.client || !this.connected || !this.client.active) {
      console.log('[WebSocket] Queuing subscription for:', destination);
      this.pendingSubscriptions.push({ destination, callback });

      // Return unsubscribe function that removes from pending queue
      return () => {
        const index = this.pendingSubscriptions.findIndex(
          (p) => p.destination === destination && p.callback === callback
        );
        if (index > -1) {
          this.pendingSubscriptions.splice(index, 1);
        }
      };
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
      if (this.client?.active) {
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
   * Process any pending subscriptions that were queued before connection
   */
  private processPendingSubscriptions(): void {
    if (this.pendingSubscriptions.length === 0) {
      return;
    }

    console.log('[WebSocket] Processing', this.pendingSubscriptions.length, 'pending subscriptions');

    const pending = [...this.pendingSubscriptions];
    this.pendingSubscriptions = [];

    pending.forEach(({ destination, callback }) => {
      // Add callback to the list
      const existingCallbacks = this.callbacks.get(destination) || [];
      existingCallbacks.push(callback);
      this.callbacks.set(destination, existingCallbacks);

      // Subscribe if not already subscribed
      if (!this.subscriptions.has(destination) && this.client?.active) {
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
   * Check if connected - verifies both our flag and STOMP client state
   */
  isConnected(): boolean {
    return this.connected && (this.client?.active ?? false);
  }
}

// Export singleton instance
export const websocketService = new WebSocketService();
