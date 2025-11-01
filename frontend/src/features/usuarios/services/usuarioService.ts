/**
 * Service para gerenciamento de usuários
 * Comunica com a API REST /api/usuarios
 */

import { api } from '@/shared/services/api';
import type {
  Usuario,
  CreateUsuarioRequest,
  UpdateUsuarioRequest,
  UsuarioFilters,
  PageResponse,
  PerfilUsuario,
} from '../types';

export const usuarioService = {
  /**
   * Listar usuários com paginação e filtros
   */
  async findAll(filters: UsuarioFilters = {}): Promise<PageResponse<Usuario>> {
    const { data } = await api.get<PageResponse<Usuario>>('/usuarios', {
      params: filters,
    });
    return data;
  },

  /**
   * Buscar usuário por ID
   */
  async findById(id: string): Promise<Usuario> {
    const { data } = await api.get<Usuario>(`/usuarios/${id}`);
    return data;
  },

  /**
   * Buscar usuário por email
   */
  async findByEmail(email: string): Promise<Usuario> {
    const { data } = await api.get<Usuario>(`/usuarios/email/${email}`);
    return data;
  },

  /**
   * Listar usuários por perfil
   */
  async findByPerfil(perfil: PerfilUsuario): Promise<Usuario[]> {
    const { data } = await api.get<Usuario[]>(`/usuarios/perfil/${perfil}`);
    return data;
  },

  /**
   * Listar apenas usuários ativos
   */
  async findAllAtivos(): Promise<Usuario[]> {
    const { data } = await api.get<Usuario[]>('/usuarios/ativos');
    return data;
  },

  /**
   * Criar novo usuário
   */
  async create(request: CreateUsuarioRequest): Promise<Usuario> {
    const { data } = await api.post<Usuario>('/usuarios', request);
    return data;
  },

  /**
   * Atualizar usuário
   */
  async update(id: string, request: UpdateUsuarioRequest): Promise<Usuario> {
    const { data } = await api.put<Usuario>(`/usuarios/${id}`, request);
    return data;
  },

  /**
   * Desativar usuário (soft delete)
   */
  async delete(id: string): Promise<void> {
    await api.delete(`/usuarios/${id}`);
  },

  /**
   * Reativar usuário
   */
  async reactivate(id: string): Promise<Usuario> {
    const { data } = await api.patch<Usuario>(`/usuarios/${id}/reativar`);
    return data;
  },
};
