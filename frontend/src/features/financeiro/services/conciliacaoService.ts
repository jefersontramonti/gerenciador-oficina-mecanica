import { api } from '../../../shared/services/api';
import type {
  ExtratoBancarioDTO,
  TransacaoExtratoDTO,
  ConciliacaoLoteDTO,
  ConciliacaoLoteResult,
  ExtratoResumo,
} from '../types/conciliacao';

const BASE_URL = '/financeiro/conciliacao';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export const conciliacaoService = {
  /**
   * Importar arquivo OFX
   */
  importarExtrato: async (
    arquivo: File,
    contaBancariaId?: string
  ): Promise<ExtratoBancarioDTO> => {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    if (contaBancariaId) {
      formData.append('contaBancariaId', contaBancariaId);
    }

    const response = await api.post<ExtratoBancarioDTO>(
      `${BASE_URL}/importar`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },

  /**
   * Listar extratos
   */
  listarExtratos: async (
    page = 0,
    size = 20
  ): Promise<PageResponse<ExtratoBancarioDTO>> => {
    const response = await api.get<PageResponse<ExtratoBancarioDTO>>(
      `${BASE_URL}/extratos`,
      {
        params: { page, size },
      }
    );
    return response.data;
  },

  /**
   * Buscar extrato por ID
   */
  buscarExtrato: async (id: string): Promise<ExtratoBancarioDTO> => {
    const response = await api.get<ExtratoBancarioDTO>(
      `${BASE_URL}/extratos/${id}`
    );
    return response.data;
  },

  /**
   * Listar transações com sugestões
   */
  listarTransacoesComSugestoes: async (
    extratoId: string
  ): Promise<TransacaoExtratoDTO[]> => {
    const response = await api.get<TransacaoExtratoDTO[]>(
      `${BASE_URL}/extratos/${extratoId}/transacoes`
    );
    return response.data;
  },

  /**
   * Buscar resumo do extrato
   */
  buscarResumo: async (extratoId: string): Promise<ExtratoResumo> => {
    const response = await api.get<ExtratoResumo>(
      `${BASE_URL}/extratos/${extratoId}/resumo`
    );
    return response.data;
  },

  /**
   * Conciliar transação com pagamento
   */
  conciliarTransacao: async (
    transacaoId: string,
    pagamentoId: string
  ): Promise<TransacaoExtratoDTO> => {
    const response = await api.post<TransacaoExtratoDTO>(
      `${BASE_URL}/transacoes/${transacaoId}/conciliar`,
      null,
      {
        params: { pagamentoId },
      }
    );
    return response.data;
  },

  /**
   * Ignorar transação
   */
  ignorarTransacao: async (
    transacaoId: string,
    observacao?: string
  ): Promise<TransacaoExtratoDTO> => {
    const response = await api.post<TransacaoExtratoDTO>(
      `${BASE_URL}/transacoes/${transacaoId}/ignorar`,
      null,
      {
        params: { observacao },
      }
    );
    return response.data;
  },

  /**
   * Desconciliar transação
   */
  desconciliarTransacao: async (
    transacaoId: string
  ): Promise<TransacaoExtratoDTO> => {
    const response = await api.post<TransacaoExtratoDTO>(
      `${BASE_URL}/transacoes/${transacaoId}/desconciliar`
    );
    return response.data;
  },

  /**
   * Conciliação em lote
   */
  conciliarEmLote: async (
    request: ConciliacaoLoteDTO
  ): Promise<ConciliacaoLoteResult> => {
    const response = await api.post<ConciliacaoLoteResult>(
      `${BASE_URL}/lote`,
      request
    );
    return response.data;
  },
};

export default conciliacaoService;
