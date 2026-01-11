package com.pitstop.notificacao.domain;

/**
 * Templates de notificação disponíveis no sistema.
 *
 * Cada template possui um identificador único e pode ser usado
 * para enviar notificações padronizadas aos usuários.
 *
 * @author PitStop Team
 */
public enum TemplateNotificacao {

    // ===== TEMPLATES DE OFICINA (SaaS) =====

    /**
     * Email de boas-vindas ao criar nova oficina.
     */
    OFICINA_WELCOME("oficina-welcome", "Bem-vindo ao PitStop!"),

    /**
     * Alerta de trial expirando em breve.
     */
    TRIAL_EXPIRING("trial-expiring", "Seu período trial está terminando"),

    /**
     * Notificação de trial expirado.
     */
    TRIAL_EXPIRED("trial-expired", "Seu período trial expirou"),

    /**
     * Alerta de pagamento vencido (plano).
     */
    PAYMENT_OVERDUE("payment-overdue", "Pagamento em atraso"),

    /**
     * Notificação de oficina suspensa.
     */
    OFICINA_SUSPENDED("oficina-suspended", "Sua conta foi suspensa"),

    /**
     * Notificação de oficina reativada.
     */
    OFICINA_ACTIVATED("oficina-activated", "Sua conta foi reativada"),

    /**
     * Resumo diário de métricas (para SUPER_ADMIN).
     */
    DAILY_METRICS("daily-metrics", "Resumo Diário - PitStop SaaS"),

    /**
     * Alerta de erro crítico no sistema (para SUPER_ADMIN).
     */
    SYSTEM_ALERT("system-alert", "Alerta do Sistema"),

    // ===== TEMPLATES DE ORDEM DE SERVIÇO =====

    /**
     * OS criada - orçamento inicial.
     */
    OS_CREATED("os-created", "Orçamento criado - OS #{{numeroOS}}"),

    /**
     * OS aguardando aprovação do cliente.
     */
    OS_WAITING_APPROVAL("os-waiting-approval", "Orçamento aguardando aprovação - OS #{{numeroOS}}"),

    /**
     * OS aprovada pelo cliente.
     */
    OS_APPROVED("os-approved", "Orçamento aprovado - OS #{{numeroOS}}"),

    /**
     * OS rejeitada pelo cliente.
     */
    OS_REJECTED("os-rejected", "Orçamento rejeitado - OS #{{numeroOS}}"),

    /**
     * OS em andamento - serviço iniciado.
     */
    OS_IN_PROGRESS("os-in-progress", "Serviço iniciado - OS #{{numeroOS}}"),

    /**
     * OS aguardando peça.
     */
    OS_WAITING_PART("os-waiting-part", "Aguardando peça - OS #{{numeroOS}}"),

    /**
     * OS finalizada - veículo pronto.
     */
    OS_COMPLETED("os-completed", "Veículo pronto para retirada - OS #{{numeroOS}}"),

    /**
     * OS entregue ao cliente.
     */
    OS_DELIVERED("os-delivered", "Veículo entregue - OS #{{numeroOS}}"),

    // ===== TEMPLATES DE PAGAMENTO =====

    /**
     * Pagamento pendente (OS).
     */
    PAYMENT_PENDING("payment-pending", "Pagamento pendente - OS #{{numeroOS}}"),

    /**
     * Confirmação de pagamento recebido (OS).
     */
    PAYMENT_CONFIRMED("payment-confirmed", "Pagamento confirmado - OS #{{numeroOS}}"),

    // ===== TEMPLATES DE LEMBRETES =====

    /**
     * Lembrete de retirada de veículo.
     */
    REMINDER_PICKUP("reminder-pickup", "Seu veículo está pronto para retirada"),

    /**
     * Lembrete de revisão/manutenção preventiva.
     */
    REMINDER_MAINTENANCE("reminder-maintenance", "Hora de fazer a revisão do seu veículo"),

    // ===== TEMPLATE DE TESTE =====

    /**
     * Mensagem de teste para validar configuração.
     */
    TEST("test", "Teste de notificação - PitStop");

    private final String templateId;
    private final String subject;

    TemplateNotificacao(String templateId, String subject) {
        this.templateId = templateId;
        this.subject = subject;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getSubject() {
        return subject;
    }

    /**
     * Retorna o nome do arquivo de template HTML (Thymeleaf).
     *
     * @return Nome do arquivo template (sem extensão)
     */
    public String getTemplateFileName() {
        return "email/" + templateId;
    }
}
