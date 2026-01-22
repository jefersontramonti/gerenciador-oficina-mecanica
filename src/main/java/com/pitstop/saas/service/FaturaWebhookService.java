package com.pitstop.saas.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.pitstop.financeiro.domain.CategoriaDespesa;
import com.pitstop.financeiro.domain.Despesa;
import com.pitstop.financeiro.domain.StatusDespesa;
import com.pitstop.financeiro.domain.TipoPagamento;
import com.pitstop.financeiro.repository.DespesaRepository;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.StatusOficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.domain.ConfiguracaoGateway;
import com.pitstop.saas.domain.Fatura;
import com.pitstop.saas.domain.StatusFatura;
import com.pitstop.saas.domain.TipoGateway;
import com.pitstop.saas.repository.FaturaRepository;
import com.pitstop.saas.repository.SaasConfigGatewayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for processing Mercado Pago webhooks for SaaS invoices.
 * Handles payment confirmations and updates invoice status.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FaturaWebhookService {

    private final FaturaRepository faturaRepository;
    private final SaasConfigGatewayRepository configuracaoGatewayRepository;
    private final OficinaRepository oficinaRepository;
    private final DespesaRepository despesaRepository;

    /**
     * Process webhook notification from Mercado Pago.
     *
     * @param topic   The notification topic (payment, merchant_order, etc.)
     * @param id      The resource ID
     * @param payload The full payload from the webhook
     */
    @Transactional
    public void processarWebhook(String topic, String id, Map<String, Object> payload) {
        log.info("Processing fatura webhook - topic: {}, id: {}", topic, id);

        if (!"payment".equals(topic) && !"merchant_order".equals(topic)) {
            log.info("Webhook ignored - topic not payment or merchant_order: {}", topic);
            return;
        }

        if ("payment".equals(topic)) {
            processarPagamento(id);
        }
    }

    /**
     * Process a payment notification.
     * Queries Mercado Pago API to get payment details and updates the invoice.
     *
     * @param paymentId The Mercado Pago payment ID
     */
    @Transactional
    public void processarPagamento(String paymentId) {
        log.info("Processing payment for fatura: {}", paymentId);

        // Get active gateway configuration
        Optional<ConfiguracaoGateway> configOpt = configuracaoGatewayRepository
            .findByTipoAndAtivoTrue(TipoGateway.MERCADO_PAGO)
            .filter(ConfiguracaoGateway::isConfigurado);

        if (configOpt.isEmpty()) {
            log.warn("No active Mercado Pago configuration found for processing webhook");
            return;
        }

        ConfiguracaoGateway config = configOpt.get();

        try {
            // Configure Mercado Pago SDK
            MercadoPagoConfig.setAccessToken(config.getAccessToken());

            // Get payment details from Mercado Pago
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(Long.parseLong(paymentId));

            if (payment == null) {
                log.warn("Payment not found in Mercado Pago: {}", paymentId);
                return;
            }

            log.info("Payment details - Status: {}, ExternalRef: {}",
                payment.getStatus(), payment.getExternalReference());

            // External reference format: FAT-{faturaId}
            String externalRef = payment.getExternalReference();
            if (externalRef == null || !externalRef.startsWith("FAT-")) {
                log.warn("Invalid external reference for fatura webhook: {}", externalRef);
                return;
            }

            String faturaIdStr = externalRef.substring(4); // Remove "FAT-" prefix
            UUID faturaId;
            try {
                faturaId = UUID.fromString(faturaIdStr);
            } catch (IllegalArgumentException e) {
                log.error("Invalid fatura ID in external reference: {}", faturaIdStr);
                return;
            }

            // Find the invoice
            Optional<Fatura> faturaOpt = faturaRepository.findById(faturaId);
            if (faturaOpt.isEmpty()) {
                log.warn("Fatura not found: {}", faturaId);
                return;
            }

            Fatura fatura = faturaOpt.get();

            // Update invoice based on payment status
            atualizarFatura(fatura, payment);

        } catch (Exception e) {
            log.error("Error processing payment webhook: {}", e.getMessage(), e);
        }
    }

    /**
     * Update invoice based on Mercado Pago payment status.
     *
     * @param fatura  The invoice to update
     * @param payment The Mercado Pago payment
     */
    private void atualizarFatura(Fatura fatura, Payment payment) {
        log.info("Updating fatura {} with payment status: {}", fatura.getId(), payment.getStatus());

        String status = payment.getStatus();

        switch (status.toLowerCase()) {
            case "approved" -> {
                // Payment approved - mark invoice as paid
                if (fatura.getStatus() != StatusFatura.PAGO) {
                    String metodoPagamento = mapearFormaPagamento(payment.getPaymentMethodId());

                    // Use the entity's business method to mark as paid
                    fatura.marcarComoPago(metodoPagamento, payment.getId().toString());

                    faturaRepository.save(fatura);
                    log.info("Fatura {} marked as PAGO - TransacaoId: {}", fatura.getId(), payment.getId());

                    // Extend workshop subscription by 30 days
                    extenderAssinaturaOficina(fatura);

                    // Register as expense in the workshop's financial module
                    registrarDespesaOficina(fatura, payment.getPaymentMethodId());

                    // TODO: Send confirmation notification to workshop
                }
            }
            case "pending", "in_process", "authorized" -> {
                // Payment pending - update link if available
                log.info("Fatura {} payment pending/in_process", fatura.getId());
            }
            case "rejected", "cancelled" -> {
                // Payment rejected/cancelled - log but don't change invoice status
                // Invoice remains pending so user can try again
                log.warn("Fatura {} payment was rejected/cancelled: {}",
                    fatura.getId(), payment.getStatusDetail());
            }
            case "refunded", "charged_back" -> {
                // Refunded - mark invoice as pending again
                if (fatura.getStatus() == StatusFatura.PAGO) {
                    fatura.setStatus(StatusFatura.PENDENTE);
                    fatura.setDataPagamento(null);
                    fatura.setMetodoPagamento(null);
                    fatura.setTransacaoId(null);
                    faturaRepository.save(fatura);
                    log.warn("Fatura {} payment was refunded - reverted to PENDENTE", fatura.getId());
                }
            }
            default -> log.warn("Unknown payment status: {}", status);
        }
    }

    /**
     * Extend workshop subscription after successful payment.
     * Adds 30 days to the current expiration date (or from today if already expired).
     * Also reactivates the workshop if it was suspended due to non-payment.
     *
     * @param fatura The paid invoice
     */
    private void extenderAssinaturaOficina(Fatura fatura) {
        Oficina oficina = fatura.getOficina();
        if (oficina == null) {
            log.warn("Fatura {} has no associated oficina - cannot extend subscription", fatura.getId());
            return;
        }

        LocalDate hoje = LocalDate.now();
        LocalDate dataVencimentoAtual = oficina.getDataVencimentoPlano();

        // Calculate new expiration date:
        // - If current expiration is in the future: add 30 days to it
        // - If current expiration is in the past (or null): add 30 days from today
        LocalDate novaDataVencimento;
        if (dataVencimentoAtual != null && dataVencimentoAtual.isAfter(hoje)) {
            // Add 30 days to current expiration
            novaDataVencimento = dataVencimentoAtual.plusDays(30);
            log.info("Extending subscription from {} to {} (added 30 days to current expiration)",
                dataVencimentoAtual, novaDataVencimento);
        } else {
            // Add 30 days from today
            novaDataVencimento = hoje.plusDays(30);
            log.info("Setting new subscription expiration to {} (30 days from today)", novaDataVencimento);
        }

        oficina.setDataVencimentoPlano(novaDataVencimento);

        // Reactivate workshop if it was suspended due to non-payment
        if (oficina.getStatus() == StatusOficina.SUSPENSA) {
            oficina.setStatus(StatusOficina.ATIVA);
            log.info("Oficina {} reactivated after payment - was SUSPENSA, now ATIVA", oficina.getId());
        }

        oficinaRepository.save(oficina);
        log.info("Oficina {} subscription extended to {} after payment of fatura {}",
            oficina.getId(), novaDataVencimento, fatura.getNumero());
    }

    /**
     * Register the SaaS payment as an expense in the workshop's financial module.
     * This allows the workshop to track this cost in their DRE and cash flow.
     *
     * @param fatura The paid invoice
     * @param paymentMethodId The Mercado Pago payment method ID
     */
    private void registrarDespesaOficina(Fatura fatura, String paymentMethodId) {
        Oficina oficina = fatura.getOficina();
        if (oficina == null) {
            log.warn("Fatura {} has no associated oficina - cannot register expense", fatura.getId());
            return;
        }

        try {
            // Map payment method
            TipoPagamento tipoPagamento = mapearTipoPagamento(paymentMethodId);

            // Create expense
            Despesa despesa = Despesa.builder()
                .oficina(oficina)
                .categoria(CategoriaDespesa.SISTEMAS_SOFTWARE)
                .descricao("Assinatura PitStop - " + fatura.getMesReferenciaFormatado())
                .valor(fatura.getValorTotal())
                .dataVencimento(fatura.getDataVencimento())
                .dataPagamento(LocalDate.now())
                .status(StatusDespesa.PAGA)
                .numeroDocumento(fatura.getNumero())
                .fornecedor("PitStop - Sistema de Gestão")
                .observacoes("Pagamento automático via Mercado Pago - Fatura " + fatura.getNumero())
                .recorrente(true)
                .tipoPagamento(tipoPagamento)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            despesaRepository.save(despesa);

            log.info("Despesa registrada para oficina {} - Fatura: {}, Valor: {}",
                oficina.getId(), fatura.getNumero(), fatura.getValorTotal());

        } catch (Exception e) {
            // Don't fail the payment flow if expense registration fails
            log.error("Error registering expense for fatura {}: {}", fatura.getId(), e.getMessage(), e);
        }
    }

    /**
     * Map Mercado Pago payment method to TipoPagamento enum.
     */
    private TipoPagamento mapearTipoPagamento(String paymentMethodId) {
        if (paymentMethodId == null) return TipoPagamento.PIX;

        return switch (paymentMethodId.toLowerCase()) {
            case "pix" -> TipoPagamento.PIX;
            case "credit_card", "visa", "master", "amex", "elo", "hipercard" -> TipoPagamento.CARTAO_CREDITO;
            case "debit_card" -> TipoPagamento.CARTAO_DEBITO;
            case "bolbradesco", "boleto" -> TipoPagamento.BOLETO;
            case "account_money" -> TipoPagamento.TRANSFERENCIA;
            default -> TipoPagamento.PIX;
        };
    }

    /**
     * Map Mercado Pago payment method to a human-readable string.
     */
    private String mapearFormaPagamento(String paymentMethodId) {
        if (paymentMethodId == null) return "Mercado Pago";

        return switch (paymentMethodId.toLowerCase()) {
            case "pix" -> "PIX";
            case "credit_card", "visa", "master", "amex", "elo", "hipercard" -> "Cartao de Credito";
            case "debit_card" -> "Cartao de Debito";
            case "bolbradesco", "boleto" -> "Boleto";
            case "account_money" -> "Saldo Mercado Pago";
            default -> "Mercado Pago - " + paymentMethodId;
        };
    }
}
