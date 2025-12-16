package com.pitstop.financeiro.domain;

/**
 * Tipos de Nota Fiscal suportados pelo sistema PitStop.
 *
 * <p>Define os modelos de notas fiscais eletrônicas que podem ser emitidas
 * pelas oficinas, conforme legislação brasileira.</p>
 *
 * <p><strong>Status de Implementação:</strong></p>
 * <ul>
 *   <li>NF-e (modelo 55): Planejada (ver NFE_IMPLEMENTATION_PLAN.md)</li>
 *   <li>NFS-e: Planejada (municipal, varia por cidade)</li>
 *   <li>NFC-e (modelo 65): Planejada (SAT ou contingência)</li>
 * </ul>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
public enum TipoNotaFiscal {

    /**
     * Nota Fiscal Eletrônica (modelo 55).
     * Usada para prestação de serviços e venda de produtos.
     * <p>Requer certificado digital, credenciamento na SEFAZ estadual.</p>
     */
    NFE("55", "NF-e - Nota Fiscal Eletrônica", "SEFAZ Estadual"),

    /**
     * Nota Fiscal de Serviço Eletrônica (municipal).
     * Específica para prestação de serviços.
     * <p>Requer credenciamento na Prefeitura. Layout varia por município.</p>
     */
    NFSE("NFS-e", "NFS-e - Nota Fiscal de Serviço Eletrônica", "Prefeitura Municipal"),

    /**
     * Nota Fiscal de Consumidor Eletrônica (modelo 65).
     * Substitui cupom fiscal para varejo.
     * <p>Requer SAT ou contingência online. Geralmente usada em PDV.</p>
     */
    NFCE("65", "NFC-e - Nota Fiscal de Consumidor Eletrônica", "SEFAZ Estadual");

    private final String modelo;
    private final String descricao;
    private final String orgaoEmissor;

    TipoNotaFiscal(String modelo, String descricao, String orgaoEmissor) {
        this.modelo = modelo;
        this.descricao = descricao;
        this.orgaoEmissor = orgaoEmissor;
    }

    public String getModelo() {
        return modelo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getOrgaoEmissor() {
        return orgaoEmissor;
    }

    /**
     * Verifica se é nota fiscal estadual (SEFAZ).
     *
     * @return true se emitida pela SEFAZ
     */
    public boolean isEstadual() {
        return this == NFE || this == NFCE;
    }

    /**
     * Verifica se é nota fiscal municipal (Prefeitura).
     *
     * @return true se emitida pela Prefeitura
     */
    public boolean isMunicipal() {
        return this == NFSE;
    }
}
