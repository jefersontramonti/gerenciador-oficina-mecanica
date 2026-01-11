package com.pitstop.financeiro.controller;

import com.pitstop.financeiro.service.MercadoPagoService;
import com.pitstop.shared.security.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Controller para receber webhooks de gateways de pagamento.
 * Endpoints públicos (sem autenticação) para receber notificações.
 *
 * <p><b>Security Features:</b></p>
 * <ul>
 *   <li>Signature validation for Mercado Pago webhooks</li>
 *   <li>Rate limiting to prevent abuse</li>
 *   <li>IP logging for audit trail</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Endpoints para receber notificações de gateways")
public class WebhookController {

    private final MercadoPagoService mercadoPagoService;
    private final RateLimitService rateLimitService;

    /**
     * Webhook secret for signature validation.
     * Set via environment variable: MERCADOPAGO_WEBHOOK_SECRET
     * Optional: if not set, signature validation is skipped (dev mode).
     */
    @Value("${mercadopago.webhook.secret:}")
    private String webhookSecret;

    /**
     * Webhook do Mercado Pago.
     * Recebe notificações de pagamentos.
     *
     * <p><b>Security Features:</b></p>
     * <ul>
     *   <li>Signature validation using X-Signature header (when webhook secret is configured)</li>
     *   <li>Rate limiting to prevent abuse</li>
     *   <li>IP logging for audit trail</li>
     * </ul>
     *
     * @param signature X-Signature header from Mercado Pago
     * @param requestId X-Request-Id header for idempotency
     * @param topic Topic from query param
     * @param id Resource ID from query param
     * @param type Type from query param
     * @param payload JSON body
     * @param request HTTP request for IP extraction
     * @return Acknowledgment response
     */
    @PostMapping("/mercadopago")
    @Operation(summary = "Receber webhook do Mercado Pago")
    public ResponseEntity<String> mercadoPagoWebhook(
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "type", required = false) String type,
            @RequestBody(required = false) Map<String, Object> payload,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);
        log.info("Webhook MP recebido - topic: {}, id: {}, type: {}, requestId: {}, IP: {}",
            topic, id, type, requestId, maskIp(clientIp));

        // Rate limiting check
        if (!rateLimitService.isWebhookAllowed(clientIp)) {
            log.warn("SECURITY: Rate limit exceeded for webhook - IP: {}", maskIp(clientIp));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body("RATE_LIMIT_EXCEEDED");
        }

        // Signature validation (if secret is configured)
        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            if (!validateSignature(signature, requestId, id, payload)) {
                log.warn("SECURITY: Invalid webhook signature - IP: {}, requestId: {}", maskIp(clientIp), requestId);
                // Return 200 to prevent retries, but don't process
                // This is a security measure - invalid signatures are silently ignored
                return ResponseEntity.ok("OK");
            }
            log.debug("Webhook signature validated successfully");
        } else {
            log.debug("Webhook signature validation skipped (no secret configured - dev mode)");
        }

        try {
            // MP pode enviar de duas formas:
            // 1. Query params: ?topic=payment&id=123
            // 2. Body JSON: { "type": "payment", "data": { "id": "123" } }

            String actualTopic = topic;
            String actualId = id;

            // Se veio no body
            if (payload != null) {
                if (type != null && actualTopic == null) {
                    actualTopic = type;
                }
                if (payload.containsKey("data") && payload.get("data") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) payload.get("data");
                    if (data.containsKey("id") && actualId == null) {
                        actualId = data.get("id").toString();
                    }
                }
                // Também pode vir como action
                if (payload.containsKey("action") && actualTopic == null) {
                    actualTopic = payload.get("action").toString();
                }
            }

            if (actualTopic != null && actualId != null) {
                mercadoPagoService.processarWebhook(actualTopic, actualId, payload);
                log.info("Webhook MP processado com sucesso - topic: {}, id: {}", actualTopic, actualId);
            } else {
                log.warn("Webhook MP com dados incompletos - topic: {}, id: {}", actualTopic, actualId);
            }

            // Mercado Pago espera status 200/201 para confirmar recebimento
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Erro ao processar webhook MP: {}", e.getMessage(), e);
            // Retornar 200 mesmo com erro para não retentar indefinidamente
            // O erro será logado e pode ser investigado
            return ResponseEntity.ok("PROCESSED_WITH_ERROR");
        }
    }

    /**
     * Validates the Mercado Pago webhook signature.
     *
     * <p>Mercado Pago uses HMAC-SHA256 to sign webhooks. The signature format is:</p>
     * <pre>ts=timestamp,v1=signature</pre>
     *
     * <p>The signature is computed over:</p>
     * <pre>id:{request_id};ts:{timestamp};{data}</pre>
     *
     * @param signature X-Signature header value
     * @param requestId X-Request-Id header value
     * @param id Resource ID from query param
     * @param payload Request body
     * @return true if signature is valid, false otherwise
     */
    private boolean validateSignature(String signature, String requestId, String id, Map<String, Object> payload) {
        if (signature == null || signature.isEmpty()) {
            log.debug("No signature provided");
            return false;
        }

        try {
            // Parse signature header: ts=timestamp,v1=signature
            String[] parts = signature.split(",");
            String timestamp = null;
            String hash = null;

            for (String part : parts) {
                if (part.startsWith("ts=")) {
                    timestamp = part.substring(3);
                } else if (part.startsWith("v1=")) {
                    hash = part.substring(3);
                }
            }

            if (timestamp == null || hash == null) {
                log.debug("Invalid signature format");
                return false;
            }

            // Build the manifest string
            // Format: id:{data.id};request-id:{x-request-id};ts:{ts};
            StringBuilder manifest = new StringBuilder();
            manifest.append("id:").append(id != null ? id : "");
            manifest.append(";request-id:").append(requestId != null ? requestId : "");
            manifest.append(";ts:").append(timestamp);
            manifest.append(";");

            // Compute HMAC-SHA256
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(manifest.toString().getBytes(StandardCharsets.UTF_8));
            String computedHash = bytesToHex(hmacBytes);

            // Compare signatures (constant time comparison)
            boolean valid = constantTimeEquals(hash, computedHash);

            if (!valid) {
                log.debug("Signature mismatch. Expected: {}, Got: {}", computedHash, hash);
            }

            return valid;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error validating signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Converts byte array to hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Constant time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Extracts the real client IP address from the request.
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {"X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP"};
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Masks IP address for logging.
     */
    private String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) return "unknown";
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            return parts.length >= 2 ? parts[0] + "." + parts[1] + ".xxx.xxx" : "xxx.xxx.xxx.xxx";
        }
        return "xxx.xxx.xxx.xxx";
    }

    /**
     * Health check para verificar se o endpoint está ativo.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check do webhook")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "message", "Webhook endpoint is active"
        ));
    }
}
