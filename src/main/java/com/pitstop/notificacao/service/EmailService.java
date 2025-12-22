package com.pitstop.notificacao.service;

import com.pitstop.notificacao.dto.NotificacaoRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Serviço para envio de emails.
 *
 * Suporta:
 * - Emails de texto simples
 * - Emails HTML com templates Thymeleaf
 * - Variáveis dinâmicas nos templates
 *
 * @author PitStop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final TemplateService templateService;

    @Value("${spring.mail.username:noreply@pitstop.com}")
    private String fromEmail;

    @Value("${spring.mail.from-name:PitStop}")
    private String fromName;

    /**
     * Envia email baseado na requisição.
     *
     * @param request Dados do email
     */
    public void enviar(NotificacaoRequest request) {
        if (request.template() != null) {
            enviarComTemplate(request);
        } else {
            enviarSimples(request);
        }
    }

    /**
     * Envia email de texto simples.
     *
     * @param request Dados do email
     */
    private void enviarSimples(NotificacaoRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromName, fromEmail));
            message.setTo(request.destinatario());
            message.setSubject(request.assunto());
            message.setText(request.mensagem());

            mailSender.send(message);

            log.info("Email simples enviado para: {}", request.destinatario());
        } catch (Exception e) {
            log.error("Erro ao enviar email simples para {}: {}", request.destinatario(), e.getMessage(), e);
            throw new RuntimeException("Falha ao enviar email", e);
        }
    }

    /**
     * Envia email HTML usando template customizável.
     *
     * @param request Dados do email (deve conter template e variáveis)
     */
    private void enviarComTemplate(NotificacaoRequest request) {
        try {
            // Busca template customizado (ou usa padrão/hardcoded)
            var templateCustomizado = templateService.obterTemplate(
                null, // TODO: Pegar oficina_id do contexto quando disponível
                request.template(),
                com.pitstop.notificacao.domain.TipoNotificacao.EMAIL
            );

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(String.format("%s <%s>", fromName, fromEmail));
            helper.setTo(request.destinatario());

            // Processa assunto com variáveis
            String assuntoProcessado = templateService.processarAssunto(templateCustomizado, request.variaveis());
            helper.setSubject(assuntoProcessado);

            // Processa corpo do template
            String htmlContent;
            if (templateCustomizado.isTemplatePadrao() && !templateCustomizado.getCorpo().startsWith("<")) {
                // Template hardcoded, processar com Thymeleaf se for HTML
                Context context = new Context();
                if (request.variaveis() != null) {
                    request.variaveis().forEach(context::setVariable);
                }
                htmlContent = templateEngine.process(
                    request.template().getTemplateFileName(),
                    context
                );
            } else {
                // Template customizado, processar variáveis simples
                htmlContent = templateService.processarCorpo(templateCustomizado, request.variaveis());
            }

            helper.setText(htmlContent, true); // true = isHtml

            mailSender.send(mimeMessage);

            log.info("Email HTML (template: {}) enviado para: {}",
                request.template().getTemplateId(), request.destinatario());
        } catch (MessagingException e) {
            log.error("Erro ao criar mensagem de email para {}: {}", request.destinatario(), e.getMessage(), e);
            throw new RuntimeException("Falha ao criar email", e);
        } catch (Exception e) {
            log.error("Erro ao enviar email com template para {}: {}", request.destinatario(), e.getMessage(), e);
            throw new RuntimeException("Falha ao enviar email", e);
        }
    }

    /**
     * Verifica se o serviço de email está configurado.
     *
     * @return true se configurado, false caso contrário
     */
    public boolean isConfigurado() {
        try {
            // Verifica se o JavaMailSender está configurado
            return mailSender != null;
        } catch (Exception e) {
            log.warn("Email service não está configurado: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envia email de teste para verificar configuração.
     *
     * @param destinatario Email de destino
     */
    public void enviarTeste(String destinatario) {
        NotificacaoRequest request = NotificacaoRequest.email(
            destinatario,
            "Teste de Email - PitStop",
            "Este é um email de teste do sistema PitStop.\n\n" +
            "Se você recebeu este email, significa que o serviço de email está configurado corretamente!\n\n" +
            "Atenciosamente,\n" +
            "Equipe PitStop"
        );

        enviar(request);
    }
}
