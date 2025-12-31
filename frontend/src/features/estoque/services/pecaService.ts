/**
 * Service para operações com Peças/Estoque
 */

import { api } from '@/shared/services/api';
import type {
  Peca,
  CreatePecaRequest,
  UpdatePecaRequest,
  PecaFilters,
  PagedResponse,
} from '../types';

const BASE_URL = '/estoque';

/**
 * Listar peças com filtros e paginação
 */
export const listarPecas = async (
  filters: PecaFilters = {}
): Promise<PagedResponse<Peca>> => {
  const { page = 0, size = 20, sort = ['descricao,asc'], ...restFilters } = filters;

  const params = new URLSearchParams();

  if (restFilters.codigo) params.append('codigo', restFilters.codigo);
  if (restFilters.descricao) params.append('descricao', restFilters.descricao);
  if (restFilters.marca) params.append('marca', restFilters.marca);
  if (restFilters.unidadeMedida) params.append('unidadeMedida', restFilters.unidadeMedida);
  if (restFilters.ativo !== undefined) params.append('ativo', restFilters.ativo.toString());
  if (restFilters.estoqueBaixo !== undefined) params.append('estoqueBaixo', restFilters.estoqueBaixo.toString());
  if (restFilters.localArmazenamentoId) params.append('localArmazenamentoId', restFilters.localArmazenamentoId);

  params.append('page', page.toString());
  params.append('size', size.toString());
  sort.forEach(s => params.append('sort', s));

  const { data } = await api.get<PagedResponse<Peca>>(BASE_URL, { params });
  return data;
};

/**
 * Buscar peça por ID
 */
export const buscarPecaPorId = async (id: string): Promise<Peca> => {
  const { data } = await api.get<Peca>(`${BASE_URL}/${id}`);
  return data;
};

/**
 * Buscar peça por código (SKU)
 */
export const buscarPecaPorCodigo = async (codigo: string): Promise<Peca> => {
  const { data } = await api.get<Peca>(`${BASE_URL}/codigo/${codigo}`);
  return data;
};

/**
 * Criar nova peça
 */
export const criarPeca = async (request: CreatePecaRequest): Promise<Peca> => {
  const { data } = await api.post<Peca>(BASE_URL, request);
  return data;
};

/**
 * Atualizar peça existente
 */
export const atualizarPeca = async (
  id: string,
  request: UpdatePecaRequest
): Promise<Peca> => {
  const { data } = await api.put<Peca>(`${BASE_URL}/${id}`, request);
  return data;
};

/**
 * Desativar peça (soft delete)
 */
export const desativarPeca = async (id: string): Promise<void> => {
  await api.delete(`${BASE_URL}/${id}`);
};

/**
 * Reativar peça desativada
 */
export const reativarPeca = async (id: string): Promise<Peca> => {
  const { data } = await api.patch<Peca>(`${BASE_URL}/${id}/reativar`);
  return data;
};

/**
 * Listar peças com estoque baixo (quantidadeAtual <= quantidadeMinima)
 */
export const listarAlertasEstoqueBaixo = async (
  page = 0,
  size = 20
): Promise<PagedResponse<Peca>> => {
  const params = new URLSearchParams();
  params.append('page', page.toString());
  params.append('size', size.toString());
  params.append('sort', 'quantidadeAtual,asc');

  const { data } = await api.get<PagedResponse<Peca>>(
    `${BASE_URL}/alertas/baixo`,
    { params }
  );
  return data;
};

/**
 * Listar peças com estoque zerado (quantidadeAtual = 0)
 */
export const listarAlertasEstoqueZerado = async (
  page = 0,
  size = 20
): Promise<PagedResponse<Peca>> => {
  const params = new URLSearchParams();
  params.append('page', page.toString());
  params.append('size', size.toString());
  params.append('sort', 'descricao,asc');

  const { data } = await api.get<PagedResponse<Peca>>(
    `${BASE_URL}/alertas/zerado`,
    { params }
  );
  return data;
};

/**
 * Obter valor total do inventário
 */
export const obterValorTotalInventario = async (): Promise<number> => {
  const { data } = await api.get<number>(`${BASE_URL}/relatorios/valor-total`);
  return data;
};

/**
 * Contar peças com estoque baixo (para badge de alerta)
 */
export const contarEstoqueBaixo = async (): Promise<number> => {
  const { data } = await api.get<number>(`${BASE_URL}/dashboard/estoque-baixo`);
  return data;
};

/**
 * Listar marcas distintas (para filtros)
 */
export const listarMarcas = async (): Promise<string[]> => {
  const { data } = await api.get<string[]>(`${BASE_URL}/filtros/marcas`);
  return data;
};

/**
 * Listar peças sem localização definida
 */
export const listarPecasSemLocalizacao = async (
  page = 0,
  size = 20
): Promise<PagedResponse<Peca>> => {
  const params = new URLSearchParams();
  params.append('page', page.toString());
  params.append('size', size.toString());

  const { data } = await api.get<PagedResponse<Peca>>(
    `${BASE_URL}/sem-localizacao`,
    { params }
  );
  return data;
};

/**
 * Contar peças sem localização (para badge/alerta)
 */
export const contarPecasSemLocalizacao = async (): Promise<number> => {
  const { data } = await api.get<number>(`${BASE_URL}/dashboard/sem-localizacao`);
  return data;
};

/**
 * Definir/alterar localização de uma peça
 */
export const definirLocalizacaoPeca = async (
  pecaId: string,
  localId?: string
): Promise<Peca> => {
  const params = new URLSearchParams();
  if (localId) params.append('localId', localId);

  const { data } = await api.post<Peca>(
    `${BASE_URL}/${pecaId}/definir-localizacao?${params.toString()}`
  );
  return data;
};

const pecaService = {
  listarPecas,
  buscarPecaPorId,
  buscarPecaPorCodigo,
  criarPeca,
  atualizarPeca,
  desativarPeca,
  reativarPeca,
  listarAlertasEstoqueBaixo,
  listarAlertasEstoqueZerado,
  obterValorTotalInventario,
  contarEstoqueBaixo,
  listarMarcas,
  listarPecasSemLocalizacao,
  contarPecasSemLocalizacao,
  definirLocalizacaoPeca,
};

export default pecaService;
