import { api } from '@/shared/services/api';
import type { PaginatedResponse } from '@/shared/types/api';
import type {
  WebhookConfig,
  WebhookConfigCreateRequest,
  WebhookConfigUpdateRequest,
  WebhookLog,
  WebhookStats,
  WebhookTestRequest,
  WebhookTestResult,
  WebhookEvento,
  WebhookFilters,
  WebhookLogFilters,
} from '../types';

const BASE_URL = '/webhooks/config';

/**
 * Webhook service
 * Handles all webhook-related API calls
 */
export const webhookService = {
  /**
   * List webhooks with pagination
   */
  async findAll(filters: WebhookFilters = {}): Promise<PaginatedResponse<WebhookConfig>> {
    const params = new URLSearchParams();

    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));
    if (filters.sort) params.append('sort', filters.sort);

    const response = await api.get<PaginatedResponse<WebhookConfig>>(
      `${BASE_URL}?${params.toString()}`
    );

    return response.data;
  },

  /**
   * Find webhook by ID
   */
  async findById(id: string): Promise<WebhookConfig> {
    const response = await api.get<WebhookConfig>(`${BASE_URL}/${id}`);
    return response.data;
  },

  /**
   * Create new webhook
   */
  async create(data: WebhookConfigCreateRequest): Promise<WebhookConfig> {
    const response = await api.post<WebhookConfig>(BASE_URL, data);
    return response.data;
  },

  /**
   * Update webhook
   */
  async update(id: string, data: WebhookConfigUpdateRequest): Promise<WebhookConfig> {
    const response = await api.put<WebhookConfig>(`${BASE_URL}/${id}`, data);
    return response.data;
  },

  /**
   * Delete webhook
   */
  async delete(id: string): Promise<void> {
    await api.delete(`${BASE_URL}/${id}`);
  },

  /**
   * Reactivate disabled webhook
   */
  async reativar(id: string): Promise<WebhookConfig> {
    const response = await api.patch<WebhookConfig>(`${BASE_URL}/${id}/reativar`);
    return response.data;
  },

  /**
   * Test webhook with sample payload
   */
  async testar(data: WebhookTestRequest): Promise<WebhookTestResult> {
    const response = await api.post<WebhookTestResult>(`${BASE_URL}/testar`, data);
    return response.data;
  },

  /**
   * List webhook logs with pagination
   */
  async findLogs(filters: WebhookLogFilters = {}): Promise<PaginatedResponse<WebhookLog>> {
    const params = new URLSearchParams();

    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));
    if (filters.sort) params.append('sort', filters.sort);
    if (filters.status) params.append('status', filters.status);
    if (filters.evento) params.append('evento', filters.evento);

    const response = await api.get<PaginatedResponse<WebhookLog>>(
      `${BASE_URL}/logs?${params.toString()}`
    );

    return response.data;
  },

  /**
   * List logs for specific webhook
   */
  async findLogsByWebhook(
    webhookId: string,
    filters: WebhookFilters = {}
  ): Promise<PaginatedResponse<WebhookLog>> {
    const params = new URLSearchParams();

    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));
    if (filters.sort) params.append('sort', filters.sort);

    const response = await api.get<PaginatedResponse<WebhookLog>>(
      `${BASE_URL}/${webhookId}/logs?${params.toString()}`
    );

    return response.data;
  },

  /**
   * Get webhook statistics
   */
  async getStats(): Promise<WebhookStats> {
    const response = await api.get<WebhookStats>(`${BASE_URL}/stats`);
    return response.data;
  },

  /**
   * List available events
   */
  async getEventos(): Promise<WebhookEvento[]> {
    const response = await api.get<WebhookEvento[]>(`${BASE_URL}/eventos`);
    return response.data;
  },
};
