package com.pitstop.webhook.domain;

/**
 * Tipos de eventos que podem disparar webhooks.
 * Cada evento representa uma ação do sistema que pode ser notificada externamente.
 *
 * @author PitStop Team
 */
public enum TipoEventoWebhook {

    // ===== ORDENS DE SERVIÇO =====

    /** Ordem de serviço criada */
    OS_CRIADA("Ordem de Serviço Criada", "Disparado quando uma nova OS é criada"),

    /** Status da OS alterado */
    OS_STATUS_ALTERADO("Status da OS Alterado", "Disparado quando o status de uma OS muda"),

    /** OS aprovada pelo cliente */
    OS_APROVADA("OS Aprovada", "Disparado quando o cliente aprova o orçamento"),

    /** OS finalizada */
    OS_FINALIZADA("OS Finalizada", "Disparado quando a OS é finalizada"),

    /** OS entregue */
    OS_ENTREGUE("OS Entregue", "Disparado quando o veículo é entregue ao cliente"),

    /** OS cancelada */
    OS_CANCELADA("OS Cancelada", "Disparado quando uma OS é cancelada"),

    // ===== CLIENTES =====

    /** Cliente cadastrado */
    CLIENTE_CRIADO("Cliente Criado", "Disparado quando um novo cliente é cadastrado"),

    /** Cliente atualizado */
    CLIENTE_ATUALIZADO("Cliente Atualizado", "Disparado quando os dados do cliente são atualizados"),

    // ===== VEÍCULOS =====

    /** Veículo cadastrado */
    VEICULO_CRIADO("Veículo Criado", "Disparado quando um novo veículo é cadastrado"),

    /** Veículo atualizado */
    VEICULO_ATUALIZADO("Veículo Atualizado", "Disparado quando os dados do veículo são atualizados"),

    // ===== FINANCEIRO =====

    /** Pagamento recebido */
    PAGAMENTO_RECEBIDO("Pagamento Recebido", "Disparado quando um pagamento é confirmado"),

    /** Pagamento cancelado/estornado */
    PAGAMENTO_CANCELADO("Pagamento Cancelado", "Disparado quando um pagamento é cancelado ou estornado"),

    // ===== ESTOQUE =====

    /** Alerta de estoque baixo */
    ESTOQUE_BAIXO("Estoque Baixo", "Disparado quando uma peça atinge o estoque mínimo"),

    /** Movimentação de estoque */
    ESTOQUE_MOVIMENTADO("Estoque Movimentado", "Disparado quando há entrada ou saída de peças"),

    // ===== MANUTENÇÃO PREVENTIVA =====

    /** Manutenção preventiva vencida */
    MANUTENCAO_VENCIDA("Manutenção Vencida", "Disparado quando uma manutenção preventiva vence"),

    /** Agendamento de manutenção criado */
    AGENDAMENTO_CRIADO("Agendamento Criado", "Disparado quando um agendamento de manutenção é criado");

    private final String nome;
    private final String descricao;

    TipoEventoWebhook(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }
}
