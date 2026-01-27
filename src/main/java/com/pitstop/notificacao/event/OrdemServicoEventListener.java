package com.pitstop.notificacao.event;

import com.pitstop.notificacao.service.NotificacaoOrchestrator;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener para eventos de Ordem de Servico.
 * Processa eventos e dispara notificacoes de forma assincrona.
 *
 * Usa @TransactionalEventListener para garantir que o evento seja processado
 * APOS o commit da transacao que criou/alterou a OS.
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
     * Executa APOS o commit da transacao para garantir que os dados estejam persistidos.
     *
     * @param event Evento de OS
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
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
