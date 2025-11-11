package com.pitstop.shared.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

/**
 * Service responsible for sending emails.
 *
 * <p>Uses Spring Boot's JavaMailSender with SMTP configuration.
 * Sends HTML emails with UTF-8 encoding.
 *
 * <p><b>Email types:</b>
 * <ul>
 *   <li>Password reset emails</li>
 *   <li>Welcome emails (future)</li>
 *   <li>Service order notifications (future)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:redefinirsenha@pitstoppro360.com}")
    private String fromEmail;

    @Value("${spring.mail.from-name:PitStop Pro}")
    private String fromName;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Sends a password reset email with a link containing the reset token.
     *
     * @param toEmail recipient email address
     * @param toName recipient name
     * @param resetToken password reset token (UUID)
     * @throws MessagingException if email sending fails
     * @throws UnsupportedEncodingException if encoding is not supported
     */
    public void sendPasswordResetEmail(String toEmail, String toName, String resetToken) throws MessagingException, UnsupportedEncodingException {
        log.debug("Sending password reset email to: {}", toEmail);

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String subject = "Redefinição de Senha - PitStop Pro";

        String htmlContent = buildPasswordResetEmailHtml(toName, resetLink);

        sendHtmlEmail(toEmail, subject, htmlContent);

        log.info("Password reset email sent successfully to: {}", toEmail);
    }

    /**
     * Sends an HTML email.
     *
     * @param to recipient email
     * @param subject email subject
     * @param htmlContent HTML content
     * @throws MessagingException if email sending fails
     * @throws UnsupportedEncodingException if encoding is not supported
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML

        mailSender.send(message);
    }

    /**
     * Builds the HTML content for password reset email.
     *
     * @param userName user's name
     * @param resetLink password reset link
     * @return HTML content
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
                                    <!-- Header -->
                                    <tr>
                                        <td style="padding: 40px 40px 20px 40px; text-align: center; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 8px 8px 0 0;">
                                            <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 600;">
                                                🔧 PitStop Pro
                                            </h1>
                                        </td>
                                    </tr>

                                    <!-- Body -->
                                    <tr>
                                        <td style="padding: 40px;">
                                            <h2 style="margin: 0 0 20px 0; color: #333333; font-size: 24px; font-weight: 600;">
                                                Redefinição de Senha
                                            </h2>

                                            <p style="margin: 0 0 20px 0; color: #666666; font-size: 16px; line-height: 1.6;">
                                                Olá <strong>%s</strong>,
                                            </p>

                                            <p style="margin: 0 0 20px 0; color: #666666; font-size: 16px; line-height: 1.6;">
                                                Recebemos uma solicitação para redefinir a senha da sua conta no <strong>PitStop Pro</strong>.
                                            </p>

                                            <p style="margin: 0 0 30px 0; color: #666666; font-size: 16px; line-height: 1.6;">
                                                Para criar uma nova senha, clique no botão abaixo:
                                            </p>

                                            <!-- Button -->
                                            <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                                                <tr>
                                                    <td align="center" style="padding: 0;">
                                                        <a href="%s"
                                                           style="display: inline-block; padding: 16px 40px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 6px rgba(102, 126, 234, 0.4);">
                                                            Redefinir Senha
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="margin: 30px 0 20px 0; color: #666666; font-size: 14px; line-height: 1.6;">
                                                Ou copie e cole o link abaixo no seu navegador:
                                            </p>

                                            <p style="margin: 0 0 30px 0; padding: 12px; background-color: #f8f9fa; border-left: 4px solid #667eea; color: #333333; font-size: 12px; word-break: break-all; font-family: monospace;">
                                                %s
                                            </p>

                                            <div style="margin: 30px 0; padding: 16px; background-color: #fff3cd; border-left: 4px solid #ffc107; border-radius: 4px;">
                                                <p style="margin: 0; color: #856404; font-size: 14px; line-height: 1.6;">
                                                    ⚠️ <strong>Importante:</strong> Este link expira em <strong>15 minutos</strong>. Se você não solicitou esta alteração, ignore este email.
                                                </p>
                                            </div>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding: 30px 40px; background-color: #f8f9fa; border-radius: 0 0 8px 8px; text-align: center;">
                                            <p style="margin: 0 0 10px 0; color: #999999; font-size: 12px;">
                                                Este é um email automático. Por favor, não responda.
                                            </p>
                                            <p style="margin: 0; color: #999999; font-size: 12px;">
                                                © 2025 PitStop Pro. Todos os direitos reservados.
                                            </p>
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
