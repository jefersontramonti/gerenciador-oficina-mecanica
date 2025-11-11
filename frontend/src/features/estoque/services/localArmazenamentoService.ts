/**
 * Service para operações com Locais de Armazenamento
 * Endpoints: /api/locais-armazenamento
 */

import { api } from '@/shared/services/api';
import type {
  LocalArmazenamento,
  CreateLocalArmazenamentoRequest,
  UpdateLocalArmazenamentoRequest,
  TipoLocal,
} from '../types';

const BASE_URL = '/locais-armazenamento';

/**
 * Criar novo local de armazenamento
 */
export const criarLocal = async (
  request: CreateLocalArmazenamentoRequest
): Promise<LocalArmazenamento> => {
  const { data } = await api.post<LocalArmazenamento>(BASE_URL, request);
  return data;
};

/**
 * Listar todos os locais ativos
 */
export const listarTodos = async (): Promise<LocalArmazenamento[]> => {
  const { data } = await api.get<LocalArmazenamento[]>(BASE_URL);
  return data;
};

/**
 * Buscar local por ID
 */
export const buscarPorId = async (id: string): Promise<LocalArmazenamento> => {
  const { data } = await api.get<LocalArmazenamento>(`${BASE_URL}/${id}`);
  return data;
};

/**
 * Buscar local por código
 */
export const buscarPorCodigo = async (codigo: string): Promise<LocalArmazenamento> => {
  const { data } = await api.get<LocalArmazenamento>(`${BASE_URL}/codigo/${codigo}`);
  return data;
};

/**
 * Atualizar local existente
 */
export const atualizarLocal = async (
  id: string,
  request: UpdateLocalArmazenamentoRequest
): Promise<LocalArmazenamento> => {
  const { data } = await api.put<LocalArmazenamento>(`${BASE_URL}/${id}`, request);
  return data;
};

/**
 * Desativar local (soft delete)
 */
export const desativarLocal = async (id: string): Promise<void> => {
  await api.patch(`${BASE_URL}/${id}/desativar`);
};

/**
 * Reativar local desativado
 */
export const reativarLocal = async (id: string): Promise<LocalArmazenamento> => {
  const { data } = await api.patch<LocalArmazenamento>(`${BASE_URL}/${id}/reativar`);
  return data;
};

/**
 * Excluir local permanentemente (hard delete)
 * Só permitido se não houver peças vinculadas
 */
export const excluirLocal = async (id: string): Promise<void> => {
  await api.delete(`${BASE_URL}/${id}`);
};

/**
 * Listar locais raiz (sem pai)
 */
export const listarLocaisRaiz = async (): Promise<LocalArmazenamento[]> => {
  const { data } = await api.get<LocalArmazenamento[]>(`${BASE_URL}/raiz`);
  return data;
};

/**
 * Listar locais filhos de um pai específico
 */
export const listarFilhos = async (paiId: string): Promise<LocalArmazenamento[]> => {
  const { data } = await api.get<LocalArmazenamento[]>(`${BASE_URL}/filhos/${paiId}`);
  return data;
};

/**
 * Listar locais por tipo
 */
export const listarPorTipo = async (tipo: TipoLocal): Promise<LocalArmazenamento[]> => {
  const { data } = await api.get<LocalArmazenamento[]>(`${BASE_URL}/tipo/${tipo}`);
  return data;
};

/**
 * Buscar locais por descrição (parcial)
 */
export const buscarPorDescricao = async (descricao: string): Promise<LocalArmazenamento[]> => {
  const params = new URLSearchParams({ descricao });
  const { data } = await api.get<LocalArmazenamento[]>(`${BASE_URL}/buscar?${params.toString()}`);
  return data;
};

const localArmazenamentoService = {
  criarLocal,
  listarTodos,
  buscarPorId,
  buscarPorCodigo,
  atualizarLocal,
  desativarLocal,
  reativarLocal,
  excluirLocal,
  listarLocaisRaiz,
  listarFilhos,
  listarPorTipo,
  buscarPorDescricao,
};

export default localArmazenamentoService;
