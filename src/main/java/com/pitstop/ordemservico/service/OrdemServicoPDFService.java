package com.pitstop.ordemservico.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.domain.Endereco;
import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.repository.OficinaRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
    private final OficinaRepository oficinaRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // Cores do tema
    private static final Color PRIMARY_COLOR = new Color(37, 99, 235);
    private static final Color SECONDARY_COLOR = new Color(100, 116, 139);
    private static final Color LIGHT_BG = new Color(243, 244, 246);

    @Transactional(readOnly = true)
    public byte[] gerarPDF(UUID osId) {
        log.info("Gerando PDF para OS ID: {}", osId);

        UUID oficinaId = TenantContext.getTenantId();

        // Busca dados da oficina
        Oficina oficina = oficinaRepository.findById(oficinaId)
                .orElseThrow(() -> new RuntimeException("Oficina não encontrada"));

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

            adicionarCabecalho(document, os, oficina);
            adicionarDadosOficina(document, oficina);
            adicionarDadosCliente(document, cliente);
            adicionarDadosVeiculo(document, veiculo);
            adicionarTabelaItens(document, os);
            adicionarTotais(document, os);
            adicionarInformacoesAdicionais(document, os, mecanico);
            adicionarRodape(document, oficina);

            document.close();
            log.info("PDF gerado com sucesso para OS {}", osId);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar PDF para OS {}", osId, e);
            throw new RuntimeException("Erro ao gerar PDF da Ordem de Serviço", e);
        }
    }

    private void adicionarCabecalho(Document document, OrdemServico os, Oficina oficina) throws DocumentException {
        // Nome da oficina
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, PRIMARY_COLOR);
        String nomeOficina = oficina.getNomeFantasia() != null ? oficina.getNomeFantasia() : oficina.getRazaoSocial();
        Paragraph title = new Paragraph(nomeOficina, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        // Número da OS
        Font subtitleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Paragraph subtitle = new Paragraph("ORDEM DE SERVIÇO Nº " + os.getNumero(), subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(5);
        document.add(subtitle);

        // Status
        Font statusFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
        Paragraph status = new Paragraph("Status: " + os.getStatus().name(), statusFont);
        status.setAlignment(Element.ALIGN_CENTER);
        status.setSpacingAfter(20);
        document.add(status);
    }

    private void adicionarDadosOficina(Document document, Oficina oficina) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(15);

        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        PdfPCell headerCell = new PdfPCell(new Phrase("DADOS DA OFICINA", headerFont));
        headerCell.setBackgroundColor(PRIMARY_COLOR);
        headerCell.setPadding(8);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(headerCell);

        // Monta dados da oficina
        StringBuilder dados = new StringBuilder();
        String nomeOficina = oficina.getNomeFantasia() != null ? oficina.getNomeFantasia() : oficina.getRazaoSocial();
        dados.append(nomeOficina);
        dados.append("\nCNPJ/CPF: ").append(oficina.getCnpjCpf());

        // Endereço
        if (oficina.getEndereco() != null) {
            Endereco end = oficina.getEndereco();
            dados.append("\n").append(formatarEndereco(end));
        }

        // Contato
        if (oficina.getContato() != null) {
            List<String> contatos = new ArrayList<>();
            if (oficina.getContato().getTelefoneCelular() != null) {
                contatos.add("Cel: " + oficina.getContato().getTelefoneCelular());
            }
            if (oficina.getContato().getTelefoneFixo() != null) {
                contatos.add("Tel: " + oficina.getContato().getTelefoneFixo());
            }
            if (oficina.getContato().getEmail() != null) {
                contatos.add("Email: " + oficina.getContato().getEmail());
            }
            if (!contatos.isEmpty()) {
                dados.append("\n").append(String.join(" | ", contatos));
            }
        }

        // Horário de funcionamento
        if (oficina.getInformacoesOperacionais() != null &&
            oficina.getInformacoesOperacionais().getHorarioFuncionamento() != null) {
            dados.append("\nHorário: ").append(oficina.getInformacoesOperacionais().getHorarioFuncionamento());
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

    private void adicionarRodape(Document document, Oficina oficina) throws DocumentException {
        // Espaço antes das assinaturas
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        // Assinaturas
        PdfPTable assinaturaTable = new PdfPTable(2);
        assinaturaTable.setWidthPercentage(100);
        assinaturaTable.setSpacingAfter(20);

        Font labelFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

        PdfPCell clienteCell = new PdfPCell();
        clienteCell.setBorder(Rectangle.NO_BORDER);
        Paragraph clienteP = new Paragraph();
        clienteP.add(new Phrase("_________________________________\n", labelFont));
        clienteP.add(new Phrase("Assinatura do Cliente", labelFont));
        clienteP.setAlignment(Element.ALIGN_CENTER);
        clienteCell.addElement(clienteP);
        assinaturaTable.addCell(clienteCell);

        PdfPCell oficinaCell = new PdfPCell();
        oficinaCell.setBorder(Rectangle.NO_BORDER);
        Paragraph oficinaP = new Paragraph();
        oficinaP.add(new Phrase("_________________________________\n", labelFont));
        oficinaP.add(new Phrase("Responsável pela Oficina", labelFont));
        oficinaP.setAlignment(Element.ALIGN_CENTER);
        oficinaCell.addElement(oficinaP);
        assinaturaTable.addCell(oficinaCell);

        document.add(assinaturaTable);

        // Redes sociais no rodapé
        List<String> redesSociais = new ArrayList<>();

        if (oficina.getContato() != null && oficina.getContato().getTelefoneCelular() != null) {
            String whatsapp = oficina.getContato().getWhatsAppSomenteNumeros();
            if (whatsapp != null) {
                redesSociais.add("WhatsApp: " + oficina.getContato().getTelefoneCelular());
            }
        }

        if (oficina.getRedesSociais() != null) {
            if (oficina.getRedesSociais().getInstagram() != null && !oficina.getRedesSociais().getInstagram().isBlank()) {
                String instagram = oficina.getRedesSociais().getInstagram();
                if (!instagram.startsWith("@")) {
                    instagram = "@" + instagram;
                }
                redesSociais.add("Instagram: " + instagram);
            }
            if (oficina.getRedesSociais().getFacebook() != null && !oficina.getRedesSociais().getFacebook().isBlank()) {
                redesSociais.add("Facebook: " + oficina.getRedesSociais().getFacebook());
            }
        }

        if (oficina.getContato() != null && oficina.getContato().getWebsite() != null && !oficina.getContato().getWebsite().isBlank()) {
            redesSociais.add("Site: " + oficina.getContato().getWebsite());
        }

        if (!redesSociais.isEmpty()) {
            // Linha separadora
            PdfPTable separador = new PdfPTable(1);
            separador.setWidthPercentage(100);
            PdfPCell linhaCell = new PdfPCell();
            linhaCell.setBorder(Rectangle.TOP);
            linhaCell.setBorderColor(SECONDARY_COLOR);
            linhaCell.setFixedHeight(1);
            separador.addCell(linhaCell);
            document.add(separador);

            // Redes sociais
            Font redesFont = new Font(Font.HELVETICA, 8, Font.NORMAL, SECONDARY_COLOR);
            Paragraph redesP = new Paragraph(String.join("  |  ", redesSociais), redesFont);
            redesP.setAlignment(Element.ALIGN_CENTER);
            redesP.setSpacingBefore(8);
            document.add(redesP);
        }

        // Mensagem de agradecimento
        Font msgFont = new Font(Font.HELVETICA, 8, Font.ITALIC, SECONDARY_COLOR);
        Paragraph agradecimento = new Paragraph("Obrigado pela preferência!", msgFont);
        agradecimento.setAlignment(Element.ALIGN_CENTER);
        agradecimento.setSpacingBefore(10);
        document.add(agradecimento);
    }
}
