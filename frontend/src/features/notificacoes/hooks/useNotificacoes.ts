import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificacaoService } from '../services/notificacaoService';
import type {
  NotificacaoFilters,
  CreateNotificacaoRequest,
  UpdateConfiguracaoNotificacaoRequest,
  TesteNotificacaoRequest,
  TipoNotificacao,
  TelegramConfigRequest,
} from '../types';

/**
 * Query keys for React Query
 */
export const notificacaoKeys = {
  all: ['notificacoes'] as const,
  lists: () => [...notificacaoKeys.all, 'list'] as const,
  list: (filters: NotificacaoFilters) => [...notificacaoKeys.lists(), filters] as const,
  details: () => [...notificacaoKeys.all, 'detail'] as const,
  detail: (id: string) => [...notificacaoKeys.details(), id] as const,
  metricas: () => [...notificacaoKeys.all, 'metricas'] as const,
  configuracoes: () => [...notificacaoKeys.all, 'configuracoes'] as const,
  configuracao: (tipo: TipoNotificacao) => [...notificacaoKeys.configuracoes(), tipo] as const,
  whatsappStatus: () => [...notificacaoKeys.all, 'whatsapp', 'status'] as const,
  telegramStatus: () => [...notificacaoKeys.all, 'telegram', 'status'] as const,
};

/**
 * Hook to fetch list of notifications with filters
 */
export const useNotificacoes = (filters: NotificacaoFilters = {}) => {
  return useQuery({
    queryKey: notificacaoKeys.list(filters),
    queryFn: () => notificacaoService.findAll(filters),
    staleTime: 30 * 1000, // 30 seconds (notifications change frequently)
  });
};

/**
 * Hook to fetch single notification by ID
 */
export const useNotificacao = (id?: string) => {
  return useQuery({
    queryKey: notificacaoKeys.detail(id || ''),
    queryFn: () => notificacaoService.findById(id!),
    enabled: !!id,
    staleTime: 1 * 60 * 1000, // 1 minute
  });
};

/**
 * Hook to fetch notification metrics
 */
export const useNotificacaoMetricas = () => {
  return useQuery({
    queryKey: notificacaoKeys.metricas(),
    queryFn: () => notificacaoService.getMetricas(),
    staleTime: 1 * 60 * 1000, // 1 minute
  });
};

/**
 * Hook to fetch all notification configurations
 */
export const useConfiguracoes = () => {
  return useQuery({
    queryKey: notificacaoKeys.configuracoes(),
    queryFn: () => notificacaoService.getConfiguracoes(),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};

/**
 * Hook to fetch configuration by type
 */
export const useConfiguracao = (tipo?: TipoNotificacao) => {
  return useQuery({
    queryKey: notificacaoKeys.configuracao(tipo || 'EMAIL'),
    queryFn: () => notificacaoService.getConfiguracaoByTipo(tipo!),
    enabled: !!tipo,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};

/**
 * Hook to fetch WhatsApp status
 */
export const useWhatsAppStatus = () => {
  return useQuery({
    queryKey: notificacaoKeys.whatsappStatus(),
    queryFn: () => notificacaoService.getWhatsAppStatus(),
    staleTime: 30 * 1000, // 30 seconds
    refetchInterval: 30 * 1000, // Auto-refetch every 30 seconds
  });
};

/**
 * Hook to create new notification
 */
export const useCreateNotificacao = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateNotificacaoRequest) => notificacaoService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.lists() });
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.metricas() });
    },
  });
};

/**
 * Hook to retry failed notification
 */
export const useRetryNotificacao = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => notificacaoService.retry(id),
    onSuccess: (updatedNotificacao) => {
      queryClient.setQueryData(
        notificacaoKeys.detail(updatedNotificacao.id),
        updatedNotificacao
      );
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.lists() });
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.metricas() });
    },
  });
};

/**
 * Hook to cancel pending notification
 */
export const useCancelNotificacao = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => notificacaoService.cancel(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.lists() });
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.metricas() });
    },
  });
};

/**
 * Hook to update notification configuration
 */
export const useUpdateConfiguracao = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ data }: { data: UpdateConfiguracaoNotificacaoRequest }) =>
      notificacaoService.updateConfiguracao(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.configuracoes() });
    },
  });
};

/**
 * Hook to reconnect WhatsApp instance
 */
export const useConnectWhatsApp = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificacaoService.reconnectWhatsApp(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.whatsappStatus() });
      queryClient.invalidateQueries({
        queryKey: notificacaoKeys.configuracao('WHATSAPP'),
      });
    },
  });
};

/**
 * Hook to create WhatsApp instance automatically
 */
export const useCreateWhatsAppInstance = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificacaoService.createWhatsAppInstance(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.whatsappStatus() });
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.configuracoes() });
    },
  });
};

/**
 * Hook to delete WhatsApp instance
 */
export const useDeleteWhatsAppInstance = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificacaoService.deleteWhatsAppInstance(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.whatsappStatus() });
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.configuracoes() });
    },
  });
};

/**
 * Hook to disconnect WhatsApp instance (logout)
 */
export const useDisconnectWhatsApp = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificacaoService.disconnectWhatsApp(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.whatsappStatus() });
      queryClient.invalidateQueries({
        queryKey: notificacaoKeys.configuracao('WHATSAPP'),
      });
    },
  });
};

/**
 * Hook to reconnect WhatsApp instance (restart)
 */
export const useReconnectWhatsApp = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificacaoService.reconnectWhatsApp(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.whatsappStatus() });
    },
  });
};

/**
 * Hook to test notification configuration
 */
export const useTestarNotificacao = () => {
  return useMutation({
    mutationFn: (data: TesteNotificacaoRequest) => notificacaoService.testarNotificacao(data),
  });
};

// ===== TELEGRAM =====

/**
 * Hook to fetch Telegram bot status
 */
export const useTelegramStatus = () => {
  return useQuery({
    queryKey: notificacaoKeys.telegramStatus(),
    queryFn: () => notificacaoService.getTelegramStatus(),
    staleTime: 30 * 1000, // 30 seconds
    refetchInterval: 30 * 1000, // Auto-refetch every 30 seconds
  });
};

/**
 * Hook to configure Telegram bot
 */
export const useConfigurarTelegram = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: TelegramConfigRequest) => notificacaoService.configurarTelegram(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.telegramStatus() });
      queryClient.invalidateQueries({ queryKey: notificacaoKeys.configuracoes() });
    },
  });
};
