package com.pitstop.estoque.exception;

import java.util.UUID;

/**
 * Exception lançada quando há tentativa de baixa de estoque com quantidade insuficiente.
 * Esta é uma exception crítica que impede a finalização de operações.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public class EstoqueInsuficienteException extends RuntimeException {

    private final UUID pecaId;
    private final String codigoPeca;
    private final String descricaoPeca;
    private final Integer quantidadeRequerida;
    private final Integer quantidadeDisponivel;

    /**
     * Construtor completo.
     *
     * @param pecaId ID da peça
     * @param codigoPeca código da peça
     * @param descricaoPeca descrição da peça
     * @param quantidadeRequerida quantidade solicitada
     * @param quantidadeDisponivel quantidade disponível em estoque
     */
    public EstoqueInsuficienteException(
            UUID pecaId,
            String codigoPeca,
            String descricaoPeca,
            Integer quantidadeRequerida,
            Integer quantidadeDisponivel
    ) {
        super(String.format(
                "Estoque insuficiente para a peça '%s' (código: %s). Requerido: %d, Disponível: %d",
                descricaoPeca, codigoPeca, quantidadeRequerida, quantidadeDisponivel
        ));
        this.pecaId = pecaId;
        this.codigoPeca = codigoPeca;
        this.descricaoPeca = descricaoPeca;
        this.quantidadeRequerida = quantidadeRequerida;
        this.quantidadeDisponivel = quantidadeDisponivel;
    }

    /**
     * Construtor simplificado com apenas ID da peça.
     *
     * @param pecaId ID da peça
     * @param quantidadeRequerida quantidade solicitada
     * @param quantidadeDisponivel quantidade disponível
     */
    public EstoqueInsuficienteException(
            UUID pecaId,
            Integer quantidadeRequerida,
            Integer quantidadeDisponivel
    ) {
        super(String.format(
                "Estoque insuficiente para a peça ID: %s. Requerido: %d, Disponível: %d",
                pecaId, quantidadeRequerida, quantidadeDisponivel
        ));
        this.pecaId = pecaId;
        this.codigoPeca = null;
        this.descricaoPeca = null;
        this.quantidadeRequerida = quantidadeRequerida;
        this.quantidadeDisponivel = quantidadeDisponivel;
    }

    public UUID getPecaId() {
        return pecaId;
    }

    public String getCodigoPeca() {
        return codigoPeca;
    }

    public String getDescricaoPeca() {
        return descricaoPeca;
    }

    public Integer getQuantidadeRequerida() {
        return quantidadeRequerida;
    }

    public Integer getQuantidadeDisponivel() {
        return quantidadeDisponivel;
    }

    /**
     * Retorna o déficit de estoque (quanto falta).
     *
     * @return quantidade faltante
     */
    public Integer getDeficit() {
        return quantidadeRequerida - quantidadeDisponivel;
    }
}
