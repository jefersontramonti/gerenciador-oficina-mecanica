package com.pitstop.ordemservico.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.ordemservico.domain.ItemOS;
import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.ordemservico.exception.OrdemServicoNotFoundException;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.repository.UsuarioRepository;
import com.pitstop.veiculo.domain.Veiculo;
import com.pitstop.veiculo.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdemServicoPDFService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${pitstop.oficina.nome}")
    private String oficinaNome;

    @Value("${pitstop.oficina.cnpj}")
    private String oficinaCnpj;

    @Value("${pitstop.oficina.endereco}")
    private String oficinaEndereco;

    @Value("${pitstop.oficina.telefone}")
    private String oficinaTelefone;

    @Value("${pitstop.oficina.email}")
    private String oficinaEmail;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Transactional(readOnly = true)
    public byte[] gerarPDF(UUID osId) {
        log.info("Gerando PDF para OS ID: {}", osId);

        UUID oficinaId = TenantContext.getTenantId();

        OrdemServico os = ordemServicoRepository.findById(osId)
                .orElseThrow(() -> new OrdemServicoNotFoundException(osId));

        Veiculo veiculo = veiculoRepository.findById(os.getVeiculoId())
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));

        // Busca cliente incluindo inativos para permitir gerar PDF de OS antigas
        Cliente cliente = clienteRepository.findByOficinaIdAndIdIncludingInactive(oficinaId, veiculo.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Usuario mecanico = os.getUsuarioId() != null
                ? usuarioRepository.findById(os.getUsuarioId()).orElse(null)
                : null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 54, 54);
            PdfWriter.getInstance(document, baos);
            document.open();

            adicionarCabecalho(document, os);
            adicionarDadosOficina(document);
            adicionarDadosCliente(document, cliente);
            adicionarDadosVeiculo(document, veiculo);
            adicionarTabelaItens(document, os);
            adicionarTotais(document, os);
            adicionarInformacoesAdicionais(document, os, mecanico);
            adicionarAssinatura(document);

            document.close();
            log.info("PDF gerado com sucesso para OS {}", osId);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar PDF para OS {}", osId, e);
            throw new RuntimeException("Erro ao gerar PDF da Ordem de Serviço", e);
        }
    }

    private void adicionarCabecalho(Document document, OrdemServico os) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(37, 99, 235));
        Paragraph title = new Paragraph(oficinaNome, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        Font subtitleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Paragraph subtitle = new Paragraph("ORDEM DE SERVIÇO Nº " + os.getNumero(), subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(5);
        document.add(subtitle);

        Font statusFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
        Paragraph status = new Paragraph("Status: " + os.getStatus().name(), statusFont);
        status.setAlignment(Element.ALIGN_CENTER);
        status.setSpacingAfter(20);
        document.add(status);
    }

    private void adicionarDadosOficina(Document document) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);

        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        PdfPCell headerCell = new PdfPCell(new Phrase("DADOS DA OFICINA", headerFont));
        headerCell.setBackgroundColor(new Color(37, 99, 235));
        headerCell.setPadding(8);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(headerCell);

        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        String dados = String.format("%s\nCNPJ: %s\n%s\nTel: %s | Email: %s",
                oficinaNome, oficinaCnpj, oficinaEndereco, oficinaTelefone, oficinaEmail);
        PdfPCell dataCell = new PdfPCell(new Phrase(dados, dataFont));
        dataCell.setPadding(8);
        table.addCell(dataCell);

        document.add(table);
    }

    private void adicionarDadosCliente(Document document, Cliente cliente) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setWidths(new int[]{1, 2});

        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        PdfPCell headerCell = new PdfPCell(new Phrase("DADOS DO CLIENTE", headerFont));
        headerCell.setBackgroundColor(new Color(37, 99, 235));
        headerCell.setPadding(8);
        headerCell.setColspan(2);
        table.addCell(headerCell);

        Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD);
        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

        table.addCell(new PdfPCell(new Phrase("Nome:", labelFont)));
        table.addCell(new PdfPCell(new Phrase(cliente.getNome(), dataFont)));

        table.addCell(new PdfPCell(new Phrase("CPF/CNPJ:", labelFont)));
        table.addCell(new PdfPCell(new Phrase(cliente.getCpfCnpj(), dataFont)));

        table.addCell(new PdfPCell(new Phrase("Telefone:", labelFont)));
        table.addCell(new PdfPCell(new Phrase(cliente.getCelular() != null ? cliente.getCelular() : "-", dataFont)));

        table.addCell(new PdfPCell(new Phrase("Email:", labelFont)));
        table.addCell(new PdfPCell(new Phrase(cliente.getEmail() != null ? cliente.getEmail() : "-", dataFont)));

        document.add(table);
    }

    private void adicionarDadosVeiculo(Document document, Veiculo veiculo) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);
        table.setWidths(new int[]{1, 2});

        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        PdfPCell headerCell = new PdfPCell(new Phrase("DADOS DO VEÍCULO", headerFont));
        headerCell.setBackgroundColor(new Color(37, 99, 235));
        headerCell.setPadding(8);
        headerCell.setColspan(2);
        table.addCell(headerCell);

        Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD);
        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

        table.addCell(new PdfPCell(new Phrase("Placa:", labelFont)));
        table.addCell(new PdfPCell(new Phrase(veiculo.getPlaca(), dataFont)));

        table.addCell(new PdfPCell(new Phrase("Modelo:", labelFont)));
        table.addCell(new PdfPCell(new Phrase(veiculo.getModelo() + " - " + veiculo.getAno(), dataFont)));

        table.addCell(new PdfPCell(new Phrase("Marca/Cor:", labelFont)));
        table.addCell(new PdfPCell(new Phrase(veiculo.getMarca() + " / " + veiculo.getCor(), dataFont)));

        table.addCell(new PdfPCell(new Phrase("KM:", labelFont)));
        table.addCell(new PdfPCell(new Phrase(veiculo.getQuilometragem() != null ? veiculo.getQuilometragem().toString() : "-", dataFont)));

        document.add(table);
    }

    private void adicionarTabelaItens(Document document, OrdemServico os) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setWidths(new int[]{3, 1, 2, 2, 2});

        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        Color headerColor = new Color(37, 99, 235);

        String[] headers = {"Descrição", "Qtd", "Valor Unit.", "Desconto", "Total"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerColor);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        Font dataFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
        if (os.getItens() != null && !os.getItens().isEmpty()) {
            for (ItemOS item : os.getItens()) {
                table.addCell(new PdfPCell(new Phrase(item.getDescricao(), dataFont)));
                table.addCell(new PdfPCell(new Phrase(item.getQuantidade().toString(), dataFont)));
                table.addCell(new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(item.getValorUnitario()), dataFont)));
                table.addCell(new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(item.getDesconto()), dataFont)));
                table.addCell(new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(item.getValorTotal()), dataFont)));
            }
        } else {
            PdfPCell emptyCell = new PdfPCell(new Phrase("Nenhum item adicionado", dataFont));
            emptyCell.setColspan(5);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            emptyCell.setPadding(10);
            table.addCell(emptyCell);
        }

        document.add(table);
    }

    private void adicionarTotais(Document document, OrdemServico os) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingAfter(15);

        Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD);
        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        Font totalFont = new Font(Font.HELVETICA, 10, Font.BOLD);

        table.addCell(new PdfPCell(new Phrase("Mão de Obra:", labelFont)));
        table.addCell(new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(os.getValorMaoObra()), dataFont)));

        table.addCell(new PdfPCell(new Phrase("Peças:", labelFont)));
        table.addCell(new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(os.getValorPecas()), dataFont)));

        table.addCell(new PdfPCell(new Phrase("Subtotal:", labelFont)));
        table.addCell(new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(os.getValorTotal()), dataFont)));

        table.addCell(new PdfPCell(new Phrase("Desconto:", labelFont)));
        table.addCell(new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(os.getDescontoValor()), dataFont)));

        PdfPCell totalLabelCell = new PdfPCell(new Phrase("VALOR FINAL:", totalFont));
        totalLabelCell.setBackgroundColor(new Color(243, 244, 246));
        table.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(os.getValorFinal()), totalFont));
        totalValueCell.setBackgroundColor(new Color(243, 244, 246));
        table.addCell(totalValueCell);

        document.add(table);
    }

    private void adicionarInformacoesAdicionais(Document document, OrdemServico os, Usuario mecanico) throws DocumentException {
        Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD);
        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

        if (os.getDataAbertura() != null) {
            Paragraph abertura = new Paragraph();
            abertura.add(new Phrase("Data de Abertura: ", labelFont));
            abertura.add(new Phrase(os.getDataAbertura().format(DATETIME_FORMATTER), dataFont));
            abertura.setSpacingAfter(5);
            document.add(abertura);
        }

        if (os.getDataPrevisao() != null) {
            Paragraph previsao = new Paragraph();
            previsao.add(new Phrase("Previsão de Entrega: ", labelFont));
            previsao.add(new Phrase(os.getDataPrevisao().format(DATE_FORMATTER), dataFont));
            previsao.setSpacingAfter(5);
            document.add(previsao);
        }

        if (mecanico != null) {
            Paragraph mec = new Paragraph();
            mec.add(new Phrase("Mecânico Responsável: ", labelFont));
            mec.add(new Phrase(mecanico.getNome(), dataFont));
            mec.setSpacingAfter(5);
            document.add(mec);
        }

        if (os.getProblemasRelatados() != null && !os.getProblemasRelatados().isBlank()) {
            Paragraph problemas = new Paragraph();
            problemas.add(new Phrase("Problemas Relatados: ", labelFont));
            problemas.add(new Phrase(os.getProblemasRelatados(), dataFont));
            problemas.setSpacingAfter(5);
            document.add(problemas);
        }

        if (os.getDiagnostico() != null && !os.getDiagnostico().isBlank()) {
            Paragraph diagnostico = new Paragraph();
            diagnostico.add(new Phrase("Diagnóstico: ", labelFont));
            diagnostico.add(new Phrase(os.getDiagnostico(), dataFont));
            diagnostico.setSpacingAfter(5);
            document.add(diagnostico);
        }

        if (os.getObservacoes() != null && !os.getObservacoes().isBlank()) {
            Paragraph obs = new Paragraph();
            obs.add(new Phrase("Observações: ", labelFont));
            obs.add(new Phrase(os.getObservacoes(), dataFont));
            obs.setSpacingAfter(15);
            document.add(obs);
        }
    }

    private void adicionarAssinatura(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        Font labelFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

        PdfPCell clienteCell = new PdfPCell();
        clienteCell.setBorder(Rectangle.NO_BORDER);
        Paragraph clienteP = new Paragraph();
        clienteP.add(new Phrase("_________________________________\n", labelFont));
        clienteP.add(new Phrase("Assinatura do Cliente", labelFont));
        clienteP.setAlignment(Element.ALIGN_CENTER);
        clienteCell.addElement(clienteP);
        table.addCell(clienteCell);

        PdfPCell oficinaCell = new PdfPCell();
        oficinaCell.setBorder(Rectangle.NO_BORDER);
        Paragraph oficinaP = new Paragraph();
        oficinaP.add(new Phrase("_________________________________\n", labelFont));
        oficinaP.add(new Phrase("Responsável pela Oficina", labelFont));
        oficinaP.setAlignment(Element.ALIGN_CENTER);
        oficinaCell.addElement(oficinaP);
        table.addCell(oficinaCell);

        document.add(table);
    }
}
