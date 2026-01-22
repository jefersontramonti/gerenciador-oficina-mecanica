package com.pitstop.financeiro.util;

import com.pitstop.financeiro.domain.TipoTransacaoBancaria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser simplificado para arquivos OFX (Open Financial Exchange).
 * Suporta os formatos SGML e XML.
 */
@Slf4j
@Component
public class OFXParser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_FORMAT_FULL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Resultado do parse do arquivo OFX.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OFXResult {
        private String bankId;
        private String accountId;
        private String accountType;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal balanceAmount;
        private List<OFXTransaction> transactions;
        private String fileHash;
    }

    /**
     * Transação do arquivo OFX.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OFXTransaction {
        private String fitId;
        private TipoTransacaoBancaria type;
        private LocalDate datePosted;
        private BigDecimal amount;
        private String name;
        private String memo;
        private String checkNum;
        private String refNum;
    }

    /**
     * Faz o parse de um arquivo OFX.
     */
    public OFXResult parse(InputStream inputStream) throws Exception {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String ofxContent = content.toString();

        // Calcular hash do arquivo
        String fileHash = calculateHash(ofxContent);

        OFXResult result = OFXResult.builder()
            .transactions(new ArrayList<>())
            .fileHash(fileHash)
            .build();

        // Extrair informações da conta
        result.setBankId(extractValue(ofxContent, "BANKID"));
        result.setAccountId(extractValue(ofxContent, "ACCTID"));
        result.setAccountType(extractValue(ofxContent, "ACCTTYPE"));

        // Extrair período
        String dtStart = extractValue(ofxContent, "DTSTART");
        String dtEnd = extractValue(ofxContent, "DTEND");
        if (dtStart != null) {
            result.setStartDate(parseDate(dtStart));
        }
        if (dtEnd != null) {
            result.setEndDate(parseDate(dtEnd));
        }

        // Extrair saldo
        String balance = extractValue(ofxContent, "BALAMT");
        if (balance != null) {
            result.setBalanceAmount(new BigDecimal(balance.replace(",", ".")));
        }

        // Extrair transações
        List<String> transactionBlocks = extractBlocks(ofxContent, "STMTTRN");

        for (String block : transactionBlocks) {
            OFXTransaction transaction = parseTransaction(block);
            if (transaction != null) {
                result.getTransactions().add(transaction);
            }
        }

        log.info("OFX parsed: {} transações encontradas", result.getTransactions().size());

        return result;
    }

    /**
     * Parse de uma transação individual.
     */
    private OFXTransaction parseTransaction(String block) {
        try {
            OFXTransaction tx = new OFXTransaction();

            // FITID - Identificador único
            tx.setFitId(extractValue(block, "FITID"));

            // Tipo de transação
            String trnType = extractValue(block, "TRNTYPE");
            if (trnType != null) {
                tx.setType(parseTransactionType(trnType));
            }

            // Data
            String dtPosted = extractValue(block, "DTPOSTED");
            if (dtPosted != null) {
                tx.setDatePosted(parseDate(dtPosted));
            }

            // Valor
            String amount = extractValue(block, "TRNAMT");
            if (amount != null) {
                tx.setAmount(new BigDecimal(amount.replace(",", ".")));
            }

            // Nome/Descrição
            tx.setName(extractValue(block, "NAME"));
            tx.setMemo(extractValue(block, "MEMO"));

            // Referências
            tx.setCheckNum(extractValue(block, "CHECKNUM"));
            tx.setRefNum(extractValue(block, "REFNUM"));

            return tx;
        } catch (Exception e) {
            log.warn("Erro ao parsear transação: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrai o valor de uma tag OFX.
     */
    private String extractValue(String content, String tagName) {
        // Pattern para formato SGML: <TAG>valor
        Pattern pattern1 = Pattern.compile("<" + tagName + ">([^<\\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(content);
        if (matcher1.find()) {
            return matcher1.group(1).trim();
        }

        // Pattern para formato XML: <TAG>valor</TAG>
        Pattern pattern2 = Pattern.compile("<" + tagName + ">([^<]+)</" + tagName + ">", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(content);
        if (matcher2.find()) {
            return matcher2.group(1).trim();
        }

        return null;
    }

    /**
     * Extrai blocos de uma tag.
     */
    private List<String> extractBlocks(String content, String tagName) {
        List<String> blocks = new ArrayList<>();

        // Pattern para formato SGML
        Pattern pattern = Pattern.compile("<" + tagName + ">(.*?)(?=<" + tagName + ">|</" + tagName + "LIST>|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            blocks.add(matcher.group(1));
        }

        return blocks;
    }

    /**
     * Parse de data OFX.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.length() < 8) {
            return null;
        }

        try {
            // Pegar apenas os primeiros 8 caracteres (YYYYMMDD)
            String datePart = dateStr.substring(0, 8);
            return LocalDate.parse(datePart, DATE_FORMAT);
        } catch (Exception e) {
            log.warn("Erro ao parsear data: {}", dateStr);
            return null;
        }
    }

    /**
     * Converte tipo de transação OFX para enum.
     */
    private TipoTransacaoBancaria parseTransactionType(String type) {
        if (type == null) return TipoTransacaoBancaria.CREDITO;

        return switch (type.toUpperCase()) {
            case "DEBIT", "CHECK", "PAYMENT", "FEE", "SRVCHG", "DEP", "ATM", "POS", "XFER", "OTHER" -> {
                // Verificar pelo valor se é débito ou crédito
                // O tipo real é determinado pelo sinal do valor
                yield TipoTransacaoBancaria.DEBITO;
            }
            case "CREDIT", "INT", "DIV", "DIRECTDEP" -> TipoTransacaoBancaria.CREDITO;
            default -> TipoTransacaoBancaria.CREDITO;
        };
    }

    /**
     * Calcula hash SHA-256 do conteúdo.
     */
    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Erro ao calcular hash", e);
            return String.valueOf(System.currentTimeMillis());
        }
    }
}
