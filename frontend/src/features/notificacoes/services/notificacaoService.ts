import { api } from '@/shared/services/api';
import type { PaginatedResponse } from '@/shared/types/api';
import type {
  Notificacao,
  NotificacaoFilters,
  CreateNotificacaoRequest,
  NotificacaoMetricas,
  ConfiguracaoNotificacao,
  UpdateConfiguracaoNotificacaoRequest,
  WhatsAppStatus,
  TesteNotificacaoRequest,
  TipoNotificacao,
  TelegramConfigRequest,
  TelegramBotStatus,
} from '../types';

/**
 * Notificacao service
 * Handles all notification-related API calls
 */
export const notificacaoService = {
  /**
   * List notifications with filters and pagination
   */
  async findAll(filters: NotificacaoFilters = {}): Promise<PaginatedResponse<Notificacao>> {
    const params = new URLSearchParams();

    if (filters.tipo) params.append('tipo', filters.tipo);
    if (filters.status) params.append('status', filters.status);
    if (filters.evento) params.append('evento', filters.evento);
    if (filters.destinatario) params.append('destinatario', filters.destinatario);
    if (filters.dataInicio) params.append('dataInicio', filters.dataInicio);
    if (filters.dataFim) params.append('dataFim', filters.dataFim);
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));
    if (filters.sort) params.append('sort', filters.sort);

    const response = await api.get<PaginatedResponse<Notificacao>>(
      `/notificacoes/historico?${params.toString()}`
    );

    return response.data;
  },

  /**
   * Find notification by ID
   */
  async findById(id: string): Promise<Notificacao> {
    const response = await api.get<Notificacao>(`/notificacoes/historico/${id}`);
    return response.data;
  },

  /**
   * Create new notification
   */
  async create(data: CreateNotificacaoRequest): Promise<Notificacao> {
    const response = await api.post<Notificacao>('/notificacoes', data);
    return response.data;
  },

  /**
   * Retry failed notification
   */
  async retry(id: string): Promise<Notificacao> {
    const response = await api.post<Notificacao>(`/notificacoes/historico/${id}/reenviar`);
    return response.data;
  },

  /**
   * Cancel pending notification
   */
  async cancel(id: string): Promise<void> {
    await api.delete(`/notificacoes/historico/${id}`);
  },

  /**
   * Get notification metrics
   */
  async getMetricas(): Promise<NotificacaoMetricas> {
    // Default to current month
    const now = new Date();
    const firstDayOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDayOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);

    const dataInicio = firstDayOfMonth.toISOString().split('T')[0];
    const dataFim = lastDayOfMonth.toISOString().split('T')[0];

    const response = await api.get<NotificacaoMetricas>(
      `/notificacoes/historico/metricas?dataInicio=${dataInicio}&dataFim=${dataFim}`
    );
    return response.data;
  },

  /**
   * Get notification configuration for current oficina
   */
  async getConfiguracoes(): Promise<ConfiguracaoNotificacao> {
    const response = await api.get<ConfiguracaoNotificacao>('/notificacoes/configuracao');
    return response.data;
  },

  /**
   * Get configuration by type
   */
  async getConfiguracaoByTipo(tipo: TipoNotificacao): Promise<ConfiguracaoNotificacao> {
    const response = await api.get<ConfiguracaoNotificacao>(
      `/notificacoes/configuracao/${tipo}`
    );
    return response.data;
  },

  /**
   * Update notification configuration
   */
  async updateConfiguracao(
    data: UpdateConfiguracaoNotificacaoRequest
  ): Promise<ConfiguracaoNotificacao> {
    const response = await api.put<ConfiguracaoNotificacao>(
      `/notificacoes/configuracao`,
      data
    );
    return response.data;
  },

  /**
   * Get WhatsApp status
   */
  async getWhatsAppStatus(): Promise<WhatsAppStatus> {
    const response = await api.get<WhatsAppStatus>('/notificacoes/configuracao/whatsapp/status');
    return response.data;
  },

  /**
   * Get WhatsApp QR Code
   */
  async getWhatsAppQrCode(): Promise<string> {
    const response = await api.get<{ qrCode: string }>('/notificacoes/configuracao/whatsapp/qrcode');
    return response.data.qrCode;
  },

  /**
   * Connect WhatsApp instance (not implemented yet - placeholder)
   */
  async connectWhatsApp(): Promise<WhatsAppStatus> {
    // Evolution API needs to be configured manually
    // This just returns current status
    const response = await api.get<WhatsAppStatus>('/notificacoes/configuracao/whatsapp/status');
    return response.data;
  },

  /**
   * Disconnect WhatsApp instance (not implemented yet - placeholder)
   */
  async disconnectWhatsApp(): Promise<void> {
    // Evolution API disconnect not implemented yet
    console.warn('Disconnect WhatsApp not implemented');
  },

  /**
   * Test notification configuration
   */
  async testarNotificacao(data: TesteNotificacaoRequest): Promise<{ sucesso: boolean; mensagem: string }> {
    const response = await api.post<{ sucesso: boolean; mensagem: string }>(
      '/notificacoes/configuracao/testar',
      data
    );
    return response.data;
  },

  // ===== TELEGRAM =====

  /**
   * Configure Telegram Bot
   */
  async configurarTelegram(data: TelegramConfigRequest): Promise<ConfiguracaoNotificacao> {
    const response = await api.put<ConfiguracaoNotificacao>(
      '/notificacoes/configuracao/telegram',
      data
    );
    return response.data;
  },

  /**
   * Get Telegram Bot status
   */
  async getTelegramStatus(): Promise<TelegramBotStatus> {
    const response = await api.get<TelegramBotStatus>('/notificacoes/configuracao/telegram/status');
    return response.data;
  },
};
