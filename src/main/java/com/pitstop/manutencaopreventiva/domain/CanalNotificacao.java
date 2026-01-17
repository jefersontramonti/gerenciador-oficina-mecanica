package com.pitstop.manutencaopreventiva.domain;

/**
 * Canal de envio de notificação.
 */
public enum CanalNotificacao {
    /** WhatsApp via Evolution API */
    WHATSAPP,
    /** Email via SMTP */
    EMAIL,
    /** Telegram via Telegram Bot API */
    TELEGRAM,
    /** SMS (futuro) */
    SMS,
    /** Push notification (futuro) */
    PUSH,
    /** Notificação interna no sistema */
    INTERNO
}
