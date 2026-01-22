package com.pitstop.saas.dto;

import java.math.BigDecimal;

/**
 * DTO returned when a workshop initiates payment for an invoice.
 */
public record IniciarPagamentoFaturaDTO(
    String faturaId,
    String faturaNumero,
    BigDecimal valor,

    // Mercado Pago
    String preferenceId,
    String initPoint,        // URL for checkout
    String sandboxInitPoint, // URL for sandbox checkout

    // PIX
    String pixQrCode,        // QR code image base64
    String pixQrCodeText,    // PIX copy-paste code
    String pixExpirationDate,

    // Status
    String status,
    String message
) {
    public static IniciarPagamentoFaturaDTO sucesso(
        String faturaId,
        String faturaNumero,
        BigDecimal valor,
        String preferenceId,
        String initPoint,
        String sandboxInitPoint,
        String pixQrCode,
        String pixQrCodeText,
        String pixExpirationDate
    ) {
        return new IniciarPagamentoFaturaDTO(
            faturaId,
            faturaNumero,
            valor,
            preferenceId,
            initPoint,
            sandboxInitPoint,
            pixQrCode,
            pixQrCodeText,
            pixExpirationDate,
            "CREATED",
            "Pagamento iniciado com sucesso"
        );
    }

    public static IniciarPagamentoFaturaDTO erro(String faturaId, String message) {
        return new IniciarPagamentoFaturaDTO(
            faturaId,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "ERROR",
            message
        );
    }
}
