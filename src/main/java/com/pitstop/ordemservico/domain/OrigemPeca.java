package com.pitstop.ordemservico.domain;

/**
 * Origem de uma peça em um item da Ordem de Serviço.
 *
 * <p>Permite distinguir entre peças do estoque, compradas externamente ou fornecidas pelo cliente:</p>
 * <ul>
 *   <li><strong>ESTOQUE:</strong> Peça do inventário da oficina (gera baixa automática)</li>
 *   <li><strong>AVULSA:</strong> Peça comprada externamente para a OS (sem controle de estoque)</li>
 *   <li><strong>CLIENTE:</strong> Peça fornecida pelo próprio cliente (sem controle de estoque)</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0
 */
public enum OrigemPeca {

    /**
     * Peça do estoque/inventário da oficina.
     *
     * <p>Características:</p>
     * <ul>
     *   <li>Requer peca_id (FK para tabela pecas) obrigatório</li>
     *   <li>Gera baixa automática de estoque ao finalizar OS</li>
     *   <li>Rastreabilidade completa via movimentações de estoque</li>
     *   <li>Custo e margem calculados automaticamente</li>
     * </ul>
     *
     * <p>Exemplo:</p>
     * <pre>
     * Filtro de óleo (código: FO-123) - Qtd: 1 - R$ 45,00
     * → Baixa automática no estoque ao finalizar
     * </pre>
     */
    ESTOQUE("Estoque", "Peça do inventário da oficina", true),

    /**
     * Peça comprada externamente para esta OS específica.
     *
     * <p>Características:</p>
     * <ul>
     *   <li>peca_id é null (não vinculada ao estoque)</li>
     *   <li>Não gera movimentação de estoque</li>
     *   <li>Requer descrição detalhada (mín. 10 caracteres)</li>
     *   <li>Valor informado manualmente</li>
     *   <li>Útil para peças raras ou compradas sob demanda</li>
     * </ul>
     *
     * <p>Exemplo:</p>
     * <pre>
     * Sensor de temperatura do ar específico BMW X5 2018
     * → Comprado diretamente do fornecedor para esta OS
     * </pre>
     */
    AVULSA("Avulsa", "Peça comprada externamente para a OS", false),

    /**
     * Peça fornecida pelo próprio cliente.
     *
     * <p>Características:</p>
     * <ul>
     *   <li>peca_id é null (não vinculada ao estoque)</li>
     *   <li>Não gera movimentação de estoque</li>
     *   <li>Requer descrição detalhada (mín. 10 caracteres)</li>
     *   <li>Valor geralmente zerado ou apenas taxa de instalação</li>
     *   <li>Cliente assume responsabilidade pela qualidade</li>
     * </ul>
     *
     * <p>Exemplo:</p>
     * <pre>
     * Jogo de pneus Michelin 205/55R16 (cliente trouxe)
     * → Apenas cobrança de montagem/balanceamento
     * </pre>
     */
    CLIENTE("Cliente", "Peça fornecida pelo cliente", false);

    private final String displayName;
    private final String descricao;
    private final boolean afetaEstoque;

    OrigemPeca(String displayName, String descricao, boolean afetaEstoque) {
        this.displayName = displayName;
        this.descricao = descricao;
        this.afetaEstoque = afetaEstoque;
    }

    /**
     * Retorna o nome para exibição ao usuário.
     *
     * @return Nome formatado da origem
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Retorna a descrição da origem.
     *
     * @return Descrição explicativa
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se esta origem gera movimentação de estoque.
     *
     * @return true se gera baixa automática de estoque
     */
    public boolean isAfetaEstoque() {
        return afetaEstoque;
    }

    /**
     * Verifica se esta origem requer peca_id (FK).
     *
     * @return true se peca_id é obrigatório
     */
    public boolean requerPecaId() {
        return this == ESTOQUE;
    }

    /**
     * Verifica se esta origem é do inventário da oficina.
     *
     * @return true se é peça do estoque
     */
    public boolean isDoEstoque() {
        return this == ESTOQUE;
    }

    /**
     * Verifica se esta origem é externa (avulsa ou cliente).
     *
     * @return true se não é do estoque
     */
    public boolean isExterna() {
        return this != ESTOQUE;
    }

    /**
     * Verifica se o cliente forneceu a peça.
     *
     * @return true se peça é do cliente
     */
    public boolean isDoCliente() {
        return this == CLIENTE;
    }
}
