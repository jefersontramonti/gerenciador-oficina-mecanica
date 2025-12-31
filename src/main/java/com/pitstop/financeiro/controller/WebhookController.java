package com.pitstop.financeiro.controller;

import com.pitstop.financeiro.service.MercadoPagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para receber webhooks de gateways de pagamento.
 * Endpoints públicos (sem autenticação) para receber notificações.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Endpoints para receber notificações de gateways")
public class WebhookController {

    private final MercadoPagoService mercadoPagoService;

    /**
     * Webhook do Mercado Pago.
     * Recebe notificações de pagamentos.
     */
    @PostMapping("/mercadopago")
    @Operation(summary = "Receber webhook do Mercado Pago")
    public ResponseEntity<String> mercadoPagoWebhook(
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "type", required = false) String type,
            @RequestBody(required = false) Map<String, Object> payload) {

        log.info("Webhook MP recebido - topic: {}, id: {}, type: {}", topic, id, type);

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
