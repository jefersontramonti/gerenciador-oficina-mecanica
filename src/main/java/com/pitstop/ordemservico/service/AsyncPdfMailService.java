package com.pitstop.ordemservico.service;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.notificacao.service.EmailService;
import com.pitstop.veiculo.domain.Veiculo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Serviço assíncrono para envio de PDF por email.
 * Executa em thread separada para não bloquear a resposta da API.
 *
 * @author PitStop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncPdfMailService {

    private final OrdemServicoPDFService pdfService;
    private final EmailService emailService;

    /**
     * Gera e envia PDF da OS finalizada para o email do cliente de forma assíncrona.
     * Este método retorna imediatamente e o processamento é feito em background.
     *
     * @param osId ID da ordem de serviço
     * @param osNumero Número da OS
     * @param cliente Cliente da OS
     * @param veiculo Veículo da OS
     * @param valorFinal Valor final da OS
     * @param nomeOficina Nome da oficina
     */
    @Async
    public void enviarPdfFinalizacaoAsync(
            UUID osId,
            Long osNumero,
            Cliente cliente,
            Veiculo veiculo,
            java.math.BigDecimal valorFinal,
            String nomeOficina
    ) {
        if (cliente.getEmail() == null || cliente.getEmail().isBlank()) {
            log.debug("Cliente {} não possui email. PDF não será enviado.", cliente.getNome());
            return;
        }

        log.info("Iniciando envio assíncrono de PDF da OS #{} para {}", osNumero, cliente.getEmail());

        try {
            // Gera PDF da OS
            byte[] pdfBytes = pdfService.gerarPDF(osId);

            // Monta assunto e corpo do email
            String assunto = String.format("OS #%d Finalizada - %s", osNumero, nomeOficina);

            String corpo = String.format("""
                <h2>Serviço Concluído!</h2>

                <p>Prezado(a) <strong>%s</strong>,</p>

                <p>Informamos que o serviço do seu veículo <strong>%s %s</strong> (placa %s) foi concluído com sucesso.</p>

                <div style="background-color: #f0f9ff; padding: 15px; border-radius: 8px; margin: 20px 0;">
                    <p><strong>Resumo:</strong></p>
                    <p>OS: #%d</p>
                    <p>Veículo: %s %s - %s</p>
                    <p>Valor Final: <strong>R$ %s</strong></p>
                </div>

                <p>Em anexo você encontra a <strong>Ordem de Serviço</strong> completa com todos os detalhes do serviço realizado.</p>

                <p>Por favor, entre em contato conosco para agendar a retirada do veículo e efetuar o pagamento.</p>

                <p>Agradecemos a preferência!</p>

                <p>Atenciosamente,<br/>
                <strong>%s</strong></p>
                """,
                cliente.getNome(),
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getPlacaFormatada(),
                osNumero,
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getPlacaFormatada(),
                NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(valorFinal),
                nomeOficina
            );

            // Nome do arquivo PDF
            String nomePdf = String.format("OS_%d_%s.pdf",
                osNumero,
                veiculo.getPlacaFormatada().replace("-", "").replace(" ", "")
            );

            // Envia email com PDF anexo
            emailService.enviarComPdf(
                cliente.getEmail(),
                assunto,
                corpo,
                pdfBytes,
                nomePdf
            );

            log.info("PDF da OS #{} enviado por email para {} com sucesso", osNumero, cliente.getEmail());

        } catch (Exception e) {
            // Log do erro mas não interrompe o fluxo (email é secundário)
            log.warn("Falha ao enviar PDF da OS #{} por email para {}: {}",
                osNumero, cliente.getEmail(), e.getMessage());
        }
    }
}
