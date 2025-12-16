package com.pitstop.financeiro.domain;

/**
 * Status de uma Nota Fiscal Eletrônica no sistema PitStop.
 *
 * <p>Define o ciclo de vida de uma NF-e/NFS-e/NFC-e desde sua criação
 * até autorização, cancelamento ou rejeição.</p>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
public enum StatusNotaFiscal {

    /**
     * Nota em digitação (ainda não enviada).
     */
    DIGITACAO("Digitação", false),

    /**
     * Nota validada (estrutura XML correta, pronta para assinar).
     */
    VALIDADA("Validada", false),

    /**
     * Nota assinada digitalmente (pronta para envio).
     */
    ASSINADA("Assinada", false),

    /**
     * Nota enviada para SEFAZ/Prefeitura (aguardando retorno).
     */
    ENVIADA("Enviada", false),

    /**
     * Nota autorizada pelo órgão emissor.
     * Estado final bem-sucedido.
     */
    AUTORIZADA("Autorizada", true),

    /**
     * Nota denegada (irregularidade cadastral do destinatário).
     * Estado final.
     */
    DENEGADA("Denegada", true),

    /**
     * Nota rejeitada pela SEFAZ/Prefeitura.
     * Estado final (pode ser reemitida com novo número).
     */
    REJEITADA("Rejeitada", true),

    /**
     * Nota cancelada após autorização.
     * Estado final.
     */
    CANCELADA("Cancelada", true),

    /**
     * Numeração inutilizada (números pulados).
     * Estado final.
     */
    INUTILIZADA("Inutilizada", true);

    private final String descricao;
    private final boolean estadoFinal;

    StatusNotaFiscal(String descricao, boolean estadoFinal) {
        this.descricao = descricao;
        this.estadoFinal = estadoFinal;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se é um estado final (não pode mais mudar).
     *
     * @return true se é estado final
     */
    public boolean isEstadoFinal() {
        return estadoFinal;
    }

    /**
     * Verifica se a nota foi emitida com sucesso.
     *
     * @return true se status é AUTORIZADA
     */
    public boolean isAutorizada() {
        return this == AUTORIZADA;
    }

    /**
     * Verifica se a nota pode ser cancelada.
     *
     * @return true se status é AUTORIZADA (único estado que permite cancelamento)
     */
    public boolean podeCancelar() {
        return this == AUTORIZADA;
    }
}
