package com.pitstop.notificacao.event;

import com.pitstop.notificacao.service.NotificacaoOrchestrator;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener para eventos de Ordem de Servico.
 * Processa eventos e dispara notificacoes de forma assincrona.
 *
 * @author PitStop Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrdemServicoEventListener {

    private final NotificacaoOrchestrator orchestrator;

    /**
     * Processa evento de Ordem de Servico e dispara notificacoes.
     *
     * @param event Evento de OS
     */
    @Async
    @EventListener
    public void handleOrdemServicoEvent(OrdemServicoEvent event) {
        log.info("Processando evento {} para OS #{}", event.getTipoEvento(), event.getNumeroOS());

        try {
            // Configura contexto de tenant
            TenantContext.setTenantId(event.getOficinaId());

            // Dispara notificacoes
            orchestrator.notificarEventoOS(
                event.getTipoEvento(),
                event.getEmailCliente(),
                event.getTelefoneCliente(),
                event.getNomeCliente(),
                event.toVariaveis(),
                event.getOrdemServicoId(),
                event.getClienteId()
            );

            log.info("Notificacoes disparadas para evento {} (OS #{})",
                event.getTipoEvento(), event.getNumeroOS());

        } catch (Exception e) {
            log.error("Erro ao processar evento {} para OS #{}: {}",
                event.getTipoEvento(), event.getNumeroOS(), e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
