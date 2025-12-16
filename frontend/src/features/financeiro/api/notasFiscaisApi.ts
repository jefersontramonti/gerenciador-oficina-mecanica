/**
 * API client para Notas Fiscais
 */

import { api } from '@/shared/services/api';
import type {
  NotaFiscal,
  NotaFiscalResumo,
  NotaFiscalRequestDTO,
  FiltrosNotaFiscal,
  StatusNotaFiscal,
} from '../types/notaFiscal';

interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const notasFiscaisApi = {
  /**
   * Listar notas fiscais com filtros e paginação
   */
  async listar(
    filtros?: FiltrosNotaFiscal,
    page = 0,
    size = 20
  ): Promise<PageResponse<NotaFiscalResumo>> {
    const params: any = { page, size };

    if (filtros?.status) params.status = filtros.status;
    if (filtros?.tipo) params.tipo = filtros.tipo;
    if (filtros?.dataInicio) params.dataInicio = filtros.dataInicio;
    if (filtros?.dataFim) params.dataFim = filtros.dataFim;

    const { data } = await api.get<PageResponse<NotaFiscalResumo>>(
      '/notas-fiscais',
      { params }
    );
    return data;
  },

  /**
   * Buscar nota fiscal por ID
   */
  async buscarPorId(id: string): Promise<NotaFiscal> {
    const { data } = await api.get<NotaFiscal>(`/notas-fiscais/${id}`);
    return data;
  },

  /**
   * Listar notas fiscais por ordem de serviço
   */
  async listarPorOrdemServico(ordemServicoId: string): Promise<NotaFiscal[]> {
    const { data } = await api.get<NotaFiscal[]>(
      `/notas-fiscais/ordem-servico/${ordemServicoId}`
    );
    return data;
  },

  /**
   * Buscar notas fiscais por status
   */
  async buscarPorStatus(
    status: StatusNotaFiscal,
    page = 0,
    size = 20
  ): Promise<PageResponse<NotaFiscalResumo>> {
    const { data } = await api.get<PageResponse<NotaFiscalResumo>>(
      `/notas-fiscais/status/${status}`,
      { params: { page, size } }
    );
    return data;
  },

  /**
   * Buscar nota fiscal por número e série
   */
  async buscarPorNumeroESerie(
    numero: number,
    serie: number
  ): Promise<NotaFiscal> {
    const { data } = await api.get<NotaFiscal>(
      `/notas-fiscais/numero/${numero}/serie/${serie}`
    );
    return data;
  },

  /**
   * Buscar nota fiscal por chave de acesso
   */
  async buscarPorChaveAcesso(chaveAcesso: string): Promise<NotaFiscal> {
    const { data } = await api.get<NotaFiscal>(
      `/notas-fiscais/chave-acesso/${chaveAcesso}`
    );
    return data;
  },

  /**
   * Criar nota fiscal
   */
  async criar(request: NotaFiscalRequestDTO): Promise<NotaFiscal> {
    const { data } = await api.post<NotaFiscal>('/notas-fiscais', request);
    return data;
  },

  /**
   * Atualizar nota fiscal
   */
  async atualizar(
    id: string,
    request: NotaFiscalRequestDTO
  ): Promise<NotaFiscal> {
    const { data } = await api.put<NotaFiscal>(
      `/notas-fiscais/${id}`,
      request
    );
    return data;
  },

  /**
   * Deletar nota fiscal
   */
  async deletar(id: string): Promise<void> {
    await api.delete(`/notas-fiscais/${id}`);
  },

  /**
   * Verificar se existe nota fiscal para uma OS
   */
  async existeParaOS(ordemServicoId: string): Promise<boolean> {
    const { data } = await api.get<boolean>(
      `/notas-fiscais/existe/ordem-servico/${ordemServicoId}`
    );
    return data;
  },

  /**
   * Buscar próximo número disponível para uma série
   */
  async buscarProximoNumero(serie: number): Promise<number> {
    const { data } = await api.get<number>(
      `/notas-fiscais/proximo-numero/serie/${serie}`
    );
    return data;
  },
};
