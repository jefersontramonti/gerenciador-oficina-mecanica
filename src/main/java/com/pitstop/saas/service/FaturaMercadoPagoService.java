package com.pitstop.saas.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.*;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.pitstop.saas.domain.ConfiguracaoGateway;
import com.pitstop.saas.domain.Fatura;
import com.pitstop.saas.domain.TipoGateway;
import com.pitstop.saas.dto.IniciarPagamentoFaturaDTO;
import com.pitstop.saas.repository.SaasConfigGatewayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for integrating Mercado Pago with SaaS invoices.
 * Credentials are loaded from database (configured by SUPER_ADMIN).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FaturaMercadoPagoService {

    private final SaasConfigGatewayRepository configuracaoGatewayRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Check if Mercado Pago is configured and active.
     */
    public boolean isConfigured() {
        return getConfiguracaoAtiva().isPresent();
    }

    /**
     * Get active Mercado Pago configuration from database.
     */
    private Optional<ConfiguracaoGateway> getConfiguracaoAtiva() {
        return configuracaoGatewayRepository.findByTipoAndAtivoTrue(TipoGateway.MERCADO_PAGO)
            .filter(ConfiguracaoGateway::isConfigurado);
    }

    /**
     * Create payment based on method selected.
     * PIX: Creates a direct payment with PIX QR code
     * CHECKOUT: Creates a preference for card/boleto checkout
     */
    public IniciarPagamentoFaturaDTO criarPreferencia(
        Fatura fatura,
        String emailPagador,
        String metodoPagamento,
        String successUrl,
        String failureUrl,
        String pendingUrl
    ) throws MPException, MPApiException {

        // Get configuration from database
        Optional<ConfiguracaoGateway> configOpt = getConfiguracaoAtiva();

        if (configOpt.isEmpty()) {
            log.warn("Mercado Pago not configured, returning error");
            return IniciarPagamentoFaturaDTO.erro(
                fatura.getId().toString(),
                "Pagamento online não disponível. O administrador precisa configurar o gateway de pagamento."
            );
        }

        ConfiguracaoGateway config = configOpt.get();

        // Configure Mercado Pago SDK with credentials from database
        MercadoPagoConfig.setAccessToken(config.getAccessToken());

        // Route to appropriate method based on payment type
        if ("PIX".equalsIgnoreCase(metodoPagamento)) {
            return criarPagamentoPix(fatura, emailPagador, config);
        } else {
            return criarPreferenciaCheckout(fatura, emailPagador, successUrl, failureUrl, pendingUrl);
        }
    }

    /**
     * Create PIX payment directly (generates QR code immediately).
     */
    private IniciarPagamentoFaturaDTO criarPagamentoPix(
        Fatura fatura,
        String emailPagador,
        ConfiguracaoGateway config
    ) throws MPException, MPApiException {

        PaymentClient client = new PaymentClient();

        // PIX expiration (24 hours)
        OffsetDateTime expiration = OffsetDateTime.now(ZoneOffset.UTC).plusHours(24);

        // External reference for webhook identification
        String externalReference = "FAT-" + fatura.getId().toString();

        // Notification URL for webhooks
        String notificationUrl = null;
        if (baseUrl != null && !baseUrl.contains("localhost") && !baseUrl.contains("127.0.0.1")) {
            notificationUrl = baseUrl + "/api/webhooks/mercadopago/fatura";
        }

        // Build PIX payment request
        PaymentCreateRequest.PaymentCreateRequestBuilder paymentBuilder = PaymentCreateRequest.builder()
            .transactionAmount(fatura.getValorTotal())
            .description("Fatura PitStop - " + fatura.getMesReferenciaFormatado())
            .paymentMethodId("pix")
            .payer(PaymentPayerRequest.builder()
                .email(emailPagador)
                .build())
            .externalReference(externalReference)
            .dateOfExpiration(expiration);

        if (notificationUrl != null) {
            paymentBuilder.notificationUrl(notificationUrl);
        }

        PaymentCreateRequest paymentRequest = paymentBuilder.build();

        // Create payment
        Payment payment = client.create(paymentRequest);

        log.info("Mercado Pago PIX payment created for invoice {}: Payment ID {}",
            fatura.getNumero(), payment.getId());

        // Extract PIX QR code from the response
        String qrCode = null;
        String qrCodeBase64 = null;

        if (payment.getPointOfInteraction() != null &&
            payment.getPointOfInteraction().getTransactionData() != null) {

            var transactionData = payment.getPointOfInteraction().getTransactionData();
            qrCode = transactionData.getQrCode();
            qrCodeBase64 = transactionData.getQrCodeBase64();
        }

        return IniciarPagamentoFaturaDTO.sucesso(
            fatura.getId().toString(),
            fatura.getNumero(),
            fatura.getValorTotal(),
            payment.getId().toString(),
            null, // No initPoint for PIX
            null,
            qrCodeBase64, // QR code image (base64)
            qrCode, // QR code text (copia e cola)
            expiration.toString()
        );
    }

    /**
     * Create checkout preference for card/boleto.
     */
    private IniciarPagamentoFaturaDTO criarPreferenciaCheckout(
        Fatura fatura,
        String emailPagador,
        String successUrl,
        String failureUrl,
        String pendingUrl
    ) throws MPException, MPApiException {

        PreferenceClient client = new PreferenceClient();

        // Create item
        PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
            .id(fatura.getId().toString())
            .title("Fatura PitStop - " + fatura.getMesReferenciaFormatado())
            .description("Fatura " + fatura.getNumero() + " - Plano " + fatura.getPlanoCodigo())
            .quantity(1)
            .currencyId("BRL")
            .unitPrice(fatura.getValorTotal())
            .build();

        List<PreferenceItemRequest> items = new ArrayList<>();
        items.add(itemRequest);

        // Create payer
        PreferencePayerRequest payer = PreferencePayerRequest.builder()
            .email(emailPagador)
            .build();

        // Create back URLs
        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
            .success(successUrl)
            .failure(failureUrl)
            .pending(pendingUrl)
            .build();

        // Configure payment methods - allow all methods (card, boleto, pix)
        // Don't set defaultPaymentMethodId to allow user choice
        PreferencePaymentMethodsRequest paymentMethods = PreferencePaymentMethodsRequest.builder()
            .installments(12) // Allow up to 12 installments for cards
            .build();

        // Expiration (48 hours for checkout)
        OffsetDateTime expiration = OffsetDateTime.now().plusHours(48);

        // External reference for webhook identification
        String externalReference = "FAT-" + fatura.getId().toString();

        // Notification URL for webhooks
        String notificationUrl = null;
        if (baseUrl != null && !baseUrl.contains("localhost") && !baseUrl.contains("127.0.0.1")) {
            notificationUrl = baseUrl + "/api/webhooks/mercadopago/fatura";
            log.debug("Notification URL set to: {}", notificationUrl);
        } else {
            log.warn("Notification URL not set - localhost not supported by Mercado Pago webhooks");
        }

        // Build preference
        PreferenceRequest.PreferenceRequestBuilder preferenceBuilder = PreferenceRequest.builder()
            .items(items)
            .payer(payer)
            .backUrls(backUrls)
            .paymentMethods(paymentMethods)
            .externalReference(externalReference)
            .expires(true)
            .expirationDateTo(expiration)
            .statementDescriptor("PITSTOP");

        // Only add notification URL if it's a public URL
        if (notificationUrl != null) {
            preferenceBuilder.notificationUrl(notificationUrl);
        }

        PreferenceRequest preferenceRequest = preferenceBuilder.build();

        // Create preference
        Preference preference = client.create(preferenceRequest);

        log.info("Mercado Pago checkout preference created for invoice {}: {}",
            fatura.getNumero(), preference.getId());

        return IniciarPagamentoFaturaDTO.sucesso(
            fatura.getId().toString(),
            fatura.getNumero(),
            fatura.getValorTotal(),
            preference.getId(),
            preference.getInitPoint(),
            preference.getSandboxInitPoint(),
            null, // No PIX QR code for checkout
            null,
            expiration.toString()
        );
    }

    /**
     * Validate webhook signature.
     */
    public boolean validarAssinaturaWebhook(String xSignature, String xRequestId, String dataId) {
        Optional<ConfiguracaoGateway> configOpt = getConfiguracaoAtiva();

        if (configOpt.isEmpty() || configOpt.get().getWebhookSecret() == null ||
            configOpt.get().getWebhookSecret().isBlank()) {
            log.warn("Webhook secret not configured, skipping signature validation");
            return true;
        }

        // TODO: Implement proper signature validation
        // For now, accept all webhooks
        return true;
    }
}
