package com.pitstop.saas.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.pitstop.cliente.domain.Endereco;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.saas.domain.Fatura;
import com.pitstop.saas.domain.ItemFatura;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Service for generating PDF invoices for SaaS billing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaturaPDFService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // Cores do tema PitStop
    private static final Color PRIMARY_COLOR = new Color(37, 99, 235);
    private static final Color SECONDARY_COLOR = new Color(100, 116, 139);
    private static final Color LIGHT_BG = new Color(243, 244, 246);
    private static final Color SUCCESS_COLOR = new Color(34, 197, 94);
    private static final Color WARNING_COLOR = new Color(234, 179, 8);
    private static final Color DANGER_COLOR = new Color(239, 68, 68);

    /**
     * Generate PDF for an invoice.
     */
    public byte[] gerarPDF(Fatura fatura) {
        log.info("Gerando PDF para fatura: {}", fatura.getNumero());

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 54, 54);
            PdfWriter.getInstance(document, baos);
            document.open();

            adicionarCabecalho(document, fatura);
            adicionarDadosEmissor(document);
            adicionarDadosOficina(document, fatura.getOficina());
            adicionarDetalhesFatura(document, fatura);
            adicionarTabelaItens(document, fatura);
            adicionarTotais(document, fatura);
            adicionarInformacoesPagamento(document, fatura);
            adicionarRodape(document);

            document.close();
            log.info("PDF gerado com sucesso para fatura {}", fatura.getNumero());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar PDF para fatura {}", fatura.getNumero(), e);
            throw new RuntimeException("Erro ao gerar PDF da fatura", e);
        }
    }

    private void adicionarCabecalho(Document document, Fatura fatura) throws DocumentException {
        // Logo/Nome PitStop
        Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, PRIMARY_COLOR);
        Paragraph title = new Paragraph("🚗 PitStop", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        document.add(title);

        // Subtítulo
        Font subtitleFont = new Font(Font.HELVETICA, 12, Font.NORMAL, SECONDARY_COLOR);
        Paragraph subtitle = new Paragraph("Sistema de Gestão para Oficinas Mecânicas", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(15);
        document.add(subtitle);

        // Número da Fatura
        Font faturaFont = new Font(Font.HELVETICA, 16, Font.BOLD);
        Paragraph faturaNum = new Paragraph("FATURA " + fatura.getNumero(), faturaFont);
        faturaNum.setAlignment(Element.ALIGN_CENTER);
        faturaNum.setSpacingAfter(5);
        document.add(faturaNum);

        // Status com cor
        Color statusColor = getCorStatus(fatura);
        Font statusFont = new Font(Font.HELVETICA, 11, Font.BOLD, statusColor);
        Paragraph status = new Paragraph("Status: " + fatura.getStatus().name(), statusFont);
        status.setAlignment(Element.ALIGN_CENTER);
        status.setSpacingAfter(20);
        document.add(status);
    }

    private Color getCorStatus(Fatura fatura) {
        return switch (fatura.getStatus()) {
            case PAGO -> SUCCESS_COLOR;
            case PENDENTE -> WARNING_COLOR;
            case VENCIDO -> DANGER_COLOR;
            case CANCELADO -> SECONDARY_COLOR;
            default -> SECONDARY_COLOR;
        };
    }

    private void adicionarDadosEmissor(Document document) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);

        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        PdfPCell headerCell = new PdfPCell(new Phrase("DADOS DO EMISSOR", headerFont));
        headerCell.setBackgroundColor(PRIMARY_COLOR);
        headerCell.setPadding(8);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(headerCell);

        StringBuilder dados = new StringBuilder();
        dados.append("PitStop - Sistema de Gestão para Oficinas Mecânicas\n");
        dados.append("CNPJ: 00.000.000/0001-00\n"); // Placeholder
        dados.append("Email: financeiro@pitstopai.com.br\n");
        dados.append("Site: www.pitstopai.com.br");

        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        PdfPCell dataCell = new PdfPCell(new Phrase(dados.toString(), dataFont));
        dataCell.setPadding(8);
        table.addCell(dataCell);

        document.add(table);
    }

    private void adicionarDadosOficina(Document document, Oficina oficina) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);

        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        PdfPCell headerCell = new PdfPCell(new Phrase("DADOS DO CLIENTE (OFICINA)", headerFont));
        headerCell.setBackgroundColor(PRIMARY_COLOR);
        headerCell.setPadding(8);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(headerCell);

        StringBuilder dados = new StringBuilder();
        String nomeOficina = oficina.getNomeFantasia() != null ? oficina.getNomeFantasia() : oficina.getRazaoSocial();
        dados.append(nomeOficina).append("\n");
        dados.append("CNPJ/CPF: ").append(oficina.getCnpjCpf());

        if (oficina.getEndereco() != null) {
            dados.append("\n").append(formatarEndereco(oficina.getEndereco()));
        }

        if (oficina.getContato() != null) {
            if (oficina.getContato().getEmail() != null) {
                dados.append("\nEmail: ").append(oficina.getContato().getEmail());
            }
            if (oficina.getContato().getTelefoneCelular() != null) {
                dados.append("\nTelefone: ").append(oficina.getContato().getTelefoneCelular());
            }
        }

        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        PdfPCell dataCell = new PdfPCell(new Phrase(dados.toString(), dataFont));
        dataCell.setPadding(8);
        table.addCell(dataCell);

        document.add(table);
    }

    private String formatarEndereco(Endereco endereco) {
        StringBuilder sb = new StringBuilder();
        if (endereco.getLogradouro() != null) {
            sb.append(endereco.getLogradouro());
            if (endereco.getNumero() != null) {
                sb.append(", ").append(endereco.getNumero());
            }
            if (endereco.getComplemento() != null && !endereco.getComplemento().isBlank()) {
                sb.append(" - ").append(endereco.getComplemento());
            }
        }
        if (endereco.getBairro() != null) {
            sb.append(" - ").append(endereco.getBairro());
        }
        if (endereco.getCidade() != null) {
            sb.append(" - ").append(endereco.getCidade());
            if (endereco.getEstado() != null) {
                sb.append("/").append(endereco.getEstado());
            }
        }
        if (endereco.getCep() != null) {
            sb.append(" - CEP: ").append(endereco.getCep());
        }
        return sb.toString();
    }

    private void adicionarDetalhesFatura(Document document, Fatura fatura) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);
        table.setWidths(new int[]{1, 1});

        Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD);
        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

        // Mês de referência
        PdfPCell c1 = new PdfPCell();
        c1.addElement(new Phrase("Mês de Referência:", labelFont));
        c1.addElement(new Phrase(fatura.getMesReferenciaFormatado(), dataFont));
        c1.setPadding(8);
        c1.setBackgroundColor(LIGHT_BG);
        table.addCell(c1);

        // Plano
        PdfPCell c2 = new PdfPCell();
        c2.addElement(new Phrase("Plano:", labelFont));
        c2.addElement(new Phrase(fatura.getPlanoCodigo() != null ? fatura.getPlanoCodigo() : "-", dataFont));
        c2.setPadding(8);
        c2.setBackgroundColor(LIGHT_BG);
        table.addCell(c2);

        // Data de Emissão
        PdfPCell c3 = new PdfPCell();
        c3.addElement(new Phrase("Data de Emissão:", labelFont));
        c3.addElement(new Phrase(fatura.getDataEmissao().format(DATE_FORMATTER), dataFont));
        c3.setPadding(8);
        table.addCell(c3);

        // Data de Vencimento
        PdfPCell c4 = new PdfPCell();
        c4.addElement(new Phrase("Data de Vencimento:", labelFont));
        String vencimento = fatura.getDataVencimento().format(DATE_FORMATTER);
        if (fatura.isVencida()) {
            vencimento += " (VENCIDA)";
        }
        Font vencFont = fatura.isVencida() ? new Font(Font.HELVETICA, 9, Font.BOLD, DANGER_COLOR) : dataFont;
        c4.addElement(new Phrase(vencimento, vencFont));
        c4.setPadding(8);
        table.addCell(c4);

        // Data de Pagamento (se paga)
        if (fatura.isPaga() && fatura.getDataPagamento() != null) {
            PdfPCell c5 = new PdfPCell();
            c5.addElement(new Phrase("Data de Pagamento:", labelFont));
            c5.addElement(new Phrase(fatura.getDataPagamento().format(DATETIME_FORMATTER), dataFont));
            c5.setPadding(8);
            c5.setBackgroundColor(new Color(220, 252, 231)); // verde claro
            table.addCell(c5);

            PdfPCell c6 = new PdfPCell();
            c6.addElement(new Phrase("Método de Pagamento:", labelFont));
            c6.addElement(new Phrase(fatura.getMetodoPagamento() != null ? fatura.getMetodoPagamento() : "-", dataFont));
            c6.setPadding(8);
            c6.setBackgroundColor(new Color(220, 252, 231));
            table.addCell(c6);
        }

        document.add(table);
    }

    private void adicionarTabelaItens(Document document, Fatura fatura) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setWidths(new int[]{4, 1, 2, 2});

        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);

        String[] headers = {"Descrição", "Qtd", "Valor Unit.", "Total"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

        if (fatura.getItens() != null && !fatura.getItens().isEmpty()) {
            for (ItemFatura item : fatura.getItens()) {
                table.addCell(new PdfPCell(new Phrase(item.getDescricao(), dataFont)));

                PdfPCell qtdCell = new PdfPCell(new Phrase(item.getQuantidade().toString(), dataFont));
                qtdCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(qtdCell);

                PdfPCell valorCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(item.getValorUnitario()), dataFont));
                valorCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(valorCell);

                PdfPCell totalCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(item.getValorTotal()), dataFont));
                totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(totalCell);
            }
        } else {
            // Item padrão se não houver itens detalhados
            table.addCell(new PdfPCell(new Phrase("Mensalidade " + fatura.getMesReferenciaFormatado(), dataFont)));

            PdfPCell qtdCell = new PdfPCell(new Phrase("1", dataFont));
            qtdCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(qtdCell);

            PdfPCell valorCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(fatura.getValorBase()), dataFont));
            valorCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(valorCell);

            PdfPCell totalCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(fatura.getValorBase()), dataFont));
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalCell);
        }

        document.add(table);
    }

    private void adicionarTotais(Document document, Fatura fatura) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingAfter(15);

        Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD);
        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        Font totalFont = new Font(Font.HELVETICA, 11, Font.BOLD);

        // Subtotal
        table.addCell(new PdfPCell(new Phrase("Subtotal:", labelFont)));
        PdfPCell subtotalCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(fatura.getValorBase()), dataFont));
        subtotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(subtotalCell);

        // Desconto (se houver)
        if (fatura.getValorDesconto() != null && fatura.getValorDesconto().compareTo(java.math.BigDecimal.ZERO) > 0) {
            table.addCell(new PdfPCell(new Phrase("Desconto:", labelFont)));
            Font descontoFont = new Font(Font.HELVETICA, 9, Font.NORMAL, SUCCESS_COLOR);
            PdfPCell descontoCell = new PdfPCell(new Phrase("-" + CURRENCY_FORMATTER.format(fatura.getValorDesconto()), descontoFont));
            descontoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(descontoCell);
        }

        // Acréscimos (se houver)
        if (fatura.getValorAcrescimos() != null && fatura.getValorAcrescimos().compareTo(java.math.BigDecimal.ZERO) > 0) {
            table.addCell(new PdfPCell(new Phrase("Acréscimos/Multa:", labelFont)));
            Font acrescimoFont = new Font(Font.HELVETICA, 9, Font.NORMAL, DANGER_COLOR);
            PdfPCell acrescimoCell = new PdfPCell(new Phrase("+" + CURRENCY_FORMATTER.format(fatura.getValorAcrescimos()), acrescimoFont));
            acrescimoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(acrescimoCell);
        }

        // Total
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("VALOR TOTAL:", totalFont));
        totalLabelCell.setBackgroundColor(LIGHT_BG);
        totalLabelCell.setPadding(8);
        table.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(fatura.getValorTotal()), totalFont));
        totalValueCell.setBackgroundColor(LIGHT_BG);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setPadding(8);
        table.addCell(totalValueCell);

        document.add(table);
    }

    private void adicionarInformacoesPagamento(Document document, Fatura fatura) throws DocumentException {
        if (fatura.isPaga()) {
            // Fatura já paga
            PdfPTable table = new PdfPTable(1);
            table.setWidthPercentage(100);
            table.setSpacingAfter(15);

            Font paidFont = new Font(Font.HELVETICA, 12, Font.BOLD, SUCCESS_COLOR);
            PdfPCell cell = new PdfPCell(new Phrase("✓ FATURA PAGA", paidFont));
            cell.setBackgroundColor(new Color(220, 252, 231));
            cell.setPadding(15);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);

            document.add(table);
        } else if (!fatura.isPagavel()) {
            // Fatura cancelada
            PdfPTable table = new PdfPTable(1);
            table.setWidthPercentage(100);
            table.setSpacingAfter(15);

            Font cancelFont = new Font(Font.HELVETICA, 12, Font.BOLD, SECONDARY_COLOR);
            PdfPCell cell = new PdfPCell(new Phrase("FATURA CANCELADA", cancelFont));
            cell.setBackgroundColor(LIGHT_BG);
            cell.setPadding(15);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);

            document.add(table);
        } else {
            // Instruções de pagamento
            PdfPTable table = new PdfPTable(1);
            table.setWidthPercentage(100);
            table.setSpacingAfter(15);

            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            PdfPCell headerCell = new PdfPCell(new Phrase("INSTRUÇÕES DE PAGAMENTO", headerFont));
            headerCell.setBackgroundColor(PRIMARY_COLOR);
            headerCell.setPadding(8);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(headerCell);

            StringBuilder instrucoes = new StringBuilder();
            instrucoes.append("Para pagar esta fatura, acesse o sistema PitStop e clique em 'Pagar Agora'.\n\n");
            instrucoes.append("Formas de pagamento disponíveis:\n");
            instrucoes.append("• PIX (pagamento instantâneo)\n");
            instrucoes.append("• Cartão de Crédito\n");
            instrucoes.append("• Boleto Bancário\n\n");
            instrucoes.append("Em caso de dúvidas, entre em contato:\n");
            instrucoes.append("Email: financeiro@pitstopai.com.br");

            Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
            PdfPCell dataCell = new PdfPCell(new Phrase(instrucoes.toString(), dataFont));
            dataCell.setPadding(10);
            table.addCell(dataCell);

            document.add(table);
        }

        // Observações (se houver)
        if (fatura.getObservacao() != null && !fatura.getObservacao().isBlank()) {
            Font obsLabelFont = new Font(Font.HELVETICA, 9, Font.BOLD);
            Font obsFont = new Font(Font.HELVETICA, 9, Font.ITALIC, SECONDARY_COLOR);

            Paragraph obs = new Paragraph();
            obs.add(new Phrase("Observações: ", obsLabelFont));
            obs.add(new Phrase(fatura.getObservacao(), obsFont));
            obs.setSpacingAfter(15);
            document.add(obs);
        }
    }

    private void adicionarRodape(Document document) throws DocumentException {
        document.add(new Paragraph(" "));

        // Linha separadora
        PdfPTable separador = new PdfPTable(1);
        separador.setWidthPercentage(100);
        PdfPCell linhaCell = new PdfPCell();
        linhaCell.setBorder(Rectangle.TOP);
        linhaCell.setBorderColor(SECONDARY_COLOR);
        linhaCell.setFixedHeight(1);
        separador.addCell(linhaCell);
        document.add(separador);

        // Informações do rodapé
        Font footerFont = new Font(Font.HELVETICA, 8, Font.NORMAL, SECONDARY_COLOR);
        Paragraph footer = new Paragraph();
        footer.add(new Phrase("PitStop - Sistema de Gestão para Oficinas Mecânicas\n", footerFont));
        footer.add(new Phrase("www.pitstopai.com.br | financeiro@pitstopai.com.br\n", footerFont));
        footer.add(new Phrase("Este documento é uma representação eletrônica da fatura.", footerFont));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);
        document.add(footer);
    }
}
