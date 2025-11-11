# WebSocket Integration - PitStop

This document describes the implementation of real-time notifications using WebSocket (STOMP + SockJS) in the PitStop frontend.

## Implementation Date
2025-11-02

## Overview

The WebSocket integration enables real-time notifications for:
- Service order status changes
- Payment confirmations
- Inventory alerts
- Dashboard updates

## Architecture

```
Backend (Spring Boot)
    |
    | STOMP over WebSocket
    |
WebSocketService (singleton)
    |
    |-- useWebSocket hook (connection management)
    |
    |-- WebSocketNotificationHandler (toast notifications)
    |
    |-- MainLayout (UI integration)
```

## Files Modified/Created

### 1. WebSocketNotificationHandler.tsx
**Location:** `frontend/src/shared/components/WebSocketNotificationHandler.tsx`

**Purpose:** Invisible component that subscribes to WebSocket notifications and displays toast messages.

**Features:**
- Automatically subscribes when WebSocket connects
- Maps notification types to appropriate toast styles
- Subscribes to both user-specific and broadcast topics

**Subscriptions:**
- `/user/queue/notifications` - User-specific messages
- `/topic/os-updates` - Service order updates
- `/topic/estoque-alerts` - Inventory alerts

**Notification Type Mapping:**
```typescript
OS_STATUS_CHANGED -> Info toast (blue)
OS_CREATED        -> Success toast (green)
OS_UPDATED        -> Info toast (blue)
PAYMENT_RECEIVED  -> Success toast (green)
STOCK_ALERT       -> Warning toast (yellow)
DASHBOARD_UPDATE  -> Silent (no toast, just console log)
```

### 2. MainLayout.tsx
**Location:** `frontend/src/shared/layouts/MainLayout.tsx`

**Changes:**
1. Added `useWebSocket()` hook to access connection state
2. Added `<WebSocketNotificationHandler />` component
3. Added visual connection status indicator in header (desktop only)

**Visual Indicator:**
- **Connected:** Green badge with pulsing dot + "Tempo real ativo"
- **Disconnected:** Gray badge with static dot + "Offline"

## How It Works

### Connection Flow

1. User logs in
2. `useWebSocket` hook automatically calls `websocketService.connect(token)`
3. WebSocket connects to `ws://localhost:8080/ws` with JWT token
4. On successful connection, `WebSocketNotificationHandler` subscribes to topics
5. Status indicator in header shows "Tempo real ativo"

### Notification Flow

1. Backend sends notification to WebSocket destination (e.g., `/user/queue/notifications`)
2. `websocketService` receives message and parses JSON
3. Calls registered callback in `WebSocketNotificationHandler`
4. Handler maps notification type to appropriate toast function
5. User sees toast notification

### Disconnection Flow

1. User logs out (or connection drops)
2. `useWebSocket` hook calls `websocketService.disconnect()`
3. All subscriptions are cleaned up
4. Status indicator shows "Offline"
5. Auto-reconnect attempts every 5 seconds (built into STOMP client)

## Configuration

### Environment Variables

```env
VITE_WS_URL=http://localhost:8080/ws
```

**Note:** In production, this should be `wss://` (secure WebSocket)

### Backend Requirements

The backend must:
1. Expose WebSocket endpoint at `/ws` with SockJS support
2. Accept JWT token in `Authorization` header during connection
3. Send notifications with this JSON structure:

```json
{
  "tipo": "OS_STATUS_CHANGED",
  "titulo": "OS Finalizada",
  "mensagem": "Ordem de serviço #123 foi finalizada",
  "timestamp": "2025-11-02T10:30:00Z",
  "dados": {
    "osId": "uuid-here",
    "novoStatus": "FINALIZADA"
  }
}
```

## Testing

### Manual Testing Steps

1. Start backend: `./mvnw spring-boot:run`
2. Start frontend: `npm run dev`
3. Login to the application
4. Check browser console for:
   ```
   [WebSocket] Connecting...
   [WebSocket] Connected
   [WebSocketNotificationHandler] Connected, subscribing to notifications
   [WebSocketNotificationHandler] Subscriptions active
   ```
