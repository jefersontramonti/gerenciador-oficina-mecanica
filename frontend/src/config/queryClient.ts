import { QueryClient } from '@tanstack/react-query';

/**
 * React Query client configuration
 * Server state management with caching and automatic refetching
 *
 * Performance optimizations:
 * - staleTime: Reduces unnecessary refetches
 * - gcTime: Keeps data in cache for faster navigation
 * - refetchOnWindowFocus: Disabled to reduce API calls
 * - retry: Limited retries with delay
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Stale time: How long data is considered fresh (5 minutes)
      staleTime: 5 * 60 * 1000,
      // Cache time: How long inactive data stays in cache (15 minutes)
      gcTime: 15 * 60 * 1000,
      // Retry failed requests with delay
      retry: 1,
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 10000),
      // Disable refetch on window focus to reduce API calls
      refetchOnWindowFocus: false,
      // Refetch on reconnect
      refetchOnReconnect: true,
      // Don't refetch on mount if data is fresh
      refetchOnMount: false,
    },
    mutations: {
      // Mutations should not retry automatically
      retry: 0,
    },
  },
});
