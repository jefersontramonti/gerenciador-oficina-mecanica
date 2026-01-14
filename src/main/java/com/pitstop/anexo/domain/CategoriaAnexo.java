package com.pitstop.anexo.domain;

/**
 * Categorias de anexos por tipo de entidade.
 *
 * <p>Categorias para ORDEM_SERVICO:</p>
 * <ul>
 *   <li>FOTO_VEICULO - Fotos do estado do veículo</li>
 *   <li>DIAGNOSTICO - Imagens de diagnóstico (scanner OBD, testes)</li>
 *   <li>AUTORIZACAO - Autorização do cliente assinada</li>
 *   <li>LAUDO_TECNICO - Laudos técnicos</li>
 * </ul>
 *
 * <p>Categorias para CLIENTE:</p>
 * <ul>
 *   <li>DOCUMENTO_PESSOAL - CNH, RG</li>
 *   <li>DOCUMENTO_EMPRESA - CNPJ, Contrato Social</li>
 *   <li>CONTRATO - Contratos assinados</li>
 *   <li>DOCUMENTO_VEICULO - CRLV, documentos do veículo</li>
 * </ul>
 *
 * <p>Categorias para PECA:</p>
 * <ul>
 *   <li>FOTO_PECA - Foto da peça</li>
 *   <li>NOTA_FISCAL - Nota fiscal de compra</li>
 *   <li>CERTIFICADO - Certificados de qualidade</li>
 * </ul>
 */
public enum CategoriaAnexo {
    // Ordem de Serviço
    FOTO_VEICULO,
    DIAGNOSTICO,
    AUTORIZACAO,
    LAUDO_TECNICO,

    // Cliente
    DOCUMENTO_PESSOAL,
    DOCUMENTO_EMPRESA,
    CONTRATO,
    DOCUMENTO_VEICULO,

    // Peça
    FOTO_PECA,
    NOTA_FISCAL,
    CERTIFICADO,

    // Genérico
    OUTROS
}
