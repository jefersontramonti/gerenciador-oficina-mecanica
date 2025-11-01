package com.pitstop.ordemservico.domain;

/**
 * Status possíveis de uma Ordem de Serviço.
 * Representa a máquina de estados do fluxo de trabalho da oficina.
 *
 * <p>Fluxo principal:</p>
 * <pre>
 * ORCAMENTO → APROVADO → EM_ANDAMENTO → FINALIZADO → ENTREGUE
 * </pre>
 *
 * <p>Fluxo alternativo (aguardando peças):</p>
 * <pre>
 * EM_ANDAMENTO → AGUARDANDO_PECA → EM_ANDAMENTO → FINALIZADO → ENTREGUE
 * </pre>
 *
 * <p>Cancelamento pode ocorrer em qualquer status antes de ENTREGUE</p>
 *
 * @author PitStop Team
 * @since 1.0
 */
public enum StatusOS {

    /**
     * OS criada, aguardando aprovação do cliente.
     * Estado inicial de toda OS.
     *
     * <p>Transições permitidas:</p>
     * <ul>
     *   <li>APROVADO - Cliente aprovou o orçamento</li>
     *   <li>CANCELADO - Cliente recusou ou desistiu</li>
     * </ul>
     */
    ORCAMENTO("Orçamento", "Aguardando aprovação do cliente"),

    /**
     * Orçamento aprovado pelo cliente, aguardando início dos trabalhos.
     *
     * <p>Transições permitidas:</p>
     * <ul>
     *   <li>EM_ANDAMENTO - Mecânico iniciou os trabalhos</li>
     *   <li>CANCELADO - Cliente desistiu antes do início</li>
     * </ul>
     */
    APROVADO("Aprovado", "Orçamento aprovado, aguardando início"),

    /**
     * Mecânico está executando os serviços.
     *
     * <p>Transições permitidas:</p>
     * <ul>
     *   <li>AGUARDANDO_PECA - Falta peça em estoque</li>
     *   <li>FINALIZADO - Serviço concluído</li>
     *   <li>CANCELADO - Problema impediu conclusão</li>
     * </ul>
     */
    EM_ANDAMENTO("Em Andamento", "Serviço sendo executado"),

    /**
     * Serviço pausado por falta de peça.
     *
     * <p>Transições permitidas:</p>
     * <ul>
     *   <li>EM_ANDAMENTO - Peça chegou, retomando trabalho</li>
     *   <li>CANCELADO - Cliente desistiu da espera</li>
     * </ul>
     */
    AGUARDANDO_PECA("Aguardando Peça", "Pausado até chegada de peça"),

    /**
     * Serviço concluído, veículo pronto para entrega.
     * Neste ponto, ocorre a baixa automática de peças do estoque.
     *
     * <p>Transições permitidas:</p>
     * <ul>
     *   <li>ENTREGUE - Veículo entregue ao cliente</li>
     * </ul>
     *
     * <p><strong>Observação:</strong> Após finalizado, a OS não pode mais ser cancelada.</p>
     */
    FINALIZADO("Finalizado", "Serviço concluído, pronto para entrega"),

    /**
     * Veículo entregue ao cliente.
     * <strong>Estado final</strong> - não há mais transições possíveis.
     *
     * <p>Requisitos:</p>
     * <ul>
     *   <li>Pagamento deve estar completo</li>
     *   <li>Cliente assinou termo de entrega (opcional)</li>
     * </ul>
     */
    ENTREGUE("Entregue", "Veículo entregue ao cliente"),

    /**
     * OS cancelada.
     * <strong>Estado final</strong> - não há mais transições possíveis.
     *
     * <p>Pode ocorrer em qualquer status antes de ENTREGUE.</p>
     * <p>Motivo do cancelamento deve ser registrado no campo "observacoes".</p>
     */
    CANCELADO("Cancelado", "OS cancelada");

    private final String displayName;
    private final String descricao;

    StatusOS(String displayName, String descricao) {
        this.displayName = displayName;
        this.descricao = descricao;
    }

    /**
     * Retorna o nome para exibição ao usuário.
     *
     * @return Nome formatado do status
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Retorna a descrição do status.
     *
     * @return Descrição explicativa do status
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se este status permite edição da OS.
     *
     * @return true se a OS pode ser editada neste status
     */
    public boolean isEditavel() {
        return this == ORCAMENTO || this == APROVADO;
    }

    /**
     * Verifica se este status permite cancelamento.
     *
     * @return true se a OS pode ser cancelada neste status
     */
    public boolean isCancelavel() {
        return this != ENTREGUE && this != CANCELADO;
    }

    /**
     * Verifica se este status é final (não permite mais transições).
     *
     * @return true se é um estado final
     */
    public boolean isFinal() {
        return this == ENTREGUE || this == CANCELADO;
    }

    /**
     * Verifica se este status indica que o serviço está em execução.
     *
     * @return true se o mecânico está trabalhando na OS
     */
    public boolean isEmExecucao() {
        return this == EM_ANDAMENTO || this == AGUARDANDO_PECA;
    }
}
