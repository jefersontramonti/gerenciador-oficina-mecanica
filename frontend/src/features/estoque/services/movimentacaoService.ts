/**
 * Service para operações com Movimentações de Estoque
 */

import { api } from '@/shared/services/api';
import type {
  MovimentacaoEstoque,
  CreateEntradaRequest,
  CreateSaidaRequest,
  CreateAjusteRequest,
  MovimentacaoFilters,
  PagedResponse,
} from '../types';

const BASE_URL = '/movimentacoes-estoque';

/**
 * Listar movimentações com filtros e paginação
 */
export const listarMovimentacoes = async (
  filters: MovimentacaoFilters = {}
): Promise<PagedResponse<MovimentacaoEstoque>> => {
  const { page = 0, size = 20, sort = ['dataMovimentacao,desc'], ...restFilters } = filters;

  const params = new URLSearchParams();

  if (restFilters.pecaId) params.append('pecaId', restFilters.pecaId);
  if (restFilters.tipo) params.append('tipo', restFilters.tipo);
  if (restFilters.dataInicio) params.append('dataInicio', restFilters.dataInicio);
  if (restFilters.dataFim) params.append('dataFim', restFilters.dataFim);
  if (restFilters.usuarioId) params.append('usuarioId', restFilters.usuarioId);

  params.append('page', page.toString());
  params.append('size', size.toString());
  sort.forEach(s => params.append('sort', s));

  const { data } = await api.get<PagedResponse<MovimentacaoEstoque>>(BASE_URL, { params });
  return data;
};

/**
 * Obter histórico de movimentações de uma peça específica
 */
export const obterHistoricoPeca = async (
  pecaId: string,
  page = 0,
  size = 20
): Promise<PagedResponse<MovimentacaoEstoque>> => {
  const params = new URLSearchParams();
  params.append('page', page.toString());
  params.append('size', size.toString());
  params.append('sort', 'dataMovimentacao,desc');

  const { data } = await api.get<PagedResponse<MovimentacaoEstoque>>(
    `${BASE_URL}/peca/${pecaId}`,
    { params }
  );
  return data;
};

/**
 * Obter movimentações vinculadas a uma Ordem de Serviço
 */
export const obterMovimentacoesPorOS = async (
  osId: string,
  page = 0,
  size = 20
): Promise<PagedResponse<MovimentacaoEstoque>> => {
  const params = new URLSearchParams();
  params.append('page', page.toString());
  params.append('size', size.toString());
  params.append('sort', 'dataMovimentacao,desc');

  const { data } = await api.get<PagedResponse<MovimentacaoEstoque>>(
    `${BASE_URL}/ordem-servico/${osId}`,
    { params }
  );
  return data;
};

/**
 * Registrar entrada de estoque
 */
export const registrarEntrada = async (
  request: CreateEntradaRequest
): Promise<MovimentacaoEstoque> => {
  const { data } = await api.post<MovimentacaoEstoque>(
    `${BASE_URL}/entrada`,
    request
  );
  return data;
};

/**
 * Registrar saída de estoque
 */
export const registrarSaida = async (
  request: CreateSaidaRequest
): Promise<MovimentacaoEstoque> => {
  const { data } = await api.post<MovimentacaoEstoque>(
    `${BASE_URL}/saida`,
    request
  );
  return data;
};

/**
 * Registrar ajuste de inventário
 */
export const registrarAjuste = async (
  request: CreateAjusteRequest
): Promise<MovimentacaoEstoque> => {
  const { data } = await api.post<MovimentacaoEstoque>(
    `${BASE_URL}/ajuste`,
    request
  );
  return data;
};

const movimentacaoService = {
  listarMovimentacoes,
  obterHistoricoPeca,
  obterMovimentacoesPorOS,
  registrarEntrada,
  registrarSaida,
  registrarAjuste,
};

export default movimentacaoService;
