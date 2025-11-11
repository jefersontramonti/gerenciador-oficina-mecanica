package com.pitstop.oficina.domain;

/**
 * Type of legal entity for a workshop.
 *
 * @since 1.0.0
 */
public enum TipoPessoa {

    /**
     * Individual person (CPF) - Microempreendedor Individual (MEI) or autonomous.
     */
    PESSOA_FISICA("Pessoa Física", "CPF", 11),

    /**
     * Legal entity (CNPJ) - Company registered with CNPJ.
     */
    PESSOA_JURIDICA("Pessoa Jurídica", "CNPJ", 14);

    private final String descricao;
    private final String documentoTipo;
    private final int documentoTamanho;

    TipoPessoa(String descricao, String documentoTipo, int documentoTamanho) {
        this.descricao = descricao;
        this.documentoTipo = documentoTipo;
        this.documentoTamanho = documentoTamanho;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDocumentoTipo() {
        return documentoTipo;
    }

    public int getDocumentoTamanho() {
        return documentoTamanho;
    }

    /**
     * Checks if document length is valid for this person type.
     *
     * @param documento CPF or CNPJ (numbers only)
     * @return true if document has correct length
     */
    public boolean isDocumentoValido(String documento) {
        if (documento == null) {
            return false;
        }
        String numeros = documento.replaceAll("\\D", "");
        return numeros.length() == documentoTamanho;
    }
}
