import { api } from '@/shared/services/api';
import type {
  PlanoAssinaturaDTO,
  AssinaturaDTO,
  CreateAssinaturaDTO,
  CancelarAssinaturaDTO,
  FaturaAssinaturaDTO,
  RegistrarPagamentoDTO,
  StatusAssinatura,
  StatusFaturaAssinatura,
} from '../types/assinatura';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

const BASE_URL = '/financeiro/assinaturas';

// ========== PLANOS ==========

export async function listarPlanos(): Promise<PlanoAssinaturaDTO[]> {
  const response = await api.get<PlanoAssinaturaDTO[]>(`${BASE_URL}/planos`);
  return response.data;
}

export async function listarPlanosAtivos(): Promise<PlanoAssinaturaDTO[]> {
  const response = await api.get<PlanoAssinaturaDTO[]>(`${BASE_URL}/planos/ativos`);
  return response.data;
}

export async function buscarPlano(id: string): Promise<PlanoAssinaturaDTO> {
  const response = await api.get<PlanoAssinaturaDTO>(`${BASE_URL}/planos/${id}`);
  return response.data;
}

export async function criarPlano(plano: PlanoAssinaturaDTO): Promise<PlanoAssinaturaDTO> {
  const response = await api.post<PlanoAssinaturaDTO>(`${BASE_URL}/planos`, plano);
  return response.data;
}

export async function atualizarPlano(id: string, plano: PlanoAssinaturaDTO): Promise<PlanoAssinaturaDTO> {
  const response = await api.put<PlanoAssinaturaDTO>(`${BASE_URL}/planos/${id}`, plano);
  return response.data;
}

export async function desativarPlano(id: string): Promise<void> {
  await api.delete(`${BASE_URL}/planos/${id}`);
}

// ========== ASSINATURAS ==========

export interface ListarAssinaturasParams {
  status?: StatusAssinatura;
  planoId?: string;
  busca?: string;
  page?: number;
  size?: number;
}

export async function listarAssinaturas(params: ListarAssinaturasParams = {}): Promise<PageResponse<AssinaturaDTO>> {
  const response = await api.get<PageResponse<AssinaturaDTO>>(BASE_URL, { params });
  return response.data;
}

export async function buscarAssinatura(id: string): Promise<AssinaturaDTO> {
  const response = await api.get<AssinaturaDTO>(`${BASE_URL}/${id}`);
  return response.data;
}

export async function criarAssinatura(dto: CreateAssinaturaDTO): Promise<AssinaturaDTO> {
  const response = await api.post<AssinaturaDTO>(BASE_URL, dto);
  return response.data;
}

export async function pausarAssinatura(id: string): Promise<AssinaturaDTO> {
  const response = await api.post<AssinaturaDTO>(`${BASE_URL}/${id}/pausar`);
  return response.data;
}

export async function reativarAssinatura(id: string): Promise<AssinaturaDTO> {
  const response = await api.post<AssinaturaDTO>(`${BASE_URL}/${id}/reativar`);
  return response.data;
}

export async function cancelarAssinatura(id: string, dto: CancelarAssinaturaDTO): Promise<AssinaturaDTO> {
  const response = await api.post<AssinaturaDTO>(`${BASE_URL}/${id}/cancelar`, dto);
  return response.data;
}

// ========== FATURAS ==========

export interface ListarFaturasParams {
  status?: StatusFaturaAssinatura;
  assinaturaId?: string;
  page?: number;
  size?: number;
}

export async function listarFaturas(params: ListarFaturasParams = {}): Promise<PageResponse<FaturaAssinaturaDTO>> {
  const response = await api.get<PageResponse<FaturaAssinaturaDTO>>(`${BASE_URL}/faturas`, { params });
  return response.data;
}

export async function buscarFatura(id: string): Promise<FaturaAssinaturaDTO> {
  const response = await api.get<FaturaAssinaturaDTO>(`${BASE_URL}/faturas/${id}`);
  return response.data;
}

export async function listarFaturasAssinatura(assinaturaId: string): Promise<FaturaAssinaturaDTO[]> {
  const response = await api.get<FaturaAssinaturaDTO[]>(`${BASE_URL}/${assinaturaId}/faturas`);
  return response.data;
}

export async function registrarPagamento(faturaId: string, dto?: RegistrarPagamentoDTO): Promise<FaturaAssinaturaDTO> {
  const response = await api.post<FaturaAssinaturaDTO>(`${BASE_URL}/faturas/${faturaId}/pagar`, dto || {});
  return response.data;
}

export async function cancelarFatura(faturaId: string, observacao?: string): Promise<FaturaAssinaturaDTO> {
  const response = await api.post<FaturaAssinaturaDTO>(`${BASE_URL}/faturas/${faturaId}/cancelar`, null, {
    params: { observacao },
  });
  return response.data;
}
