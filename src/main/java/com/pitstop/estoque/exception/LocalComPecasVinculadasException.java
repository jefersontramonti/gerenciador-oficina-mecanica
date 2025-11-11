package com.pitstop.estoque.exception;

/**
 * Exception lançada quando se tenta excluir (hard delete) um local que possui peças vinculadas.
 *
 * Regra de negócio:
 * - Soft delete (desativar) sempre é permitido
 * - Hard delete (remover do banco) só é permitido se não houver peças vinculadas
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public class LocalComPecasVinculadasException extends RuntimeException {

    private final long quantidadePecas;

    /**
     * Construtor com mensagem padrão.
     *
     * @param quantidadePecas quantidade de peças vinculadas ao local
     */
    public LocalComPecasVinculadasException(long quantidadePecas) {
        super(String.format(
                "Não é possível excluir este local: existem %d peça(s) vinculada(s). " +
                "Primeiro mova as peças para outro local ou desative o local ao invés de excluir.",
                quantidadePecas
        ));
        this.quantidadePecas = quantidadePecas;
    }

    /**
     * Construtor com mensagem customizada.
     *
     * @param mensagem mensagem de erro
     * @param quantidadePecas quantidade de peças vinculadas
     */
    public LocalComPecasVinculadasException(String mensagem, long quantidadePecas) {
        super(mensagem);
        this.quantidadePecas = quantidadePecas;
    }

    /**
     * Retorna a quantidade de peças vinculadas.
     *
     * @return quantidade de peças
     */
    public long getQuantidadePecas() {
        return quantidadePecas;
    }
}
