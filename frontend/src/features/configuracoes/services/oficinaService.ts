import { api } from '@/shared/services/api';
import type {
  Oficina,
  UpdateOficinaRequest,
  TipoPessoa,
  RegimeTributario,
  Contato,
  Endereco,
  InformacoesOperacionais,
  RedesSociais,
  DadosBancarios,
} from '../types';

/**
 * Oficina service for configuration settings
 *
 * Allows ADMIN/GERENTE to view and update their workshop data.
 * Uses the oficina endpoints with tenant-aware authorization.
 */
export const oficinaService = {
  /**
   * Get oficina by ID
   *
   * Used to fetch the current user's workshop data.
   * The oficinaId comes from the authenticated user's JWT token.
   *
   * @param id - Workshop ID (from user.oficinaId)
   * @returns Workshop data
   */
  async findById(id: string): Promise<Oficina> {
    const response = await api.get<Oficina>(`/oficinas/${id}`);
    return response.data;
  },

  /**
   * Update oficina data
   *
   * Partial update - only non-null fields are modified.
   * Requires ADMIN or GERENTE role for the current workshop.
   *
   * @param id - Workshop ID
   * @param data - Fields to update (all optional)
   * @returns Updated workshop data
   */
  async update(id: string, data: UpdateOficinaRequest): Promise<Oficina> {
    const response = await api.put<Oficina>(`/oficinas/${id}`, data);
    return response.data;
  },

  /**
   * Update basic info (nome, contato, endereco)
   *
   * Convenience method for the "Dados Basicos" tab.
   *
   * @param id - Workshop ID
   * @param data - Basic info fields
   * @returns Updated workshop data
   */
  async updateBasico(
    id: string,
    data: {
      nome?: string;
      nomeFantasia?: string;
      tipoPessoa?: TipoPessoa;
      contato?: Contato;
      endereco?: Endereco;
    }
  ): Promise<Oficina> {
    return this.update(id, data);
  },

  /**
   * Update operational info (horarios, capacidade, especialidades, redes sociais, valorHora)
   *
   * Convenience method for the "Operacional" tab.
   *
   * @param id - Workshop ID
   * @param data - Operational info fields
   * @returns Updated workshop data
   */
  async updateOperacional(
    id: string,
    data: {
      informacoesOperacionais?: InformacoesOperacionais;
      redesSociais?: RedesSociais;
      valorHora?: number;
    }
  ): Promise<Oficina> {
    return this.update(id, data);
  },

  /**
   * Update financial info (dados bancarios)
   *
   * Convenience method for the "Financeiro" tab.
   *
   * @param id - Workshop ID
   * @param data - Financial info fields
   * @returns Updated workshop data
   */
  async updateFinanceiro(
    id: string,
    data: {
      dadosBancarios?: DadosBancarios;
    }
  ): Promise<Oficina> {
    return this.update(id, data);
  },

  /**
   * Update fiscal info (inscricoes, regime tributario)
   *
   * Convenience method for the "Fiscal" tab.
   *
   * @param id - Workshop ID
   * @param data - Fiscal info fields
   * @returns Updated workshop data
   */
  async updateFiscal(
    id: string,
    data: {
      inscricaoEstadual?: string;
      inscricaoMunicipal?: string;
      regimeTributario?: RegimeTributario;
    }
  ): Promise<Oficina> {
    return this.update(id, data);
  },
};
