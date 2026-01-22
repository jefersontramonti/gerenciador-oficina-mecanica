package com.pitstop.webhook.controller;

import com.pitstop.shared.security.feature.RequiresFeature;
import com.pitstop.webhook.dto.*;
import com.pitstop.webhook.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller para gerenciamento de webhooks.
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/webhooks/config")
@RequiredArgsConstructor
@Tag(name = "Webhooks Config", description = "Gerenciamento de webhooks de saída")
@RequiresFeature(value = "WEBHOOK_NOTIFICATIONS", name = "Webhooks", requiredPlan = "PROFISSIONAL")
@PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
public class WebhookConfigController {

    private final WebhookService webhookService;

    // ========== CRUD de Configuração ==========

    @GetMapping
    @Operation(summary = "Lista webhooks da oficina")
    public ResponseEntity<Page<WebhookConfigDTO>> listar(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(webhookService.listar(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca webhook por ID")
    public ResponseEntity<WebhookConfigDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(webhookService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cria novo webhook")
    public ResponseEntity<WebhookConfigDTO> criar(@Valid @RequestBody WebhookConfigCreateDTO dto) {
        WebhookConfigDTO created = webhookService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza webhook")
    public ResponseEntity<WebhookConfigDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody WebhookConfigUpdateDTO dto) {
        return ResponseEntity.ok(webhookService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove webhook")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        webhookService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    @Operation(summary = "Reativa webhook desativado por falhas")
    public ResponseEntity<WebhookConfigDTO> reativar(@PathVariable UUID id) {
        return ResponseEntity.ok(webhookService.reativar(id));
    }

    // ========== Teste ==========

    @PostMapping("/testar")
    @Operation(summary = "Testa webhook com payload de exemplo")
    public ResponseEntity<WebhookTestResultDTO> testar(@Valid @RequestBody WebhookTestDTO dto) {
        return ResponseEntity.ok(webhookService.testar(dto));
    }

    // ========== Logs e Estatísticas ==========

    @GetMapping("/logs")
    @Operation(summary = "Lista logs de webhooks da oficina")
    public ResponseEntity<Page<WebhookLogDTO>> listarLogs(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(webhookService.listarLogs(pageable));
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "Lista logs de um webhook específico")
    public ResponseEntity<Page<WebhookLogDTO>> listarLogsPorWebhook(
            @PathVariable UUID id,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(webhookService.listarLogsPorWebhook(id, pageable));
    }

    @GetMapping("/stats")
    @Operation(summary = "Estatísticas de webhooks")
    public ResponseEntity<WebhookStatsDTO> getEstatisticas() {
        return ResponseEntity.ok(webhookService.getEstatisticas());
    }

    @GetMapping("/eventos")
    @Operation(summary = "Lista tipos de eventos disponíveis")
    public ResponseEntity<List<Map<String, String>>> listarEventos() {
        return ResponseEntity.ok(webhookService.listarEventos());
    }
}
