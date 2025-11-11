package com.pitstop.estoque.exception;

/**
 * Exception lançada quando uma operação criaria um ciclo na hierarquia de locais.
 *
 * Exemplo de ciclo inválido:
 * - Local A tem pai B
 * - Local B tem pai C
 * - Tentar fazer C ter pai A criaria: A -> B -> C -> A (ciclo)
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public class CicloHierarquicoException extends RuntimeException {

    /**
     * Construtor com mensagem padrão.
     */
    public CicloHierarquicoException() {
        super("Operação não permitida: criaria ciclo na hierarquia de locais");
    }

    /**
     * Construtor com mensagem customizada.
     *
     * @param mensagem mensagem de erro
     */
    public CicloHierarquicoException(String mensagem) {
        super(mensagem);
    }

    /**
     * Construtor com mensagem e causa.
     *
     * @param mensagem mensagem de erro
     * @param causa causa da exception
     */
    public CicloHierarquicoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
