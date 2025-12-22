package com.pitstop.notificacao.service;

import com.pitstop.notificacao.domain.TipoNotificacao;
import com.pitstop.notificacao.dto.NotificacaoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço principal para envio de notificações.
 *
 * Orquestra o envio de notificações através de diferentes canais:
 * - Email (Spring Mail)
 * - WhatsApp (Twilio ou Evolution API)
 * - Telegram (Bot API)
 * - SMS (Twilio)
 *
 * @author PitStop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoService {

    private final EmailService emailService;
    // TODO: Adicionar WhatsAppService quando implementado
    // TODO: Adicionar TelegramService quando implementado
    // TODO: Adicionar SmsService quando implementado

    /**
     * Envia notificação de acordo com o tipo especificado.
     *
     * @param request Dados da notificação
     * @throws IllegalArgumentException se o tipo de notificação não for suportado
     */
    public void enviar(NotificacaoRequest request) {
        log.info("Enviando notificação {} para: {}", request.tipo(), request.destinatario());

        try {
            switch (request.tipo()) {
                case EMAIL -> emailService.enviar(request);
                case WHATSAPP -> {
                    log.warn("WhatsApp não implementado. Mensagem para {}: {}",
                        request.destinatario(), request.mensagem());
                    // TODO: whatsAppService.enviar(request);
                }
                case TELEGRAM -> {
                    log.warn("Telegram não implementado. Mensagem para {}: {}",
                        request.destinatario(), request.mensagem());
                    // TODO: telegramService.enviar(request);
                }
                case SMS -> {
                    log.warn("SMS não implementado. Mensagem para {}: {}",
                        request.destinatario(), request.mensagem());
                    // TODO: smsService.enviar(request);
                }
                default -> throw new IllegalArgumentException("Tipo de notificação não suportado: " + request.tipo());
            }

            log.info("Notificação {} enviada com sucesso para: {}", request.tipo(), request.destinatario());
        } catch (Exception e) {
            log.error("Erro ao enviar notificação {} para {}: {}",
                request.tipo(), request.destinatario(), e.getMessage(), e);
            throw new RuntimeException("Falha ao enviar notificação", e);
        }
    }

    /**
     * Envia notificação de forma assíncrona (não bloqueia a thread).
     *
     * @param request Dados da notificação
     */
    public void enviarAsync(NotificacaoRequest request) {
        // TODO: Implementar com @Async quando necessário
        log.info("Enviando notificação assíncrona {} para: {}", request.tipo(), request.destinatario());
        enviar(request);
    }

    /**
     * Verifica se um tipo de notificação está disponível/configurado.
     *
     * @param tipo Tipo de notificação
     * @return true se disponível, false caso contrário
     */
    public boolean isDisponivel(TipoNotificacao tipo) {
        return switch (tipo) {
            case EMAIL -> emailService.isConfigurado();
            case WHATSAPP, TELEGRAM, SMS -> false; // Não implementados ainda
        };
    }
}
