package com.pitstop.notificacao.domain;

/**
 * Tipos de notificação suportados pelo sistema.
 *
 * @author PitStop Team
 */
public enum TipoNotificacao {
    /**
     * Notificação por email.
     */
    EMAIL,

    /**
     * Notificação por WhatsApp (via Twilio ou Evolution API).
     */
    WHATSAPP,

    /**
     * Notificação por Telegram (bot).
     */
    TELEGRAM,

    /**
     * Notificação por SMS.
     */
    SMS
}
