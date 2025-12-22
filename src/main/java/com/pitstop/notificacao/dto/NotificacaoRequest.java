package com.pitstop.notificacao.dto;

import com.pitstop.notificacao.domain.TemplateNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Map;

/**
 * Request para envio de notificação.
 *
 * @author PitStop Team
 */
@Builder
public record NotificacaoRequest(
    /**
     * Tipo de notificação (EMAIL, WHATSAPP, TELEGRAM, SMS).
     */
    @NotNull(message = "Tipo de notificação é obrigatório")
    TipoNotificacao tipo,

    /**
     * Destinatário da notificação.
     * - Para EMAIL: endereço de email
     * - Para WHATSAPP: número de telefone (formato: +5511999999999)
     * - Para TELEGRAM: chat_id
     * - Para SMS: número de telefone
     */
    @NotBlank(message = "Destinatário é obrigatório")
    String destinatario,

    /**
     * Assunto da mensagem (usado apenas para EMAIL).
     */
    String assunto,

    /**
     * Corpo da mensagem (texto simples ou HTML para email).
     */
    @NotBlank(message = "Mensagem é obrigatória")
    String mensagem,

    /**
     * Template a ser usado (opcional).
     * Se informado, ignora o campo 'mensagem' e usa o template com as variáveis.
     */
    TemplateNotificacao template,

    /**
     * Variáveis do template (opcional).
     * Exemplo: {"nome": "João", "valor": "199.90", "dataVencimento": "2025-01-15"}
     */
    Map<String, Object> variaveis
) {
    /**
     * Factory method para criar notificação de email simples.
     */
    public static NotificacaoRequest email(
        String destinatario,
        String assunto,
        String mensagem
    ) {
        return NotificacaoRequest.builder()
            .tipo(TipoNotificacao.EMAIL)
            .destinatario(destinatario)
            .assunto(assunto)
            .mensagem(mensagem)
            .build();
    }

    /**
     * Factory method para criar notificação de email com template.
     */
    public static NotificacaoRequest emailComTemplate(
        String destinatario,
        TemplateNotificacao template,
        Map<String, Object> variaveis
    ) {
        return NotificacaoRequest.builder()
            .tipo(TipoNotificacao.EMAIL)
            .destinatario(destinatario)
            .assunto(template.getSubject())
            .template(template)
            .variaveis(variaveis)
            .mensagem("") // Será ignorado ao usar template
            .build();
    }

    /**
     * Factory method para criar notificação de WhatsApp.
     */
    public static NotificacaoRequest whatsapp(
        String numeroTelefone,
        String mensagem
    ) {
        return NotificacaoRequest.builder()
            .tipo(TipoNotificacao.WHATSAPP)
            .destinatario(numeroTelefone)
            .mensagem(mensagem)
            .build();
    }

    /**
     * Factory method para criar notificação de Telegram.
     */
    public static NotificacaoRequest telegram(
        String chatId,
        String mensagem
    ) {
        return NotificacaoRequest.builder()
            .tipo(TipoNotificacao.TELEGRAM)
            .destinatario(chatId)
            .mensagem(mensagem)
            .build();
    }
}
