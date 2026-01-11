package com.pitstop.notificacao.domain;

import lombok.Getter;

import java.util.Map;

/**
 * Enum que define os eventos de negócio que podem disparar notificações.
 * Cada evento possui variáveis dinâmicas disponíveis para uso em templates.
 */
@Getter
public enum EventoNotificacao {

    // ===== ORDEM DE SERVIÇO =====
    OS_CRIADA(
        "OS Criada",
        "Notifica cliente que orçamento foi criado",
        Map.of(
            "numeroOS", "Número da OS",
            "nomeCliente", "Nome do cliente",
            "veiculoPlaca", "Placa do veículo",
            "veiculoModelo", "Modelo do veículo",
            "valorTotal", "Valor total estimado",
            "dataAbertura", "Data de abertura",
            "nomeOficina", "Nome da oficina"
        ),
        TemplateNotificacao.OS_CREATED
    ),

    OS_AGUARDANDO_APROVACAO(
        "Aguardando Aprovação",
        "Lembra cliente que precisa aprovar orçamento",
        Map.of(
            "numeroOS", "Número da OS",
            "nomeCliente", "Nome do cliente",
            "valorTotal", "Valor total",
            "dataAbertura", "Data de abertura",
            "nomeOficina", "Nome da oficina"
        ),
        TemplateNotificacao.OS_WAITING_APPROVAL
    ),

    OS_APROVADA(
        "OS Aprovada",
        "Confirma que orçamento foi aprovado pelo cliente",
        Map.of(
            "numeroOS", "Número da OS",
            "nomeCliente", "Nome do cliente",
            "dataAprovacao", "Data da aprovação",
            "nomeOficina", "Nome da oficina"
        ),
        TemplateNotificacao.OS_APPROVED
    ),

    OS_REJEITADA(
        "OS Rejeitada",
        "Notifica que orçamento foi rejeitado pelo cliente",
        Map.of(
            "numeroOS", "Número da OS",
            "nomeCliente", "Nome do cliente",
            "motivoRejeicao", "Motivo da rejeição",
            "dataRejeicao", "Data da rejeição",
            "nomeOficina", "Nome da oficina"
        ),
        TemplateNotificacao.OS_REJECTED
    ),

    OS_EM_ANDAMENTO(
        "OS Em Andamento",
        "Notifica cliente que serviço iniciou",
        Map.of(
            "numeroOS", "Número da OS",
            "nomeCliente", "Nome do cliente",
            "veiculoPlaca", "Placa do veículo",
            "veiculoModelo", "Modelo do veículo",
            "mecanico", "Nome do mecânico responsável",
            "dataPrevisao", "Data prevista de conclusão",
            "nomeOficina", "Nome da oficina"
        ),
        TemplateNotificacao.OS_IN_PROGRESS
    ),

    OS_AGUARDANDO_PECA(
        "Aguardando Peça",
        "Notifica cliente que serviço está aguardando peça",
        Map.of(
            "numeroOS", "Número da OS",
            "nomeCliente", "Nome do cliente",
            "veiculoPlaca", "Placa do veículo",
            "pecaAguardada", "Peça aguardada",
            "previsaoChegada", "Previsão de chegada",
            "nomeOficina", "Nome da oficina"
        ),
        TemplateNotificacao.OS_WAITING_PART
    ),

    OS_FINALIZADA(
        "OS Finalizada",
        "Notifica cliente que veículo está pronto para retirada",
        Map.of(
            "numeroOS", "Número da OS",
            "nomeCliente", "Nome do cliente",
            "veiculoPlaca", "Placa do veículo",
            "veiculoModelo", "Modelo do veículo",
            "valorTotal", "Valor total",
            "servicosRealizados", "Serviços realizados",
            "nomeOficina", "Nome da oficina"
        ),
        TemplateNotificacao.OS_COMPLETED
    ),

    OS_ENTREGUE(
        "OS Entregue",
        "Confirmação de entrega do veículo ao cliente",
        Map.of(
            "numeroOS", "Número da OS",
            "nomeCliente", "Nome do cliente",
            "veiculoPlaca", "Placa do veículo",
            "dataEntrega", "Data da entrega",
            "nomeOficina", "Nome da oficina"
        ),
        TemplateNotificacao.OS_DELIVERED
    ),

    // ===== PAGAMENTO =====
    PAGAMENTO_PENDENTE(
        "Pagamento Pendente",
        "Lembra cliente sobre pagamento em aberto",
        Map.of(
            "numeroOS", "Número da OS",
            "nomeCliente", "Nome do cliente",
            "valorPendente", "Valor pendente",
            "dataVencimento", "Data de vencimento",
            "nomeOficina", "Nome da oficina"
        ),
        TemplateNotificacao.PAYMENT_PENDING
    ),

    PAGAMENTO_CONFIRMADO(
        "Pagamento Confirmado",
        "Confirma recebimento de pagamento",
        Map.of(
            "numeroOS", "Número da OS",
            "nomeCliente", "Nome do cliente",
            "valorPago", "Valor pago",
            "dataPagamento", "Data do pagamento",
            "formaPagamento", "Forma de pagamento",
            "nomeOficina", "Nome da oficina"
        ),
        TemplateNotificacao.PAYMENT_CONFIRMED
    ),

    // ===== LEMBRETES =====
    LEMBRETE_RETIRADA(
        "Lembrete de Retirada",
        "Lembra cliente que veículo está pronto há alguns dias",
        Map.of(
            "numeroOS", "Número da OS",
            "nomeCliente", "Nome do cliente",
            "veiculoPlaca", "Placa do veículo",
            "diasEsperando", "Dias aguardando retirada",
            "nomeOficina", "Nome da oficina"
        ),
        TemplateNotificacao.REMINDER_PICKUP
    ),

    LEMBRETE_REVISAO(
        "Lembrete de Revisão",
        "Lembra cliente sobre revisão preventiva",
        Map.of(
            "nomeCliente", "Nome do cliente",
            "veiculoPlaca", "Placa do veículo",
            "veiculoModelo", "Modelo do veículo",
            "quilometragemAtual", "Quilometragem atual",
            "proximaRevisao", "Próxima revisão",
            "nomeOficina", "Nome da oficina"
        ),
        TemplateNotificacao.REMINDER_MAINTENANCE
    ),

    // ===== TESTE =====
    TESTE(
        "Teste de Notificação",
        "Mensagem de teste para validar configuração",
        Map.of(
            "nomeOficina", "Nome da oficina"
        ),
        TemplateNotificacao.TEST
    );

    private final String nome;
    private final String descricao;
    private final Map<String, String> variaveisDisponiveis;
    private final TemplateNotificacao templatePadrao;

    EventoNotificacao(
        String nome,
        String descricao,
        Map<String, String> variaveisDisponiveis,
        TemplateNotificacao templatePadrao
    ) {
        this.nome = nome;
        this.descricao = descricao;
        this.variaveisDisponiveis = Map.copyOf(variaveisDisponiveis);
        this.templatePadrao = templatePadrao;
    }
}
