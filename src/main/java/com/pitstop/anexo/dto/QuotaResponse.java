package com.pitstop.anexo.dto;

/**
 * DTO de resposta para informações de quota de storage.
 */
public record QuotaResponse(
        long usadoBytes,
        String usadoFormatado,
        long limiteBytes,
        String limiteFormatado,
        double percentualUsado,
        long totalAnexos
) {
    /**
     * Cria um QuotaResponse com valores calculados.
     */
    public static QuotaResponse of(long usadoBytes, long limiteBytes, long totalAnexos) {
        double percentual = limiteBytes > 0 ? (usadoBytes * 100.0 / limiteBytes) : 0;
        return new QuotaResponse(
                usadoBytes,
                formatarBytes(usadoBytes),
                limiteBytes,
                formatarBytes(limiteBytes),
                Math.round(percentual * 10) / 10.0,
                totalAnexos
        );
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
