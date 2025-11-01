package com.pitstop.ordemservico.domain;

/**
 * Tipo de item em uma Ordem de Serviço.
 *
 * <p>Um ItemOS pode ser:</p>
 * <ul>
 *   <li><strong>PECA:</strong> Peça física do estoque (requer FK para tabela pecas)</li>
 *   <li><strong>SERVICO:</strong> Mão de obra ou serviço executado</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0
 */
public enum TipoItem {

    /**
     * Item representa uma peça do estoque.
     *
     * <p>Características:</p>
     * <ul>
     *   <li>Deve ter peca_id (FK) obrigatório</li>
     *   <li>Afeta o estoque (baixa automática ao finalizar OS)</li>
     *   <li>Preço vem do cadastro de peças</li>
     *   <li>Gera movimentação de estoque</li>
     * </ul>
     *
     * <p>Exemplos:</p>
     * <ul>
     *   <li>Óleo de motor 5W30</li>
     *   <li>Filtro de ar</li>
     *   <li>Pastilha de freio</li>
     *   <li>Lâmpada H4</li>
     * </ul>
     */
    PECA("Peça", "Item do estoque de peças"),

    /**
     * Item representa um serviço ou mão de obra.
     *
     * <p>Características:</p>
     * <ul>
     *   <li>Não possui peca_id (opcional/nullable)</li>
     *   <li>Não afeta o estoque</li>
     *   <li>Preço definido manualmente ou tabela de serviços</li>
     *   <li>Não gera movimentação de estoque</li>
     * </ul>
     *
     * <p>Exemplos:</p>
     * <ul>
     *   <li>Troca de óleo</li>
     *   <li>Alinhamento e balanceamento</li>
     *   <li>Revisão completa</li>
     *   <li>Reparo de motor</li>
     * </ul>
     */
    SERVICO("Serviço", "Mão de obra ou serviço executado");

    private final String displayName;
    private final String descricao;

    TipoItem(String displayName, String descricao) {
        this.displayName = displayName;
        this.descricao = descricao;
    }

    /**
     * Retorna o nome para exibição ao usuário.
     *
     * @return Nome formatado do tipo
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Retorna a descrição do tipo.
     *
     * @return Descrição explicativa do tipo
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se este tipo requer peça do estoque.
     *
     * @return true se o campo peca_id é obrigatório
     */
    public boolean requerPeca() {
        return this == PECA;
    }

    /**
     * Verifica se este tipo afeta o estoque.
     *
     * @return true se gera movimentação de estoque
     */
    public boolean afetaEstoque() {
        return this == PECA;
    }
}
