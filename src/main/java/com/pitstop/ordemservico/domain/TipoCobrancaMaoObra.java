package com.pitstop.ordemservico.domain;

/**
 * Tipo de cobrança de mão de obra em uma Ordem de Serviço.
 *
 * <p>Permite o modelo híbrido de cobrança:</p>
 * <ul>
 *   <li><strong>VALOR_FIXO:</strong> Valor definido no orçamento (modelo tradicional)</li>
 *   <li><strong>POR_HORA:</strong> Valor calculado por horas trabalhadas na finalização</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0
 */
public enum TipoCobrancaMaoObra {

    /**
     * Modelo de cobrança com valor fixo definido no orçamento.
     *
     * <p>Características:</p>
     * <ul>
     *   <li>Valor da mão de obra definido na criação da OS</li>
     *   <li>Cliente aprova o valor exato</li>
     *   <li>Não requer informar horas trabalhadas na finalização</li>
     *   <li>Ideal para serviços com escopo bem definido</li>
     * </ul>
     *
     * <p>Exemplo de uso:</p>
     * <pre>
     * Mão de obra: R$ 400,00 (valor fixo aprovado pelo cliente)
     * </pre>
     */
    VALOR_FIXO("Valor Fixo", "Mão de obra com valor definido no orçamento"),

    /**
     * Modelo de cobrança por hora trabalhada.
     *
     * <p>Características:</p>
     * <ul>
     *   <li>Valor da mão de obra calculado na finalização: horas × valor/hora</li>
     *   <li>Cliente aprova um limite máximo de horas</li>
     *   <li>Requer informar horas trabalhadas na finalização</li>
     *   <li>Valor/hora é capturado como snapshot da oficina</li>
     *   <li>Ideal para serviços com escopo variável</li>
     * </ul>
     *
     * <p>Exemplo de uso:</p>
     * <pre>
     * Limite aprovado: até 5 horas × R$ 80/hora = máx R$ 400,00
     * Horas trabalhadas: 4 horas
     * Mão de obra final: R$ 320,00
     * </pre>
     */
    POR_HORA("Por Hora", "Mão de obra calculada por horas trabalhadas");

    private final String displayName;
    private final String descricao;

    TipoCobrancaMaoObra(String displayName, String descricao) {
        this.displayName = displayName;
        this.descricao = descricao;
    }

    /**
     * Retorna o nome para exibição ao usuário.
     *
     * @return Nome formatado do tipo de cobrança
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Retorna a descrição do tipo de cobrança.
     *
     * @return Descrição explicativa
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se este tipo requer informar horas trabalhadas na finalização.
     *
     * @return true se requer horas trabalhadas
     */
    public boolean requerHorasTrabalhadas() {
        return this == POR_HORA;
    }

    /**
     * Verifica se este tipo tem valor fixo definido no orçamento.
     *
     * @return true se o valor de mão de obra é fixo
     */
    public boolean isValorFixo() {
        return this == VALOR_FIXO;
    }

    /**
     * Verifica se o valor da mão de obra é calculado dinamicamente.
     *
     * @return true se o valor é calculado por horas
     */
    public boolean isValorCalculado() {
        return this == POR_HORA;
    }
}