5. Verify status indicator shows "Tempo real ativo" (green badge)
6. Trigger a notification from backend (e.g., update service order status)
7. Verify toast notification appears
8. Logout and verify status changes to "Offline"

### Console Logs

The implementation includes detailed console logs for debugging:
- Connection/disconnection events
- Subscription lifecycle
- Notification reception
- Error handling

**Production:** Set `env.isDevelopment = false` to disable verbose STOMP debug logs

## Known Limitations

1. **Mobile View:** Connection status indicator is hidden on mobile (only visible on desktop)
2. **Offline Mode:** No offline queue - notifications sent while disconnected are lost
3. **Reconnection:** Uses exponential backoff (5s delay) but no manual "reconnect" button
4. **Toast Overflow:** Multiple simultaneous notifications stack - no limit implemented

## Future Enhancements

### Short-term
- [ ] Add connection status indicator to mobile header
- [ ] Implement notification sound (optional, user setting)
- [ ] Add notification history/center (persist recent notifications)
- [ ] Add manual reconnect button when offline

### Medium-term
- [ ] Implement offline notification queue with retry
- [ ] Add notification preferences (which types to show)
- [ ] Integrate with React Query cache invalidation (auto-refresh data on notifications)
- [ ] Add desktop notifications (browser Notification API)

### Long-term
- [ ] Implement notification categories with filtering
- [ ] Add read/unread status for notifications
- [ ] Implement notification actions (e.g., "View OS" button in toast)
- [ ] Add analytics (track notification delivery, click-through rates)

## Security Considerations

1. **JWT Token:** Token is passed in connection header - ensure HTTPS/WSS in production
2. **User Isolation:** Backend must enforce user isolation via JWT claims
3. **Rate Limiting:** Backend should implement rate limiting to prevent notification spam
4. **XSS Protection:** Notification messages are displayed in toast - ensure backend sanitizes input

## Performance Notes

1. **Auto-reconnect:** 5-second delay prevents rapid reconnection attempts
2. **Heartbeat:** 4-second interval keeps connection alive through proxies/firewalls
3. **Lazy Subscription:** Only subscribes after successful connection
4. **Memory:** Subscriptions are properly cleaned up to prevent memory leaks

## Troubleshooting

### Problem: Connection fails with 401
**Cause:** Invalid or expired JWT token
**Solution:** Check token validity, ensure refresh token flow works

### Problem: Connection fails with 403
**Cause:** User not authorized for WebSocket access
**Solution:** Verify backend RBAC configuration

### Problem: Notifications not appearing
**Cause:** Subscription failed or wrong topic name
**Solution:** Check console logs, verify backend destination matches frontend subscription

### Problem: Connection indicator stuck on "Offline"
**Cause:** `isConnected` state not updating
**Solution:** Check `websocketService.isConnected()` implementation, verify STOMP callbacks fire

### Problem: Multiple duplicate notifications
**Cause:** Component re-mounting causing duplicate subscriptions
**Solution:** Verify cleanup in useEffect, check React StrictMode behavior

## References

- STOMP.js documentation: https://stomp-js.github.io/stomp-websocket/
- SockJS client: https://github.com/sockjs/sockjs-client
- React Hot Toast: https://react-hot-toast.com/
- Spring WebSocket guide: https://docs.spring.io/spring-framework/reference/web/websocket.html

## Related Files

- `frontend/src/shared/services/websocket.ts` - WebSocketService implementation
- `frontend/src/shared/hooks/useWebSocket.ts` - React hook for WebSocket
- `frontend/src/shared/utils/notifications.ts` - Toast notification utilities
- `frontend/src/config/env.ts` - Environment configuration
- `CLAUDE.md` (backend) - WebSocket configuration and message format

---

**Last Updated:** 2025-11-02
**Author:** Claude (Senior Frontend Engineer Agent)
