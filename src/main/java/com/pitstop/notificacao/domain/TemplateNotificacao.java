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
     * Alerta de pagamento vencido.
     */
    PAYMENT_OVERDUE("payment-overdue", "Pagamento em atraso"),

    /**
     * Confirmação de pagamento recebido.
     */
    PAYMENT_CONFIRMED("payment-confirmed", "Pagamento confirmado"),

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
    SYSTEM_ALERT("system-alert", "Alerta do Sistema");

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
