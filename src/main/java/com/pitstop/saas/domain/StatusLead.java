package com.pitstop.saas.domain;

/**
 * Status do lead no funil de vendas.
 *
 * Representa as diferentes fases do ciclo de vida de um lead
 * desde a captura inicial até a conversão ou perda.
 *
 * @author PitStop Team
 */
public enum StatusLead {

    /**
     * Lead recém-capturado, ainda não contatado.
     */
    NOVO,

    /**
     * Lead que já recebeu primeiro contato da equipe de vendas.
     */
    CONTATADO,

    /**
     * Lead qualificado com alto potencial de conversão.
     */
    QUALIFICADO,

    /**
     * Lead que se tornou cliente (converteu em oficina).
     */
    CONVERTIDO,

    /**
     * Lead que não converteu e foi descartado.
     */
    PERDIDO
}
