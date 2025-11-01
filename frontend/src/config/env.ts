/**
 * Environment variables configuration
 * All environment variables should be prefixed with VITE_
 */

export const env = {
  apiUrl: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  wsUrl: import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws',
  isDevelopment: import.meta.env.DEV,
  isProduction: import.meta.env.PROD,
} as const;
