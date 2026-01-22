package com.pitstop.webhook.event;

import com.pitstop.notificacao.event.OrdemServicoEvent;
import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.webhook.domain.TipoEventoWebhook;
import com.pitstop.webhook.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listener para eventos do sistema que dispara webhooks.
 *
 * @author PitStop Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookEventListener {

    private final WebhookService webhookService;

    /**
     * Processa eventos de Ordem de Serviço e dispara webhooks.
     */
    @Async
    @EventListener
    public void handleOrdemServicoEvent(OrdemServicoEvent event) {
        TipoEventoWebhook tipoWebhook = mapEventoNotificacaoToWebhook(event.getTipoEvento());

        if (tipoWebhook == null) {
            log.debug("Evento {} não mapeado para webhook", event.getTipoEvento());
            return;
        }

        log.debug("Disparando webhook para evento {} (OS #{})", tipoWebhook, event.getNumeroOS());

        try {
            TenantContext.setTenantId(event.getOficinaId());

            Map<String, Object> payload = Map.of(
                "ordemServico", Map.of(
                    "id", event.getOrdemServicoId().toString(),
                    "numero", event.getNumeroOS(),
                    "valorTotal", event.getValorTotal() != null ? event.getValorTotal().toString() : null
                ),
                "cliente", Map.of(
                    "id", event.getClienteId() != null ? event.getClienteId().toString() : null,
                    "nome", event.getNomeCliente(),
                    "email", event.getEmailCliente(),
                    "telefone", event.getTelefoneCliente()
                ),
                "veiculo", Map.of(
                    "placa", event.getVeiculoPlaca(),
                    "modelo", event.getVeiculoModelo()
                ),
                "oficina", Map.of(
                    "id", event.getOficinaId().toString(),
                    "nome", event.getNomeOficina()
                ),
                "dadosExtras", event.toVariaveis()
            );

            webhookService.dispararEvento(
                tipoWebhook,
                event.getOrdemServicoId(),
                "OrdemServico",
                payload
            );

        } catch (Exception e) {
            log.error("Erro ao disparar webhook para evento {}: {}", tipoWebhook, e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Mapeia EventoNotificacao para TipoEventoWebhook.
     */
    private TipoEventoWebhook mapEventoNotificacaoToWebhook(EventoNotificacao evento) {
        return switch (evento) {
            case OS_CRIADA -> TipoEventoWebhook.OS_CRIADA;
            case OS_APROVADA -> TipoEventoWebhook.OS_APROVADA;
            case OS_REJEITADA -> TipoEventoWebhook.OS_CANCELADA;
            case OS_EM_ANDAMENTO -> TipoEventoWebhook.OS_STATUS_ALTERADO;
            case OS_FINALIZADA -> TipoEventoWebhook.OS_FINALIZADA;
            case OS_ENTREGUE -> TipoEventoWebhook.OS_ENTREGUE;
            default -> null;
        };
    }
}
