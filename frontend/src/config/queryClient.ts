import { QueryClient } from '@tanstack/react-query';

/**
 * React Query client configuration
 * Server state management with caching and automatic refetching
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Stale time: How long data is considered fresh (5 minutes)
      staleTime: 5 * 60 * 1000,
      // Cache time: How long inactive data stays in cache (10 minutes)
      gcTime: 10 * 60 * 1000,
      // Retry failed requests
      retry: 1,
      // Refetch on window focus in production
      refetchOnWindowFocus: import.meta.env.PROD,
      // Refetch on reconnect
      refetchOnReconnect: true,
    },
    mutations: {
      // Retry failed mutations once
      retry: 1,
    },
  },
});
