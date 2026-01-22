import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { webhookService } from '../services/webhookService';
import type {
  WebhookConfigCreateRequest,
  WebhookConfigUpdateRequest,
  WebhookTestRequest,
  WebhookFilters,
  WebhookLogFilters,
} from '../types';

// Query keys
export const webhookKeys = {
  all: ['webhooks'] as const,
  lists: () => [...webhookKeys.all, 'list'] as const,
  list: (filters: WebhookFilters) => [...webhookKeys.lists(), filters] as const,
  details: () => [...webhookKeys.all, 'detail'] as const,
  detail: (id: string) => [...webhookKeys.details(), id] as const,
  logs: () => [...webhookKeys.all, 'logs'] as const,
  logList: (filters: WebhookLogFilters) => [...webhookKeys.logs(), 'list', filters] as const,
  logsByWebhook: (webhookId: string, filters: WebhookFilters) =>
    [...webhookKeys.logs(), 'webhook', webhookId, filters] as const,
  stats: () => [...webhookKeys.all, 'stats'] as const,
  eventos: () => [...webhookKeys.all, 'eventos'] as const,
};

/**
 * Hook to list webhooks with pagination
 */
export function useWebhooks(filters: WebhookFilters = {}) {
  return useQuery({
    queryKey: webhookKeys.list(filters),
    queryFn: () => webhookService.findAll(filters),
    staleTime: 1 * 60 * 1000, // 1 minute
  });
}

/**
 * Hook to get webhook by ID
 */
export function useWebhook(id?: string) {
  return useQuery({
    queryKey: webhookKeys.detail(id || ''),
    queryFn: () => webhookService.findById(id!),
    enabled: !!id,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

/**
 * Hook to create webhook
 */
export function useCreateWebhook() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: WebhookConfigCreateRequest) => webhookService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: webhookKeys.lists() });
      queryClient.invalidateQueries({ queryKey: webhookKeys.stats() });
    },
  });
}

/**
 * Hook to update webhook
 */
export function useUpdateWebhook() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: WebhookConfigUpdateRequest }) =>
      webhookService.update(id, data),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: webhookKeys.detail(updated.id) });
      queryClient.invalidateQueries({ queryKey: webhookKeys.lists() });
      queryClient.invalidateQueries({ queryKey: webhookKeys.stats() });
    },
  });
}

/**
 * Hook to delete webhook
 */
export function useDeleteWebhook() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => webhookService.delete(id),
    onSuccess: (_, deletedId) => {
      queryClient.invalidateQueries({ queryKey: webhookKeys.lists() });
      queryClient.removeQueries({ queryKey: webhookKeys.detail(deletedId) });
      queryClient.invalidateQueries({ queryKey: webhookKeys.stats() });
    },
  });
}

/**
 * Hook to reactivate webhook
 */
export function useReativarWebhook() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => webhookService.reativar(id),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: webhookKeys.detail(updated.id) });
      queryClient.invalidateQueries({ queryKey: webhookKeys.lists() });
      queryClient.invalidateQueries({ queryKey: webhookKeys.stats() });
    },
  });
}

/**
 * Hook to test webhook
 */
export function useTestWebhook() {
  return useMutation({
    mutationFn: (data: WebhookTestRequest) => webhookService.testar(data),
  });
}

/**
 * Hook to get webhook logs
 */
export function useWebhookLogs(filters: WebhookLogFilters = {}) {
  return useQuery({
    queryKey: webhookKeys.logList(filters),
    queryFn: () => webhookService.findLogs(filters),
    staleTime: 30 * 1000, // 30 seconds
  });
}

/**
 * Hook to get logs for specific webhook
 */
export function useWebhookLogsByWebhook(webhookId: string, filters: WebhookFilters = {}) {
  return useQuery({
    queryKey: webhookKeys.logsByWebhook(webhookId, filters),
    queryFn: () => webhookService.findLogsByWebhook(webhookId, filters),
    enabled: !!webhookId,
    staleTime: 30 * 1000, // 30 seconds
  });
}

/**
 * Hook to get webhook statistics
 */
export function useWebhookStats() {
  return useQuery({
    queryKey: webhookKeys.stats(),
    queryFn: () => webhookService.getStats(),
    staleTime: 1 * 60 * 1000, // 1 minute
  });
}

/**
 * Hook to get available events
 */
export function useWebhookEventos() {
  return useQuery({
    queryKey: webhookKeys.eventos(),
    queryFn: () => webhookService.getEventos(),
    staleTime: 10 * 60 * 1000, // 10 minutes (rarely changes)
  });
}
