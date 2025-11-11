/**
 * Tipos e interfaces para o módulo de Usuários
 */

/**
 * Enum de perfis de usuário
 */
export const PerfilUsuario = {
  ADMIN: 'ADMIN',
  GERENTE: 'GERENTE',
  ATENDENTE: 'ATENDENTE',
  MECANICO: 'MECANICO',
} as const;

export type PerfilUsuario = (typeof PerfilUsuario)[keyof typeof PerfilUsuario];

/**
 * Interface do usuário (resposta da API)
 * Nota: LocalDateTime pode vir como string ISO ou array [year, month, day, hour, minute, second]
 */
export interface Usuario {
  id: string;
  nome: string;
  email: string;
  perfil: PerfilUsuario;
  perfilNome?: string;
  ativo: boolean;
  ultimoAcesso?: string | number[] | null;
  createdAt: string | number[];
  updatedAt: string | number[];
}

/**
 * Request para criar usuário
 */
export interface CreateUsuarioRequest {
  nome: string;
  email: string;
  senha: string;
  perfil: PerfilUsuario;
}

/**
 * Request para atualizar usuário
 */
export interface UpdateUsuarioRequest {
  nome?: string;
  email?: string;
  senha?: string;
  perfil?: PerfilUsuario;
}

/**
 * Filtros para listagem de usuários
 */
export interface UsuarioFilters {
  perfil?: PerfilUsuario;
  ativo?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * Resposta paginada
 */
export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  number: number;
  size: number;
}
