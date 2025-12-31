package com.pitstop.notificacao.service;

import com.pitstop.notificacao.dto.NotificacaoRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

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

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

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
     * Envia email de texto simples ou HTML (detecta automaticamente).
     *
     * @param request Dados do email
     */
    private void enviarSimples(NotificacaoRequest request) {
        try {
            String mensagem = request.mensagem();
            boolean isHtml = contemHtml(mensagem);

            if (isHtml) {
                // Mensagem contém HTML - usar MimeMessage
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                helper.setFrom(String.format("%s <%s>", fromName, fromEmail));
                helper.setTo(request.destinatario());
                helper.setSubject(request.assunto());
                helper.setText(wrapInHtmlLayout(mensagem), true);

                mailSender.send(mimeMessage);
                log.info("Email HTML enviado para: {}", request.destinatario());
            } else {
                // Mensagem é texto puro - usar SimpleMailMessage
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(String.format("%s <%s>", fromName, fromEmail));
                message.setTo(request.destinatario());
                message.setSubject(request.assunto());
                message.setText(mensagem);

                mailSender.send(message);
                log.info("Email simples enviado para: {}", request.destinatario());
            }
        } catch (Exception e) {
            log.error("Erro ao enviar email para {}: {}", request.destinatario(), e.getMessage(), e);
            throw new RuntimeException("Falha ao enviar email", e);
        }
    }

    /**
     * Verifica se a mensagem contém tags HTML.
     */
    private boolean contemHtml(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) {
            return false;
        }
        // Verifica padrões comuns de HTML
        return mensagem.contains("<h1>") || mensagem.contains("<h2>") ||
               mensagem.contains("<p>") || mensagem.contains("<br") ||
               mensagem.contains("<div>") || mensagem.contains("<table") ||
               mensagem.contains("<strong>") || mensagem.contains("<b>") ||
               mensagem.contains("</") || mensagem.contains("<html");
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
                String corpoProcessado = templateService.processarCorpo(templateCustomizado, request.variaveis());
                // Envolver em estrutura HTML completa se for fragmento
                htmlContent = wrapInHtmlLayout(corpoProcessado);
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

    /**
     * Envia email com anexo (PDF ou outro arquivo).
     *
     * @param destinatario Email de destino
     * @param assunto Assunto do email
     * @param corpo Corpo do email (pode ser HTML)
     * @param anexoBytes Conteudo do anexo em bytes
     * @param nomeArquivo Nome do arquivo anexo
     * @param tipoMime Tipo MIME do anexo (ex: application/pdf)
     */
    public void enviarComAnexo(
        String destinatario,
        String assunto,
        String corpo,
        byte[] anexoBytes,
        String nomeArquivo,
        String tipoMime
    ) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(String.format("%s <%s>", fromName, fromEmail));
            helper.setTo(destinatario);
            helper.setSubject(assunto);

            // Verifica se e HTML
            boolean isHtml = contemHtml(corpo);
            if (isHtml) {
                helper.setText(wrapInHtmlLayout(corpo), true);
            } else {
                helper.setText(corpo, false);
            }

            // Adiciona o anexo
            helper.addAttachment(nomeArquivo, new ByteArrayResource(anexoBytes), tipoMime);

            mailSender.send(mimeMessage);
            log.info("Email com anexo enviado para: {} (arquivo: {})", destinatario, nomeArquivo);

        } catch (MessagingException e) {
            log.error("Erro ao criar email com anexo para {}: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Falha ao criar email com anexo", e);
        }
    }

    /**
     * Envia email com PDF anexo.
     *
     * @param destinatario Email de destino
     * @param assunto Assunto do email
     * @param corpo Corpo do email
     * @param pdfBytes Conteudo do PDF
     * @param nomePdf Nome do arquivo PDF
     */
    public void enviarComPdf(
        String destinatario,
        String assunto,
        String corpo,
        byte[] pdfBytes,
        String nomePdf
    ) {
        String nomeCompleto = nomePdf.endsWith(".pdf") ? nomePdf : nomePdf + ".pdf";
        enviarComAnexo(destinatario, assunto, corpo, pdfBytes, nomeCompleto, "application/pdf");
    }

    /**
     * Sends a password reset email with a link containing the reset token.
     *
     * @param toEmail recipient email address
     * @param toName recipient name
     * @param resetToken password reset token (UUID)
     * @throws MessagingException if email sending fails
     * @throws UnsupportedEncodingException if encoding is not supported
     */
    public void sendPasswordResetEmail(String toEmail, String toName, String resetToken)
            throws MessagingException, UnsupportedEncodingException {
        log.debug("Sending password reset email to: {}", toEmail);

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String subject = "Redefinição de Senha - PitStop Pro";

        String htmlContent = buildPasswordResetEmailHtml(toName, resetLink);

        sendHtmlEmail(toEmail, subject, htmlContent);

        log.info("Password reset email sent successfully to: {}", toEmail);
    }

    /**
     * Sends an HTML email directly (without template).
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * Envolve o conteúdo do template em uma estrutura HTML completa.
     * Necessário para que clientes de email renderizem corretamente o HTML.
     *
     * @param content Conteúdo HTML do template (fragmento)
     * @return HTML completo com estrutura DOCTYPE, html, head, body
     */
    private String wrapInHtmlLayout(String content) {
        // Se já é um documento HTML completo, retorna como está
        if (content.trim().toLowerCase().startsWith("<!doctype") ||
            content.trim().toLowerCase().startsWith("<html")) {
            return content;
        }

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>PitStop Pro</title>
                <style>
                    body {
                        margin: 0;
                        padding: 0;
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background-color: #f4f4f4;
                        line-height: 1.6;
                    }
                    .email-container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: #ffffff;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        overflow: hidden;
                    }
                    .email-header {
                        padding: 30px 40px;
                        text-align: center;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                    }
                    .email-header h1 {
                        margin: 0;
                        color: #ffffff;
                        font-size: 24px;
                        font-weight: 600;
                    }
                    .email-body {
                        padding: 40px;
                        color: #333333;
                    }
                    .email-body h1 {
                        color: #333333;
                        font-size: 22px;
                        margin: 0 0 20px 0;
                    }
                    .email-body p {
                        margin: 0 0 15px 0;
                        color: #666666;
                        font-size: 16px;
                    }
                    .email-body strong {
                        color: #333333;
                    }
                    .email-footer {
                        padding: 25px 40px;
                        background-color: #f8f9fa;
                        text-align: center;
                    }
                    .email-footer p {
                        margin: 0;
                        color: #999999;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <table role="presentation" style="width: 100%%; border-collapse: collapse; padding: 40px 0;">
                    <tr>
                        <td align="center">
                            <div class="email-container">
                                <div class="email-header">
                                    <h1>🔧 PitStop Pro</h1>
                                </div>
                                <div class="email-body">
                                    %s
                                </div>
                                <div class="email-footer">
                                    <p>Este é um email automático. Por favor, não responda.</p>
                                    <p style="margin-top: 10px;">© 2025 PitStop Pro. Todos os direitos reservados.</p>
                                </div>
                            </div>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(content);
    }

    /**
     * Builds the HTML content for password reset email.
     */
    private String buildPasswordResetEmailHtml(String userName, String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Redefinição de Senha</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                    <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                        <tr>
                            <td align="center" style="padding: 40px 0;">
                                <table role="presentation" style="width: 600px; border-collapse: collapse; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                    <tr>
                                        <td style="padding: 40px 40px 20px 40px; text-align: center; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 8px 8px 0 0;">
                                            <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 600;">🔧 PitStop Pro</h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 40px;">
                                            <h2 style="margin: 0 0 20px 0; color: #333333; font-size: 24px; font-weight: 600;">Redefinição de Senha</h2>
                                            <p style="margin: 0 0 20px 0; color: #666666; font-size: 16px; line-height: 1.6;">Olá <strong>%s</strong>,</p>
                                            <p style="margin: 0 0 20px 0; color: #666666; font-size: 16px; line-height: 1.6;">Recebemos uma solicitação para redefinir a senha da sua conta no <strong>PitStop Pro</strong>.</p>
                                            <p style="margin: 0 0 30px 0; color: #666666; font-size: 16px; line-height: 1.6;">Para criar uma nova senha, clique no botão abaixo:</p>
                                            <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                                                <tr>
                                                    <td align="center" style="padding: 0;">
                                                        <a href="%s" style="display: inline-block; padding: 16px 40px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 6px rgba(102, 126, 234, 0.4);">Redefinir Senha</a>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin: 30px 0 20px 0; color: #666666; font-size: 14px; line-height: 1.6;">Ou copie e cole o link abaixo no seu navegador:</p>
                                            <p style="margin: 0 0 30px 0; padding: 12px; background-color: #f8f9fa; border-left: 4px solid #667eea; color: #333333; font-size: 12px; word-break: break-all; font-family: monospace;">%s</p>
                                            <div style="margin: 30px 0; padding: 16px; background-color: #fff3cd; border-left: 4px solid #ffc107; border-radius: 4px;">
                                                <p style="margin: 0; color: #856404; font-size: 14px; line-height: 1.6;">⚠️ <strong>Importante:</strong> Este link expira em <strong>15 minutos</strong>. Se você não solicitou esta alteração, ignore este email.</p>
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 30px 40px; background-color: #f8f9fa; border-radius: 0 0 8px 8px; text-align: center;">
                                            <p style="margin: 0 0 10px 0; color: #999999; font-size: 12px;">Este é um email automático. Por favor, não responda.</p>
                                            <p style="margin: 0; color: #999999; font-size: 12px;">© 2025 PitStop Pro. Todos os direitos reservados.</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(userName, resetLink, resetLink);
    }
}
