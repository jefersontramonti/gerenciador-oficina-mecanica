package com.pitstop.financeiro.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO com informações do checkout criado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutResponseDTO {

    /**
     * ID do pagamento online no sistema.
     */
    private UUID pagamentoOnlineId;

    /**
     * ID da preferência no gateway.
     */
    private String preferenceId;

    /**
     * URL para redirecionar o cliente ao checkout.
     */
    private String urlCheckout;

    /**
     * URL do QR Code PIX (se disponível).
     */
    private String urlQrCode;

    /**
     * Código copia-cola do PIX (se disponível).
     */
    private String codigoPix;

    /**
     * Valor do pagamento.
     */
    private BigDecimal valor;

    /**
     * Data de expiração do checkout.
     */
    private LocalDateTime dataExpiracao;

    /**
     * Status inicial do pagamento.
     */
    private String status;

    /**
     * Gateway utilizado.
     */
    private String gateway;

    /**
     * Mensagem adicional.
     */
    private String mensagem;
}
