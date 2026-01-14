/**
 * Tipos de entidades que podem ter anexos.
 */
export type EntidadeTipo = 'ORDEM_SERVICO' | 'CLIENTE' | 'PECA';

/**
 * Categorias de anexos.
 */
export type CategoriaAnexo =
  // Ordem de Serviço
  | 'FOTO_VEICULO'
  | 'DIAGNOSTICO'
  | 'AUTORIZACAO'
  | 'LAUDO_TECNICO'
  // Cliente
  | 'DOCUMENTO_PESSOAL'
  | 'DOCUMENTO_EMPRESA'
  | 'CONTRATO'
  | 'DOCUMENTO_VEICULO'
  // Peça
  | 'FOTO_PECA'
  | 'NOTA_FISCAL'
  | 'CERTIFICADO'
  // Genérico
  | 'OUTROS';

/**
 * Resposta de anexo da API.
 */
export interface AnexoResponse {
  id: string;
  entidadeTipo: EntidadeTipo;
  entidadeId: string;
  categoria: CategoriaAnexo;
  nomeOriginal: string;
  tamanhoBytes: number;
  tamanhoFormatado: string;
  mimeType: string;
  isImagem: boolean;
  isPdf: boolean;
  descricao: string | null;
  uploadedByNome: string | null;
  uploadedAt: string;
  urlDownload: string;
  urlThumbnail: string | null;
  visivelParaCliente: boolean;
}

/**
 * Resposta de anexo público (para página de aprovação).
 */
export interface AnexoPublicoResponse {
  id: string;
  categoria: CategoriaAnexo;
  nomeOriginal: string;
  mimeType: string;
  isImagem: boolean;
  descricao: string | null;
}

/**
 * Request para alterar visibilidade de anexo.
 */
export interface AlterarVisibilidadeRequest {
  visivelParaCliente: boolean;
}

/**
 * Request para upload de anexo.
 */
export interface AnexoUploadRequest {
  file: File;
  entidadeTipo: EntidadeTipo;
  entidadeId: string;
  categoria?: CategoriaAnexo;
  descricao?: string;
}

/**
 * Resposta de quota.
 */
export interface QuotaResponse {
  usadoBytes: number;
  usadoFormatado: string;
  limiteBytes: number;
  limiteFormatado: string;
  percentualUsado: number;
  totalAnexos: number;
}

/**
 * Labels para categorias de anexo.
 */
export const categoriaLabels: Record<CategoriaAnexo, string> = {
  FOTO_VEICULO: 'Foto do Veículo',
  DIAGNOSTICO: 'Diagnóstico',
  AUTORIZACAO: 'Autorização',
  LAUDO_TECNICO: 'Laudo Técnico',
  DOCUMENTO_PESSOAL: 'Documento Pessoal',
  DOCUMENTO_EMPRESA: 'Documento Empresa',
  CONTRATO: 'Contrato',
  DOCUMENTO_VEICULO: 'Documento do Veículo',
  FOTO_PECA: 'Foto da Peça',
  NOTA_FISCAL: 'Nota Fiscal',
  CERTIFICADO: 'Certificado',
  OUTROS: 'Outros',
};

/**
 * Categorias disponíveis por tipo de entidade.
 */
export const categoriasPorEntidade: Record<EntidadeTipo, CategoriaAnexo[]> = {
  ORDEM_SERVICO: ['FOTO_VEICULO', 'DIAGNOSTICO', 'AUTORIZACAO', 'LAUDO_TECNICO', 'OUTROS'],
  CLIENTE: ['DOCUMENTO_PESSOAL', 'DOCUMENTO_EMPRESA', 'CONTRATO', 'DOCUMENTO_VEICULO', 'OUTROS'],
  PECA: ['FOTO_PECA', 'NOTA_FISCAL', 'CERTIFICADO', 'OUTROS'],
};
