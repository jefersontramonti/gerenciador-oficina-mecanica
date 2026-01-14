import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { anexoService } from '../services/anexoService';
import type { AnexoUploadRequest, EntidadeTipo } from '../types';
import { toast } from 'sonner';

/**
 * Query keys para anexos.
 */
export const anexoKeys = {
  all: ['anexos'] as const,
  entidade: (tipo: EntidadeTipo, id: string) =>
    [...anexoKeys.all, 'entidade', tipo, id] as const,
  detail: (id: string) => [...anexoKeys.all, 'detail', id] as const,
  quota: () => [...anexoKeys.all, 'quota'] as const,
  count: (tipo: EntidadeTipo, id: string) =>
    [...anexoKeys.all, 'count', tipo, id] as const,
};

/**
 * Hook para listar anexos de uma entidade.
 */
export function useAnexos(entidadeTipo: EntidadeTipo, entidadeId: string) {
  return useQuery({
    queryKey: anexoKeys.entidade(entidadeTipo, entidadeId),
    queryFn: () => anexoService.listarPorEntidade(entidadeTipo, entidadeId),
    enabled: !!entidadeId,
  });
}

/**
 * Hook para buscar um anexo por ID.
 */
export function useAnexo(id: string) {
  return useQuery({
    queryKey: anexoKeys.detail(id),
    queryFn: () => anexoService.buscarPorId(id),
    enabled: !!id,
  });
}

/**
 * Hook para obter quota de storage.
 */
export function useQuota() {
  return useQuery({
    queryKey: anexoKeys.quota(),
    queryFn: () => anexoService.getQuota(),
  });
}

/**
 * Hook para contar anexos de uma entidade.
 */
export function useAnexosCount(entidadeTipo: EntidadeTipo, entidadeId: string) {
  return useQuery({
    queryKey: anexoKeys.count(entidadeTipo, entidadeId),
    queryFn: () => anexoService.contarPorEntidade(entidadeTipo, entidadeId),
    enabled: !!entidadeId,
  });
}

/**
 * Hook para upload de anexo.
 */
export function useUploadAnexo() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: AnexoUploadRequest) => anexoService.upload(request),
    onSuccess: (_data, variables) => {
      // Invalida cache da lista de anexos da entidade
      queryClient.invalidateQueries({
        queryKey: anexoKeys.entidade(variables.entidadeTipo, variables.entidadeId),
      });
      // Invalida cache de contagem
      queryClient.invalidateQueries({
        queryKey: anexoKeys.count(variables.entidadeTipo, variables.entidadeId),
      });
      // Invalida cache de quota
      queryClient.invalidateQueries({
        queryKey: anexoKeys.quota(),
      });
      toast.success('Arquivo enviado com sucesso!');
    },
    onError: (error: any) => {
      // RFC 7807 ProblemDetail usa 'detail', mas alguns handlers usam 'message'
      const message =
        error.response?.data?.detail ||
        error.response?.data?.message ||
        'Erro ao enviar arquivo';
      toast.error(message);
    },
  });
}

/**
 * Hook para deletar anexo.
 */
export function useDeleteAnexo() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id }: { id: string; entidadeTipo: EntidadeTipo; entidadeId: string }) =>
      anexoService.deletar(id),
    onSuccess: (_, variables) => {
      // Invalida cache da lista de anexos da entidade
      queryClient.invalidateQueries({
        queryKey: anexoKeys.entidade(variables.entidadeTipo, variables.entidadeId),
      });
      // Invalida cache de contagem
      queryClient.invalidateQueries({
        queryKey: anexoKeys.count(variables.entidadeTipo, variables.entidadeId),
      });
      // Invalida cache de quota
      queryClient.invalidateQueries({
        queryKey: anexoKeys.quota(),
      });
      toast.success('Arquivo removido com sucesso!');
    },
    onError: (error: any) => {
      // RFC 7807 ProblemDetail usa 'detail', mas alguns handlers usam 'message'
      const message =
        error.response?.data?.detail ||
        error.response?.data?.message ||
        'Erro ao remover arquivo';
      toast.error(message);
    },
  });
}

/**
 * Hook para download de anexo.
 */
export function useDownloadAnexo() {
  return useMutation({
    mutationFn: async ({
      id,
      nomeOriginal,
    }: {
      id: string;
      nomeOriginal: string;
    }) => {
      const blob = await anexoService.download(id);
      // Cria link temporário para download
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = nomeOriginal;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    },
    onError: (error: any) => {
      // RFC 7807 ProblemDetail usa 'detail', mas alguns handlers usam 'message'
      const message =
        error.response?.data?.detail ||
        error.response?.data?.message ||
        'Erro ao baixar arquivo';
      toast.error(message);
    },
  });
}

/**
 * Hook para alterar visibilidade de um anexo para o cliente.
 */
export function useAlterarVisibilidade() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      visivelParaCliente,
    }: {
      id: string;
      visivelParaCliente: boolean;
      entidadeTipo: EntidadeTipo;
      entidadeId: string;
    }) => anexoService.alterarVisibilidade(id, { visivelParaCliente }),
    onSuccess: (data, variables) => {
      // Invalida cache da lista de anexos da entidade
      queryClient.invalidateQueries({
        queryKey: anexoKeys.entidade(variables.entidadeTipo, variables.entidadeId),
      });
      // Invalida cache do anexo individual
      queryClient.invalidateQueries({
        queryKey: anexoKeys.detail(variables.id),
      });
      toast.success(
        data.visivelParaCliente
          ? 'Anexo agora está visível para o cliente'
          : 'Anexo oculto para o cliente'
      );
    },
    onError: (error: any) => {
      const message =
        error.response?.data?.detail ||
        error.response?.data?.message ||
        'Erro ao alterar visibilidade';
      toast.error(message);
    },
  });
}

/**
 * Hook para obter URL de blob para visualização de imagem autenticada.
 * Usa React Query para cache e gerencia limpeza automática do blob URL.
 */
export function useAnexoImageUrl(anexoId: string | null, enabled = true) {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);

  const query = useQuery({
    queryKey: [...anexoKeys.all, 'blob', anexoId],
    queryFn: async () => {
      if (!anexoId) return null;
      const blob = await anexoService.view(anexoId);
      return blob;
    },
    enabled: enabled && !!anexoId,
    staleTime: 5 * 60 * 1000, // 5 minutos
    gcTime: 10 * 60 * 1000, // 10 minutos
  });

  useEffect(() => {
    if (query.data) {
      const url = URL.createObjectURL(query.data);
      setBlobUrl(url);
      return () => {
        URL.revokeObjectURL(url);
      };
    }
    return undefined;
  }, [query.data]);

  return {
    url: blobUrl,
    isLoading: query.isLoading,
    error: query.error,
  };
}
