package com.pitstop.anexo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando a quota de storage é excedida.
 */
@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
public class QuotaExceededException extends RuntimeException {

    private final long usadoBytes;
    private final long limiteBytes;
    private final long arquivoBytes;

    public QuotaExceededException(long usadoBytes, long limiteBytes, long arquivoBytes) {
        super(String.format(
                "Quota de armazenamento excedida. Usado: %s, Limite: %s, Arquivo: %s",
                formatarBytes(usadoBytes),
                formatarBytes(limiteBytes),
                formatarBytes(arquivoBytes)
        ));
        this.usadoBytes = usadoBytes;
        this.limiteBytes = limiteBytes;
        this.arquivoBytes = arquivoBytes;
    }

    public long getUsadoBytes() {
        return usadoBytes;
    }

    public long getLimiteBytes() {
        return limiteBytes;
    }

    public long getArquivoBytes() {
        return arquivoBytes;
    }

    private static String formatarBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
