package com.pitstop.notificacao.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico para armazenamento temporario de arquivos.
 *
 * <p>Permite hospedar arquivos (PDFs, imagens) temporariamente para envio
 * via WhatsApp, Telegram e outros canais que necessitam de URL publica.</p>
 *
 * <p>Arquivos sao automaticamente removidos apos 24 horas.</p>
 *
 * @author PitStop Team
 */
@Service
@Slf4j
public class TempFileService {

    private static final long EXPIRATION_HOURS = 24;
    private static final long EXPIRATION_MILLIS = EXPIRATION_HOURS * 60 * 60 * 1000;

    /**
     * Representa um arquivo temporario armazenado.
     */
    public record TempFile(
        String token,
        byte[] content,
        String fileName,
        String contentType,
        Instant createdAt,
        Instant expiresAt
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    // Armazenamento em memoria (em producao, considerar Redis ou S3)
    private final Map<String, TempFile> storage = new ConcurrentHashMap<>();

    /**
     * Armazena um arquivo temporariamente.
     *
     * @param content Conteudo do arquivo em bytes
     * @param fileName Nome original do arquivo
     * @param contentType Tipo MIME (ex: application/pdf)
     * @return Token unico para acesso ao arquivo
     */
    public String store(byte[] content, String fileName, String contentType) {
        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(EXPIRATION_MILLIS);

        TempFile tempFile = new TempFile(
            token,
            content,
            fileName,
            contentType,
            now,
            expiresAt
        );

        storage.put(token, tempFile);
        log.info("Arquivo temporario armazenado: {} ({} bytes, expira em {})",
            fileName, content.length, expiresAt);

        return token;
    }

    /**
     * Armazena um PDF temporariamente.
     *
     * @param pdfContent Conteudo do PDF em bytes
     * @param fileName Nome do arquivo (sem extensao)
     * @return Token unico para acesso ao arquivo
     */
    public String storePdf(byte[] pdfContent, String fileName) {
        String fullFileName = fileName.endsWith(".pdf") ? fileName : fileName + ".pdf";
        return store(pdfContent, fullFileName, "application/pdf");
    }

    /**
     * Recupera um arquivo pelo token.
     *
     * @param token Token do arquivo
     * @return Arquivo ou null se nao encontrado/expirado
     */
    public TempFile retrieve(String token) {
        TempFile file = storage.get(token);

        if (file == null) {
            log.debug("Arquivo temporario nao encontrado: {}", token);
            return null;
        }

        if (file.isExpired()) {
            log.debug("Arquivo temporario expirado: {}", token);
            storage.remove(token);
            return null;
        }

        return file;
    }

    /**
     * Remove um arquivo do armazenamento.
     *
     * @param token Token do arquivo
     * @return true se removido, false se nao existia
     */
    public boolean delete(String token) {
        TempFile removed = storage.remove(token);
        if (removed != null) {
            log.debug("Arquivo temporario removido: {}", token);
            return true;
        }
        return false;
    }

    /**
     * Verifica se um arquivo existe e nao esta expirado.
     *
     * @param token Token do arquivo
     * @return true se existe e valido
     */
    public boolean exists(String token) {
        TempFile file = storage.get(token);
        return file != null && !file.isExpired();
    }

    /**
     * Retorna o numero de arquivos armazenados.
     */
    public int getStorageSize() {
        return storage.size();
    }

    /**
     * Limpa arquivos expirados.
     * Executado automaticamente a cada hora.
     */
    @Scheduled(fixedRate = 3600000) // 1 hora
    public void cleanupExpired() {
        int before = storage.size();

        storage.entrySet().removeIf(entry -> entry.getValue().isExpired());

        int removed = before - storage.size();
        if (removed > 0) {
            log.info("Limpeza de arquivos temporarios: {} removidos, {} restantes",
                removed, storage.size());
        }
    }
}
