/**
 * Authentication types
 */

export const PerfilUsuario = {
  ADMIN: 'ADMIN',
  GERENTE: 'GERENTE',
  ATENDENTE: 'ATENDENTE',
  MECANICO: 'MECANICO',
} as const;

export type PerfilUsuario = typeof PerfilUsuario[keyof typeof PerfilUsuario];

export interface Usuario {
  id: string;
  nome: string;
  email: string;
  perfil: PerfilUsuario;
  perfilNome?: string | null;
  ativo: boolean;
  ultimoAcesso: string | number[] | null;
  createdAt: string | number[];
  updatedAt: string | number[];
}

export interface LoginRequest {
  email: string;
  senha: string;
  rememberMe?: boolean;
}

export interface RegisterRequest {
  nome: string;
  email: string;
  senha: string;
  perfil: PerfilUsuario;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  usuario: Usuario;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface AuthState {
  user: Usuario | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}
