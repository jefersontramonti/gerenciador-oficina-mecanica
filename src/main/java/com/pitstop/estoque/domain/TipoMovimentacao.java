package com.pitstop.estoque.domain;

/**
 * Enum que representa os tipos de movimentação de estoque.
 * Cada tipo tem um impacto diferente na quantidade de estoque disponível.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public enum TipoMovimentacao {

    /**
     * Entrada de estoque - aumenta quantidade disponível.
     * Usado para: compras de fornecedor, transferências recebidas, etc.
     */
    ENTRADA("Entrada de estoque", true),

    /**
     * Saída manual de estoque - diminui quantidade disponível.
     * Usado para: vendas avulsas, uso interno, transferências enviadas, etc.
     */
    SAIDA("Saída manual de estoque", false),

    /**
     * Ajuste de inventário - pode aumentar ou diminuir.
     * Usado para: correções de inventário físico, perdas, quebras, etc.
     */
    AJUSTE("Ajuste de inventário", null),

    /**
     * Devolução - aumenta quantidade disponível (estorno).
     * Usado para: estorno de OS cancelada, devolução de cliente, etc.
     */
    DEVOLUCAO("Devolução (estorno)", true),

    /**
     * Baixa automática por Ordem de Serviço - diminui quantidade disponível.
     * Usado internamente quando OS é finalizada (rastreabilidade específica).
     */
    BAIXA_OS("Baixa automática por OS", false);

    private final String descricao;
    private final Boolean aumentaEstoque; // null = pode aumentar ou diminuir (ajuste)

    TipoMovimentacao(String descricao, Boolean aumentaEstoque) {
        this.descricao = descricao;
        this.aumentaEstoque = aumentaEstoque;
    }

    /**
     * Retorna a descrição do tipo de movimentação.
     *
     * @return descrição completa
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Indica se o tipo de movimentação aumenta o estoque.
     *
     * @return true se aumenta, false se diminui, null se depende do contexto (AJUSTE)
     */
    public Boolean getAumentaEstoque() {
        return aumentaEstoque;
    }

    /**
     * Verifica se é um tipo de movimentação que aumenta estoque.
     *
     * @return true se aumenta estoque
     */
    public boolean isEntrada() {
        return Boolean.TRUE.equals(aumentaEstoque);
    }

    /**
     * Verifica se é um tipo de movimentação que diminui estoque.
     *
     * @return true se diminui estoque
     */
    public boolean isSaida() {
        return Boolean.FALSE.equals(aumentaEstoque);
    }

    /**
     * Verifica se é um ajuste (pode aumentar ou diminuir).
     *
     * @return true se é ajuste
     */
    public boolean isAjuste() {
        return aumentaEstoque == null;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
