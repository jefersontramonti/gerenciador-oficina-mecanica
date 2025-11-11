package com.pitstop.estoque.domain;

import lombok.Getter;

/**
 * Tipos de locais de armazenamento para o sistema de localização física.
 * Representa diferentes tipos de estruturas de organização do estoque.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Getter
public enum TipoLocal {

    /**
     * Prateleira - Estrutura horizontal para armazenamento.
     * Pode conter caixas ou peças diretamente.
     * Normalmente está dentro de um armário ou depósito.
     */
    PRATELEIRA("Prateleira", true, new TipoLocal[]{}),

    /**
     * Gaveta - Compartimento deslizante para pequenas peças.
     * Deve estar dentro de um armário ou prateleira.
     */
    GAVETA("Gaveta", true, new TipoLocal[]{PRATELEIRA}),

    /**
     * Armário - Estrutura vertical com prateleiras/gavetas.
     * Normalmente está dentro de um depósito ou é raiz.
     */
    ARMARIO("Armário", false, new TipoLocal[]{}),

    /**
     * Depósito - Local físico principal (warehouse).
     * Normalmente é um local raiz (sem pai).
     */
    DEPOSITO("Depósito", false, new TipoLocal[]{}),

    /**
     * Caixa organizadora - Recipiente para agrupar peças.
     * Pode estar em prateleiras ou gavetas.
     */
    CAIXA("Caixa Organizadora", true, new TipoLocal[]{PRATELEIRA, GAVETA}),

    /**
     * Vitrine - Estrutura de exposição (normalmente para peças de venda).
     * Pode ser raiz ou dentro de depósito.
     */
    VITRINE("Vitrine", false, new TipoLocal[]{}),

    /**
     * Outro - Tipo genérico para locais que não se enquadram nas categorias.
     * Usado principalmente para migração de dados legados.
     */
    OUTRO("Outro", false, new TipoLocal[]{});

    /**
     * Descrição amigável do tipo de local.
     */
    private final String descricao;

    /**
     * Indica se este tipo de local deve obrigatoriamente ter um pai.
     * Exemplo: GAVETA sempre deve estar dentro de algo (ARMARIO ou PRATELEIRA).
     */
    private final boolean requerPai;

    /**
     * Tipos de locais permitidos como pai deste tipo.
     * Array vazio significa que qualquer tipo é permitido (ou nenhum se requerPai=false).
     */
    private final TipoLocal[] tiposPaiPermitidos;

    /**
     * Construtor do enum.
     *
     * @param descricao descrição amigável
     * @param requerPai se true, não pode ser local raiz
     * @param tiposPaiPermitidos array de tipos permitidos como pai (vazio = qualquer)
     */
    TipoLocal(String descricao, boolean requerPai, TipoLocal[] tiposPaiPermitidos) {
        this.descricao = descricao;
        this.requerPai = requerPai;
        this.tiposPaiPermitidos = tiposPaiPermitidos;
    }

    /**
     * Valida se este tipo pode ter o tipo informado como pai.
     *
     * @param tipoPai tipo do local pai
     * @return true se é permitido
     */
    public boolean aceitaTipoPai(TipoLocal tipoPai) {
        // Se não há restrições específicas, aceita qualquer tipo
        if (tiposPaiPermitidos == null || tiposPaiPermitidos.length == 0) {
            return true;
        }

        // Verifica se o tipoPai está na lista de permitidos
        for (TipoLocal permitido : tiposPaiPermitidos) {
            if (permitido == tipoPai) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica se este tipo pode ser um local raiz (sem pai).
     *
     * @return true se pode ser raiz
     */
    public boolean podeSerRaiz() {
        return !requerPai;
    }

    /**
     * Retorna a descrição amigável.
     *
     * @return descrição
     */
    @Override
    public String toString() {
        return descricao;
    }
}
