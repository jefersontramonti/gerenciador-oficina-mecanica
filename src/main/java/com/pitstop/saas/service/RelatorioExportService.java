package com.pitstop.saas.service;

import com.pitstop.saas.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Serviço para exportação de relatórios em diferentes formatos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelatorioExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ========== RELATÓRIO FINANCEIRO ==========

    public byte[] exportarRelatorioFinanceiro(RelatorioFinanceiroDTO relatorio, RelatorioRequest.FormatoExport formato) {
        return switch (formato) {
            case PDF -> exportarFinanceiroPDF(relatorio);
            case EXCEL -> exportarFinanceiroExcel(relatorio);
            case CSV -> exportarFinanceiroCSV(relatorio);
            case JSON -> exportarFinanceiroJSON(relatorio);
        };
    }

    private byte[] exportarFinanceiroPDF(RelatorioFinanceiroDTO relatorio) {
        // Implementação simplificada - em produção usar iText ou similar
        StringBuilder sb = new StringBuilder();
        sb.append("RELATÓRIO FINANCEIRO\n");
        sb.append("=".repeat(50)).append("\n\n");
        sb.append(String.format("Período: %s a %s\n\n",
            relatorio.dataInicio().format(DATE_FORMATTER),
            relatorio.dataFim().format(DATE_FORMATTER)));

        sb.append("RESUMO FINANCEIRO\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append(String.format("Receita Total: R$ %,.2f\n", relatorio.receitaTotal()));
        sb.append(String.format("MRR Atual: R$ %,.2f\n", relatorio.mrrAtual()));
        sb.append(String.format("ARR Atual: R$ %,.2f\n", relatorio.arrAtual()));
        sb.append(String.format("Ticket Médio: R$ %,.2f\n", relatorio.ticketMedio()));
        sb.append(String.format("Variação vs Período Anterior: %,.2f%%\n\n", relatorio.variacaoPercentual()));

        sb.append("FATURAS\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append(String.format("Total: %d\n", relatorio.totalFaturas()));
        sb.append(String.format("Pagas: %d (R$ %,.2f)\n", relatorio.faturasPagas(), relatorio.valorFaturasPagas()));
        sb.append(String.format("Pendentes: %d (R$ %,.2f)\n", relatorio.faturasPendentes(), relatorio.valorFaturasPendentes()));
        sb.append(String.format("Vencidas: %d (R$ %,.2f)\n", relatorio.faturasVencidas(), relatorio.valorFaturasVencidas()));
        sb.append(String.format("Canceladas: %d\n\n", relatorio.faturasCanceladas()));

        sb.append("INADIMPLÊNCIA\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append(String.format("Oficinas Inadimplentes: %d\n", relatorio.oficinasInadimplentes()));
        sb.append(String.format("Valor Total: R$ %,.2f\n", relatorio.valorInadimplente()));
        sb.append(String.format("Taxa de Inadimplência: %,.2f%%\n", relatorio.taxaInadimplencia()));

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportarFinanceiroExcel(RelatorioFinanceiroDTO relatorio) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Sheet de resumo
            Sheet resumoSheet = workbook.createSheet("Resumo");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            int rowNum = 0;

            // Título
            Row titleRow = resumoSheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("RELATÓRIO FINANCEIRO");

            rowNum++; // Linha em branco

            // Período
            Row periodoRow = resumoSheet.createRow(rowNum++);
            periodoRow.createCell(0).setCellValue("Período:");
            periodoRow.createCell(1).setCellValue(
                relatorio.dataInicio().format(DATE_FORMATTER) + " a " + relatorio.dataFim().format(DATE_FORMATTER));

            rowNum++; // Linha em branco

            // Headers
            Row headerRow = resumoSheet.createRow(rowNum++);
            String[] headers = {"Métrica", "Valor"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Dados
            addFinanceiroRow(resumoSheet, rowNum++, "Receita Total", relatorio.receitaTotal(), currencyStyle);
            addFinanceiroRow(resumoSheet, rowNum++, "Receita Mensal", relatorio.receitaMensal(), currencyStyle);
            addFinanceiroRow(resumoSheet, rowNum++, "MRR Atual", relatorio.mrrAtual(), currencyStyle);
            addFinanceiroRow(resumoSheet, rowNum++, "ARR Atual", relatorio.arrAtual(), currencyStyle);
            addFinanceiroRow(resumoSheet, rowNum++, "Ticket Médio", relatorio.ticketMedio(), currencyStyle);
            addFinanceiroRow(resumoSheet, rowNum++, "Valor Inadimplente", relatorio.valorInadimplente(), currencyStyle);

            // Auto-size columns
            for (int i = 0; i < 2; i++) {
                resumoSheet.autoSizeColumn(i);
            }

            // Sheet de receita por plano
            if (relatorio.receitaPorPlano() != null && !relatorio.receitaPorPlano().isEmpty()) {
                Sheet planosSheet = workbook.createSheet("Receita por Plano");
                rowNum = 0;

                Row planosHeader = planosSheet.createRow(rowNum++);
                String[] planosHeaders = {"Plano", "Qtd Oficinas", "Receita Total", "% Receita"};
                for (int i = 0; i < planosHeaders.length; i++) {
                    Cell cell = planosHeader.createCell(i);
                    cell.setCellValue(planosHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }

                for (RelatorioFinanceiroDTO.ReceitaPorPlano plano : relatorio.receitaPorPlano()) {
                    Row row = planosSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(plano.planoNome());
                    row.createCell(1).setCellValue(plano.quantidadeOficinas());
                    Cell valorCell = row.createCell(2);
                    valorCell.setCellValue(plano.receitaTotal().doubleValue());
                    valorCell.setCellStyle(currencyStyle);
                    row.createCell(3).setCellValue(plano.percentualReceita().doubleValue() + "%");
                }

                for (int i = 0; i < 4; i++) {
                    planosSheet.autoSizeColumn(i);
                }
            }

            // Converter para bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar Excel do relatório financeiro", e);
            throw new RuntimeException("Erro ao gerar Excel", e);
        }
    }

    private byte[] exportarFinanceiroCSV(RelatorioFinanceiroDTO relatorio) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Header
        pw.println("Métrica,Valor");

        // Dados
        pw.printf("Receita Total,%.2f%n", relatorio.receitaTotal());
        pw.printf("Receita Mensal,%.2f%n", relatorio.receitaMensal());
        pw.printf("MRR Atual,%.2f%n", relatorio.mrrAtual());
        pw.printf("ARR Atual,%.2f%n", relatorio.arrAtual());
        pw.printf("Ticket Médio,%.2f%n", relatorio.ticketMedio());
        pw.printf("Total Faturas,%d%n", relatorio.totalFaturas());
        pw.printf("Faturas Pagas,%d%n", relatorio.faturasPagas());
        pw.printf("Faturas Pendentes,%d%n", relatorio.faturasPendentes());
        pw.printf("Faturas Vencidas,%d%n", relatorio.faturasVencidas());
        pw.printf("Valor Inadimplente,%.2f%n", relatorio.valorInadimplente());
        pw.printf("Taxa Inadimplência,%.2f%n", relatorio.taxaInadimplencia());

        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportarFinanceiroJSON(RelatorioFinanceiroDTO relatorio) {
        // Em produção, usar ObjectMapper do Jackson
        return "{}".getBytes(StandardCharsets.UTF_8);
    }

    // ========== RELATÓRIO OPERACIONAL ==========

    public byte[] exportarRelatorioOperacional(RelatorioOperacionalDTO relatorio, RelatorioRequest.FormatoExport formato) {
        return switch (formato) {
            case PDF -> exportarOperacionalPDF(relatorio);
            case EXCEL -> exportarOperacionalExcel(relatorio);
            case CSV -> exportarOperacionalCSV(relatorio);
            case JSON -> exportarOperacionalJSON(relatorio);
        };
    }

    private byte[] exportarOperacionalPDF(RelatorioOperacionalDTO relatorio) {
        StringBuilder sb = new StringBuilder();
        sb.append("RELATÓRIO OPERACIONAL\n");
        sb.append("=".repeat(50)).append("\n\n");
        sb.append(String.format("Período: %s a %s\n\n",
            relatorio.dataInicio().format(DATE_FORMATTER),
            relatorio.dataFim().format(DATE_FORMATTER)));

        sb.append("OFICINAS\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append(String.format("Total: %d\n", relatorio.totalOficinas()));
        sb.append(String.format("Ativas: %d\n", relatorio.oficinasAtivas()));
        sb.append(String.format("Em Trial: %d\n", relatorio.oficinasEmTrial()));
        sb.append(String.format("Suspensas: %d\n", relatorio.oficinasSuspensas()));
        sb.append(String.format("Canceladas: %d\n\n", relatorio.oficinasCanceladas()));

        sb.append("USUÁRIOS\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append(String.format("Total: %d\n", relatorio.totalUsuarios()));
        sb.append(String.format("Ativos: %d\n", relatorio.usuariosAtivos()));

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportarOperacionalExcel(RelatorioOperacionalDTO relatorio) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Operacional");

            CellStyle headerStyle = createHeaderStyle(workbook);

            int rowNum = 0;

            // Título
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.createCell(0).setCellValue("RELATÓRIO OPERACIONAL");

            rowNum++;

            // Headers
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Métrica", "Valor"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Dados
            addDataRow(sheet, rowNum++, "Total Oficinas", relatorio.totalOficinas());
            addDataRow(sheet, rowNum++, "Oficinas Ativas", relatorio.oficinasAtivas());
            addDataRow(sheet, rowNum++, "Oficinas Trial", relatorio.oficinasEmTrial());
            addDataRow(sheet, rowNum++, "Oficinas Suspensas", relatorio.oficinasSuspensas());
            addDataRow(sheet, rowNum++, "Oficinas Canceladas", relatorio.oficinasCanceladas());
            addDataRow(sheet, rowNum++, "Total Usuários", relatorio.totalUsuarios());
            addDataRow(sheet, rowNum++, "Usuários Ativos", relatorio.usuariosAtivos());

            for (int i = 0; i < 2; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar Excel do relatório operacional", e);
            throw new RuntimeException("Erro ao gerar Excel", e);
        }
    }

    private byte[] exportarOperacionalCSV(RelatorioOperacionalDTO relatorio) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("Métrica,Valor");
        pw.printf("Total Oficinas,%d%n", relatorio.totalOficinas());
        pw.printf("Oficinas Ativas,%d%n", relatorio.oficinasAtivas());
        pw.printf("Oficinas Trial,%d%n", relatorio.oficinasEmTrial());
        pw.printf("Oficinas Suspensas,%d%n", relatorio.oficinasSuspensas());
        pw.printf("Oficinas Canceladas,%d%n", relatorio.oficinasCanceladas());
        pw.printf("Total Usuários,%d%n", relatorio.totalUsuarios());
        pw.printf("Usuários Ativos,%d%n", relatorio.usuariosAtivos());

        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportarOperacionalJSON(RelatorioOperacionalDTO relatorio) {
        return "{}".getBytes(StandardCharsets.UTF_8);
    }

    // ========== RELATÓRIO CRESCIMENTO ==========

    public byte[] exportarRelatorioCrescimento(RelatorioCrescimentoDTO relatorio, RelatorioRequest.FormatoExport formato) {
        return switch (formato) {
            case PDF -> exportarCrescimentoPDF(relatorio);
            case EXCEL -> exportarCrescimentoExcel(relatorio);
            case CSV -> exportarCrescimentoCSV(relatorio);
            case JSON -> exportarCrescimentoJSON(relatorio);
        };
    }

    private byte[] exportarCrescimentoPDF(RelatorioCrescimentoDTO relatorio) {
        StringBuilder sb = new StringBuilder();
        sb.append("RELATÓRIO DE CRESCIMENTO\n");
        sb.append("=".repeat(50)).append("\n\n");
        sb.append(String.format("Período: %s a %s\n\n",
            relatorio.dataInicio().format(DATE_FORMATTER),
            relatorio.dataFim().format(DATE_FORMATTER)));

        sb.append("CRESCIMENTO\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append(String.format("Novas Oficinas: %d\n", relatorio.novasOficinas()));
        sb.append(String.format("Cancelamentos: %d\n", relatorio.cancelamentos()));
        sb.append(String.format("Crescimento Líquido: %d\n", relatorio.crescimentoLiquido()));
        sb.append(String.format("Taxa de Crescimento: %,.2f%%\n\n", relatorio.taxaCrescimento()));

        sb.append("CHURN\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append(String.format("Churn Rate: %,.2f%%\n", relatorio.churnRate()));
        sb.append(String.format("Churn MRR: R$ %,.2f\n\n", relatorio.churnMRR()));

        sb.append("TRIAL\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append(String.format("Trials Iniciados: %d\n", relatorio.trialsIniciados()));
        sb.append(String.format("Trials Convertidos: %d\n", relatorio.trialsConvertidos()));
        sb.append(String.format("Taxa de Conversão: %,.2f%%\n", relatorio.taxaConversaoTrial()));

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportarCrescimentoExcel(RelatorioCrescimentoDTO relatorio) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Crescimento");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            int rowNum = 0;

            Row titleRow = sheet.createRow(rowNum++);
            titleRow.createCell(0).setCellValue("RELATÓRIO DE CRESCIMENTO");

            rowNum++;

            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Métrica", "Valor"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            addDataRow(sheet, rowNum++, "Novas Oficinas", relatorio.novasOficinas());
            addDataRow(sheet, rowNum++, "Cancelamentos", relatorio.cancelamentos());
            addDataRow(sheet, rowNum++, "Crescimento Líquido", relatorio.crescimentoLiquido());
            addDataRow(sheet, rowNum++, "Taxa de Crescimento (%)", relatorio.taxaCrescimento().doubleValue());
            addDataRow(sheet, rowNum++, "Churn Rate (%)", relatorio.churnRate().doubleValue());
            addFinanceiroRow(sheet, rowNum++, "LTV", relatorio.ltv(), currencyStyle);
            addFinanceiroRow(sheet, rowNum++, "CAC", relatorio.cac(), currencyStyle);
            addDataRow(sheet, rowNum++, "LTV/CAC Ratio", relatorio.ltvCacRatio().doubleValue());
            addDataRow(sheet, rowNum++, "Trials Iniciados", relatorio.trialsIniciados());
            addDataRow(sheet, rowNum++, "Trials Convertidos", relatorio.trialsConvertidos());
            addDataRow(sheet, rowNum++, "Taxa Conversão Trial (%)", relatorio.taxaConversaoTrial().doubleValue());

            for (int i = 0; i < 2; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar Excel do relatório de crescimento", e);
            throw new RuntimeException("Erro ao gerar Excel", e);
        }
    }

    private byte[] exportarCrescimentoCSV(RelatorioCrescimentoDTO relatorio) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("Métrica,Valor");
        pw.printf("Novas Oficinas,%d%n", relatorio.novasOficinas());
        pw.printf("Cancelamentos,%d%n", relatorio.cancelamentos());
        pw.printf("Crescimento Líquido,%d%n", relatorio.crescimentoLiquido());
        pw.printf("Taxa Crescimento,%.2f%n", relatorio.taxaCrescimento());
        pw.printf("Churn Rate,%.2f%n", relatorio.churnRate());
        pw.printf("LTV,%.2f%n", relatorio.ltv());
        pw.printf("CAC,%.2f%n", relatorio.cac());
        pw.printf("LTV/CAC Ratio,%.2f%n", relatorio.ltvCacRatio());
        pw.printf("Trials Iniciados,%d%n", relatorio.trialsIniciados());
        pw.printf("Trials Convertidos,%d%n", relatorio.trialsConvertidos());
        pw.printf("Taxa Conversão Trial,%.2f%n", relatorio.taxaConversaoTrial());

        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportarCrescimentoJSON(RelatorioCrescimentoDTO relatorio) {
        return "{}".getBytes(StandardCharsets.UTF_8);
    }

    // ========== Métodos auxiliares ==========

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("R$ #,##0.00"));
        return style;
    }

    private void addFinanceiroRow(Sheet sheet, int rowNum, String metrica, BigDecimal valor, CellStyle currencyStyle) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(metrica);
        Cell valorCell = row.createCell(1);
        valorCell.setCellValue(valor != null ? valor.doubleValue() : 0);
        valorCell.setCellStyle(currencyStyle);
    }

    private void addDataRow(Sheet sheet, int rowNum, String metrica, Number valor) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(metrica);
        row.createCell(1).setCellValue(valor != null ? valor.doubleValue() : 0);
    }
}
